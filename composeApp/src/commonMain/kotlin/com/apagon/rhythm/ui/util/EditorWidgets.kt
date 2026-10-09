package com.apagon.rhythm.ui.util

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButtonColors
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.ui.components.crystalButtonColors
import com.apagon.rhythm.ui.components.crystalCardSurface
import com.apagon.rhythm.ui.components.crystalChipSurface
import com.apagon.rhythm.ui.components.crystalControlColor
import com.apagon.rhythm.ui.components.crystalControlElevation
import com.apagon.rhythm.ui.components.crystalControlSurface
import com.apagon.rhythm.ui.components.crystalSelectedChipColor
import com.apagon.rhythm.ui.components.crystalSelectedChipContentColor
import com.apagon.rhythm.ui.components.isCrystal

/*
 * The building blocks every Android editor sheet is made of (`ui/util/SharedComposables.kt`),
 * ported so the desktop editors have the same shape: a big bold title, labels *above* glass
 * fields, related controls grouped into glass panels, and a glass primary button.
 */

/** Android's `EditorialTitle`: the sheet's big bold title. */
@Composable
fun EditorialTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
    )
}

/** Android's `FluidTextField`: a small uppercase-style label above a borderless glass field. */
@Composable
fun FluidTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (label.isNotBlank()) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(modifier = Modifier.fillMaxWidth().crystalControlSurface()) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                singleLine = singleLine,
                maxLines = if (singleLine) 1 else maxLines,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                textStyle = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

/** Android's `EditorSection`: a labelled glass panel grouping related controls. Panel bold, the
 * controls inside quiet. Outside Crystal it gets a hairline border instead of glass. */
@Composable
fun EditorSection(label: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val crystal = isCrystal()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .crystalCardSurface()
            .then(
                if (crystal) Modifier
                else Modifier.border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), MaterialTheme.shapes.large)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
    }
}

/** Android's `MomentumButton`: the editor's primary action, a glass pill with an accent label
 * under Crystal, a solid accent pill otherwise. */
@Composable
fun MomentumButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val crystal = isCrystal()
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        colors = crystalButtonColors(),
        modifier = modifier
            .height(56.dp)
            .then(if (crystal) Modifier.crystalChipSurface(MaterialTheme.colorScheme.surfaceContainerHighest) else Modifier)
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

/** Android's `PickerSummaryCard`: the chosen time or date shown large, with an icon; opens the
 * picker when clicked. */
