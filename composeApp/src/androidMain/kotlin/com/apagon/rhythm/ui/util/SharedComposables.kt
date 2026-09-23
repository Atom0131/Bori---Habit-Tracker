package com.apagon.rhythm.ui.util

import org.koin.compose.viewmodel.koinViewModel

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Larger, more expressive title using the Editorial typeface. */
@Composable
internal fun EditorialTitle(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    style: TextStyle = MaterialTheme.typography.headlineLarge,
    textAlign: TextAlign? = null,
    padding: Dp = 0.dp
) {
    Text(
        text = text,
        style = style,
        fontWeight = FontWeight.Bold,
        color = color,
        textAlign = textAlign,
        modifier = modifier.padding(horizontal = padding)
    )
}

/** Minimalist, more 3D-looking text field using tonal stacking and subtle elevation. */
@Composable
internal fun FluidTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    maxLines: Int = 1
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            shadowElevation = 2.dp,
            tonalElevation = 1.dp,
            border = BorderStroke(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        ) {
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
                maxLines = maxLines,
                textStyle = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

/** High-impact pill button using a solid primary color. */
@Composable
fun MomentumButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = modifier.height(56.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

/** Standard text field colors for sheets: ensures onSurface text in both focus states. */
@Composable
internal fun sheetTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
)

/** Reusable swipe-left-to-delete and swipe-right-to-archive wrapper. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SwipeToDeleteBox(
    onDelete: () -> Unit,
    onArchive: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    deleteContentDescription: String = "Delete",
    archiveContentDescription: String = "Archive",
    content: @Composable () -> Unit
) {
    if (!enabled) {
        Box(modifier = modifier) { content() }
        return
    }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    true
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    if (onArchive != null) {
                        onArchive()
                        true
                    } else false
                }
                else -> false
            }
        },
        positionalThreshold = { distance -> distance * 0.6f }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = onArchive != null,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val color = when (direction) {
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.tertiaryContainer
                else -> Color.Transparent
            }
            val alignment = when (direction) {
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                else -> Alignment.Center
            }
            val icon = when (direction) {
                SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
                SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Archive
                else -> Icons.Default.Delete
            }
            val tint = when (direction) {
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.onErrorContainer
                SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.onTertiaryContainer
                else -> Color.Transparent
            }

            Box(
                contentAlignment = alignment,
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = color,
                        shape = MaterialTheme.shapes.large
                    )
                    .padding(horizontal = 20.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = if (direction == SwipeToDismissBoxValue.StartToEnd) archiveContentDescription else deleteContentDescription,
                    tint = tint
                )
            }
        },
        modifier = modifier
    ) {
        content()
    }
}

/** Reusable row of colour circles (from habitColorPalette) with selection indicator and custom picker. */
@Composable
internal fun ColorPickerRow(
    colorIndex: Int,
    colorArgb: Int? = null,
    isPro: Boolean = true,
    onColorSelected: (Int, Int?) -> Unit,
    onShowPaywall: (String) -> Unit = {},
    viewModel: ColorPickerViewModel = koinViewModel()
) {
    var showCustomPicker by remember { mutableStateOf(false) }
    var pendingSlotIndex by remember { mutableIntStateOf(-1) }
    val customColors by viewModel.customColors.collectAsState()

    val isCustomSelected = colorArgb != null && (0..5).all { customColors.getOrNull(it) != colorArgb }

    // Row 1: custom picker button + 5 default presets
    // Row 2: 5 custom slots (Pro only)
    // Uses Column+Row instead of FlowRow to avoid the experimental FlowRowOverflow API
    // that has an unstable signature across Compose Foundation versions.
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Combined Custom Color Button / Preview
            Box(
                modifier = Modifier.weight(1f).aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(if (isCustomSelected) Color(colorArgb!!) else MaterialTheme.colorScheme.surfaceContainerHighest)
                        .border(
                            2.dp,
                            if (isCustomSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            CircleShape
                        )
                        .clickable {
                            if (isPro) {
                                pendingSlotIndex = -1
                                showCustomPicker = true
                            } else {
                                onShowPaywall("Upgrade to Pro to use the custom color wheel!")
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Palette,
                        contentDescription = "Custom Color",
                        tint = if (isCustomSelected) {
                            if (Color(colorArgb!!).luminance() > 0.5f) Color.Black else Color.White
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
                if (!isPro) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "Pro Feature",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(16.dp)
                            .align(Alignment.BottomEnd)
                            .background(MaterialTheme.colorScheme.surface, CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), CircleShape)
                            .padding(3.dp)
                    )
                }
            }

            // 5 Default Presets
            habitColorPalette.forEachIndexed { index, color ->
                val isSelected = colorIndex == index && colorArgb == null
                Box(
                    modifier = Modifier
                        .weight(1f).aspectRatio(1f)
                        .clip(CircleShape)
                        .background(color)
                        .clickable { onColorSelected(index, null) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 5 Custom Slots (Pro only)
        if (isPro) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                customColors.forEachIndexed { index, argb ->
                    val isSelected = colorArgb == argb && argb != null
                    if (argb == null) {
                        // Empty Slot
                        Box(
                            modifier = Modifier
                                .weight(1f).aspectRatio(1f)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                .border(
                                    BorderStroke(1.dp, Brush.linearGradient(listOf(MaterialTheme.colorScheme.outlineVariant, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)))),
                                    CircleShape
                                )
                                .clickable {
                                    pendingSlotIndex = index
                                    showCustomPicker = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add custom preset",
                                tint = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        // Filled Slot
                        Box(
                            modifier = Modifier
                                .weight(1f).aspectRatio(1f)
                                .clip(CircleShape)
                                .background(Color(argb))
                                .combinedClickable(
                                    onClick = { onColorSelected(-1, argb) },
                                    onLongClick = { viewModel.removeCustomColor(index) }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = if (Color(argb).luminance() > 0.5f) Color.Black else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCustomPicker) {
        CustomColorPickerDialog(
            initialColor = if (pendingSlotIndex != -1 && customColors[pendingSlotIndex] != null) 
                Color(customColors[pendingSlotIndex]!!) 
            else if (colorArgb != null) 
                Color(colorArgb) 
            else 
                habitColorPalette[colorIndex.coerceIn(0, habitColorPalette.lastIndex)],
            onColorSelected = { newColor ->
                val newArgb = newColor.toArgb()
                if (pendingSlotIndex != -1) {
                    viewModel.saveCustomColor(pendingSlotIndex, newArgb)
                }
                onColorSelected(-1, newArgb)
                pendingSlotIndex = -1
            },
            onDismiss = { 
                showCustomPicker = false
                pendingSlotIndex = -1
            }
        )
    }
}

@Composable
internal fun HabitCard(
    modifier: Modifier = Modifier,
    isDone: Boolean = false,
    habitColor: Color = Color.Unspecified,
    content: @Composable () -> Unit
) {
    val cardColor = when {
        isDone -> MaterialTheme.colorScheme.surfaceVariant
        habitColor != Color.Unspecified ->
            lerp(MaterialTheme.colorScheme.surfaceContainer, habitColor, 0.12f)
        else -> MaterialTheme.colorScheme.surfaceContainer
    }
    Surface(
        shape = MaterialTheme.shapes.large,
        color = cardColor,
        border = null,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        modifier = modifier
    ) {
        content()
    }
}

/** Reusable collapsible section header with expand/collapse icon. */
@Composable
internal fun CollapsibleSectionHeader(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    horizontalPadding: Dp = 16.dp
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = horizontalPadding, vertical = 12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.Bold
            )
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Circular icon bubble used on habit cards and icon pickers.
 * Draws a tinted semi-transparent circle background with the icon centered inside.
 */
@Composable
internal fun PlayfulHabitIcon(
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(28.dp)
        )
    }
}

/**
 * Tappable row showing the selected icon; opens [IconPickerModalSheet] on click.
 * Replaces the old inline grid to keep the New Habit form compact.
 * [selectedIconIndex] of -1 means no icon chosen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun IconPickerButton(
    selectedIconIndex: Int,
    accentColor: Color,
    onIconSelected: (Int) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }
    val selectedEntry = habitIconLibrary.getOrNull(selectedIconIndex)

    Surface(
        onClick = { showPicker = true },
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shadowElevation = 2.dp,
        tonalElevation = 1.dp,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                if (selectedEntry != null) {
                    Icon(
                        imageVector = selectedEntry.second,
                        contentDescription = selectedEntry.first,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(selectedEntry.first, style = MaterialTheme.typography.bodyLarge)
                } else {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "No icon selected",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                "Change",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }

    if (showPicker) {
        IconPickerModalSheet(
            selectedIconIndex = selectedIconIndex,
            accentColor = accentColor,
            onIconSelected = { index ->
                onIconSelected(index)
                showPicker = false
            },
            onDismiss = { showPicker = false }
        )
    }
}

/**
 * Full-screen bottom sheet with a scrollable [LazyVerticalGrid] of all habit icons.
 * Opened by [IconPickerButton].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun IconPickerModalSheet(
    selectedIconIndex: Int,
    accentColor: Color,
    onIconSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val cellShape = MaterialTheme.shapes.small
    var tempSelectedIndex by remember { mutableIntStateOf(selectedIconIndex) }
    var searchQuery by remember { mutableStateOf("") }
    val displayIcons = remember(searchQuery) {
        habitIconLibrary.mapIndexed { i, p -> Triple(i, p.first, p.second) }.let { all ->
            if (searchQuery.isBlank()) all
            else all.filter { it.second.contains(searchQuery, ignoreCase = true) }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
                .blockSheetBodyDrag(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.padding(bottom = 4.dp)) {
                Text(
                    "Pick an Icon",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Choose a symbol for your habit",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shadowElevation = 2.dp,
                tonalElevation = 1.dp,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            "Search icons...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (displayIcons.isEmpty()) {
                Text(
                    "No icons found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    textAlign = TextAlign.Center
                )
            } else {
                val unselectedBg    = MaterialTheme.colorScheme.surfaceContainerHighest
                val onSurfaceColor  = MaterialTheme.colorScheme.onSurface
                val borderColor     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp)
                        .blockSheetBoundaryOverscroll()
                ) {
                    itemsIndexed(displayIcons, key = { _, triple -> triple.first }) { _, triple ->
                        val (originalIndex, label, icon) = triple
                        val isSelected = tempSelectedIndex == originalIndex
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(cellShape)
                                .background(if (isSelected) accentColor else unselectedBg)
                                .then(
                                    if (!isSelected) Modifier.border(0.5.dp, borderColor, cellShape)
                                    else Modifier
                                )
                                .clickable {
                                    tempSelectedIndex =
                                        if (tempSelectedIndex == originalIndex) -1 else originalIndex
                                }
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (isSelected) Color.White else onSurfaceColor,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = { onIconSelected(tempSelectedIndex) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text("Select")
                }
            }
        }
    }
}

/** Reusable label + value column used in detail sheets and stats. */
@Composable
internal fun DetailField(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * Ringtone picker button shared by Alarm, Reminder, and Timer sheets.
 * [soundUri] is the currently stored URI string ("" = default, "silent" = silent, else URI).
 * [ringtoneType] should be [RingtoneManager.TYPE_NOTIFICATION] or [RingtoneManager.TYPE_ALARM].
 */
@Composable
internal fun SoundPickerButton(
    soundUri: String,
    ringtoneType: Int,
    onSoundSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val soundName = remember(soundUri) {
        when {
            soundUri.isEmpty() -> "Default"
            soundUri == "silent" -> "Silent"
            else -> try {
                val uri = Uri.parse(soundUri)
                RingtoneManager.getRingtone(context, uri)?.getTitle(context) ?: "Custom"
            } catch (e: Exception) {
                "Custom"
            }
        }
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.let { data ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                data.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
        }
        onSoundSelected(uri?.toString() ?: "silent")
    }
    Surface(
        onClick = {
            val existingUri = soundUri.takeIf { it.isNotEmpty() && it != "silent" }
                ?.let { Uri.parse(it) }
                ?: RingtoneManager.getDefaultUri(ringtoneType)
            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, ringtoneType)
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existingUri)
            }
            launcher.launch(intent)
        },
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shadowElevation = 2.dp,
        tonalElevation = 1.dp,
        border = BorderStroke(
            width = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(8.dp))
            Text("Sound: $soundName", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
internal fun VibrationPatternButton(
    patternId: String,
    onPatternSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }
    var pendingId by remember(patternId) { mutableStateOf(patternId) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Vibration Pattern") },
            text = {
                Column {
                    VibrationPatterns.ALL.forEach { id ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { pendingId = id }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(selected = pendingId == id, onClick = { pendingId = id })
                            Spacer(Modifier.width(8.dp))
                            Text(VibrationPatterns.nameOf(id), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            scope.launch {
                                AlertVibrator.start(context, pendingId)
                                kotlinx.coroutines.delay(1500)
                                AlertVibrator.stop(context)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Vibration, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Test Vibration")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { onPatternSelected(pendingId); showDialog = false }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            }
        )
    }

    Surface(
        onClick = { pendingId = patternId; showDialog = true },
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shadowElevation = 2.dp,
        tonalElevation = 1.dp,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Vibration,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(8.dp))
            Text("Vibration: ${VibrationPatterns.nameOf(patternId)}", style = MaterialTheme.typography.labelLarge)
        }
    }
}
