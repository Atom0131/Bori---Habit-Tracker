package com.apagon.rhythm.ui.sync

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apagon.rhythm.data.sync.AutoSyncChoices
import com.apagon.rhythm.data.sync.AutoSyncRunner
import com.apagon.rhythm.data.sync.SyncCoordinator
import com.apagon.rhythm.data.sync.SyncDevice
import com.apagon.rhythm.data.sync.SyncPreferences
import com.apagon.rhythm.data.sync.SyncResult
import com.apagon.rhythm.data.sync.PhoneSyncOutcome
import com.apagon.rhythm.ui.components.CrystalIconButton
import com.apagon.rhythm.ui.components.crystalCardSurface
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

/**
 * The desktop's sync device list and one-tap sync, mirroring Android's `ui/sync` (QuickSync card +
 * Notes button, 2026-10-11; per-computer list with on/off switches, 2026-10-13).
 *  - Outgoing computers (keyed by address) are what Sync now covers — Linux ↔ Windows. Phones always
 *    connect *to* a computer, so they appear as incoming devices.
 *  - Switching a device off: an outgoing computer is skipped; an incoming phone/computer is refused
 *    by this computer's sync server (see SyncServer).
 *  - No Android toast: results show in place.
 */
sealed interface QuickSyncStatus {
    data object Idle : QuickSyncStatus
    data object Syncing : QuickSyncStatus
    data class Done(val message: String) : QuickSyncStatus
    data class Failed(val message: String) : QuickSyncStatus
}