@Composable
fun PickerSummaryCard(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.fillMaxWidth().crystalControlSurface()
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(icon, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Text(value, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

/** Formats a clock time the way Android's `formatTime` does. */
fun formatClockTime(hour: Int, minute: Int, is24Hour: Boolean): String =
    if (is24Hour) "%02d:%02d".format(hour, minute)
    else "%d:%02d %s".format(when { hour == 0 -> 12; hour > 12 -> hour - 12; else -> hour }, minute, if (hour < 12) "AM" else "PM")

/** Android's segmented-button colours (frequency, duration unit). */
@Composable
fun crystalSegmentedButtonColors(): SegmentedButtonColors = if (isCrystal()) {
    SegmentedButtonDefaults.colors(
        activeContainerColor = crystalSelectedChipColor(MaterialTheme.colorScheme.secondaryContainer),
        activeContentColor = MaterialTheme.colorScheme.primary,
        activeBorderColor = MaterialTheme.colorScheme.outlineVariant,
        inactiveContainerColor = crystalControlColor(MaterialTheme.colorScheme.surfaceContainerHighest),
        inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        inactiveBorderColor = MaterialTheme.colorScheme.outlineVariant
    )
} else {
    SegmentedButtonDefaults.colors()
}

/** Android's filter-chip colours ("Until end of year"). */
@Composable
fun crystalFilterChipColors(): SelectableChipColors = if (isCrystal()) {
    FilterChipDefaults.filterChipColors(
        containerColor = crystalControlColor(MaterialTheme.colorScheme.surfaceContainerHighest),
        selectedContainerColor = crystalSelectedChipColor(MaterialTheme.colorScheme.secondaryContainer),
        selectedLabelColor = MaterialTheme.colorScheme.primary,
        selectedLeadingIconColor = MaterialTheme.colorScheme.primary
    )
} else {
    FilterChipDefaults.filterChipColors()
}

/** Android's weekday circles. Bit 0 = Monday, as habits store it (alarms are Sunday-first). */
private val weekDayLabels = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")

@Composable
fun WeekDayPicker(mask: Int, onMaskChange: (Int) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        weekDayLabels.forEachIndexed { index, label ->
            val selected = (mask and (1 shl index)) != 0
            DayCircle(
                label = label,
                selected = selected,
                onClick = { onMaskChange(if (selected) mask and (1 shl index).inv() else mask or (1 shl index)) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** One circle of [WeekDayPicker] or the month-day grid. */
@Composable
fun DayCircle(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) crystalSelectedChipColor(MaterialTheme.colorScheme.primaryContainer)
        else crystalControlColor(MaterialTheme.colorScheme.surfaceContainerHighest),
        shadowElevation = crystalControlElevation(if (selected) 4.dp else 2.dp),
        tonalElevation = crystalControlElevation(if (selected) 2.dp else 1.dp),
        border = if (selected) null else BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier.aspectRatio(1f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) crystalSelectedChipContentColor(MaterialTheme.colorScheme.onPrimaryContainer)
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Android's `IconPickerButton`: a row showing the chosen icon (or "No icon selected") and
 * "Change", opening [IconPickerModalSheet]. [selectedIconIndex] -1 = none. */
@Composable
fun IconPickerButton(selectedIconIndex: Int, accentColor: Color, onIconSelected: (Int) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    val selectedEntry = habitIconLibrary.getOrNull(selectedIconIndex)
    Surface(
        onClick = { showPicker = true },
        shape = MaterialTheme.shapes.small,
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth().crystalControlSurface()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                if (selectedEntry != null) {
                    Icon(selectedEntry.second, contentDescription = selectedEntry.first, tint = accentColor, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(selectedEntry.first, style = MaterialTheme.typography.bodyLarge)
                } else {
                    Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("No icon selected", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text("Change", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
    if (showPicker) {
        IconPickerModalSheet(
            selectedIconIndex = selectedIconIndex,
            accentColor = accentColor,
            onIconSelected = { onIconSelected(it); showPicker = false },
            onDismiss = { showPicker = false }
        )
    }
}

/** Android's `IconPickerModalSheet`: searchable grid of every habit icon, Cancel / Select. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconPickerModalSheet(selectedIconIndex: Int, accentColor: Color, onIconSelected: (Int) -> Unit, onDismiss: () -> Unit) {
    val cellShape = MaterialTheme.shapes.small
    var tempSelectedIndex by remember { mutableIntStateOf(selectedIconIndex) }
    var searchQuery by remember { mutableStateOf("") }
    val displayIcons = remember(searchQuery) {
        habitIconLibrary.mapIndexed { i, p -> Triple(i, p.first, p.second) }.let { all ->
            if (searchQuery.isBlank()) all else all.filter { it.second.contains(searchQuery, ignoreCase = true) }
        }
    }
    RhythmSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.padding(bottom = 4.dp)) {
                Text("Pick an Icon", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Choose a symbol for your habit", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(
                shape = MaterialTheme.shapes.small,
                color = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth().crystalControlSurface()
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search icons...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (displayIcons.isEmpty()) {
                Text(
                    "No icons found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    textAlign = TextAlign.Center
                )
            } else {
                val unselectedBg = MaterialTheme.colorScheme.surfaceContainerHighest
                val onSurfaceColor = MaterialTheme.colorScheme.onSurface
                val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp)
                ) {
                    itemsIndexed(displayIcons, key = { _, t -> t.first }) { _, (originalIndex, label, icon) ->
                        val isSelected = tempSelectedIndex == originalIndex
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(cellShape)
                                .background(if (isSelected) accentColor else unselectedBg)
                                .then(if (!isSelected) Modifier.border(0.5.dp, borderColor, cellShape) else Modifier)
                                .clickable { tempSelectedIndex = if (tempSelectedIndex == originalIndex) -1 else originalIndex }
                        ) {
                            Icon(icon, contentDescription = label, tint = if (isSelected) Color.White else onSurfaceColor, modifier = Modifier.size(28.dp))
                        }
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(
                    onClick = { onIconSelected(tempSelectedIndex) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) { Text("Select") }
            }
        }
    }
}
