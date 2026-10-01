package com.apagon.rhythm.ui.habit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apagon.rhythm.data.model.Habit
import com.apagon.rhythm.data.model.HabitFrequency
import com.apagon.rhythm.data.repository.HabitRepository
import com.apagon.rhythm.data.sync.LocalSyncAddress
import com.apagon.rhythm.data.sync.SyncCoordinator
import com.apagon.rhythm.data.sync.SyncPreferences
import com.apagon.rhythm.platform.ReminderScheduling
import com.apagon.rhythm.ui.util.isScheduledForDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/**
 * A deliberately narrow ViewModel for the core-first desktop milestone —
 * just enough to list/add/complete habits for today. Unlike the full
 * [HabitListViewModel] (which also owns Calendar/Todo/Journal/Billing, none
 * of which have desktop bindings yet), this depends on [HabitRepository]
 * alone. See the plan's "Stage 3" section for why this isn't just a reuse
 * of HabitListViewModel.
 */
data class DesktopHabitUiState(
    val date: LocalDate,
    val habits: List<Habit>,
    val completedHabitIds: Set<Long>
)

class DesktopHabitViewModel(
    private val repository: HabitRepository,
    private val syncCoordinator: SyncCoordinator,
    private val syncPreferences: SyncPreferences,
    private val scheduler: ReminderScheduling,
    localSyncAddress: LocalSyncAddress
) : ViewModel() {

    private val today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

    // Stage 13: this device's own address for a phone to sync against, shown
    // read-only so the user can read it off this screen and type it into the
    // phone's peer-address field (desktop is always the sync server; see
    // ref_notes/plan_2026-09-25_stage13_tailscale_pairing.md's design
    // decision on sync direction).
    val ownSyncAddress: String = localSyncAddress.display
    val ownSyncAddressForPairing: String = localSyncAddress.addressForPairing

    private val _peerAddress = MutableStateFlow("")
    val peerAddress: StateFlow<String> = _peerAddress.asStateFlow()

    private val _syncStatus = MutableStateFlow<String?>(null)
    val syncStatus: StateFlow<String?> = _syncStatus.asStateFlow()

    init {
        viewModelScope.launch {
            _peerAddress.value = syncPreferences.getPeerAddress() ?: ""
        }
    }

    fun updatePeerAddress(address: String) {
        _peerAddress.value = address
    }

    fun syncNow() {
        val address = _peerAddress.value.trim()
        if (address.isEmpty()) {
            _syncStatus.value = "Enter a peer address first"
            return
        }
        viewModelScope.launch {
            _syncStatus.value = "Syncing…"
            runCatching {
                syncPreferences.setPeerAddress(address)
                syncCoordinator.syncWith(address)
            }.onSuccess { result ->
                _syncStatus.value =
                    "Synced — habits +${result.habitsInserted}/${result.habitsUpdated}, " +
                        "completions +${result.completionsInserted}/${result.completionsUpdated}"
            }.onFailure { e ->
                _syncStatus.value = "Sync failed: ${e.message}"
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DesktopHabitUiState> =
        combine(
            repository.getAllActiveHabits(),
            repository.getCompletionsByDate(today.toString())
        ) { habits, completions ->
            val scheduledToday = habits.filter { it.isScheduledForDate(today) }
            DesktopHabitUiState(
                date = today,
                habits = scheduledToday,
                completedHabitIds = completions.map { it.habitId }.toSet()
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DesktopHabitUiState(today, emptyList(), emptySet())
        )

    fun addHabit(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.addHabit(Habit(name = name.trim(), frequency = HabitFrequency.DAILY))
        }
    }

    /**
     * Full-featured add, mirroring androidMain's `HabitListViewModel.addHabit(...)` — schedule,
     * color, icon, checklist, and a reminder time, all of which [addHabit] above hardcodes away.
     * Deliberately on this narrowly-scoped ViewModel rather than pulling in [HabitListViewModel]:
     * that one's `init {}` runs an `archiveOverdueTodos()` side-effect pass and depends on
     * Calendar/Journal/billing repositories this screen has no other reason to touch.
     */
    fun addHabit(
        name: String,
        description: String,
        frequency: HabitFrequency,
        weekDaysMask: Int,
        monthDaysMask: Int,
        isChecklist: Boolean = false,
        checklistItems: List<String> = emptyList(),
        colorIndex: Int = 0,
        colorArgb: Int? = null,
        durationDays: Int = 0,
        iconIndex: Int = -1,
        reminderTime: String? = null
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val habit = Habit(
                name = name.trim(),
                description = description.trim(),
                frequency = frequency,
                targetDaysPerWeek = weekDaysMask.countOneBits(),
                targetDaysPerMonth = monthDaysMask.countOneBits(),
                weekDaysMask = weekDaysMask,
                monthDaysMask = monthDaysMask,
                isChecklist = isChecklist,
                colorIndex = colorIndex,
                colorArgb = colorArgb,
                durationDays = durationDays,
                iconIndex = iconIndex,
                reminderTime = reminderTime
            )
            val savedId = if (isChecklist && checklistItems.isNotEmpty()) {
                repository.addHabitWithItems(habit, checklistItems)
            } else {
                repository.addHabit(habit)
            }
            if (reminderTime != null && savedId > 0) {
                scheduler.scheduleReminder(habit.copy(id = savedId))
            }
        }
    }

    fun toggleCompletion(habitId: Long, isCurrentlyDone: Boolean) {
        viewModelScope.launch {
            if (isCurrentlyDone) {
                repository.markIncomplete(habitId, today.toString())
            } else {
                repository.markComplete(habitId, today.toString())
            }
        }
    }
}