class QuickSyncViewModel(
    private val syncCoordinator: SyncCoordinator,
    private val syncPreferences: SyncPreferences,
    private val autoSync: AutoSyncRunner
) : ViewModel() {
    /** "Sync automatically", in minutes (0 = off); null until read. */
    val autoSyncMinutes: StateFlow<Int?> = autoSync.minutes

    fun setAutoSyncMinutes(minutes: Int) = viewModelScope.launch { autoSync.setMinutes(minutes) }

    private val _devices = MutableStateFlow<List<SyncDevice>>(emptyList())
    val devices: StateFlow<List<SyncDevice>> = _devices.asStateFlow()

    private val _status = MutableStateFlow<QuickSyncStatus>(QuickSyncStatus.Idle)
    val status: StateFlow<QuickSyncStatus> = _status.asStateFlow()

    /** Peer key → time of the last successful sync with it (phones by device id, computers by address). */
    private val _lastSynced = MutableStateFlow<Map<String, Long>>(emptyMap())
    val lastSynced: StateFlow<Map<String, Long>> = _lastSynced.asStateFlow()

    /** Outgoing computer address → reachable right now; missing = not checked yet. */
    private val _online = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val online: StateFlow<Map<String, Boolean>> = _online.asStateFlow()

    /** Outgoing computer address → its last outcome this session. */
    private val _peerResult = MutableStateFlow<Map<String, String>>(emptyMap())
    val peerResult: StateFlow<Map<String, String>> = _peerResult.asStateFlow()

    /** Ticks so "x min ago" stays true and a phone syncing in shows up without reopening. */
    private val _now = MutableStateFlow(System.currentTimeMillis())
    val now: StateFlow<Long> = _now.asStateFlow()

    init {
        viewModelScope.launch {
            var tick = 0
            while (true) {
                refresh()
                if (tick++ % 2 == 0) checkOnline()
                delay(10_000L)
            }
        }
    }

    private suspend fun refresh() {
        _devices.value = syncPreferences.devices()
        _lastSynced.value = syncPreferences.allLastSyncedAt()
        _now.value = System.currentTimeMillis()
    }

    fun lastSyncedFor(device: SyncDevice): Long = _lastSynced.value[device.key.trim().lowercase()] ?: 0L

    private suspend fun checkOnline() {
        val outgoing = _devices.value.filter { it.outgoing }
        _online.value = coroutineScope {
            outgoing.map { d -> async { d.key to syncCoordinator.isReachable(d.key) } }.awaitAll().toMap()
        }
    }

    /** host or host:port → host:port, so the key matches the one the sync client records. */
    private fun normalize(address: String): String {
        val trimmed = address.trim()
        return if (':' in trimmed) trimmed else "$trimmed:47890"
    }

    fun addComputerAndSync(address: String) {
        val a = normalize(address)
        if (a.startsWith(":")) return
        viewModelScope.launch {
            syncPreferences.addComputer(a)
            syncPreferences.setPeerAddress(a)
            refresh()
            runSync(listOf(SyncDevice(key = a, outgoing = true)), emptyList())
        }
    }

    fun setEnabled(device: SyncDevice, enabled: Boolean) = viewModelScope.launch {
        syncPreferences.setDeviceEnabled(device.key, device.outgoing, enabled)
        refresh()
    }

    fun remove(device: SyncDevice) = viewModelScope.launch {
        syncPreferences.removeDevice(device.key, device.outgoing)
        refresh()
    }

    /** Phones with Bori open and connected to this computer right now (device ids, lowercase). */
    val connectedPhones: StateFlow<Set<String>> = syncCoordinator.connectedPhones

    fun isConnected(device: SyncDevice): Boolean = device.key.trim().lowercase() in connectedPhones.value

    /**
     * Syncs with everything switched on: computers this one syncs to, directly; phones by asking
     * them over their presence socket (they can't be reached otherwise). Computer ↔ computer works
     * exactly as before.
     */
    fun syncAll() = viewModelScope.launch {
        val devices = syncPreferences.devices().filter { it.enabled }
        val computers = devices.filter { it.outgoing }
        val phones = devices.filter { it.isPhone }
        if (computers.isEmpty() && phones.isEmpty()) {
            _status.value = QuickSyncStatus.Failed("No devices to sync with yet. Pair your phone or add a computer.")
            return@launch
        }
        runSync(computers, phones)
    }

    fun syncOne(device: SyncDevice) = viewModelScope.launch {
        if (device.outgoing) runSync(listOf(device), emptyList()) else runSync(emptyList(), listOf(device))
    }

    /**
     * Forgets every watermark, then syncs with every switched-on computer, so rows made before sync
     * ever ran (or before an entity joined sync) go across too. Once per device, not routinely.
     */
    fun forceFullResyncThenSync() = viewModelScope.launch {
        syncPreferences.clearLastSyncedAt()
        syncAll()
    }

    private suspend fun runSync(computers: List<SyncDevice>, phones: List<SyncDevice>) {
        if (_status.value == QuickSyncStatus.Syncing) return
        _status.value = QuickSyncStatus.Syncing
        var received = 0
        val synced = mutableListOf<String>()
        val problems = mutableListOf<String>()
        fun nameOf(d: SyncDevice) = d.name ?: if (d.isPhone) "your phone" else d.key
        for (computer in computers) {
            runCatching { syncCoordinator.syncWith(computer.key) }
                .onSuccess { r: SyncResult ->
                    synced += nameOf(computer)
                    received += r.habitsInserted + r.habitsUpdated + r.completionsInserted +
                        r.completionsUpdated + r.otherInserted + r.otherUpdated
                    _peerResult.value = _peerResult.value + (computer.key to "Synced just now")
                    _online.value = _online.value + (computer.key to true)
                }
                .onFailure { e ->
                    val message = e.message ?: e::class.simpleName ?: "Sync failed"
                    problems += "${nameOf(computer)}: $message"
                    _peerResult.value = _peerResult.value + (computer.key to message)
                }
        }
        val catchingUp = mutableListOf<String>()
        for (phone in phones) {
            val result = when (val outcome = syncCoordinator.requestPhoneSync(phone.key)) {
                is PhoneSyncOutcome.Done -> { synced += nameOf(phone); "Synced just now" }
                is PhoneSyncOutcome.Failed -> outcome.message.also { problems += "${nameOf(phone)}: $it" }
                // A closed phone can't be reached (Android only lets it call out). With its own
                // automatic sync on that's normal, not a failure: it collects everything next time.
                PhoneSyncOutcome.NotConnected -> when (val minutes = phone.autoSyncMinutes) {
                    null, 0 -> "Open Bori on ${nameOf(phone)} to sync, or turn on Sync automatically there".also { problems += it }
                    else -> "Catches up on its own (${AutoSyncChoices.label(minutes).lowercase()})".also {
                        catchingUp += "${nameOf(phone)} will catch up on its own (${AutoSyncChoices.label(minutes).lowercase()})"
                    }
                }
                PhoneSyncOutcome.TimedOut -> "${nameOf(phone)} didn't finish in time".also { problems += it }
            }
            _peerResult.value = _peerResult.value + (phone.key to result)
        }
        refresh()
        val head = when {
            synced.isEmpty() -> null
            computers.isNotEmpty() && received == 0 && synced.size == computers.size && phones.isEmpty() -> "Already up to date"
            else -> "Synced: " + synced.joinToString(" · ")
        }
        _status.value = when {
            head != null -> QuickSyncStatus.Done((listOf(head) + catchingUp + problems).joinToString("\n"))
            problems.isEmpty() && catchingUp.isNotEmpty() -> QuickSyncStatus.Done(catchingUp.joinToString("\n"))
            else -> QuickSyncStatus.Failed((problems + catchingUp).joinToString("\n"))
        }
    }

    fun clearResult() {
        if (_status.value is QuickSyncStatus.Done || _status.value is QuickSyncStatus.Failed) {
            _status.value = QuickSyncStatus.Idle
        }
    }
}

