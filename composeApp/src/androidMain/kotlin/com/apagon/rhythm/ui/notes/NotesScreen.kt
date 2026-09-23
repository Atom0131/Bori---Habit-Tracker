package com.apagon.rhythm.ui.notes

import org.koin.compose.viewmodel.koinViewModel

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.data.model.Notebook
import com.apagon.rhythm.ui.util.ProPaywallSheet
import androidx.compose.ui.platform.LocalContext
import com.apagon.rhythm.ui.util.findActivity
import com.apagon.rhythm.ui.util.resolveDisplayColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    viewModel: NotesViewModel = koinViewModel(),
    onNavigateToSettings: () -> Unit = {},
    onNavigateToNotebook: (Long) -> Unit = {},
    onNavigateToSearch: () -> Unit = {}
) {
    val notebooksWithCount by viewModel.notebooksWithCount.collectAsState()
    val isPro by viewModel.isPro.collectAsState()
    var showAddNotebook by remember { mutableStateOf(false) }
    var editingNotebook by remember { mutableStateOf<Notebook?>(null) }
    var showPaywall by remember { mutableStateOf(false) }
    var paywallReason by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Notes",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSearch) {
                        Icon(Icons.Default.Search, contentDescription = "Search notes")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (!isPro && notebooksWithCount.size >= 3) {
                        paywallReason = "Upgrade to Pro to create more than 3 Notebooks!"
                        showPaywall = true
                    } else {
                        showAddNotebook = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Notebook")
            }
        }
    ) { innerPadding ->
        if (notebooksWithCount.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Organize your life with Notebooks",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Tap + to create your first one",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(notebooksWithCount, key = { it.notebook.id }) { item ->
                    NotebookCard(
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
        AddNotebookSheet(
            onDismiss = { showAddNotebook = false },
            onSave = { name, colorIdx, argb ->
                viewModel.addNotebook(name, colorIdx, argb)
            }
        )
    }

    editingNotebook?.let { notebook ->
        AddNotebookSheet(
            initialNotebook = notebook,
            onDismiss = { editingNotebook = null },
            onSave = { name, colorIdx, argb ->
                viewModel.updateNotebook(notebook.copy(
                    name = name,
                    colorIndex = colorIdx,
                    colorArgb = argb,
                    updatedAt = System.currentTimeMillis()
                ))
            }
        )
    }

    val context = LocalContext.current
    if (showPaywall) {
        ProPaywallSheet(
            reason = paywallReason,
            onDismiss = {
                showPaywall = false
                paywallReason = null
            },
            onUpgrade = { productId ->
                viewModel.startBillingFlow(productId)
                showPaywall = false
                paywallReason = null
            }
        )
    }
}

@Composable
private fun NotebookCard(
    notebook: Notebook,
    noteCount: Int,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val displayColor = resolveDisplayColor(notebook.colorIndex, notebook.colorArgb)
    var showMenu by remember { mutableStateOf(false) }

    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(displayColor.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Book,
                        contentDescription = null,
                        tint = displayColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
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
                            text = { Text("Edit") },
                            onClick = { showMenu = false; onEdit() }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            onClick = { showMenu = false; onDelete() }
                        )
                    }
                }
            }
            Text(
                text = notebook.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (noteCount == 1) "1 note" else "$noteCount notes",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
