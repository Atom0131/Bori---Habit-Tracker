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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.Icons
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import com.apagon.rhythm.ui.components.crystalControlSurface
import com.apagon.rhythm.ui.util.RhythmDropdownMenu
import com.apagon.rhythm.ui.util.MomentumButton
import com.apagon.rhythm.ui.util.FluidTextField
import com.apagon.rhythm.ui.util.EditorialTitle

/** True if [candidate] is [ancestorId] or sits anywhere below it (a notebook can't be filed
 * under itself or its own descendants). */
private fun isDescendantOfOrSelf(candidate: Notebook, ancestorId: Long, all: List<Notebook>): Boolean {
    var current: Notebook? = candidate
    var guard = 0
    while (current != null && guard < 50) {
        if (current.id == ancestorId) return true
        current = current.parentId?.let { pid -> all.find { it.id == pid } }
        guard++
    }
    return false
}

/**
 * Port of Android's AddNotebookSheet: name, colour (the shared custom-hue picker) and an optional
 * parent notebook. [defaultParentId] pre-selects the parent when creating a subfolder from inside
 * a notebook. Private notebooks never get a parent (and aren't offered as one), as on Android:
 * they stay off sync, and a synced child must not point at a notebook the peer never sees.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopAddNotebookSheet(
    initialNotebook: Notebook? = null,
    allNotebooks: List<Notebook> = emptyList(),
    defaultParentId: Long? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, colorIndex: Int, colorArgb: Int?, parentId: Long?) -> Unit
) {
    var name by remember { mutableStateOf(initialNotebook?.name ?: "") }
    var colorIndex by remember { mutableIntStateOf(initialNotebook?.colorIndex ?: 0) }
    var colorArgb by remember { mutableStateOf(initialNotebook?.colorArgb) }
    var parentId by remember { mutableStateOf(initialNotebook?.parentId ?: defaultParentId) }
    var showParentMenu by remember { mutableStateOf(false) }

    val parentOptions = remember(allNotebooks, initialNotebook?.id) {
        allNotebooks.filter { candidate ->
            !candidate.isPrivate && (initialNotebook == null || !isDescendantOfOrSelf(candidate, initialNotebook.id, allNotebooks))
        }
    }
    val parentName = parentOptions.find { it.id == parentId }?.name

    RhythmSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            EditorialTitle(if (initialNotebook == null) "New Notebook" else "Edit Notebook", modifier = Modifier.padding(bottom = 8.dp))

            FluidTextField(value = name, onValueChange = { name = it }, label = "NOTEBOOK NAME")

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Notebook Color", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ColorPickerRow(
                    colorIndex = colorIndex,
                    colorArgb = colorArgb,
                    onColorSelected = { idx, argb -> colorIndex = idx; colorArgb = argb },
                    viewModel = koinViewModel()
                )
            }

            if (initialNotebook?.isPrivate != true) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Parent Notebook (Optional)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Box {
                        Surface(
                            onClick = { showParentMenu = true },
                            shape = MaterialTheme.shapes.small,
                            color = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth().crystalControlSurface()
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Folder, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text(parentName ?: "None (top level)", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                        RhythmDropdownMenu(expanded = showParentMenu, onDismissRequest = { showParentMenu = false }) {
                            DropdownMenuItem(text = { Text("None (top level)") }, onClick = { parentId = null; showParentMenu = false })
                            parentOptions.forEach { nb ->
                                DropdownMenuItem(text = { Text(nb.name) }, onClick = { parentId = nb.id; showParentMenu = false })
                            }
                        }
                    }
                }
            }

            MomentumButton(
                text = "Save Notebook",
                onClick = { onSave(name, colorIndex, colorArgb, if (initialNotebook?.isPrivate == true) null else parentId); onDismiss() },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