fun syncAgo(then: Long, now: Long): String {
    if (then <= 0) return "Not synced yet"
    val mins = (now - then).coerceAtLeast(0) / 60_000
    return "Last synced " + when {
        mins < 1 -> "just now"
        mins < 60 -> "$mins min ago"
        mins < 24 * 60 -> "${mins / 60} h ago"
        else -> "${mins / (24 * 60)} d ago"
    }
}

/** "Phone 5 min ago · natp 2 h ago": the newest sync with any phone, then each switched-on computer. */
private fun lastSyncedLine(devices: List<SyncDevice>, lastSynced: Map<String, Long>, now: Long): String {
    fun at(d: SyncDevice) = lastSynced[d.key.trim().lowercase()] ?: 0L
    fun ago(t: Long) = syncAgo(t, now).removePrefix("Last synced ")
    val parts = mutableListOf<String>()
    devices.filter { it.isPhone }.maxByOrNull { at(it) }?.takeIf { at(it) > 0 }
        ?.let { parts += "${it.name ?: "Phone"} ${ago(at(it))}" }
    devices.filter { it.outgoing && it.enabled && at(it) > 0 }
        .forEach { parts += "${it.name ?: it.key} ${ago(at(it))}" }
    return if (parts.isEmpty()) "Not synced yet" else "Last synced: " + parts.joinToString(" · ")
}

