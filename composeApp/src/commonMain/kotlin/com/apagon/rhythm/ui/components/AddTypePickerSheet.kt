package com.apagon.rhythm.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apagon.rhythm.ui.util.RhythmSheet

data class AddOption(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val description: String? = null
)

/**
 * Port of the Android app's `AddTypePickerSheet`, used by both "+" buttons (Today and Clock). The
 * desktop used a plain text dropdown here, which was the one place adding something looked like a
 * different app: Android shows a sheet of glass icon tiles with a one-line description each.
 * See the Android file for why the tile fill is `crystalAddTileFill()` and the glyph is bevelled.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTypePickerSheet(
    onDismiss: () -> Unit,
    options: List<AddOption>,
    onOptionSelected: (String) -> Unit
) {
    RhythmSheet(onDismiss = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
            Text(
                text = "What would you like to add?",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )
            options.forEach { option ->
                AddOptionItem(option, onClick = { onOptionSelected(option.id); onDismiss() })
            }
        }
    }
}

@Composable
private fun AddOptionItem(option: AddOption, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(48.dp).crystalTileSurface(fill = crystalAddTileFill()),
            contentAlignment = Alignment.Center
        ) {
            CrystalGlyph(imageVector = option.icon, tint = MaterialTheme.colorScheme.primary, size = 24.dp)
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(option.title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
            if (option.description != null) {
                Text(option.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
