package com.apagon.rhythm.ui.habit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apagon.rhythm.core.time.*
import com.apagon.rhythm.data.model.HabitCompletion
import com.apagon.rhythm.data.repository.HabitRepository
import com.apagon.rhythm.ui.stats.HabitYearStats
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.LocalDate

/**
 * A deliberately narrow ViewModel, HabitRepository only — same shape as
 * DesktopHabitViewModel/DesktopCalendarViewModel. Unlike StatsViewModel
 * (which computes year stats for every active habit at once), this one
 * takes a single habitId and exposes just that habit's HabitYearStats,
 * since the desktop habit-detail screen only ever shows one habit at a time.
 * habitId is a plain constructor param, supplied at the koinViewModel(...)
 * call site via Koin's parametersOf, matching how this project already
 * injects a runtime value into a ViewModel (see DesktopCalendarViewModel).
 */
class DesktopHabitDetailViewModel(
    habitId: Long,
    repository: HabitRepository
) : ViewModel() {

    private val today: LocalDate = LocalDate.now()
    private val yearStart: LocalDate = LocalDate.of(today.year, 1, 1)
    private val yearEnd: LocalDate = LocalDate.of(today.year, 12, 31)

    val habitStat: StateFlow<HabitYearStats?> = combine(
        repository.getHabitById(habitId),
        repository.getCompletionsBetweenDates(
            startDate = yearStart.toString(),
            endDate = yearEnd.toString()
        )
    ) { habit, completions ->
        if (habit == null) null
        else HabitYearStats(
            habit = habit,
            completedDates = completions.filter { it.habitId == habitId }.map(HabitCompletion::dateCompleted).toSet()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