/** A card for the top of Settings: when each device last synced, and Sync now. */
@Composable
fun QuickSyncCard(modifier: Modifier = Modifier, viewModel: QuickSyncViewModel = koinViewModel()) {
    val devices by viewModel.devices.collectAsState()
    val status by viewModel.status.collectAsState()
    val lastSynced by viewModel.lastSynced.collectAsState()
    val now by viewModel.now.collectAsState()
    // Nothing to show until something has been set up, as on Android.
    if (devices.isEmpty()) return
    // Phones count too (2026-10-15): the computer asks them over their presence socket. Before,
    // a computer with only a phone paired showed this card with nothing to click.
    val canSync = devices.any { it.enabled && (it.outgoing || it.isPhone) }
    val syncing = status is QuickSyncStatus.Syncing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .crystalCardSurface()
            .clickable(enabled = canSync && !syncing, role = androidx.compose.ui.semantics.Role.Button) { viewModel.syncAll() }
            .pointerHoverIcon(if (canSync && !syncing) PointerIcon.Hand else PointerIcon.Default)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Sync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Sync", style = MaterialTheme.typography.bodyLarge)
            Text(
                when (val s = status) {
                    is QuickSyncStatus.Syncing -> "Syncing…"
                    is QuickSyncStatus.Failed -> s.message
                    is QuickSyncStatus.Done -> s.message
                    QuickSyncStatus.Idle -> lastSyncedLine(devices, lastSynced, now)
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (status is QuickSyncStatus.Failed) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (canSync) {
            FilledTonalButton(onClick = { viewModel.syncAll() }, enabled = !syncing) {
                if (syncing) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                else Text("Sync now")
            }
        }
    }
}

/** A top-bar action: the sync arrows, a spinner while running, then the result for a few seconds. */
@Composable
fun QuickSyncIconButton(viewModel: QuickSyncViewModel = koinViewModel()) {
    val devices by viewModel.devices.collectAsState()
    val status by viewModel.status.collectAsState()
    if (devices.none { it.enabled && (it.outgoing || it.isPhone) }) return
    LaunchedEffect(status) {
        if (status is QuickSyncStatus.Done || status is QuickSyncStatus.Failed) {
            delay(4_000L)
            viewModel.clearResult()
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        when (val s = status) {
            is QuickSyncStatus.Done -> Text(s.message.lineSequence().first(), style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            is QuickSyncStatus.Failed -> Text(s.message.lineSequence().first(), style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error, maxLines = 1)
            else -> {}
        }
        if (status is QuickSyncStatus.Syncing) {
            CircularProgressIndicator(modifier = Modifier.padding(8.dp).size(20.dp), strokeWidth = 2.dp)
        } else {
            CrystalIconButton(icon = Icons.Default.Sync, contentDescription = "Sync now", onClick = { viewModel.syncAll() })
        }
    }
}

/**
 * Every device this computer syncs with, as Android's "Your computers": name, kind, Online/Offline
 * (outgoing computers), last synced, an on/off switch, and Sync/Remove where they apply.
 */
@Composable
fun SyncDeviceList(viewModel: QuickSyncViewModel) {
    val devices by viewModel.devices.collectAsState()
    val online by viewModel.online.collectAsState()
    val results by viewModel.peerResult.collectAsState()
    val status by viewModel.status.collectAsState()
    val now by viewModel.now.collectAsState()
    val connectedPhones by viewModel.connectedPhones.collectAsState()
    var removing by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<SyncDevice?>(null) }

    removing?.let { d ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { removing = null },
            title = { Text("Remove ${d.name ?: d.key}?") },
            text = {
                Text(
                    if (d.outgoing) "This computer stops syncing with it. Nothing is deleted on either device; add its address again to pair it back."
                    else "It leaves this list. Nothing is deleted. If it syncs again it comes back, switched on; switch it off instead to keep it out."
                )
            },
            confirmButton = { androidx.compose.material3.TextButton(onClick = { viewModel.remove(d); removing = null }) { Text("Remove") } },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { removing = null }) { Text("Cancel") } }
        )
    }

    if (devices.isEmpty()) {
        Text(
            "No devices yet. When this computer shows Ready for your phone, scan its QR code with the phone — or see How to set up sync.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    val syncing = status is QuickSyncStatus.Syncing
    Column(
        modifier = Modifier.fillMaxWidth().crystalCardSurface().padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        devices.sortedWith(compareBy<SyncDevice> { it.outgoing }.thenBy { (it.name ?: it.key).lowercase() })
            .forEachIndexed { i, d ->
                if (i > 0) androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OsMark(d.os ?: if (d.isPhone) "android" else null)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(d.name ?: if (d.isPhone) "Phone" else "Computer", style = MaterialTheme.typography.bodyLarge)
                        val kind = when {
                            d.isPhone -> "Phone · syncs with this computer"
                            d.outgoing -> "Computer · this one syncs to it"
                            else -> "Computer · syncs with this one"
                        }
                        // Computers: is their sync port answering. Phones: is Bori open there and
                        // connected to this computer (only then can Sync reach it).
                        val up = when {
                            d.outgoing -> online[d.key]
                            d.isPhone -> d.key.trim().lowercase() in connectedPhones
                            else -> null
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (up != null) {
                                androidx.compose.foundation.layout.Box(
                                    Modifier.size(8.dp).background(
                                        if (up) Color(0xFF34A853) else MaterialTheme.colorScheme.outline,
                                        CircleShape
                                    )
                                )
                                Spacer(Modifier.width(6.dp))
                            }
                            Text(
                                (when {
                                    up == null -> ""
                                    d.isPhone -> if (up) "Bori open · " else "Bori closed · "
                                    up -> "Online · "
                                    else -> "Offline · "
                                }) + kind +
                                    (if (d.outgoing) " · ${d.key}" else ""),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            results[d.key] ?: syncAgo(viewModel.lastSyncedFor(d), now),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (d.outgoing || d.isPhone) {
                        androidx.compose.material3.TextButton(onClick = { viewModel.syncOne(d) }, enabled = !syncing && d.enabled) { Text("Sync") }
                    }
                    androidx.compose.material3.TextButton(onClick = { removing = d }) {
                        Text("Remove", color = MaterialTheme.colorScheme.error)
                    }
                    androidx.compose.material3.Switch(
                        checked = d.enabled,
                        onCheckedChange = { viewModel.setEnabled(d, it) },
                        colors = com.apagon.rhythm.ui.components.crystalSwitchColors()
                    )
                }
            }
    }
}

/**
 * The Sync row for a Settings group (Data Management): the whole row is the Sync button whenever
 * there's a device to sync with, and it shows "Syncing…" and then the result in place.
 */
@Composable
fun QuickSyncItem(viewModel: QuickSyncViewModel = koinViewModel()) {
    val devices by viewModel.devices.collectAsState()
    val status by viewModel.status.collectAsState()
    val lastSynced by viewModel.lastSynced.collectAsState()
    val now by viewModel.now.collectAsState()
    val canSync = devices.any { it.enabled && (it.outgoing || it.isPhone) }
    val syncing = status is QuickSyncStatus.Syncing
    com.apagon.rhythm.ui.settings.SettingsItem(
        title = "Sync now",
        description = when (val s = status) {
            is QuickSyncStatus.Syncing -> "Syncing…"
            is QuickSyncStatus.Failed -> s.message
            is QuickSyncStatus.Done -> s.message
            QuickSyncStatus.Idle -> if (devices.isEmpty()) "Pair your phone or add a computer to start syncing"
                                    else lastSyncedLine(devices, lastSynced, now)
        },
        descriptionColor = if (status is QuickSyncStatus.Failed) MaterialTheme.colorScheme.error else Color.Unspecified,
        icon = Icons.Default.Sync,
        onClick = { viewModel.syncAll() },
        enabled = canSync && !syncing,
        trailing = {
            if (syncing) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            else if (canSync) FilledTonalButton(onClick = { viewModel.syncAll() }) { Text("Sync now") }
        }
    )
}
