package com.apagon.rhythm.ui.calendar

import com.apagon.rhythm.platform.PurchaseLauncher

import com.apagon.rhythm.platform.ReminderScheduling
import com.apagon.rhythm.core.time.*

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apagon.rhythm.data.model.ChecklistItem
import com.apagon.rhythm.data.model.ChecklistProgress
import com.apagon.rhythm.data.model.Habit
import com.apagon.rhythm.data.model.HabitCompletion
import com.apagon.rhythm.data.model.CalendarEvent
import com.apagon.rhythm.data.model.Reminder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.apagon.rhythm.data.repository.CalendarEventRepository
import com.apagon.rhythm.data.repository.DeviceCalendarIntegration
import com.apagon.rhythm.data.repository.HabitRepository
import com.apagon.rhythm.data.repository.ReminderRepository
import com.apagon.rhythm.data.preferences.ThemePreferences
import com.apagon.rhythm.ui.util.flatMapToChecklistItems
import com.apagon.rhythm.ui.util.isScheduledForDate
import com.apagon.rhythm.ui.util.toChecklistProgressMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate
import com.apagon.rhythm.core.time.YearMonth
import com.apagon.rhythm.core.time.DateTimeFormatter.Companion.ISO_LOCAL_DATE

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel constructor(
    private val repository: HabitRepository,
    private val reminderRepository: ReminderRepository,
    private val calendarEventRepository: CalendarEventRepository,
    private val calendarIntegrationRepository: DeviceCalendarIntegration,
    private val themePreferences: ThemePreferences,
    private val purchaseLauncher: PurchaseLauncher,
    private val scheduler: ReminderScheduling
) : ViewModel() {

    val isPro: StateFlow<Boolean> = themePreferences.isPro
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val calendarIntegrationEnabled: StateFlow<Boolean> = themePreferences.calendarIntegrationEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val enabledCalendars: StateFlow<Set<String>> = themePreferences.enabledCalendars
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private val _availableCalendars = MutableStateFlow<List<com.apagon.rhythm.data.repository.DeviceCalendar>>(emptyList())
    val availableCalendars: StateFlow<List<com.apagon.rhythm.data.repository.DeviceCalendar>> = _availableCalendars

    fun refreshAvailableCalendars() {
        viewModelScope.launch {
            _availableCalendars.value = calendarIntegrationRepository.fetchAvailableCalendars()
        }
    }

    fun startBillingFlow(productId: String = PurchaseLauncher.PRO_MONTHLY_ID) {
        purchaseLauncher.launchPurchase(productId)
    }

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    val currentMonth: StateFlow<YearMonth> = _currentMonth.asStateFlow()

    private val _selectedDay = MutableStateFlow<LocalDate?>(LocalDate.now())
    val selectedDay: StateFlow<LocalDate?> = _selectedDay.asStateFlow()

    val monthCompletions: StateFlow<Map<String, Set<Long>>> = _currentMonth
        .flatMapLatest { month ->
            val start = month.atDay(1).format(ISO_LOCAL_DATE)
            val end = month.atEndOfMonth().format(ISO_LOCAL_DATE)
            repository.getCompletionsBetweenDates(start, end).map { list ->
                list.groupBy(HabitCompletion::dateCompleted)
                    .mapValues { (_, v) -> v.map { it.habitId }.toSet() }
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val habits: StateFlow<List<Habit>> = repository.getAllActiveHabits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // IDs of checklist habits
    private val checklistHabitIds: StateFlow<List<Long>> = habits
        .map { list -> list.filter { it.isChecklist }.map { it.id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Items for all checklist habits, keyed by habitId
    private val allChecklistItems: StateFlow<Map<Long, List<ChecklistItem>>> =
        checklistHabitIds
            .flatMapToChecklistItems { repository.getItemsForHabit(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    // ChecklistProgress for the selected day (empty when no day is selected)
    val selectedDayChecklistProgress: StateFlow<Map<Long, ChecklistProgress>> =
        combine(
            _selectedDay.flatMapLatest { day ->
                if (day == null) return@flatMapLatest flowOf(emptySet())
                val dateStr = day.format(ISO_LOCAL_DATE)
                checklistHabitIds.flatMapLatest { ids ->
                    if (ids.isEmpty()) flowOf(emptySet())
                    else repository.getItemCompletionsByHabitsOnDate(ids, dateStr)
                        .map { list -> list.map { it.itemId }.toSet() }
                }
            },
            allChecklistItems
        ) { checkedIds, itemsMap ->
            itemsMap.toChecklistProgressMap(checkedIds)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    // Empty when no day is selected
    val selectedDayHabits: StateFlow<List<Habit>> = combine(habits, _selectedDay) { allHabits, day ->
        if (day == null) emptyList()
        else allHabits.filter { it.isScheduledForDate(day) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // All active reminders
    private val allReminders: StateFlow<List<Reminder>> = reminderRepository.getAllActiveReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Reminders scheduled for the selected day
    val selectedDayReminders: StateFlow<List<Reminder>> =
        combine(allReminders, _selectedDay) { reminders, day ->
            if (day == null) emptyList()
            else reminders.filter { it.dateTime.startsWith(day.format(ISO_LOCAL_DATE)) }
                .sortedBy { it.dateTime }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Dates in the current month that have at least one reminder (for dot indicator)
    val monthReminderDates: StateFlow<Set<String>> =
        combine(allReminders, _currentMonth) { reminders, month ->
            val monthPrefix = month.toString() // "yyyy-MM"
            reminders
                .map { it.dateTime.take(10) }
                .filter { it.startsWith(monthPrefix) }
                .toSet()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    // Dates in the current month that have at least one habit scheduled
    val monthHabitDates: StateFlow<Set<String>> =
        combine(habits, _currentMonth) { allHabits, month ->
            val daysInMonth = month.lengthOfMonth()
            val scheduledDates = mutableSetOf<String>()
            for (dayNum in 1..daysInMonth) {
                val date = month.atDay(dayNum)
                if (allHabits.any { it.isScheduledForDate(date) }) {
                    scheduledDates.add(date.format(ISO_LOCAL_DATE))
                }
            }
            scheduledDates
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    // All active calendar events for the current month window
    val monthCalendarEvents: StateFlow<List<CalendarEvent>> = _currentMonth
        .flatMapLatest { month ->
            val start = month.atDay(1).format(ISO_LOCAL_DATE)
            val end = month.atEndOfMonth().format(ISO_LOCAL_DATE)
            calendarEventRepository.getEventsInRange(start, end)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _refreshTrigger = MutableStateFlow(0L)

    // Events that cover the selected day (startDate <= day <= endDate)
    val selectedDayEvents: StateFlow<List<CalendarEvent>> =
        combine(monthCalendarEvents, _selectedDay, calendarIntegrationEnabled, enabledCalendars, _refreshTrigger) { events, day, enabled, enabledIds, _ ->
            if (day == null) emptyList()
            else {
                val internal = events.filter { 
                    try {
                        val start = LocalDate.parse(it.startDate, ISO_LOCAL_DATE)
                        val end = LocalDate.parse(it.endDate, ISO_LOCAL_DATE)
                        !day.isBefore(start) && !day.isAfter(end)
                    } catch (e: Exception) {
                        false
                    }
                }
                val external = if (enabled) calendarIntegrationRepository.fetchEventsForDate(day, enabledIds) else emptyList()
                
                (internal + external).sortedWith(compareBy(
                    { it.startTime == null },  // all-day first
                    { it.startTime }
                ))
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Dates in the current month that have at least one event (for dot indicator)
    val monthEventDates: StateFlow<Set<String>> =
        combine(monthCalendarEvents, _currentMonth, calendarIntegrationEnabled, enabledCalendars, _refreshTrigger) { events, month, enabled, enabledIds, _ ->
            val dates = mutableSetOf<String>()
            val rangeStart = month.minusMonths(1).atDay(1)
            val rangeEnd = month.plusMonths(1).atEndOfMonth()

            events.forEach { event ->
                try {
                    val eventStart = maxOf(LocalDate.parse(event.startDate, ISO_LOCAL_DATE), rangeStart)
                    val eventEnd = minOf(LocalDate.parse(event.endDate, ISO_LOCAL_DATE), rangeEnd)
                    var d = eventStart
                    while (!d.isAfter(eventEnd)) {
                        dates.add(d.format(ISO_LOCAL_DATE))
                        d = d.plusDays(1)
                    }
                } catch (e: Exception) {}
            }
            
            if (enabled) {
                dates.addAll(calendarIntegrationRepository.fetchEventDatesInRange(rangeStart, rangeEnd, enabledIds))
            }
            dates
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    // Consolidated indicator dates (habits, events, or reminders)
    val monthIndicatorDates: StateFlow<Set<String>> =
        combine(monthEventDates, monthReminderDates, monthHabitDates) { events, reminders, habits ->
            events + reminders + habits
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun selectDay(date: LocalDate) { 
        _selectedDay.value = date 
        _currentMonth.value = YearMonth.from(date)
    }
    fun previousMonth() { _currentMonth.update { it.minusMonths(1) } }
    fun nextMonth() { _currentMonth.update { it.plusMonths(1) } }

    fun toggleCompletion(habitId: Long, isDone: Boolean, date: String) {
        viewModelScope.launch {
            val habit = habits.value.firstOrNull { it.id == habitId }
            if (isDone) {
                repository.markIncomplete(habitId, date)
                if (habit?.isChecklist == true) {
                    val items = selectedDayChecklistProgress.value[habitId]?.items ?: emptyList()
                    items.forEach { repository.uncheckItem(it.id, date) }
                }
            } else if (habit?.isChecklist != true) {
                repository.markComplete(habitId, date)
            }
        }
    }

    fun toggleReminderCompletion(reminder: Reminder) {
        viewModelScope.launch {
            reminderRepository.updateReminder(reminder.copy(isCompleted = !reminder.isCompleted))
        }
    }

    fun updateReminder(reminder: Reminder) {
        viewModelScope.launch {
            reminderRepository.updateReminder(reminder)
            scheduler.scheduleOneShot(reminder)
        }
    }

    fun addCalendarEvent(event: CalendarEvent, targetCalendarId: Long? = null) {
        viewModelScope.launch {
            if (targetCalendarId != null && targetCalendarId != 0L) {
                calendarIntegrationRepository.insertEventToNativeCalendar(event, targetCalendarId)
            } else {
                calendarEventRepository.addEvent(event)
            }
        }
    }

    fun updateCalendarEvent(event: CalendarEvent) {
        viewModelScope.launch {
            if (event.id < 0) {
                calendarIntegrationRepository.updateEventInNativeCalendar(event)
            } else {
                calendarEventRepository.updateEvent(event)
            }
        }
    }

    fun deleteCalendarEvent(event: CalendarEvent) {
        viewModelScope.launch {
            if (event.id < 0) {
                calendarIntegrationRepository.deleteEventFromNativeCalendar(event.id)
            } else {
                calendarEventRepository.deleteEvent(event)
            }
            _refreshTrigger.value = System.currentTimeMillis()
        }
    }

    fun toggleItemCompletion(habitId: Long, item: ChecklistItem, isCurrentlyDone: Boolean, date: String) {
        viewModelScope.launch {
            val progress = selectedDayChecklistProgress.value[habitId]
            repository.toggleChecklistItem(
                habitId = habitId,
                item = item,
                isCurrentlyDone = isCurrentlyDone,
                date = date,
                habitCompletedIds = monthCompletions.value[date] ?: emptySet(),
                allItemsForHabit = progress?.items ?: emptyList(),
                checkedItemIds = progress?.checkedItemIds ?: emptySet()
            )
        }
    }
}
