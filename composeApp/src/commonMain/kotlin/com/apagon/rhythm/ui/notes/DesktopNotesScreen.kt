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
import com.apagon.rhythm.ui.components.crystalFabContainerColor
import com.apagon.rhythm.ui.components.crystalFabContentColor
import com.apagon.rhythm.ui.components.crystalFabElevation
import com.apagon.rhythm.ui.components.crystalFabSurface
import com.apagon.rhythm.ui.components.crystalScaffoldColor
import com.apagon.rhythm.ui.components.crystalScaffoldContentColor
import com.apagon.rhythm.ui.components.crystalTopAppBarColors
import com.apagon.rhythm.ui.components.crystalTileSurface
import com.apagon.rhythm.ui.theme.resolveDisplayColor
import com.apagon.rhythm.ui.util.RhythmDropdownMenu
import org.koin.compose.viewmodel.koinViewModel

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
                onNavigateToNote = { noteId, notebookId, template -> navState = NotesNavState.Editor(noteId, notebookId, template) }
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
            Text("← All notebooks")
        }
        LazyColumnRailItems(
            notebooks = notebooksWithCount,
            selectedNotebookId = selectedNotebookId,
            onSelectNotebook = onSelectNotebook
        )
    }
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
        items(notebooks, key = { it.notebook.id }) { item ->
            val isSelected = item.notebook.id == selectedNotebookId
            Row(
                modifier = Modifier
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
 * fires; no icons library, text/glyph buttons throughout.
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
                    TextButton(onClick = onNavigateToSearch) { Text("⌕") }
                },
                colors = crystalTopAppBarColors()
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddNotebook = true },
                modifier = Modifier.crystalFabSurface(),
                containerColor = crystalFabContainerColor(),
                contentColor = crystalFabContentColor(),
                elevation = crystalFabElevation()
            ) {
                Text("+", style = MaterialTheme.typography.headlineSmall)
            }
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
                items(notebooksWithCount, key = { it.notebook.id }) { item ->
                    DesktopNotebookCard(
                        notebook = item.notebook,
                        noteCount = item.count,
                        onClick = { onNavigateToNotebook(item.notebook.id) },
                        onEdit = { editingNotebook = item.notebook },
                        onDelete = { viewModel.deleteNotebook(item.notebook) }
                    )
                }
            }
        }
    }

    if (showAddNotebook) {
        DesktopAddNotebookSheet(
            onDismiss = { showAddNotebook = false },
            onSave = { name, colorIdx, argb -> viewModel.addNotebook(name, colorIdx, argb) }
        )
    }

    editingNotebook?.let { notebook ->
        DesktopAddNotebookSheet(
            initialNotebook = notebook,
            onDismiss = { editingNotebook = null },
            onSave = { name, colorIdx, argb ->
                viewModel.updateNotebook(notebook.copy(name = name, colorIndex = colorIdx, colorArgb = argb))
            }
        )
    }
}

@Composable
private fun DesktopNotebookCard(
    notebook: Notebook,
    noteCount: Int,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val displayColor = resolveDisplayColor(notebook.colorIndex, notebook.colorArgb)
    var showMenu by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth().crystalCardSurface().clickable(onClick = onClick)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(displayColor.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📓", style = MaterialTheme.typography.titleMedium)
                }
                Box {
                    TextButton(onClick = { showMenu = true }) { Text("⋮") }
                    RhythmDropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(text = { Text("Edit") }, onClick = { showMenu = false; onEdit() })
                        DropdownMenuItem(text = { Text("Delete", color = MaterialTheme.colorScheme.error) }, onClick = { showMenu = false; onDelete() })
                    }
                }
            }
            Text(text = notebook.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                text = if (noteCount == 1) "1 note" else "$noteCount notes",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
