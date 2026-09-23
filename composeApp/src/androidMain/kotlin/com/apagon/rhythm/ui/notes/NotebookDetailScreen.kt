package com.apagon.rhythm.ui.notes

import org.koin.compose.viewmodel.koinViewModel

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.data.model.Note
import com.apagon.rhythm.ui.util.TutorialCard
import com.apagon.rhythm.ui.util.TutorialViewModel
import com.apagon.rhythm.ui.util.blockSheetBodyDrag
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NotebookDetailScreen(
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

    var searchExpanded by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }
    val context = LocalContext.current

    var showSortSheet by remember { mutableStateOf(false) }
    var showTemplatePicker by remember { mutableStateOf(false) }
    var showMoveSheet by remember { mutableStateOf(false) }
    var showBulkTagSheet by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var noteToMove by remember { mutableStateOf<Note?>(null) }

    LaunchedEffect(searchExpanded) {
        if (searchExpanded) searchFocusRequester.requestFocus()
    }

    BackHandler(enabled = isInSelectMode) { viewModel.clearSelection() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    if (searchExpanded) {
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(searchFocusRequester),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            singleLine = true,
                            decorationBox = { inner ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        "Search notes…",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                inner()
                            }
                        )
                    } else if (isInSelectMode) {
                        Text(
                            "${selectedNoteIds.size} selected",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            notebook?.name ?: "Notebook",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    if (searchExpanded) {
                        IconButton(onClick = { searchExpanded = false; viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Close search")
                        }
                    } else if (isInSelectMode) {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "Exit selection")
                        }
                    } else {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (isInSelectMode) {
                        IconButton(onClick = { showMoveSheet = true; noteToMove = null }) {
                            Icon(Icons.Default.FolderOpen, contentDescription = "Move selected")
                        }
                        IconButton(onClick = { showBulkTagSheet = true }) {
                            Icon(Icons.Default.Label, contentDescription = "Tag selected")
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete selected", tint = MaterialTheme.colorScheme.error)
                        }
                    } else if (!searchExpanded) {
                        IconButton(onClick = { searchExpanded = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                        IconButton(onClick = { showSortSheet = true }) {
                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!isInSelectMode) {
                FloatingActionButton(
                    onClick = { showTemplatePicker = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New Note")
                }
            }
        }
    ) { innerPadding ->
        if (notes.isEmpty() && allTags.isEmpty() && !searchExpanded) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No notes yet. Tap + to start writing.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (allTags.isNotEmpty()) {
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedTag == null,
                                    onClick = { viewModel.setTag(null) },
                                    label = { Text("All") }
                                )
                            }
                            items(allTags) { tag ->
                                FilterChip(
                                    selected = selectedTag == tag,
                                    onClick = { viewModel.setTag(if (selectedTag == tag) null else tag) },
                                    label = { Text(tag) }
                                )
                            }
                        }
                    }
                }

                if (notes.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No notes match your filter.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(notes, key = { it.id }) { note ->
                        val isSelected = note.id in selectedNoteIds
                        NoteItem(
                            note = note,
                            isSelected = isSelected,
                            isInSelectMode = isInSelectMode,
                            onClick = {
                                if (isInSelectMode) {
                                    viewModel.toggleSelection(note.id)
                                } else {
                                    onNavigateToNote(note.id, notebookId, null)
                                }
                            },
                            onLongClick = { viewModel.toggleSelection(note.id) },
                            onDelete = { viewModel.deleteNote(note) },
                            onTogglePin = { viewModel.togglePin(note) },
                            onMove = { noteToMove = note; showMoveSheet = true },
                            onExport = {
                                val text = buildShareText(note)
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, note.title.ifBlank { "Note" })
                                    putExtra(Intent.EXTRA_TEXT, text)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share note"))
                            },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }

    // Delete confirmation
    if (showDeleteConfirm) {
        val count = selectedNoteIds.size
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete ${count} ${if (count == 1) "note" else "notes"}?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; viewModel.deleteSelectedNotes() }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    // Sort sheet
    if (showSortSheet) {
        ModalBottomSheet(onDismissRequest = { showSortSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "Sort Notes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                NoteSort.values().forEach { mode ->
                    val label = when (mode) {
                        NoteSort.NEWEST -> "Newest first"
                        NoteSort.OLDEST -> "Oldest first"
                        NoteSort.A_TO_Z -> "A → Z"
                        NoteSort.Z_TO_A -> "Z → A"
                        NoteSort.PINNED_FIRST -> "Pinned first"
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                        RadioButton(
                            selected = sortMode == mode,
                            onClick = { viewModel.setSort(mode); showSortSheet = false }
                        )
                    }
                }
            }
        }
    }

    // Template picker sheet
    if (showTemplatePicker) {
        val templateSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showTemplatePicker = false },
            sheetState = templateSheetState,
            dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 48.dp)
                    .blockSheetBodyDrag(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "Choose a Template",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                val tutorialViewModel: TutorialViewModel = koinViewModel()
                val hasSeenNotesTemplateTutorial by tutorialViewModel.hasSeenNotesTemplateTutorial.collectAsState(initial = true)
                if (!hasSeenNotesTemplateTutorial) {
                    TutorialCard(
                        title = "Templates",
                        description = "Want to know how these work?",
                        bullets = listOf(
                            "Each template just pre-fills a structure — you can still edit or delete anything",
                            "Pick the one that matches what you're writing, or start Blank",
                            "Every template below shows what it includes, right under its name"
                        ),
                        onDismiss = { tutorialViewModel.setHasSeenNotesTemplateTutorial(true) },
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                NoteTemplate.all.forEach { template ->
                    Surface(
                        onClick = {
                            showTemplatePicker = false
                            val templateArg = if (template == NoteTemplate.Blank) null else template.label
                            onNavigateToNote(-1L, notebookId, templateArg)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp)) {
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

    // Move sheet
    if (showMoveSheet) {
        val targetNotebooks = allNotebooks.filter { it.id != notebookId }
        ModalBottomSheet(onDismissRequest = { showMoveSheet = false; noteToMove = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "Move to Notebook",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                if (targetNotebooks.isEmpty()) {
                    Text(
                        "No other notebooks available.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    targetNotebooks.forEach { nb ->
                        Surface(
                            onClick = {
                                showMoveSheet = false
                                noteToMove?.let { viewModel.moveNote(it, nb.id) }
                                    ?: viewModel.moveSelectedNotes(nb.id)
                                noteToMove = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(
                                text = nb.name,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Bulk tag sheet
    if (showBulkTagSheet) {
        var tagInput by remember { mutableStateOf("") }
        ModalBottomSheet(onDismissRequest = { showBulkTagSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Add Tag to Selected",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = tagInput,
                    onValueChange = { tagInput = it },
                    label = { Text("Tag name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        viewModel.addTagToSelectedNotes(tagInput)
                        showBulkTagSheet = false
                    },
                    enabled = tagInput.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add Tag")
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteItem(
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
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    var showMenu by remember { mutableStateOf(false) }
    val parsedPreview = remember(note.content) { parseNoteContent(note.content) }
    val noteTags = remember(note.tags) {
        note.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surfaceContainerHighest
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AnimatedVisibility(
                            visible = isInSelectMode,
                            enter = fadeIn(tween(200)) + expandHorizontally(),
                            exit = fadeOut(tween(150)) + shrinkHorizontally()
                        ) {
                            Icon(
                                if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp).padding(end = 4.dp),
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (note.isPinned) {
                            Icon(
                                Icons.Default.PushPin,
                                contentDescription = "Pinned",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(
                            text = note.title.ifBlank { "Untitled Note" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = dateFormat.format(Date(note.updatedAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!isInSelectMode) {
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Options",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (note.isPinned) "Unpin" else "Pin") },
                                onClick = { showMenu = false; onTogglePin() }
                            )
                            DropdownMenuItem(
                                text = { Text("Move to notebook…") },
                                leadingIcon = { Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(18.dp)) },
                                onClick = { showMenu = false; onMove() }
                            )
                            DropdownMenuItem(
                                text = { Text("Share") },
                                leadingIcon = { Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp)) },
                                onClick = { showMenu = false; onExport() }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                onClick = { showMenu = false; onDelete() }
                            )
                        }
                    }
                }
            }

            if (parsedPreview.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = parsedPreview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (noteTags.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    noteTags.forEach { tag ->
                        SuggestionChip(
                            onClick = {},
                            label = { Text(tag, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        }
    }
}

private val TEXT_BLOCK_TYPES = setOf(
    "text", "todo", "checklist", "header",
    "bullet_list", "numbered_list", "quote", "code"
)

private fun parseNoteContent(json: String): String {
    if (json.isBlank()) return ""
    return try {
        val array = org.json.JSONArray(json)
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
