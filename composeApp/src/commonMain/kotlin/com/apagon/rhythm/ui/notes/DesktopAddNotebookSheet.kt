package com.apagon.rhythm.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.data.model.Notebook
import com.apagon.rhythm.ui.components.CrystalWindowContent
import com.apagon.rhythm.ui.components.crystalButtonColors
import com.apagon.rhythm.ui.components.crystalSheetColor
import com.apagon.rhythm.ui.components.crystalTextFieldColors
import com.apagon.rhythm.ui.components.crystalTextFieldShape
import com.apagon.rhythm.ui.theme.habitColorPalette

/**
 * Desktop port of AddNotebookSheet.kt — swaps ColorPickerRow/FluidTextField/
 * EditorialTitle/MomentumButton (all androidMain SharedComposables) for
 * plain Material3 primitives and the habitColorPalette swatch-row pattern
 * already established in DesktopAddCalendarEventSheet.kt (Stage 8).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopAddNotebookSheet(
    initialNotebook: Notebook? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, colorIndex: Int, colorArgb: Int?) -> Unit
) {
    var name by remember { mutableStateOf(initialNotebook?.name ?: "") }
    var colorIndex by remember { mutableIntStateOf(initialNotebook?.colorIndex ?: 0) }
    val colorArgb = initialNotebook?.colorArgb

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = crystalSheetColor(fallback = MaterialTheme.colorScheme.surface)) {
        // Stage 14 invariant #2: this window is separate from the main one, so its inherited
        // blur field (if any) is unusable — CrystalWindowContent replaces it with a fresh one
        // scoped to this sheet.
        CrystalWindowContent {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = if (initialNotebook == null) "New Notebook" else "Edit Notebook",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Notebook name") },
                singleLine = true,
                colors = crystalTextFieldColors(),
                shape = crystalTextFieldShape(),
                modifier = Modifier.fillMaxWidth()
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Notebook Color", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    habitColorPalette.forEachIndexed { index, color ->
                        val isSelected = colorIndex == index
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 44.dp else 40.dp)
                                .clip(CircleShape)
                                .background(color)
                                .then(
                                    if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier
                                )
                                .clickable { colorIndex = index }
                        )
                    }
                }
            }

            Button(
                onClick = { onSave(name, colorIndex, colorArgb); onDismiss() },
                enabled = name.isNotBlank(),
                colors = crystalButtonColors(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save Notebook") }
        }
        }
    }
}
