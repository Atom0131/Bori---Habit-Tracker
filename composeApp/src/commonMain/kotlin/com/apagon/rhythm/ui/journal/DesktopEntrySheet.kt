package com.apagon.rhythm.ui.journal

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.apagon.rhythm.core.time.*
import com.apagon.rhythm.data.model.Habit
import com.apagon.rhythm.data.model.JournalEntry
import com.apagon.rhythm.platform.FilePicker
import com.apagon.rhythm.platform.ImageBitmapLoader
import com.apagon.rhythm.platform.PhotoStorage
import com.apagon.rhythm.ui.components.CrystalWindowContent
import com.apagon.rhythm.ui.components.crystalSheetColor
import com.apagon.rhythm.ui.theme.resolveDisplayColor
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.koin.compose.koinInject

/**
 * Desktop port of AddReflectionSheet.kt's EntrySheet. The Pro paywall is
 * dropped (desktop is unconditionally Pro), so there's no photo-count gate
 * or unlimited-photos upsell. Photo picking goes through the native
 * FileDialog (pickFileToOpen, built in Stage 6) plus PhotoStorage.importPhoto
 * called directly here — not through JournalViewModel.saveEntry's
 * content://-prefix gate, which a desktop absolute path would never match.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DesktopEntrySheet(
    entry: JournalEntry?,
    selectedDate: LocalDate,
    habits: List<Habit>,
    onDelete: (JournalEntry) -> Unit,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        content: String,
        feelings: List<String>,
        tags: List<String>,
        photoUris: List<String>,
        habitId: Long?
    ) -> Unit
) {
    val photoStorage = koinInject<PhotoStorage>()
    val imageLoader = koinInject<ImageBitmapLoader>()
    val filePicker = koinInject<FilePicker>()
    val coroutineScope = rememberCoroutineScope()

    var title by rememberSaveable { mutableStateOf(entry?.title ?: "") }
    var content by rememberSaveable { mutableStateOf(entry?.content ?: "") }
    val selectedFeelings = remember { mutableStateListOf<String>().also { it.addAll(entry?.feelingList() ?: emptyList()) } }
    val tags = remember { mutableStateListOf<String>().also { it.addAll(entry?.tagList() ?: emptyList()) } }
    val photoUris = remember { mutableStateListOf<String>().also { it.addAll(entry?.photoUriList() ?: emptyList()) } }
    var selectedHabitId by rememberSaveable { mutableStateOf(entry?.habitId) }
    var tagInput by rememberSaveable { mutableStateOf("") }
    var habitDropdownExpanded by rememberSaveable { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false, usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = crystalSheetColor(fallback = MaterialTheme.colorScheme.surface)) {
            // Stage 14 invariant #2: this Dialog is a separate window from the main one, so its
            // inherited blur field (if any) is unusable — CrystalWindowContent replaces it with a
            // fresh one scoped to this dialog.
            CrystalWindowContent {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onDismiss) { Text("×") }
                        if (entry != null) {
                            TextButton(onClick = { onDelete(entry); onDismiss() }) {
                                Text("Delete", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    Text(
                        if (entry == null) "New Entry" else "Edit Entry",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(onClick = {
                        onSave(title, content, selectedFeelings.toList(), tags.toList(), photoUris.toList(), selectedHabitId)
                    }) {
                        Text("Save", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                }

                SuggestionChip(
                    onClick = {},
                    label = { Text(selectedDate.format(DateTimeFormatter.ofPattern("EEE, MMM d")), style = MaterialTheme.typography.labelMedium) }
                )

                Text("Feelings", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    journalFeelings.forEach { (key, emoji) ->
                        val selected = key in selectedFeelings
                        Surface(
                            onClick = { if (selected) selectedFeelings.remove(key) else selectedFeelings.add(key) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                            border = if (selected) null else BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text(text = "$emoji $key", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Entry title...", style = MaterialTheme.typography.titleLarge) },
                    textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    shape = RoundedCornerShape(16.dp)
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("What happened today? How did it feel?") },
                    minLines = 6,
                    shape = RoundedCornerShape(16.dp)
                )

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (photoUris.size < 6) {
                        Surface(
                            onClick = {
                                val path = filePicker.pickImagePath()
                                if (path != null) {
                                    coroutineScope.launch {
                                        val imported = photoStorage.importPhoto(path, "journal/images")
                                        if (imported != null) photoUris.add(imported)
                                    }
                                }
                            },
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text("+ Photo", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    photoUris.forEachIndexed { index, path ->
                        Box {
                            imageLoader.LoadedImage(
                                path = path,
                                modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            TextButton(
                                onClick = { photoUris.removeAt(index) },
                                modifier = Modifier.align(Alignment.TopEnd).size(20.dp).background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Text("×", color = Color.White)
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = tagInput,
                        onValueChange = { tagInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Add tag...") },
                        trailingIcon = {
                            TextButton(onClick = { if (tagInput.isNotBlank()) { tags.add(tagInput.trim()); tagInput = "" } }) {
                                Text("+")
                            }
                        },
                        shape = RoundedCornerShape(16.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        tags.forEachIndexed { index, tag ->
                            InputChip(
                                selected = true,
                                onClick = { tags.removeAt(index) },
                                label = { Text("#$tag") }
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Link to Habit", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Box(Modifier.fillMaxWidth()) {
                        val habit = habits.find { it.id == selectedHabitId }
                        Surface(
                            onClick = { habitDropdownExpanded = true },
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (habit != null) {
                                    Box(Modifier.size(10.dp).clip(CircleShape).background(resolveDisplayColor(habit.colorIndex, habit.colorArgb)))
                                    Spacer(Modifier.width(8.dp))
                                    Text(habit.name)
                                } else {
                                    Text("No habit linked")
                                }
                                Spacer(Modifier.weight(1f))
                                Text("▾")
                            }
                        }

                        DropdownMenu(expanded = habitDropdownExpanded, onDismissRequest = { habitDropdownExpanded = false }) {
                            DropdownMenuItem(text = { Text("None") }, onClick = { selectedHabitId = null; habitDropdownExpanded = false })
                            habits.forEach { h ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(Modifier.size(10.dp).clip(CircleShape).background(resolveDisplayColor(h.colorIndex, h.colorArgb)))
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
            }
            }
        }
    }
}
