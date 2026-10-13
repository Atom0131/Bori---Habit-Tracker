package com.apagon.rhythm.ui.sync

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.data.sync.AutoSyncChoices
import com.apagon.rhythm.ui.util.RhythmAlertDialog

/**
 * "Sync automatically: Every hour" — tap to pick from [AutoSyncChoices.CHOICES]. Shown on the Sync screen
 * and on Settings → Data Management. A copy of Android's `ui/sync/AutoSyncRow.kt`; the desktop
 * adds [note], because it can only reach computers — phones sync in on their own schedule.
 */
@Composable
fun AutoSyncRow(minutes: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier, note: String? = null) {
    var picking by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { picking = true }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text("Sync automatically", style = MaterialTheme.typography.bodyLarge)
            Text(
                if (minutes <= 0) "Off — only when you tap Sync"
                else "${AutoSyncChoices.label(minutes)}, with every computer that's switched on",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(onClick = { picking = true }) { Text("Change") }
    }
    if (picking) {
        RhythmAlertDialog(
            onDismissRequest = { picking = false },
            title = { Text("Sync automatically") },
            text = {
                Column {
                    Text(
                        "Bori syncs in the background while Tailscale is on. A computer that's asleep or has Bori closed is skipped until next time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    note?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    AutoSyncChoices.CHOICES.forEach { (value, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = value == minutes,
                                    role = Role.RadioButton,
                                    onClick = { onChange(value); picking = false }
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = value == minutes, onClick = null, modifier = Modifier.padding(8.dp))
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { picking = false }) { Text("Close") } }
        )
    }
}
