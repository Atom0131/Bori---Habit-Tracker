package com.apagon.rhythm.ui.notes

import org.koin.compose.viewmodel.koinViewModel

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesSearchScreen(
    viewModel: NotesSearchViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToNote: (noteId: Long, notebookId: Long) -> Unit
) {
    val query by viewModel.query.collectAsState()
    val results by viewModel.results.collectAsState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    BasicTextField(
                        value = query,
                        onValueChange = { viewModel.setQuery(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        singleLine = true,
                        decorationBox = { inner ->
                            if (query.isEmpty()) {
                                Text(
                                    "Search all notes…",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            inner()
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        val searchState = when {
            query.length < 2  -> "short"
            results.isEmpty() -> "empty"
            else              -> "results"
        }
        AnimatedContent(
            targetState = searchState,
            transitionSpec = { fadeIn(tween(250, easing = EaseInOut)) togetherWith fadeOut(tween(200, easing = EaseInOut)) },
            label = "searchState"
        ) { state ->
        when (state) {
            "short" -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Search across all your notebooks",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            "empty" -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No notes found for \"$query\"",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    items(results, key = { it.note.id }) { result ->
                        SearchResultCard(
                            result = result,
                            onClick = { onNavigateToNote(result.note.id, result.note.notebookId) }
                        )
                    }
                }
            }
        }
        } // end AnimatedContent
    }
}

@Composable
private fun SearchResultCard(result: NoteSearchResult, onClick: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                    SuggestionChip(
                        onClick = {},
                        label = {
                            Text(
                                result.notebookName,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }
            Text(
                text = dateFormat.format(Date(result.note.updatedAt)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val preview = remember(result.note.content) { parseNotePreviewForSearch(result.note.content) }
            if (preview.isNotBlank()) {
                Text(
                    text = preview,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun parseNotePreviewForSearch(json: String): String {
    if (json.isBlank()) return ""
    return try {
        val array = org.json.JSONArray(json)
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
