package com.apagon.rhythm.ui.notes

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.data.model.Notebook
import com.apagon.rhythm.ui.util.ColorPickerRow
import com.apagon.rhythm.ui.util.sheetTextFieldColors
import com.apagon.rhythm.ui.util.EditorialTitle
import com.apagon.rhythm.ui.util.FluidTextField
import com.apagon.rhythm.ui.util.MomentumButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNotebookSheet(
    initialNotebook: Notebook? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, colorIndex: Int, colorArgb: Int?) -> Unit
) {
    var name by remember { mutableStateOf(initialNotebook?.name ?: "") }
    var colorIndex by remember { mutableIntStateOf(initialNotebook?.colorIndex ?: 0) }
    var colorArgb by remember { mutableStateOf(initialNotebook?.colorArgb) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            EditorialTitle(
                text = if (initialNotebook == null) "New Notebook" else "Edit Notebook",
                modifier = Modifier.padding(bottom = 8.dp)
            )

            FluidTextField(
                value = name,
                onValueChange = { name = it },
                label = "NOTEBOOK NAME",
                singleLine = true
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Notebook Color",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ColorPickerRow(
                    colorIndex = colorIndex,
                    colorArgb = colorArgb,
                    isPro = true, // Notebook colors are free for now or handled by Pro globally
                    onColorSelected = { idx, argb ->
                        colorIndex = idx
                        colorArgb = argb
                    }
                )
            }

            MomentumButton(
                text = "Save Notebook",
                onClick = {
                    onSave(name, colorIndex, colorArgb)
                    onDismiss()
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
