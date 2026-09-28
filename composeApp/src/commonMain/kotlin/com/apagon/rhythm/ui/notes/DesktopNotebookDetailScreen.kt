package com.apagon.rhythm.ui.notes

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.core.json.JSONArray
import com.apagon.rhythm.core.time.*
import com.apagon.rhythm.data.model.Note
import com.apagon.rhythm.ui.components.CrystalWindowContent
import com.apagon.rhythm.ui.components.crystalCardSurface
import com.apagon.rhythm.ui.components.crystalFabContainerColor
import com.apagon.rhythm.ui.components.crystalFabContentColor
import com.apagon.rhythm.ui.components.crystalFabElevation
import com.apagon.rhythm.ui.components.crystalScaffoldColor
import com.apagon.rhythm.ui.components.crystalScaffoldContentColor
import com.apagon.rhythm.ui.components.crystalSheetColor
import com.apagon.rhythm.ui.components.crystalTileSurface
import com.apagon.rhythm.ui.util.RhythmAlertDialog
import com.apagon.rhythm.ui.util.RhythmDropdownMenu
import kotlin.time.Instant
import org.koin.compose.viewmodel.koinViewModel

/**
 * Desktop port of NotebookDetailScreen.kt. BackHandler dropped (no desktop
 * back-press/gesture to intercept — the select-mode exit is reachable via
 * the visible "×" button either way). Share/Export goes to the clipboard
 * (LocalClipboardManager, part of standard Compose UI on every platform,
 * confirmed via javap against ui-desktop's AwtClipboardManager) instead of
 * Android's Intent.ACTION_SEND chooser — a decision made once here and not
 * revisited per call site. TutorialCard (first-run template hint) is
 * dropped — desktop has no onboarding flow to hang it off yet.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DesktopNotebookDetailScreen(
    notebookId: Long,
    viewModel: NotebookDetailViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToNote: (noteId: Long, notebookId: Long, template: String?) -> Unit
) {
    LaunchedEffect(notebookId) { viewModel.setNotebookId(notebookId) }

    val notebook by viewModel.notebook.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    val selectedTag by viewModel.selectedTag.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortMode by viewModel.sortMode.collectAsState()
    val selectedNoteIds by viewModel.selectedNoteIds.collectAsState()
    val isInSelectMode by viewModel.isInSelectMode.collectAsState()
    val allNotebooks by viewModel.allNotebooks.collectAsState()
    val clipboard = LocalClipboardManager.current

    var searchExpanded by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }
    var showTemplatePicker by remember { mutableStateOf(false) }
    var showMoveSheet by remember { mutableStateOf(false) }
    var showBulkTagSheet by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var noteToMove by remember { mutableStateOf<Note?>(null) }

    Scaffold(
        containerColor = crystalScaffoldColor(),
        contentColor = crystalScaffoldContentColor(),
        topBar = {
            TopAppBar(
                title = {
                    if (searchExpanded) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Search notes…") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else if (isInSelectMode) {
                        Text("${selectedNoteIds.size} selected", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    } else {
                        Text(notebook?.name ?: "Notebook", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    when {
                        searchExpanded -> TextButton(onClick = { searchExpanded = false; viewModel.setSearchQuery("") }) { Text("×") }
                        isInSelectMode -> TextButton(onClick = { viewModel.clearSelection() }) { Text("×") }
                        else -> TextButton(onClick = onNavigateBack) { Text("← Back") }
                    }
                },
                actions = {
                    if (isInSelectMode) {
                        TextButton(onClick = { showMoveSheet = true; noteToMove = null }) { Text("Move") }
                        TextButton(onClick = { showBulkTagSheet = true }) { Text("Tag") }
                        TextButton(onClick = { showDeleteConfirm = true }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                    } else if (!searchExpanded) {
                        TextButton(onClick = { searchExpanded = true }) { Text("⌕") }
                        TextButton(onClick = { showSortSheet = true }) { Text("Sort") }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!isInSelectMode) {
                FloatingActionButton(
                    onClick = { showTemplatePicker = true },
                    containerColor = crystalFabContainerColor(),
                    contentColor = crystalFabContentColor(),
                    elevation = crystalFabElevation()
                ) { Text("+", style = MaterialTheme.typography.headlineSmall) }
            }
        }
    ) { innerPadding ->
        if (notes.isEmpty() && allTags.isEmpty() && !searchExpanded) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("No notes yet. Tap + to start writing.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (allTags.isNotEmpty()) {
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                            item { FilterChip(selected = selectedTag == null, onClick = { viewModel.setTag(null) }, label = { Text("All") }) }
                            items(allTags) { tag ->
                                FilterChip(selected = selectedTag == tag, onClick = { viewModel.setTag(if (selectedTag == tag) null else tag) }, label = { Text(tag) })
                            }
                        }
                    }
                }

                if (notes.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                            Text("No notes match your filter.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    items(notes, key = { it.id }) { note ->
                        val isSelected = note.id in selectedNoteIds
                        DesktopNoteItem(
                            note = note,
                            isSelected = isSelected,
                            isInSelectMode = isInSelectMode,
                            onClick = { if (isInSelectMode) viewModel.toggleSelection(note.id) else onNavigateToNote(note.id, notebookId, null) },
                            onLongClick = { viewModel.toggleSelection(note.id) },
                            onDelete = { viewModel.deleteNote(note) },
                            onTogglePin = { viewModel.togglePin(note) },
                            onMove = { noteToMove = note; showMoveSheet = true },
                            onExport = { clipboard.setText(AnnotatedString(buildShareText(note))) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        val count = selectedNoteIds.size
        RhythmAlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete $count ${if (count == 1) "note" else "notes"}?") },
            text = { Text("This can't be undone.") },
            confirmButton = { TextButton(onClick = { showDeleteConfirm = false; viewModel.deleteSelectedNotes() }) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }

    if (showSortSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSortSheet = false },
            containerColor = crystalSheetColor(fallback = MaterialTheme.colorScheme.surface)
        ) {
            CrystalWindowContent {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 48.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Sort Notes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                NoteSort.entries.forEach { mode ->
                    val label = when (mode) {
                        NoteSort.NEWEST -> "Newest first"
                        NoteSort.OLDEST -> "Oldest first"
                        NoteSort.A_TO_Z -> "A → Z"
                        NoteSort.Z_TO_A -> "Z → A"
                        NoteSort.PINNED_FIRST -> "Pinned first"
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                        RadioButton(selected = sortMode == mode, onClick = { viewModel.setSort(mode); showSortSheet = false })
                    }
                }
            }
            }
        }
    }

    if (showTemplatePicker) {
        ModalBottomSheet(
            onDismissRequest = { showTemplatePicker = false },
            containerColor = crystalSheetColor(fallback = MaterialTheme.colorScheme.surface)
        ) {
            CrystalWindowContent {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 48.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Choose a Template", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                NoteTemplate.all.forEach { template ->
                    Column(
                        modifier = Modifier.fillMaxWidth()
                            .crystalTileSurface()
                            .clickable {
                                showTemplatePicker = false
                                val templateArg = if (template == NoteTemplate.Blank) null else template.label
                                onNavigateToNote(-1L, notebookId, templateArg)
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp)
                    ) {
                        Text(text = template.label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        val desc = when (template) {
                            NoteTemplate.Blank -> "Empty note"
                            NoteTemplate.MeetingNotes -> "Agenda · Decisions · Action items"
                            NoteTemplate.BookNotes -> "Title · Chapter · Key ideas · Quotes"
                            NoteTemplate.WeeklyPlan -> "Goals · Tasks · Blockers · Reflection"
                            NoteTemplate.LectureNotes -> "Objectives · Key concepts · Questions"
                            NoteTemplate.Brainstorm -> "Problem · Raw ideas · Next steps"
                            else -> ""
                        }
                        if (desc.isNotBlank()) {
                            Text(text = desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            }
        }
    }

    if (showMoveSheet) {
        val targetNotebooks = allNotebooks.filter { it.id != notebookId }
        ModalBottomSheet(
            onDismissRequest = { showMoveSheet = false; noteToMove = null },
            containerColor = crystalSheetColor(fallback = MaterialTheme.colorScheme.surface)
        ) {
            CrystalWindowContent {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 48.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Move to Notebook", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                if (targetNotebooks.isEmpty()) {
                    Text("No other notebooks available.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    targetNotebooks.forEach { nb ->
                        Box(
                            modifier = Modifier.fillMaxWidth()
                                .crystalTileSurface()
                                .clickable {
                                    showMoveSheet = false
                                    noteToMove?.let { viewModel.moveNote(it, nb.id) } ?: viewModel.moveSelectedNotes(nb.id)
                                    noteToMove = null
                                }
                        ) {
                            Text(text = nb.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp))
                        }
                    }
                }
            }
            }
        }
    }

    if (showBulkTagSheet) {
        var tagInput by remember { mutableStateOf("") }
        ModalBottomSheet(
            onDismissRequest = { showBulkTagSheet = false },
            containerColor = crystalSheetColor(fallback = MaterialTheme.colorScheme.surface)
        ) {
            CrystalWindowContent {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 48.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Add Tag to Selected", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(value = tagInput, onValueChange = { tagInput = it }, label = { Text("Tag name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Button(
                    onClick = { viewModel.addTagToSelectedNotes(tagInput); showBulkTagSheet = false },
                    enabled = tagInput.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Add Tag") }
            }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DesktopNoteItem(
    note: Note,
    isSelected: Boolean,
    isInSelectMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit,
    onTogglePin: () -> Unit,
    onMove: () -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateLabel = remember(note.updatedAt) {
        Instant.ofEpochMilli(note.updatedAt).atZone(ZoneId.systemDefault()).toLocalDate()
            .format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    }
    var showMenu by remember { mutableStateOf(false) }
    val parsedPreview = remember(note.content) { parseNoteContent(note.content) }
    val noteTags = remember(note.tags) { note.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() } }

    Box(
        modifier = modifier.fillMaxWidth()
            .crystalCardSurface(
                fill = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.surfaceContainerHighest
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isInSelectMode) {
                            Text(if (isSelected) "●" else "○", modifier = Modifier.padding(end = 6.dp), color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (note.isPinned) {
                            Text("📌", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(end = 4.dp))
                        }
                        Text(
                            text = note.title.ifBlank { "Untitled Note" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(text = dateLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (!isInSelectMode) {
                    Box {
                        TextButton(onClick = { showMenu = true }) { Text("⋮") }
                        RhythmDropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(text = { Text(if (note.isPinned) "Unpin" else "Pin") }, onClick = { showMenu = false; onTogglePin() })
                            DropdownMenuItem(text = { Text("Move to notebook…") }, onClick = { showMenu = false; onMove() })
                            DropdownMenuItem(text = { Text("Copy to clipboard") }, onClick = { showMenu = false; onExport() })
                            DropdownMenuItem(text = { Text("Delete", color = MaterialTheme.colorScheme.error) }, onClick = { showMenu = false; onDelete() })
                        }
                    }
                }
            }

            if (parsedPreview.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(text = parsedPreview, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }

            if (noteTags.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    noteTags.forEach { tag -> SuggestionChip(onClick = {}, label = { Text(tag, style = MaterialTheme.typography.labelSmall) }) }
                }
            }
        }
    }
}

private val TEXT_BLOCK_TYPES = setOf("text", "todo", "checklist", "header", "bullet_list", "numbered_list", "quote", "code")

private fun parseNoteContent(json: String): String {
    if (json.isBlank()) return ""
    return try {
        val array = JSONArray(json)
        val sb = StringBuilder()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            if (obj.has("type") && obj.has("content")) {
                val type = obj.getString("type").lowercase()
                val content = obj.getString("content")
                if (type in TEXT_BLOCK_TYPES && content.isNotBlank()) {
                    if (sb.isNotEmpty()) sb.append(" ")
                    sb.append(content)
                }
            }
        }
        sb.toString()
    } catch (e: Exception) {
        json
    }
}

private fun buildShareText(note: Note): String {
    val title = note.title
    val sb = StringBuilder()
    if (title.isNotBlank()) {
        sb.appendLine(title)
        sb.appendLine("=".repeat(title.length))
        sb.appendLine()
    }
    if (note.tags.isNotBlank()) {
        val tags = note.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        sb.appendLine("Tags: ${tags.joinToString(", ")}")
        sb.appendLine()
    }
    sb.append(parseNoteContent(note.content))
    return sb.toString().trimEnd()
}
