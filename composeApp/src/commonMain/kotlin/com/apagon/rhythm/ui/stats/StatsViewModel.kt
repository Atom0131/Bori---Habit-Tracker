package com.apagon.rhythm.ui.stats
import com.apagon.rhythm.core.time.*

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apagon.rhythm.data.model.Habit
import com.apagon.rhythm.data.model.HabitCompletion
import com.apagon.rhythm.data.repository.HabitRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.LocalDate
import com.apagon.rhythm.core.time.DateTimeFormatter.Companion.ISO_LOCAL_DATE

data class HabitYearStats(
    val habit: Habit,
    val completedDates: Set<String>
)
class StatsViewModel constructor(
    private val repository: HabitRepository
) : ViewModel() {

    private val today: LocalDate = LocalDate.now()
    private val yearStart: LocalDate = LocalDate.of(today.year, 1, 1)
    private val yearEnd: LocalDate = LocalDate.of(today.year, 12, 31)

    val habitStats: StateFlow<List<HabitYearStats>> = combine(
        repository.getAllActiveHabits(),
        repository.getCompletionsBetweenDates(
            startDate = yearStart.format(ISO_LOCAL_DATE),
            endDate = yearEnd.format(ISO_LOCAL_DATE)
        )
    ) { habits, completions ->
        val completionsByHabit: Map<Long, Set<String>> =
            completions
                .groupBy(HabitCompletion::habitId)
                .mapValues { (_, list) -> list.map { it.dateCompleted }.toSet() }

        habits.map { habit ->
            HabitYearStats(
                habit = habit,
                completedDates = completionsByHabit[habit.id] ?: emptySet()
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )
}
