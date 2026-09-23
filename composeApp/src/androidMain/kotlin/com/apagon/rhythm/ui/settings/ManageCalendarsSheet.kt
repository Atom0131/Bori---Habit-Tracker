package com.apagon.rhythm.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCalendarsSheet(
    onDismiss: () -> Unit,
    viewModel: SettingsViewModel
) {
    val availableCalendars by viewModel.availableCalendars.collectAsState()
    val enabledCalendars by viewModel.enabledCalendars.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshAvailableCalendars()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Manage Calendars",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )

            if (availableCalendars.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No calendars found on this device",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val grouped = availableCalendars.groupBy { it.accountName }
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    grouped.forEach { (account, calendars) ->
                        item {
                            Text(
                                text = account,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
                            )
                        }
                        
                        items(calendars) { calendar ->
                            val isEnabled = enabledCalendars.isEmpty() || enabledCalendars.contains(calendar.id.toString())
                            
                            Surface(
                                onClick = { viewModel.toggleCalendar(calendar.id.toString()) },
                                shape = MaterialTheme.shapes.medium,
                                color = if (isEnabled) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f) else Color.Transparent,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(Color(calendar.color))
                                    )
                                    Spacer(Modifier.width(16.dp))
                                    Text(
                                        text = calendar.displayName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Checkbox(
                                        checked = isEnabled,
                                        onCheckedChange = { viewModel.toggleCalendar(calendar.id.toString()) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            TextButton(
                onClick = {
                    val allIds = availableCalendars.map { it.id.toString() }.toSet()
                    viewModel.setAllCalendars(allIds)
                },
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Text("Select All")
            }
        }
    }
}
