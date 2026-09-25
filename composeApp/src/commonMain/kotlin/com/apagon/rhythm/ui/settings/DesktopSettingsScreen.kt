package com.apagon.rhythm.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.data.preferences.DarkReadability
import com.apagon.rhythm.data.preferences.ThemeMode
import com.apagon.rhythm.ui.theme.habitColorPalette
import org.koin.compose.viewmodel.koinViewModel

// Desktop counterpart to androidMain's SettingsScreen.kt (Stage 11) — a
// LazyColumn of sections rather than that 1512-line screen's full layout
// (BackHandler/permission launchers/Intent deep-links/billing all have no
// desktop equivalent). Reaches the same DesktopSettingsViewModel state Android's
// SettingsViewModel exposes, minus billing. No Material Icons, matching the
// desktop-wide convention (DesktopTodoScreen.kt, Stage 7).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopSettingsScreen(viewModel: DesktopSettingsViewModel = koinViewModel()) {
    val userName by viewModel.userName.collectAsState(initial = "")
    val userNickname by viewModel.userNickname.collectAsState(initial = "")
    val userAge by viewModel.userAge.collectAsState(initial = "")
    val userPronouns by viewModel.userPronouns.collectAsState(initial = "")
    val profilePictureUri by viewModel.profilePictureUri.collectAsState(initial = null)

    val themeMode by viewModel.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val amoledMode by viewModel.amoledMode.collectAsState(initial = false)
    val darkReadability by viewModel.darkReadability.collectAsState(initial = DarkReadability.STANDARD)
    val accentColorIndex by viewModel.accentColorIndex.collectAsState(initial = 0)

    val swipeSectionsEnabled by viewModel.swipeSectionsEnabled.collectAsState(initial = false)
    val homeViewCalendar by viewModel.homeViewCalendar.collectAsState()
    val calendarIntegrationEnabled by viewModel.calendarIntegrationEnabled.collectAsState()

    val archivedHabits by viewModel.archivedHabits.collectAsState()
    val archivedTodos by viewModel.archivedTodos.collectAsState()
    val backupState by viewModel.backupState.collectAsState()

    var showEditProfile by remember { mutableStateOf(false) }
    var showArchivedHabits by remember { mutableStateOf(false) }
    var showArchivedTodos by remember { mutableStateOf(false) }
    var showPrivacyPolicy by remember { mutableStateOf(false) }

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                SettingsSection("Profile") {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { showEditProfile = true },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                userNickname.ifBlank { userName.ifBlank { "Add your name" } },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (userPronouns.isNotBlank() || userAge.isNotBlank()) {
                                Text(
                                    listOf(userPronouns, userAge).filter { it.isNotBlank() }.joinToString(" • "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        TextButton(onClick = { showEditProfile = true }) { Text("Edit") }
                    }
                }
            }

            item {
                SettingsSection("Appearance") {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        SettingsRow("Theme") {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                ThemeMode.entries.forEach { mode ->
                                    TextButton(onClick = { viewModel.setThemeMode(mode) }) {
                                        Text(
                                            mode.name.lowercase().replaceFirstChar { it.uppercase() },
                                            fontWeight = if (mode == themeMode) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                        SettingsRow("AMOLED Black") {
                            Switch(checked = amoledMode, onCheckedChange = { viewModel.setAmoledMode(it) })
                        }
                        SettingsRow("Dark Readability") {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                DarkReadability.entries.forEach { level ->
                                    TextButton(onClick = { viewModel.setDarkReadability(level) }) {
                                        Text(
                                            level.name.lowercase().replaceFirstChar { it.uppercase() },
                                            fontWeight = if (level == darkReadability) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Accent Color", style = MaterialTheme.typography.bodyLarge)
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                habitColorPalette.forEachIndexed { index, color ->
                                    val isSelected = accentColorIndex == index
                                    Box(
                                        modifier = Modifier
                                            .size(if (isSelected) 40.dp else 36.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .then(
                                                if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                                else Modifier
                                            )
                                            .clickable { viewModel.setAccentColor(index) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                SettingsSection("Layout") {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SettingsRow("Calendar as home view") {
                            Switch(checked = homeViewCalendar, onCheckedChange = { viewModel.setHomeViewCalendar(it) })
                        }
                        SettingsRow("Swipeable sections") {
                            Switch(checked = swipeSectionsEnabled, onCheckedChange = { viewModel.setSwipeSectionsEnabled(it) })
                        }
                    }
                }
            }

            item {
                SettingsSection("Calendar Integration") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SettingsRow("Enable device calendars") {
                            Switch(checked = calendarIntegrationEnabled, onCheckedChange = { viewModel.setCalendarIntegrationEnabled(it) })
                        }
                        if (calendarIntegrationEnabled) {
                            Text(
                                "No calendars found on this device.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                SettingsSection("Data Management") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { viewModel.exportBackup() }) { Text("Export Backup") }
                            TextButton(onClick = { viewModel.importBackup() }) { Text("Import Backup") }
                        }
                        when (val state = backupState) {
                            is DesktopBackupState.Success -> Text(state.message, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                            is DesktopBackupState.Error -> Text(state.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            DesktopBackupState.Idle -> {}
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { showArchivedHabits = true }) { Text("Archived Habits (${archivedHabits.size})") }
                            TextButton(onClick = { showArchivedTodos = true }) { Text("Archived To-dos (${archivedTodos.size})") }
                        }
                    }
                }
            }

            item {
                SettingsSection("About") {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Rhythm", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { showPrivacyPolicy = true }, modifier = Modifier.padding(start = 0.dp)) {
                            Text("Privacy Policy")
                        }
                    }
                }
            }
        }
    }

    if (showEditProfile) {
        DesktopEditProfileSheet(
            currentName = userName,
            currentNickname = userNickname,
            currentAge = userAge,
            currentPronouns = userPronouns,
            profilePictureUri = profilePictureUri,
            onDismiss = { showEditProfile = false },
            onPickPhoto = { viewModel.pickAndSetProfilePicture() },
            onRemovePhoto = { viewModel.setProfilePictureUri(null) },
            onSave = { name, nickname, age, pronouns ->
                viewModel.setUserName(name)
                viewModel.setUserNickname(nickname)
                viewModel.setUserAge(age)
                viewModel.setUserPronouns(pronouns)
                showEditProfile = false
            }
        )
    }

    if (showArchivedHabits) {
        DesktopArchivedHabitsSheet(
            habits = archivedHabits,
            onDismiss = { showArchivedHabits = false },
            onUnarchive = { viewModel.unarchiveHabit(it) }
        )
    }

    if (showArchivedTodos) {
        DesktopArchivedTodosSheet(
            todos = archivedTodos,
            onDismiss = { showArchivedTodos = false },
            onUnarchive = { viewModel.unarchiveTodo(it) }
        )
    }

    if (showPrivacyPolicy) {
        DesktopPrivacyPolicySheet(onDismiss = { showPrivacyPolicy = false })
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Box(modifier = Modifier.padding(16.dp)) { content() }
        }
    }
}

@Composable
private fun SettingsRow(label: String, trailing: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        trailing()
    }
}
