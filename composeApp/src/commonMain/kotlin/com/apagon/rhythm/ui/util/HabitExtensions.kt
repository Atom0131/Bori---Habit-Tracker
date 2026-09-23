package com.apagon.rhythm.ui.util

import com.apagon.rhythm.core.time.value
import com.apagon.rhythm.data.model.ChecklistItem
import com.apagon.rhythm.data.model.ChecklistProgress
import com.apagon.rhythm.data.model.Habit
import com.apagon.rhythm.data.model.HabitFrequency
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
internal fun Flow<List<Long>>.flatMapToChecklistItems(
    getItems: (Long) -> Flow<List<ChecklistItem>>
): Flow<Map<Long, List<ChecklistItem>>> = flatMapLatest { ids ->
    if (ids.isEmpty()) flowOf(emptyMap())
    else combine(ids.map { id -> getItems(id).map { items -> id to items } }) { it.toMap() }
}

internal fun Map<Long, List<ChecklistItem>>.toChecklistProgressMap(
    checkedIds: Set<Long>
): Map<Long, ChecklistProgress> = mapValues { (_, items) ->
    ChecklistProgress(
        items = items,
        checkedItemIds = items.filter { item -> item.id in checkedIds }.map { item -> item.id }.toSet()
    )
}

internal fun Habit.isScheduledForDate(date: LocalDate): Boolean = when (frequency) {
    HabitFrequency.DAILY -> true
    HabitFrequency.WEEKLY -> {
        val bit = date.dayOfWeek.value - 1  // Mon=0 … Sun=6
        (weekDaysMask shr bit) and 1 == 1
    }
    HabitFrequency.MONTHLY -> {
        val bit = date.dayOfMonth - 1
        (monthDaysMask shr bit) and 1 == 1
    }
}
