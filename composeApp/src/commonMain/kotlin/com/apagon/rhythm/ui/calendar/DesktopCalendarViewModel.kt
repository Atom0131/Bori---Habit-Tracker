package com.apagon.rhythm.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apagon.rhythm.core.time.*
import com.apagon.rhythm.core.time.DateTimeFormatter.Companion.ISO_LOCAL_DATE
import com.apagon.rhythm.data.model.CalendarEvent
import com.apagon.rhythm.data.model.Habit
import com.apagon.rhythm.data.repository.CalendarEventRepository
import com.apagon.rhythm.data.repository.HabitRepository
import com.apagon.rhythm.ui.util.isScheduledForDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

/**
 * A deliberately narrow ViewModel, same reasoning as [com.apagon.rhythm.ui.deleted.DesktopRecentlyDeletedViewModel]:
 * the real [CalendarViewModel] takes ReminderRepository/DeviceCalendarIntegration/
 * PurchaseLauncher/ReminderScheduling, none of which exist in usable form on
 * desktop yet (Reminders are Stage 12 scope; device-calendar sync and billing
 * are permanent no-ops on desktop). This covers events plus habit-scheduled-day
 * indicators only. See ref_notes/plan_2026-09-24_calendar_port.md.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DesktopCalendarViewModel(
    private val habitRepository: HabitRepository,
    private val calendarEventRepository: CalendarEventRepository
) : ViewModel() {

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    val currentMonth: StateFlow<YearMonth> = _currentMonth.asStateFlow()

    private val _selectedDay = MutableStateFlow<LocalDate?>(LocalDate.now())
    val selectedDay: StateFlow<LocalDate?> = _selectedDay.asStateFlow()

    val habits: StateFlow<List<Habit>> = habitRepository.getAllActiveHabits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val monthHabitDates: StateFlow<Set<String>> =
        combine(habits, _currentMonth) { allHabits, month ->
            val scheduled = mutableSetOf<String>()
            for (dayNum in 1..month.lengthOfMonth()) {
                val date = month.atDay(dayNum)
                if (allHabits.any { it.isScheduledForDate(date) }) scheduled.add(date.format(ISO_LOCAL_DATE))
            }
            scheduled
        }.flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val monthCalendarEvents: StateFlow<List<CalendarEvent>> = _currentMonth
        .flatMapLatest { month ->
            calendarEventRepository.getEventsInRange(
                month.atDay(1).format(ISO_LOCAL_DATE),
                month.atEndOfMonth().format(ISO_LOCAL_DATE)
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val monthEventDates: StateFlow<Set<String>> = combine(monthCalendarEvents, _currentMonth) { events, month ->
        val dates = mutableSetOf<String>()
        val rangeStart = month.minusMonths(1).atDay(1)
        val rangeEnd = month.plusMonths(1).atEndOfMonth()
        events.forEach { event ->
            val start = maxOf(LocalDate.parse(event.startDate, ISO_LOCAL_DATE), rangeStart)
            val end = minOf(LocalDate.parse(event.endDate, ISO_LOCAL_DATE), rangeEnd)
            var d = start
            while (!d.isAfter(end)) {
                dates.add(d.format(ISO_LOCAL_DATE))
                d = d.plusDays(1)
            }
        }
        dates
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val monthIndicatorDates: StateFlow<Set<String>> =
        combine(monthEventDates, monthHabitDates) { e, h -> e + h }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val selectedDayEvents: StateFlow<List<CalendarEvent>> =
        combine(monthCalendarEvents, _selectedDay) { events, day ->
            if (day == null) emptyList()
            else events.filter {
                val start = LocalDate.parse(it.startDate, ISO_LOCAL_DATE)
                val end = LocalDate.parse(it.endDate, ISO_LOCAL_DATE)
                !day.isBefore(start) && !day.isAfter(end)
            }.sortedWith(compareBy({ it.startTime == null }, { it.startTime }))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun selectDay(date: LocalDate) {
        _selectedDay.value = date
        _currentMonth.value = YearMonth.from(date)
    }

    fun previousMonth() { _currentMonth.update { it.minusMonths(1) } }
    fun nextMonth() { _currentMonth.update { it.plusMonths(1) } }

    fun addCalendarEvent(event: CalendarEvent) {
        viewModelScope.launch { calendarEventRepository.addEvent(event) }
    }

    fun updateCalendarEvent(event: CalendarEvent) {
        viewModelScope.launch { calendarEventRepository.updateEvent(event) }
    }

    fun deleteCalendarEvent(event: CalendarEvent) {
        viewModelScope.launch { calendarEventRepository.deleteEvent(event) }
    }
}
