package com.apagon.rhythm.ui.journal

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.ui.util.RhythmAlertDialog

/**
 * The Journal's feelings, laid out on the Mood Meter (Yale Center for Emotional Intelligence; the
 * chart How We Feel is built on): energy up/down, pleasantness left/right, four coloured zones.
 * 2026-11-12: 8 feelings became 40, ten per zone.
 *
 * **Keys are stored and synced as plain strings** (`JournalEntry.feelings`, a JSON list), so:
 * - The first eight keep their exact spelling and emoji. Entries written before this still show them.
 * - A key must never be renamed. An old entry would then show a bare word with no emoji or zone.
 * - Adding one is free. An older app or desktop that receives an unknown key shows it as plain text.
 *
 * Android keeps the same list in `ui/journal/JournalFeelings.kt`; the two must match.
 */
enum class FeelingZone(val label: String, val hint: String, val color: Color) {
    RED("Stressed", "High energy · unpleasant", Color(0xFFE5534B)),
    YELLOW("Excited", "High energy · pleasant", Color(0xFFF2B630)),
    BLUE("Down", "Low energy · unpleasant", Color(0xFF4A8FD9)),
    GREEN("Calm", "Low energy · pleasant", Color(0xFF3FB47E))
}

data class Feeling(val key: String, val emoji: String, val zone: FeelingZone)

val journalFeelingList: List<Feeling> = buildList {
    fun z(zone: FeelingZone, vararg f: Pair<String, String>) = f.forEach { add(Feeling(it.first, it.second, zone)) }
    z(
        FeelingZone.YELLOW,
        "Excited" to "🎉", "Happy" to "😄", "Proud" to "💪", "Focused" to "🎯", "Joyful" to "😁",
        "Inspired" to "✨", "Energized" to "⚡", "Hopeful" to "🌅", "Confident" to "😎", "Motivated" to "🚀"
    )
    z(
        FeelingZone.RED,
        "Anxious" to "😰", "Stressed" to "😫", "Angry" to "😠", "Frustrated" to "😤", "Overwhelmed" to "🤯",
        "Nervous" to "😬", "Irritated" to "😒", "Worried" to "😟", "Embarrassed" to "😳", "Restless" to "🌀"
    )
    z(
        FeelingZone.BLUE,
        "Tired" to "😴", "Sad" to "😢", "Lonely" to "🥺", "Bored" to "🥱", "Disappointed" to "😞",
        "Discouraged" to "😔", "Hurt" to "💔", "Drained" to "🪫", "Numb" to "😶", "Guilty" to "😣"
    )
    z(
        FeelingZone.GREEN,
        "Grateful" to "🙏", "Calm" to "😌", "Content" to "🙂", "Relaxed" to "😊", "Peaceful" to "🕊️",
        "Loved" to "🥰", "Relieved" to "😮‍💨", "Rested" to "🛌", "Safe" to "🤗", "Balanced" to "⚖️"
    )
}

private val feelingByKey = journalFeelingList.associateBy { it.key }

/** Key → emoji pairs, the shape the cards and editors have always read. */
val journalFeelings: List<Pair<String, String>> = journalFeelingList.map { it.key to it.emoji }

fun feelingFor(key: String): Feeling? = feelingByKey[key]

/** "😄 Happy", or the bare key for a word this version doesn't know. */
fun feelingLabel(key: String): String = feelingFor(key)?.let { "${it.emoji} ${it.key}" } ?: key

/** A picked feeling, tinted with its zone. Tapping it calls [onClick] (remove, or reopen the picker). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeelingChip(key: String, onClick: () -> Unit) {
    val zone = feelingFor(key)?.zone
    FilterChip(
        selected = true,
        onClick = onClick,
        label = { Text(feelingLabel(key)) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = (zone?.color ?: MaterialTheme.colorScheme.primary).copy(alpha = 0.28f),
            selectedLabelColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(1.dp, (zone?.color ?: MaterialTheme.colorScheme.primary).copy(alpha = 0.6f))
    )
}

/**
 * The Mood Meter picker: four zones, then that zone's words. Several feelings can be picked across
 * zones; [selected] is the editor's own list and is changed in place, so the editor's autosave sees
 * every pick the same way it saw the old chip row.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodMeterSheet(selected: MutableList<String>, onDismiss: () -> Unit) {
    var zone by remember { mutableStateOf<FeelingZone?>(null) }
    // A dialog on the desktop, where Android uses a bottom sheet; the content is the same.
    RhythmAlertDialog(
        onDismissRequest = onDismiss,
        maxWidth = 640.dp,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done", fontWeight = FontWeight.SemiBold) } },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (zone != null) {
                    IconButton(onClick = { zone = null }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to the zones")
                    }
                }
                Column {
                    Text(zone?.label ?: "How do you feel?", fontWeight = FontWeight.SemiBold)
                    Text(
                        zone?.hint ?: "Pick the square that fits, then the words",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                val current = zone
                if (current == null) {
                    Text("↑ More energy", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    listOf(
                        listOf(FeelingZone.RED, FeelingZone.YELLOW),
                        listOf(FeelingZone.BLUE, FeelingZone.GREEN)
                    ).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { z ->
                                ZoneTile(
                                    zone = z,
                                    picked = selected.count { feelingFor(it)?.zone == z },
                                    onClick = { zone = z },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                    Text(
                        "More pleasant →",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    journalFeelingList.filter { it.zone == current }.chunked(2).forEach { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            pair.forEach { f ->
                                val on = f.key in selected
                                FilterChip(
                                    selected = on,
                                    onClick = { if (on) selected.remove(f.key) else selected.add(f.key) },
                                    label = { Text("${f.emoji}  ${f.key}", style = MaterialTheme.typography.bodyLarge) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = current.color.copy(alpha = 0.32f),
                                        selectedLabelColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    border = BorderStroke(
                                        if (on) 1.5.dp else 1.dp,
                                        current.color.copy(alpha = if (on) 0.9f else 0.35f)
                                    ),
                                    modifier = Modifier.weight(1f).height(44.dp)
                                )
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
        }
    )
}

@Composable
private fun ZoneTile(zone: FeelingZone, picked: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = MaterialTheme.shapes.large
    Column(
        modifier
            .height(120.dp)
            .clip(shape)
            .background(zone.color.copy(alpha = 0.26f))
            .border(1.dp, zone.color.copy(alpha = 0.6f), shape)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(zone.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(zone.hint, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                journalFeelingList.filter { it.zone == zone }.take(3).joinToString(" ") { it.emoji },
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            if (picked > 0) {
                Box(
                    Modifier.clip(MaterialTheme.shapes.small).background(zone.color.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("$picked", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
