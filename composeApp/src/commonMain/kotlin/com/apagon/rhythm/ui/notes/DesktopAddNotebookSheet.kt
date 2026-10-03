package com.apagon.rhythm.ui.notes

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.data.model.Notebook
import com.apagon.rhythm.ui.components.crystalButtonColors
import com.apagon.rhythm.ui.components.crystalTextFieldColors
import com.apagon.rhythm.ui.components.crystalTextFieldShape
import com.apagon.rhythm.ui.util.ColorPickerRow
import com.apagon.rhythm.ui.util.RhythmSheet
import org.koin.compose.viewmodel.koinViewModel

/**
 * Desktop port of AddNotebookSheet.kt — now wired to the same `ColorPickerRow` custom-hue-wheel
 * picker as Accent Color / Habit / Calendar-event colors (round 3 of the custom-picker plan).
 * Originally swapped it for the plain `habitColorPalette` swatch row when first ported (Stage 8);
 * that was the one remaining inconsistency with Android, since the data model (`Notebook.colorArgb`)
 * already supported a custom color — only the picker UI itself never exposed it.
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
    var colorArgb by remember { mutableStateOf(initialNotebook?.colorArgb) }

    RhythmSheet(onDismiss = onDismiss) {
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
                ColorPickerRow(
                    colorIndex = colorIndex,
                    colorArgb = colorArgb,
                    onColorSelected = { idx, argb -> colorIndex = idx; colorArgb = argb },
                    viewModel = koinViewModel()
                )
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
