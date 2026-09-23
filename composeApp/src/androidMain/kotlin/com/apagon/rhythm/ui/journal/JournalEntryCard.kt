package com.apagon.rhythm.ui.journal
import com.apagon.rhythm.core.time.*

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.apagon.rhythm.data.model.Habit
import com.apagon.rhythm.data.model.JournalEntry
import com.apagon.rhythm.ui.util.*
import kotlin.time.Instant
import com.apagon.rhythm.core.time.ZoneId
import com.apagon.rhythm.core.time.DateTimeFormatter

val journalFeelings = listOf(
    "Happy" to "😄",
    "Grateful" to "🙏",
    "Anxious" to "😰",
    "Excited" to "🎉",
    "Calm" to "😌",
    "Tired" to "😴",
    "Proud" to "💪",
    "Focused" to "🎯"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JournalEntryCard(
    entry: JournalEntry,
    habits: List<Habit>,
    isLocked: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val time = remember(entry.createdAt) {
        Instant.ofEpochMilli(entry.createdAt)
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
            .format(DateTimeFormatter.ofPattern("h:mm a"))
    }
    val tags = remember(entry.tags) { entry.tagList() }
    val photos = remember(entry.photoUris) { entry.photoUriList() }
    val feelings = remember(entry.feelings) { entry.feelingList() }
    val linkedHabit = remember(entry.habitId, habits) {
        habits.firstOrNull { it.id == entry.habitId }
    }

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            // Time row (always visible)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = time,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isLocked) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "Locked",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            if (isLocked) {
                Spacer(Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "Content locked · Tap to unlock",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(Modifier.height(4.dp))
            } else {
                // Photo grid (up to 4)
                if (photos.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        photos.take(4).forEach { uri ->
                            AsyncImage(
                                model = uri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        }
                    }
                }

                // Title
                if (entry.title.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Body
                if (entry.content.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = entry.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3
                    )
                }

                // Chips
                val hasChips = feelings.isNotEmpty() || tags.isNotEmpty() || linkedHabit != null
                if (hasChips) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        feelings.forEach { feeling ->
                            val pair = journalFeelings.firstOrNull { it.first == feeling }
                            val label = if (pair != null) "${pair.second} ${pair.first}" else feeling
                            SuggestionChip(
                                onClick = {},
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                        tags.forEach { tag ->
                            SuggestionChip(
                                onClick = {},
                                label = { Text("#$tag", style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                        linkedHabit?.let { habit ->
                            val habitColor = resolveDisplayColor(habit.colorIndex, habit.colorArgb)
                            AssistChip(
                                onClick = {},
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(habitColor)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(habit.name, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
