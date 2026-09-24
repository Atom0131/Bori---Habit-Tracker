package com.apagon.rhythm.ui.habit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apagon.rhythm.data.model.Habit
import com.apagon.rhythm.data.model.HabitFrequency
import com.apagon.rhythm.data.repository.HabitRepository
import com.apagon.rhythm.data.sync.SyncCoordinator
import com.apagon.rhythm.data.sync.SyncPreferences
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
    private val syncPreferences: SyncPreferences
) : ViewModel() {

    private val today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

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
