package com.apagon.rhythm.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.apagon.rhythm.ui.theme.resolveDisplayColor
import org.koin.compose.viewmodel.koinViewModel

/**
 * Stage 10's Notes tab entry point — a small local nav state (list ↔
 * notebook detail ↔ note editor ↔ search), matching this project's existing
 * showX/selectedX toggle pattern (DesktopHabitScreen's selectedHabitId, Stage
 * 9) rather than a real navigation library, which is Stage 11+ territory.
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
        is NotesNavState.Detail -> DesktopNotebookDetailScreen(
            notebookId = state.notebookId,
            onNavigateBack = { navState = NotesNavState.List },
            onNavigateToNote = { noteId, notebookId, template -> navState = NotesNavState.Editor(noteId, notebookId, template) }
        )
        is NotesNavState.Editor -> DesktopNoteEditorScreen(
            noteId = state.noteId,
            notebookId = state.notebookId,
            template = state.template,
            onNavigateBack = { navState = NotesNavState.Detail(state.notebookId) }
        )
        is NotesNavState.Search -> DesktopNotesSearchScreen(
            onNavigateBack = { navState = NotesNavState.List },
            onNavigateToNote = { noteId, notebookId -> navState = NotesNavState.Editor(noteId, notebookId, null) }
        )
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
        topBar = {
            TopAppBar(
                title = { Text("Notes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) },
                actions = {
                    TextButton(onClick = onNavigateToSearch) { Text("⌕") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddNotebook = true }) {
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

    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
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
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
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
