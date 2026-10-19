package com.apagon.rhythm.data.backup

import com.apagon.rhythm.platform.AppHome
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.util.Properties

/**
 * The computer's own automatic backup, as the phone's (AutoBackupWorker + writeAutoBackup):
 * checked at launch and every hour, written when the newest one is older than 20 hours (daily)
 * or 6 days (weekly), `bori_autobackup_<date>.json` in [BackupFolder], newest 14 kept (2026-10-19).
 * Before this a computer made no backup unless asked. The desktop never remembers a backup
 * password, so these are plain JSON, like a phone backup without one.
 *
 * The frequency is this computer's own (`auto_backup.properties`), not a synced setting.
 */
class DesktopAutoBackup(private val backup: DesktopBackupManager) : AutoBackupControl {

    private val settingsFile get() = File(AppHome.dir, "auto_backup.properties")
    private val _frequency = MutableStateFlow(readFrequency())
    override val frequency: StateFlow<AutoBackupFrequency> = _frequency

    private val _lastBackupLabel = MutableStateFlow<String?>(null)
    override val lastBackupLabel: StateFlow<String?> = _lastBackupLabel

    override val folderLabel: String
        get() = BackupFolder.dir.path.replaceFirst(System.getProperty("user.home"), "~")

    private val lock = Mutex()

    private fun readFrequency(): AutoBackupFrequency = runCatching {
        val p = Properties().apply { settingsFile.inputStream().use { load(it) } }
        AutoBackupFrequency.valueOf(p.getProperty("frequency", AutoBackupFrequency.DAILY.name))
    }.getOrDefault(AutoBackupFrequency.DAILY)

    override fun setFrequency(frequency: AutoBackupFrequency) {
        _frequency.value = frequency
        runCatching {
            Properties().apply { setProperty("frequency", frequency.name) }
                .let { p -> settingsFile.outputStream().use { p.store(it, null) } }
        }
    }

    fun start(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            while (true) {
                runCatching { runOnce() }.onFailure { it.printStackTrace() }
                delay(CHECK_EVERY_MS)
            }
        }
    }

    /** Writes a backup when one is due (or always, with [force]). Returns the file written, if any. */
    suspend fun runOnce(force: Boolean = false): File? = withContext(Dispatchers.IO) {
        lock.withLock {
            val folder = BackupFolder.dir
            val newest = BackupFolder.autoBackups(folder).firstOrNull()
            _lastBackupLabel.value = newest?.let(::label)
            val gap = when (_frequency.value) {
                AutoBackupFrequency.OFF -> if (force) 0L else return@withLock null
                AutoBackupFrequency.DAILY -> 20 * HOUR
                AutoBackupFrequency.WEEKLY -> 6 * 24 * HOUR
            }
            if (!force && newest != null && System.currentTimeMillis() - newest.lastModified() < gap) return@withLock null

            folder.mkdirs()
            val dest = File(folder, "${BackupFolder.AUTO_PREFIX}${LocalDate.now()}.json")
            val tmp = File(folder, "${dest.name}.part")
            tmp.writeText(backup.exportToJson(null))
            if (dest.exists()) dest.delete()
            if (!tmp.renameTo(dest)) return@withLock null
            BackupFolder.prune(folder)
            _lastBackupLabel.value = label(dest)
            dest
        }
    }

    override suspend fun backUpNow(): String? =
        runCatching { runOnce(force = true) }.fold({ if (it == null) "Couldn't write the backup file." else null }, { it.message ?: "Backup failed." })

    override fun openFolder() {
        runCatching {
            BackupFolder.dir.mkdirs()
            java.awt.Desktop.getDesktop().open(BackupFolder.dir)
        }.onFailure {
            // No AWT desktop integration (some Linux sessions): hand it to the file manager.
            runCatching { ProcessBuilder("xdg-open", BackupFolder.dir.path).start() }
        }
    }

    private fun label(file: File): String {
        val at = java.time.Instant.ofEpochMilli(file.lastModified()).atZone(java.time.ZoneId.systemDefault())
        val day = when (at.toLocalDate()) {
            LocalDate.now() -> "today"
            LocalDate.now().minusDays(1) -> "yesterday"
            else -> at.toLocalDate().toString()
        }
        return "$day at ${at.toLocalTime().withSecond(0).withNano(0)}"
    }

    private companion object {
        const val HOUR = 60 * 60 * 1000L
        // Hourly rather than the phone's 12 h: a computer is often on for a few hours at a time,
        // and a check is only a folder listing.
        const val CHECK_EVERY_MS = HOUR
    }
}
