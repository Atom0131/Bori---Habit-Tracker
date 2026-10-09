package com.apagon.rhythm.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.data.model.Notebook
import com.apagon.rhythm.ui.components.crystalCardSurface
import com.apagon.rhythm.ui.components.crystalScaffoldColor
import com.apagon.rhythm.ui.components.crystalScaffoldContentColor
import com.apagon.rhythm.ui.components.crystalTopAppBarColors
import com.apagon.rhythm.ui.components.CrystalIconButton
import com.apagon.rhythm.ui.components.crystalTileSurface
import com.apagon.rhythm.ui.theme.resolveDisplayColor
import com.apagon.rhythm.ui.util.RhythmAddFab
import com.apagon.rhythm.ui.util.RhythmDropdownMenu
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.graphics.luminance
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import com.apagon.rhythm.ui.util.RhythmAlertDialog
import com.apagon.rhythm.ui.util.legibleMarkerOn
import com.apagon.rhythm.ui.util.NotebookIcon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.automirrored.filled.ArrowBack

/**
 * Stage 10's Notes tab entry point — a small local nav state (list ↔
 * notebook detail ↔ note editor ↔ search), matching this project's existing
 * showX/selectedX toggle pattern (DesktopHabitScreen's selectedHabitId, Stage
 * 9) rather than a real navigation library, which is Stage 11+ territory.
 *
 * Stage 15c: once inside a notebook (Detail/Editor), the notebook list stays
 * visible as a persistent left rail instead of being replaced by the
 * notebook's content — the master-detail pattern research turned up as
 * standard wherever an app has this exact list-of-things + one-thing's-
 * content shape (Notion's page tree, Asana's task list + detail pane). The
 * List state itself (browsing/creating notebooks) stays the full-width grid
 * unchanged — it's the entry point, not a detail view, so there's nothing to
 * show a detail pane against yet.
 */
private sealed class NotesNavState {
    object List : NotesNavState()
    data class Detail(val notebookId: Long) : NotesNavState()
    data class Editor(val noteId: Long, val notebookId: Long, val template: String?) : NotesNavState()
    object Search : NotesNavState()
}

@Composable
fun DesktopNotesTab() {
    var navState by remember { mutableStateOf<NotesNavState>(NotesNavState.List) }

    when (val state = navState) {
        is NotesNavState.List -> DesktopNotesScreen(
            onNavigateToNotebook = { navState = NotesNavState.Detail(it) },
            onNavigateToSearch = { navState = NotesNavState.Search }
        )
        is NotesNavState.Detail -> NotesWithRail(
            selectedNotebookId = state.notebookId,
            onSelectNotebook = { navState = NotesNavState.Detail(it) },
            onExitToList = { navState = NotesNavState.List }
        ) {
            DesktopNotebookDetailScreen(
                notebookId = state.notebookId,
                onNavigateBack = { navState = NotesNavState.List },
                onNavigateToNote = { noteId, notebookId, template -> navState = NotesNavState.Editor(noteId, notebookId, template) },
                onNavigateToNotebook = { navState = NotesNavState.Detail(it) }
            )
        }
        is NotesNavState.Editor -> NotesWithRail(
            selectedNotebookId = state.notebookId,
            onSelectNotebook = { navState = NotesNavState.Detail(it) },
            onExitToList = { navState = NotesNavState.List }
        ) {
            DesktopNoteEditorScreen(
                noteId = state.noteId,
                notebookId = state.notebookId,
                template = state.template,
                onNavigateBack = { navState = NotesNavState.Detail(state.notebookId) }
            )
        }
        is NotesNavState.Search -> DesktopNotesSearchScreen(
            onNavigateBack = { navState = NotesNavState.List },
            onNavigateToNote = { noteId, notebookId -> navState = NotesNavState.Editor(noteId, notebookId, null) }
        )
    }
}

/** The Stage 15c split-pane shell: a persistent notebook rail on the left, [content] (either the
 * notebook's note list or the note editor) filling the rest. */
