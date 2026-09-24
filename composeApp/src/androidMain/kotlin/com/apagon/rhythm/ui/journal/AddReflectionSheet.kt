package com.apagon.rhythm.ui.journal
import com.apagon.rhythm.core.time.*

import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.apagon.rhythm.data.model.Habit
import com.apagon.rhythm.data.model.JournalEntry
import com.apagon.rhythm.data.repository.LockType
import com.apagon.rhythm.ui.util.*
import com.apagon.rhythm.ui.theme.resolveDisplayColor
import kotlinx.datetime.LocalDate
import com.apagon.rhythm.core.time.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockSettingsSheet(
    currentLockType: LockType,
    currentPin: String?,
    currentPassword: String?,
    biometricEnabled: Boolean,
    isPro: Boolean,
    onDismiss: () -> Unit,
    onShowPaywall: (String) -> Unit,
    onSavePin: (String?) -> Unit,
    onSavePassword: (String?) -> Unit,
    onClearAll: () -> Unit,
    onToggleBiometric: (Boolean) -> Unit
) {
    var mode by remember { mutableStateOf("MAIN") } // MAIN, SET_PIN, SET_PASSWORD, VERIFY_DISABLE
    var tempInput by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)) }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            AnimatedContent(
                targetState = mode,
                transitionSpec = { fadeIn(tween(300, easing = EaseInOut)) togetherWith fadeOut(tween(250, easing = EaseInOut)) },
                label = "lockModeContent"
            ) { currentMode ->
            when (currentMode) {
                "MAIN" -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    EditorialTitle("Journal Security")

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        LockOptionRow(
                            title = "Numeric PIN",
                            description = if (currentLockType == LockType.PIN) "Currently active" else "4-digit numeric code",
                            selected = currentLockType == LockType.PIN,
                            isPro = isPro,
                            onClick = { mode = "SET_PIN"; tempInput = ""; error = null }
                        )

                        LockOptionRow(
                            title = "Custom Password",
                            description = if (currentLockType == LockType.PASSWORD) "Currently active" else "Alphanumeric security",
                            selected = currentLockType == LockType.PASSWORD,
                            isPro = isPro,
                            onClick = {
                                if (isPro) {
                                    mode = "SET_PASSWORD"
                                    tempInput = ""
                                    error = null
                                } else {
                                    onShowPaywall("Upgrade to Pro to use Alphanumeric Passwords!")
                                }
                            }
                        )

                        if (currentLockType != LockType.NONE) {
                            TextButton(
                                onClick = { mode = "VERIFY_DISABLE"; tempInput = ""; error = null },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Disable All Locks", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    val bioEnabled = biometricEnabled && currentLockType != LockType.NONE
                    val bioLocked = currentLockType == LockType.NONE
                    Surface(
                        onClick = {
                            if (!bioLocked) {
                                if (isPro) onToggleBiometric(!bioEnabled)
                                else onShowPaywall("Upgrade to Pro to use Biometric Unlock!")
                            }
                        },
                        shape = MaterialTheme.shapes.medium,
                        color = if (bioEnabled) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "Biometric Unlock",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (bioLocked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                                else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (!isPro) {
                                        Spacer(Modifier.width(8.dp))
                                        Icon(
                                            Icons.Default.Lock,
                                            contentDescription = "Pro",
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Text(
                                    if (bioLocked) "Set a lock method first" else "Use fingerprint or face",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = bioEnabled,
                                onCheckedChange = {
                                    if (isPro) onToggleBiometric(it)
                                    else onShowPaywall("Upgrade to Pro to use Biometric Unlock!")
                                },
                                enabled = !bioLocked
                            )
                        }
                    }
                }

                "SET_PIN" -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    EditorialTitle("Set PIN")
                    Text("Enter a 4-digit code to lock your journal.", style = MaterialTheme.typography.bodyMedium)

                    // PIN dot indicators
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        repeat(4) { i ->
                            Box(
                                Modifier.size(16.dp).clip(CircleShape).background(
                                    if (i < tempInput.length) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                            )
                        }
                    }

                    // 3×4 keypad grid
                    listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("C", "0", "✓")
                    ).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            row.forEach { key ->
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                        .clickable {
                                            when (key) {
                                                "C" -> if (tempInput.isNotEmpty()) tempInput = tempInput.dropLast(1)
                                                "✓" -> if (tempInput.length == 4) { onSavePin(tempInput); onDismiss() }
                                                else -> if (tempInput.length < 4) tempInput += key
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(key, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    TextButton(onClick = { mode = "MAIN" }, modifier = Modifier.fillMaxWidth()) {
                        Text("Back")
                    }
                }

                "SET_PASSWORD" -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    EditorialTitle("Set Password")
                    Text("Choose a strong alphanumeric password.", style = MaterialTheme.typography.bodyMedium)

                    FluidTextField(
                        value = tempInput,
                        onValueChange = { tempInput = it },
                        label = "NEW PASSWORD",
                        modifier = Modifier.fillMaxWidth()
                    )

                    MomentumButton(
                        text = "Save Password",
                        onClick = { onSavePassword(tempInput); onDismiss() },
                        enabled = tempInput.length >= 4,
                        modifier = Modifier.fillMaxWidth()
                    )

                    TextButton(onClick = { mode = "MAIN" }, modifier = Modifier.fillMaxWidth()) {
                        Text("Cancel")
                    }
                }

                "VERIFY_DISABLE" -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    EditorialTitle("Verify Lock")
                    Text(
                        "Confirm your current ${if (currentLockType == LockType.PIN) "PIN" else "password"} to disable security.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    if (currentLockType == LockType.PIN) {
                        // PIN dot indicators
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            repeat(4) { i ->
                                Box(
                                    Modifier.size(16.dp).clip(CircleShape).background(
                                        if (i < tempInput.length) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceContainerHigh
                                    )
                                )
                            }
                        }

                        if (error != null) {
                            Text(
                                error!!,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }

                        // 3×4 keypad grid
                        listOf(
                            listOf("1", "2", "3"),
                            listOf("4", "5", "6"),
                            listOf("7", "8", "9"),
                            listOf("C", "0", "✓")
                        ).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                row.forEach { key ->
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                            .clickable {
                                                when (key) {
                                                    "C" -> if (tempInput.isNotEmpty()) tempInput = tempInput.dropLast(1)
                                                    "✓" -> {
                                                        if (tempInput == currentPin) { onClearAll(); onDismiss() }
                                                        else { error = "Incorrect PIN"; tempInput = "" }
                                                    }
                                                    else -> if (tempInput.length < 4) tempInput += key
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(key, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    } else {
                        FluidTextField(value = tempInput, onValueChange = { tempInput = it }, label = "ENTER PASSWORD", modifier = Modifier.fillMaxWidth())
                        if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
                        MomentumButton(text = "Disable Lock", onClick = {
                            if (tempInput == currentPassword) { onClearAll(); onDismiss() }
                            else { error = "Incorrect Password"; tempInput = "" }
                        }, enabled = tempInput.isNotBlank(), modifier = Modifier.fillMaxWidth())
                    }

                    TextButton(onClick = { mode = "MAIN" }, modifier = Modifier.fillMaxWidth()) {
                        Text("Cancel")
                    }
                }
            }
            } // end AnimatedContent
        }
    }
}

@Composable
fun LockOptionRow(
    title: String,
    description: String,
    selected: Boolean,
    isPro: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (!isPro && title != "Numeric PIN") {
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.Lock, contentDescription = "Pro", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            RadioButton(selected = selected, onClick = null)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EntrySheet(
    entry: JournalEntry?,
    selectedDate: LocalDate,
    habits: List<Habit>,
    isPro: Boolean = true,
    onDelete: (JournalEntry) -> Unit,
    onDismiss: () -> Unit,
    onShowPaywall: (String) -> Unit = {},
    onSave: (
        title: String,
        content: String,
        feelings: List<String>,
        tags: List<String>,
        photoUris: List<String>,
        habitId: Long?
    ) -> Unit
) {
    val context = LocalContext.current

    var title by rememberSaveable { mutableStateOf(entry?.title ?: "") }
    var content by rememberSaveable { mutableStateOf(entry?.content ?: "") }
    val selectedFeelings = remember {
        mutableStateListOf<String>().also { it.addAll(entry?.feelingList() ?: emptyList()) }
    }
    val tags = remember {
        mutableStateListOf<String>().also { it.addAll(entry?.tagList() ?: emptyList()) }
    }
    val photoUris = remember {
        mutableStateListOf<String>().also { it.addAll(entry?.photoUriList() ?: emptyList()) }
    }
    var selectedHabitId by rememberSaveable { mutableStateOf(entry?.habitId) }
    var tagInput by rememberSaveable { mutableStateOf("") }
    var habitDropdownExpanded by rememberSaveable { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 6)
    ) { uris: List<Uri> ->
        uris.take(6 - photoUris.size).forEach { uri ->
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            photoUris.add(uri.toString())
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                    if (entry != null) {
                        IconButton(onClick = { onDelete(entry); onDismiss() }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                Text(
                    if (entry == null) "New Entry" else "Edit Entry",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = {
                    onSave(
                        title, content,
                        selectedFeelings.toList(), tags.toList(), photoUris.toList(),
                        selectedHabitId
                    )
                }) {
                    Text(
                        "Save",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Date chip
            SuggestionChip(
                onClick = {},
                label = {
                    Text(
                        selectedDate.format(DateTimeFormatter.ofPattern("EEE, MMM d")),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            )

            // Feelings chips
            Text(
                "Feelings",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                journalFeelings.forEach { (key, emoji) ->
                    val selected = key in selectedFeelings
                    Surface(
                        onClick = {
                            if (selected) selectedFeelings.remove(key)
                            else selectedFeelings.add(key)
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHighest,
                        shadowElevation = if (selected) 4.dp else 2.dp,
                        tonalElevation = if (selected) 2.dp else 1.dp,
                        border = if (selected) null else BorderStroke(
                            width = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Text(text = "$emoji $key", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            // Title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        "Entry title...",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )

            // Body
            Column {
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            "What happened today? How did it feel?",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    minLines = 6,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            // Photos
            if (photoUris.isNotEmpty() || true) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (photoUris.size < 6) {
                        Surface(
                            onClick = {
                                if (isPro || photoUris.size < 1) {
                                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                } else {
                                    onShowPaywall("Upgrade to Pro to add unlimited photos!")
                                }
                            },
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            shadowElevation = 2.dp,
                            tonalElevation = 1.dp,
                            border = BorderStroke(
                                width = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.AddAPhoto,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Add Photo", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                    photoUris.forEachIndexed { index, uri ->
                        Box {
                            AsyncImage(
                                model = uri,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { photoUris.removeAt(index) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(20.dp)
                                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
            }

            // Tags
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = tagInput,
                    onValueChange = { tagInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Add tag...") },
                    trailingIcon = {
                        IconButton(onClick = {
                            if (tagInput.isNotBlank()) {
                                tags.add(tagInput.trim())
                                tagInput = ""
                            }
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Tag")
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    tags.forEachIndexed { index, tag ->
                        InputChip(
                            selected = true,
                            onClick = { tags.removeAt(index) },
                            label = { Text("#$tag") },
                            trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }
            }

            // Habit link
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Link to Habit",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(Modifier.fillMaxWidth()) {
                    val habit = habits.find { it.id == selectedHabitId }
                    Surface(
                        onClick = { habitDropdownExpanded = true },
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shadowElevation = 2.dp,
                        tonalElevation = 1.dp,
                        border = BorderStroke(
                            width = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (habit != null) {
                                Box(
                                    Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(resolveDisplayColor(habit.colorIndex, habit.colorArgb))
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(habit.name)
                            } else {
                                Text("No habit linked")
                            }
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }

                    DropdownMenu(
                        expanded = habitDropdownExpanded,
                        onDismissRequest = { habitDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("None") },
                            onClick = { selectedHabitId = null; habitDropdownExpanded = false }
                        )
                        habits.forEach { h ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(resolveDisplayColor(h.colorIndex, h.colorArgb))
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(h.name)
                                    }
                                },
                                onClick = { selectedHabitId = h.id; habitDropdownExpanded = false }
                            )
                        }
                    }
                }
            }
        } // Column
        } // Surface
    } // Dialog
}
