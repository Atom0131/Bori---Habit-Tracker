package com.apagon.rhythm.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.data.backup.AutoBackupControl
import com.apagon.rhythm.data.backup.AutoBackupFrequency
import com.apagon.rhythm.ui.util.crystalSegmentedButtonColors
import org.koin.compose.koinInject

/**
 * Settings → Data Management → Auto Backup: the phone's Off / Daily / Weekly choice, plus where
 * the files are, since a computer's Downloads folder is easy to lose track of (2026-10-19).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DesktopAutoBackupRows(control: AutoBackupControl = koinInject()) {
    val frequency by control.frequency.collectAsState()
    val last by control.lastBackupLabel.collectAsState()
    SettingsGroup(
        header = "Auto Backup",
        footer = "Includes your notes' and journal's pictures. Copies of your phone's backups are kept here too, in a folder named after the phone."
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                when {
                    frequency == AutoBackupFrequency.OFF -> "Off. Bori only backs up when you export."
                    last != null -> "Writes a backup to ${control.folderLabel} on its own. Last one: $last."
                    else -> "Writes a backup to ${control.folderLabel} on its own, and keeps the newest 14."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val options = listOf(AutoBackupFrequency.OFF, AutoBackupFrequency.DAILY, AutoBackupFrequency.WEEKLY)
                options.forEachIndexed { idx, option ->
                    SegmentedButton(
                        colors = crystalSegmentedButtonColors(),
                        selected = frequency == option,
                        onClick = { control.setFrequency(option) },
                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = options.size),
                        label = { Text(option.label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }
        SettingsDivider()
        SettingsItem(
            title = "Open backup folder",
            description = control.folderLabel,
            icon = Icons.Default.FolderOpen,
            onClick = { control.openFolder() },
            trailing = { SettingsChevron() }
        )
    }
}