@Composable
private fun NotesWithRail(
    selectedNotebookId: Long,
    onSelectNotebook: (Long) -> Unit,
    onExitToList: () -> Unit,
    content: @Composable () -> Unit
) {
    Row(modifier = Modifier.fillMaxSize()) {
        NotebookRail(
            selectedNotebookId = selectedNotebookId,
            onSelectNotebook = onSelectNotebook,
            onExitToList = onExitToList,
            modifier = Modifier.width(240.dp).fillMaxHeight()
        )
        VerticalDivider()
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) { content() }
    }
}

@Composable
private fun NotebookRail(
    selectedNotebookId: Long,
    onSelectNotebook: (Long) -> Unit,
    onExitToList: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NotesViewModel = koinViewModel()
) {
    val notebooksWithCount by viewModel.notebooksWithCount.collectAsState()

    Column(modifier = modifier.padding(vertical = 8.dp)) {
        TextButton(onClick = onExitToList, modifier = Modifier.padding(horizontal = 8.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("All notebooks")
        }
        LazyColumnRailItems(
            notebooks = notebooksWithCount,
            selectedNotebookId = selectedNotebookId,
            onSelectNotebook = onSelectNotebook
        )
    }
}

/** Notebooks in tree order (each followed by its children) with their depth. A notebook whose
 * parent is missing (deleted, or private on the other device) shows at the top level. */
private fun List<NotebookWithCount>.inTreeOrder(): List<Pair<NotebookWithCount, Int>> {
    val ids = map { it.notebook.id }.toSet()
    val childrenOf = groupBy { it.notebook.parentId?.takeIf { p -> p in ids } }
    val out = mutableListOf<Pair<NotebookWithCount, Int>>()
    val seen = mutableSetOf<Long>()
    fun walk(parent: Long?, depth: Int) {
        childrenOf[parent].orEmpty().forEach { item ->
            if (seen.add(item.notebook.id)) { out += item to depth; walk(item.notebook.id, depth + 1) }
        }
    }
    walk(null, 0)
    // Anything left (a parent cycle) still gets listed rather than vanishing.
    filter { it.notebook.id !in seen }.forEach { out += it to 0 }
    return out
}

@Composable
private fun LazyColumnRailItems(
    notebooks: List<NotebookWithCount>,
    selectedNotebookId: Long,
    onSelectNotebook: (Long) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(notebooks.inTreeOrder(), key = { it.first.notebook.id }) { (item, depth) ->
            val isSelected = item.notebook.id == selectedNotebookId
            Row(
                modifier = Modifier
                    .padding(start = (depth * 16).dp)
                    .fillMaxWidth()
                    .then(
                        if (isSelected) Modifier.crystalTileSurface(fill = MaterialTheme.colorScheme.secondaryContainer)
                        else Modifier
                    )
                    .clickable { onSelectNotebook(item.notebook.id) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    NotebookIcon,
                    contentDescription = null,
                    tint = resolveDisplayColor(item.notebook.colorIndex, item.notebook.colorArgb)
                        .legibleMarkerOn(MaterialTheme.colorScheme.surface.luminance() < 0.5f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = item.notebook.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = item.count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Desktop port of NotesScreen.kt — the Pro paywall is dropped entirely
 * (desktop is unconditionally Pro), so the notebook-count FAB gate never
 * fires.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopNotesScreen(
    viewModel: NotesViewModel = koinViewModel(),
    onNavigateToNotebook: (Long) -> Unit,
    onNavigateToSearch: () -> Unit
) {
    val notebooksWithCount by viewModel.notebooksWithCount.collectAsState()
    var showAddNotebook by remember { mutableStateOf(false) }
    var editingNotebook by remember { mutableStateOf<Notebook?>(null) }

    Scaffold(
        containerColor = crystalScaffoldColor(),
        contentColor = crystalScaffoldContentColor(),
        topBar = {
            TopAppBar(
                title = { Text("Notes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) },
                actions = {
                    CrystalIconButton(icon = Icons.Default.Search, contentDescription = "Search", onClick = onNavigateToSearch)
                },
                colors = crystalTopAppBarColors()
            )
        },
        floatingActionButton = {
            RhythmAddFab(onClick = { showAddNotebook = true })
        }
    ) { innerPadding ->
        if (notebooksWithCount.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Organize your notes with Notebooks", style = MaterialTheme.typography.titleMedium)
                    Text("Tap + to create your first one", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // As Android: only top-level notebooks are peers in the grid; a nested one is reached
                // through its parent, like a folder.
                // Unlike Android, a notebook whose parent is gone (deleted, or private on the other
                // device) stays reachable here instead of disappearing.
                val presentIds = notebooksWithCount.map { it.notebook.id }.toSet()
                items(notebooksWithCount.filter { it.notebook.parentId == null || it.notebook.parentId !in presentIds }, key = { it.notebook.id }) { item ->
                    DesktopNotebookCard(
                        notebook = item.notebook,
                        noteCount = item.count,
                        onClick = { onNavigateToNotebook(item.notebook.id) },
                        onEdit = { editingNotebook = item.notebook },
                        onDelete = { viewModel.deleteNotebook(item.notebook) },
                        onTogglePrivate = { viewModel.setNotebookPrivate(item.notebook, !item.notebook.isPrivate) }
                    )
                }
            }
        }
    }

    if (showAddNotebook) {
        DesktopAddNotebookSheet(
            onDismiss = { showAddNotebook = false },
            allNotebooks = notebooksWithCount.map { it.notebook },
            onSave = { name, colorIdx, argb, parentId -> viewModel.addNotebook(name, colorIdx, argb, parentId) }
        )
    }

    editingNotebook?.let { notebook ->
        DesktopAddNotebookSheet(
            initialNotebook = notebook,
            allNotebooks = notebooksWithCount.map { it.notebook },
            onDismiss = { editingNotebook = null },
            onSave = { name, colorIdx, argb, parentId ->
                viewModel.updateNotebook(notebook.copy(name = name, colorIndex = colorIdx, colorArgb = argb, parentId = parentId))
            }
        )
    }
}

/** Android's `NotebookCard` (`NotesScreen.kt`): the notebook glyph in the notebook's colour, made
 * legible, on a soft disc of the same colour; overflow menu with Edit, private lock and Delete
 * (behind a confirm); a lock beside the name when private. */
@Composable
private fun DesktopNotebookCard(
    notebook: Notebook,
    noteCount: Int,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTogglePrivate: () -> Unit
) {
    val displayColor = resolveDisplayColor(notebook.colorIndex, notebook.colorArgb)
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        RhythmAlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete notebook?") },
            text = {
                Text(
                    if (noteCount == 1) "\"${notebook.name}\" and the 1 note in it move to the Trash Bin, where they can be restored for 14 days."
                    else "\"${notebook.name}\" and the $noteCount notes in it move to the Trash Bin, where they can be restored for 14 days."
                )
            },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDelete() }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }

    Box(modifier = Modifier.fillMaxWidth().crystalCardSurface().clickable(onClick = onClick)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                val markerColor = displayColor.legibleMarkerOn(MaterialTheme.colorScheme.surface.luminance() < 0.5f)
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(markerColor.copy(alpha = 0.28f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(NotebookIcon, contentDescription = null, tint = markerColor, modifier = Modifier.size(22.dp))
                }
                Box {
                    CrystalIconButton(icon = Icons.Default.MoreVert, contentDescription = "Options", onClick = { showMenu = true }, size = 28.dp)
                    RhythmDropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(text = { Text("Edit") }, onClick = { showMenu = false; onEdit() })
                        DropdownMenuItem(
                            text = {
                                Icon(
                                    if (notebook.isPrivate) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = if (notebook.isPrivate) "Make public" else "Make private"
                                )
                            },
                            onClick = { showMenu = false; onTogglePrivate() }
                        )
                        DropdownMenuItem(text = { Text("Delete", color = MaterialTheme.colorScheme.error) }, onClick = { showMenu = false; showDeleteConfirm = true })
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (notebook.isPrivate) {
                    Icon(Icons.Default.Lock, contentDescription = "Private notebook", modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                }
                Text(text = notebook.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Text(
                text = if (noteCount == 1) "1 note" else "$noteCount notes",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
