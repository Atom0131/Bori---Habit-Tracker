package com.apagon.rhythm.ui.habit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.data.model.HabitFrequency
import com.apagon.rhythm.data.model.TodoPriority
import com.apagon.rhythm.data.preferences.ThemePreferences
import com.apagon.rhythm.platform.ImageBitmapLoader
import com.apagon.rhythm.platform.QrCodeRenderer
import com.apagon.rhythm.ui.todos.TodoViewModel
import com.apagon.rhythm.ui.components.crystalBareTextFieldColors
import com.apagon.rhythm.ui.components.crystalButtonColors
import com.apagon.rhythm.ui.components.crystalCardSurface
import com.apagon.rhythm.ui.components.DesktopLayout
import com.apagon.rhythm.ui.todos.todoSection
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

// Stage 3's minimal desktop Habit screen, extended for the layout-parity round to match the
// phone app's organization: a greeting/streak/%-done header and Daily/Weekly/Monthly collapsible
// sections (ui/habit/HabitListScreen.kt, HabitFrequencySection.kt on Android), while deliberately
// staying a plain list rather than a full port (no checklist items, no swipe actions, no per-habit
// edit sheet — none of those exist on desktop today). Grouped/streak/%-done state comes from
// HabitListViewModel, which already implements it and just wasn't wired into Koin until now;
// DesktopHabitViewModel is kept for its desktop-only sync/QR/add-habit responsibilities.
// Stage 17e: the real Android app has no separate Habits/To-dos/Calendar tabs — HabitListScreen.kt
// (the "Today" tab) shows habits and to-dos together in one continuous list. This is now
// DesktopTodayScreen's list-mode content: habit sections plus, via DesktopHabitList's extraItems
// hook, a to-do section appended in the same LazyColumn. No longer called directly from the
// sidebar (DesktopTodayScreen.kt owns that now) — kept as its own composable since the
// selectedHabitId/detail-pane split-pane logic (Stage 15e) is still exactly right for list mode.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DesktopTodayListContent(
    viewModel: DesktopHabitViewModel = koinViewModel(),
    habitListViewModel: HabitListViewModel = koinViewModel(),
    todoViewModel: TodoViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val peerAddress by viewModel.peerAddress.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    var newHabitName by remember { mutableStateOf("") }
    var showSync by remember { mutableStateOf(false) }
    var showQrCode by remember { mutableStateOf(false) }
    val qrCodeRenderer = koinInject<QrCodeRenderer>()
    // Stage 9: tapping a habit navigates to DesktopHabitDetailScreen (Stats).
    // Plain local state, matching this project's existing showX/editingX
    // toggle pattern rather than a real navigation library (Stage 11 territory).
    var selectedHabitId by remember { mutableStateOf<Long?>(null) }

    val themePreferences = koinInject<ThemePreferences>()
    val userName by themePreferences.userName.collectAsState(initial = "")
    val profilePictureUri by themePreferences.profilePictureUri.collectAsState(initial = null)

    val habitsUiState by habitListViewModel.habitsUiState.collectAsState()
    val dailyStreak by habitListViewModel.dailyStreak.collectAsState()
    val completionRate by habitListViewModel.completionRate.collectAsState()

    val pendingTodos by todoViewModel.pendingTodos.collectAsState()
    val completedTodos by todoViewModel.completedTodos.collectAsState()
    var newTodoTitle by remember { mutableStateOf("") }
    // Stage 18: matches the phone app's HabitListScreen — its "todos" section uses the exact same
    // collapsedSections set as the habit frequency sections, seeded closed. Kept as its own flag
    // here rather than widening collapsedSections' type, since To-dos isn't a HabitFrequency.
    var todosExpanded by remember { mutableStateOf(false) }

    // Seeded fully closed, matching the phone app's HabitListScreen: the planner opens with every
    // primary section closed rather than in a mix of open/closed states.
    var collapsedSections by remember {
        mutableStateOf(setOf(HabitFrequency.DAILY, HabitFrequency.WEEKLY, HabitFrequency.MONTHLY))
    }
    var expandedDoneGroups by remember { mutableStateOf(setOf<HabitFrequency>()) }

    // Stage 15e: the habit list stays visible as a left column when a habit is selected, with
    // DesktopHabitDetailScreen filling a right pane instead of replacing the whole screen — lighter
    // than Notes/Journal's persistent-rail treatment (Habits is this app's primary landing screen,
    // not a browse-then-drill-down surface), but the same "don't discard the list" principle.
    Row(modifier = Modifier.fillMaxSize()) {
        Box(modifier = if (selectedHabitId != null) Modifier.weight(1f).fillMaxHeight() else Modifier.fillMaxSize()) {
            DesktopHabitList(
                viewModel = viewModel,
                habitListViewModel = habitListViewModel,
                state = state,
                peerAddress = peerAddress,
                syncStatus = syncStatus,
                newHabitName = newHabitName,
                onNewHabitNameChange = { newHabitName = it },
                showSync = showSync,
                onToggleShowSync = { showSync = !showSync },
                showQrCode = showQrCode,
                onToggleShowQrCode = { showQrCode = !showQrCode },
                qrCodeRenderer = qrCodeRenderer,
                userName = userName,
                profilePictureUri = profilePictureUri,
                habitsUiState = habitsUiState,
                dailyStreak = dailyStreak,
                completionRate = completionRate,
                collapsedSections = collapsedSections,
                onToggleCollapsed = { frequency ->
                    collapsedSections = if (frequency in collapsedSections) collapsedSections - frequency else collapsedSections + frequency
                },
                expandedDoneGroups = expandedDoneGroups,
                onToggleDoneExpanded = { frequency ->
                    expandedDoneGroups = if (frequency in expandedDoneGroups) expandedDoneGroups - frequency else expandedDoneGroups + frequency
                },
                onSelectHabit = { selectedHabitId = it },
                extraItems = {
                    todoSection(
                        pending = pendingTodos,
                        completed = completedTodos,
                        expanded = todosExpanded,
                        onToggleExpanded = { todosExpanded = !todosExpanded },
                        newTitle = newTodoTitle,
                        onNewTitleChange = { newTodoTitle = it },
                        onAdd = {
                            if (newTodoTitle.isNotBlank()) {
                                todoViewModel.addTodo(title = newTodoTitle, note = "", dueDate = "", priority = TodoPriority.NONE)
                                newTodoTitle = ""
                            }
                        },
                        onToggle = { todoViewModel.toggleCompletion(it) },
                        onDelete = { todoViewModel.deleteTodo(it) }
                    )
                }
            )
        }

        selectedHabitId?.let { habitId ->
            VerticalDivider()
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                DesktopHabitDetailScreen(habitId = habitId, onBack = { selectedHabitId = null })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DesktopHabitList(
    viewModel: DesktopHabitViewModel,
    habitListViewModel: HabitListViewModel,
    state: DesktopHabitUiState,
    peerAddress: String,
    syncStatus: String?,
    newHabitName: String,
    onNewHabitNameChange: (String) -> Unit,
    showSync: Boolean,
    onToggleShowSync: () -> Unit,
    showQrCode: Boolean,
    onToggleShowQrCode: () -> Unit,
    qrCodeRenderer: QrCodeRenderer,
    userName: String,
    profilePictureUri: String?,
    habitsUiState: HabitsUiState,
    dailyStreak: Int,
    completionRate: Int,
    collapsedSections: Set<HabitFrequency>,
    onToggleCollapsed: (HabitFrequency) -> Unit,
    expandedDoneGroups: Set<HabitFrequency>,
    onToggleDoneExpanded: (HabitFrequency) -> Unit,
    onSelectHabit: (Long) -> Unit,
    extraItems: LazyListScope.() -> Unit = {}
) {
    // Stage 17e: no longer owns its own Scaffold — DesktopTodayScreen provides the single shared
    // Scaffold/TopAppBar/background for the whole Today screen (list mode + calendar mode both
    // live under it), so this is just the scrollable content now. extraItems lets Today append the
    // to-do section after the habit sections in the same LazyColumn, matching how the real Android
    // "Today" screen shows habits and to-dos in one continuous list, not two separate screens.
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        // Stage 16a: same centered content-width cap Stage 15g gave To-dos/Clock, now applied
        // here too so every primary screen's card column reads the same width.
        LazyColumn(modifier = Modifier.fillMaxHeight().widthIn(max = DesktopLayout.contentMaxWidth).fillMaxWidth()) {
            item {
                HomeHeader(
                    dailyStreak = dailyStreak,
                    completionRate = completionRate,
                    userName = userName,
                    profilePictureUri = profilePictureUri,
                    date = state.date.toString()
                )
            }

            item {
                Column(modifier = Modifier.padding(horizontal = DesktopLayout.screenPadding)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().crystalCardSurface().padding(DesktopLayout.compactCardPadding),
                        horizontalArrangement = Arrangement.spacedBy(DesktopLayout.itemSpacing),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newHabitName,
                            onValueChange = onNewHabitNameChange,
                            label = { Text("New habit") },
                            colors = crystalBareTextFieldColors(),
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            colors = crystalButtonColors(),
                            onClick = {
                                viewModel.addHabit(newHabitName)
                                onNewHabitNameChange("")
                            }
                        ) {
                            Text("Add")
                        }
                    }

                    TextButton(onClick = onToggleShowSync, modifier = Modifier.padding(top = 8.dp)) {
                        Text(if (showSync) "Hide sync with phone" else "Sync with phone")
                    }

                    if (showSync) {
                        // Stage 13: this device's own address, read-only — the user reads it off
                        // this line and types it into the phone's peer-address field (desktop is
                        // always the sync server). The QR toggle below is the easier path — same
                        // address, scanned instead of typed.
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = DesktopLayout.itemSpacing)
                                .crystalCardSurface().padding(DesktopLayout.compactCardPadding),
                            horizontalArrangement = Arrangement.spacedBy(DesktopLayout.itemSpacing),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Your address: ${viewModel.ownSyncAddress}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            TextButton(onClick = onToggleShowQrCode) {
                                Text(if (showQrCode) "Hide QR Code" else "Show QR Code")
                            }
                        }
                        if (showQrCode) {
                            qrCodeRenderer.QrCodeImage(
                                text = viewModel.ownSyncAddressForPairing,
                                modifier = Modifier.size(200.dp).padding(top = 8.dp)
                            )
                        }

                        // Stage 4b: local sync test UI, still used for the reverse direction
                        // (desktop-initiates-sync) and local dev testing.
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = DesktopLayout.itemSpacing)
                                .crystalCardSurface().padding(DesktopLayout.compactCardPadding),
                            horizontalArrangement = Arrangement.spacedBy(DesktopLayout.itemSpacing),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = peerAddress,
                                onValueChange = { viewModel.updatePeerAddress(it) },
                                label = { Text("Peer address (host:port)") },
                                colors = crystalBareTextFieldColors(),
                                modifier = Modifier.weight(1f)
                            )
                            Button(colors = crystalButtonColors(), onClick = { viewModel.syncNow() }) {
                                Text("Sync")
                            }
                        }
                        if (syncStatus != null) {
                            Text(syncStatus!!, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }

            val uiState = habitsUiState
            val anyHabits = uiState.groupedHabits.values.any { it.isNotEmpty() }
            if (!anyHabits) {
                item {
                    Text(
                        "No habits scheduled for today yet — add one above.",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                for (frequency in listOf(HabitFrequency.DAILY, HabitFrequency.WEEKLY, HabitFrequency.MONTHLY)) {
                    val pending = uiState.pendingGroupedHabits[frequency].orEmpty()
                    val completed = uiState.completedHabits.filter { it.frequency == frequency }
                    habitFrequencySection(
                        frequency = frequency,
                        pending = pending,
                        completed = completed,
                        expanded = frequency !in collapsedSections,
                        onToggleExpanded = { onToggleCollapsed(frequency) },
                        doneExpanded = frequency in expandedDoneGroups,
                        onToggleDone = { onToggleDoneExpanded(frequency) },
                        onToggleCompletion = habitListViewModel::toggleCompletion,
                        onView = { habit -> onSelectHabit(habit.id) }
                    )
                }
            }

            extraItems()
        }
    }
}

@Composable
private fun HomeHeader(
    dailyStreak: Int,
    completionRate: Int,
    userName: String,
    profilePictureUri: String?,
    date: String
) {
    Column(modifier = Modifier.fillMaxWidth().padding(start = DesktopLayout.screenPadding, end = DesktopLayout.screenPadding, top = DesktopLayout.screenPadding, bottom = DesktopLayout.itemSpacing)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (profilePictureUri != null) {
                    val imageLoader = koinInject<ImageBitmapLoader>()
                    imageLoader.LoadedImage(
                        path = profilePictureUri,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        "?",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Hello",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = userName.ifBlank { "Friend" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = date,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(DesktopLayout.itemSpacing)
        ) {
            Box(modifier = Modifier.weight(1f).fillMaxHeight().crystalCardSurface()) {
                Column(
                    modifier = Modifier.padding(DesktopLayout.cardPadding).fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "🔥 $dailyStreak",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Day streak",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Box(modifier = Modifier.weight(1f).fillMaxHeight().crystalCardSurface()) {
                Column(
                    modifier = Modifier.padding(DesktopLayout.cardPadding).fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "$completionRate%",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        LinearProgressIndicator(
                            progress = { (completionRate / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    }
                    Text(
                        text = "Done today",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
