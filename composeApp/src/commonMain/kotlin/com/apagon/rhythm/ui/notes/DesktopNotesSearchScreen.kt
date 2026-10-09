package com.apagon.rhythm.ui.notes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import com.apagon.rhythm.core.json.JSONArray
import com.apagon.rhythm.core.time.*
import com.apagon.rhythm.ui.components.crystalCardSurface
import com.apagon.rhythm.ui.components.crystalScaffoldColor
import com.apagon.rhythm.ui.components.crystalScaffoldContentColor
import com.apagon.rhythm.ui.components.crystalTopAppBarColors
import com.apagon.rhythm.ui.components.crystalTextFieldColors
import com.apagon.rhythm.ui.components.crystalTextFieldShape
import kotlin.time.Instant
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close

/**
 * Desktop port of NotesSearchScreen.kt. Drops androidx.compose.animation's
 * AnimatedContent cross-fade between search states (short/empty/results) for
 * a plain conditional swap — same reasoning as DesktopJournalWeekStrip
 * (Stage 9): not worth chasing an animation for a state swap this simple, and
 * simpler to keep consistent across the port than to special-case reintroduce
 * animation here just because it happens to resolve on the classpath.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopNotesSearchScreen(
    viewModel: NotesSearchViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToNote: (noteId: Long, notebookId: Long) -> Unit
) {
    val query by viewModel.query.collectAsState()
    val results by viewModel.results.collectAsState()

    Scaffold(
        containerColor = crystalScaffoldColor(),
        contentColor = crystalScaffoldContentColor(),
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { viewModel.setQuery(it) },
                        placeholder = { Text("Search all notes...") },
                        singleLine = true,
                        colors = crystalTextFieldColors(),
                        shape = crystalTextFieldShape(),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                navigationIcon = { TextButton(onClick = onNavigateBack) { Text("← Back") } },
                actions = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setQuery("") }) { Icon(Icons.Default.Close, contentDescription = "Clear search") }
                    }
                },
                colors = crystalTopAppBarColors()
            )
        }
    ) { innerPadding ->
        when {
            query.length < 2 -> {
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    Text("Search across all your notebooks", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            results.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    Text("No notes found for \"$query\"", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            "${results.size} result${if (results.size == 1) "" else "s"}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(results, key = { it.note.id }) { result ->
                        DesktopSearchResultCard(result = result, onClick = { onNavigateToNote(result.note.id, result.note.notebookId) })
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopSearchResultCard(result: NoteSearchResult, onClick: () -> Unit) {
    val dateLabel = remember(result.note.updatedAt) {
        Instant.ofEpochMilli(result.note.updatedAt).atZone(ZoneId.systemDefault()).toLocalDate()
            .format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    }
    Box(modifier = Modifier.fillMaxWidth().crystalCardSurface().clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = result.note.title.ifBlank { "Untitled Note" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (result.notebookName.isNotBlank()) {
                    Spacer(Modifier.width(8.dp))
                    SuggestionChip(onClick = {}, label = { Text(result.notebookName, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis) })
                }
            }
            Text(text = dateLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val preview = remember(result.note.content) { parseNotePreviewForSearch(result.note.content) }
            if (preview.isNotBlank()) {
                Text(text = preview, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

private fun parseNotePreviewForSearch(json: String): String {
    if (json.isBlank()) return ""
    return try {
        val array = JSONArray(json)
        val sb = StringBuilder()
        val textTypes = setOf("text", "checklist", "header", "bullet_list", "numbered_list", "quote", "code")
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val type = obj.optString("type", "text").lowercase()
            val content = obj.optString("content", "")
            if (type in textTypes && content.isNotBlank()) {
                if (sb.isNotEmpty()) sb.append(" ")
                sb.append(content)
                if (sb.length > 120) break
            }
        }
        sb.toString()
    } catch (e: Exception) { "" }
}
