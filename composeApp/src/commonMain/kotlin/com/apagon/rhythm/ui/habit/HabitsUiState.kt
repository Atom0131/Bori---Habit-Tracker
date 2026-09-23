package com.apagon.rhythm.ui.habit
import com.apagon.rhythm.core.time.*

import com.apagon.rhythm.data.model.ChecklistProgress
import com.apagon.rhythm.data.model.Habit
import com.apagon.rhythm.data.model.HabitFrequency
import kotlinx.datetime.LocalDate

data class HabitsUiState(
    val date: LocalDate,
    val groupedHabits: Map<HabitFrequency, List<Habit>>,
    val pendingGroupedHabits: Map<HabitFrequency, List<Habit>>,
    val completedHabits: List<Habit>,
    val completions: Set<Long>,
    val checklistProgress: Map<Long, ChecklistProgress>
)
