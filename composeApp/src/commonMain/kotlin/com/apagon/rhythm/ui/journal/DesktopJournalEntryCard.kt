package com.apagon.rhythm.ui.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.core.time.*
import com.apagon.rhythm.data.model.Habit
import com.apagon.rhythm.data.model.JournalEntry
import com.apagon.rhythm.platform.ImageBitmapLoader
import com.apagon.rhythm.ui.components.crystalCardSurface
import com.apagon.rhythm.ui.theme.resolveDisplayColor
import kotlin.time.Instant
import org.koin.compose.koinInject
import com.apagon.rhythm.ui.components.crystalAssistChipColors
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.Icons

/**
 * Android's `RedactedJournalPreview`: grey bars roughly the shape of the hidden text. The text is
 * never composed while concealed (not blurred, not transparent), so it can't leak into a
 * screenshot or a screen reader; the caller passes lengths, not text.
 */
@Composable
fun RedactedJournalPreview(hasTitle: Boolean, contentLength: Int, photoCount: Int) {
    val barColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)

    @Composable
    fun Bar(fraction: Float, height: androidx.compose.ui.unit.Dp) {
        Box(Modifier.fillMaxWidth(fraction).height(height).clip(RoundedCornerShape(4.dp)).background(barColor))
    }

    if (photoCount > 0) {
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(photoCount.coerceAtMost(4)) {
                Box(Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(8.dp)).background(barColor))
            }
            repeat(4 - photoCount.coerceAtMost(4)) { Spacer(Modifier.weight(1f)) }
        }
    }
    if (hasTitle) {
        Spacer(Modifier.height(10.dp))
        Bar(fraction = 0.55f, height = 14.dp)
    }
    if (contentLength > 0) {
        val lines = ((contentLength + 39) / 40).coerceIn(1, 3)
        Spacer(Modifier.height(if (hasTitle) 8.dp else 10.dp))
        repeat(lines) { index ->
            if (index > 0) Spacer(Modifier.height(6.dp))
            val fraction = if (index == lines - 1) {
                val remainder = contentLength % 40
                if (remainder == 0) 0.9f else (0.35f + (remainder / 40f) * 0.55f)
            } else 1f
            Bar(fraction = fraction, height = 10.dp)
        }
    }
    if (!hasTitle && contentLength == 0 && photoCount == 0) {
        Spacer(Modifier.height(10.dp))
        Bar(fraction = 0.4f, height = 10.dp)
    }
    Spacer(Modifier.height(4.dp))
}

/**
 * Port of Android's `JournalEntryCard`. [concealed] (the default, as on Android) shows
 * [RedactedJournalPreview] instead of the entry; [locked] only adds the lock glyph by the time —
 * opening a locked entry is gated by the screen. Photos go through ImageBitmapLoader.
 */
@Composable
fun DesktopJournalEntryCard(
    entry: JournalEntry,
    habits: List<Habit>,
    concealed: Boolean,
    locked: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val imageLoader = koinInject<ImageBitmapLoader>()
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

    Box(
        modifier = modifier.fillMaxWidth().crystalCardSurface().clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = time, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (locked) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "Locked",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            if (concealed) {
                RedactedJournalPreview(
                    hasTitle = entry.title.isNotBlank(),
                    contentLength = entry.content.length,
                    photoCount = photos.size
                )
            } else {
                if (photos.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        photos.take(4).forEach { path ->
                            imageLoader.LoadedImage(
                                path = path,
                                modifier = Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                if (entry.title.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(text = entry.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                if (entry.content.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = entry.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3
                    )
                }

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
                            SuggestionChip(colors = crystalAssistChipColors(), onClick = {}, label = { Text(label, style = MaterialTheme.typography.labelSmall) })
                        }
                        tags.forEach { tag ->
                            SuggestionChip(colors = crystalAssistChipColors(), onClick = {}, label = { Text("#$tag", style = MaterialTheme.typography.labelSmall) })
                        }
                        linkedHabit?.let { habit ->
                            val habitColor = resolveDisplayColor(habit.colorIndex, habit.colorArgb)
                            AssistChip(
                                colors = crystalAssistChipColors(),
                                onClick = {},
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.size(8.dp).clip(CircleShape).background(habitColor))
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
