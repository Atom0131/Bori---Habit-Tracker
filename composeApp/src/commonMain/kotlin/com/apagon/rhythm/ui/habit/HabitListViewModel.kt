package com.apagon.rhythm.ui.habit

import com.apagon.rhythm.platform.PurchaseLauncher

import com.apagon.rhythm.platform.ReminderScheduling
import com.apagon.rhythm.platform.WidgetRefresher
import com.apagon.rhythm.core.time.*

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apagon.rhythm.data.model.CalendarEvent
import com.apagon.rhythm.data.model.ChecklistItem
import com.apagon.rhythm.data.model.ChecklistProgress
import com.apagon.rhythm.data.model.Habit
import com.apagon.rhythm.data.model.HabitFrequency
import com.apagon.rhythm.data.preferences.ThemePreferences
import com.apagon.rhythm.data.model.JournalEntry
import com.apagon.rhythm.data.model.Reminder
import com.apagon.rhythm.data.model.Todo
import com.apagon.rhythm.data.repository.CalendarEventRepository
import com.apagon.rhythm.data.repository.DeviceCalendarIntegration
import com.apagon.rhythm.data.repository.HabitRepository
import com.apagon.rhythm.data.repository.JournalRepository
import com.apagon.rhythm.data.repository.ReminderRepository
import com.apagon.rhythm.data.repository.TodoRepository
import com.apagon.rhythm.ui.util.flatMapToChecklistItems
import com.apagon.rhythm.ui.util.getDueDateAsLocalDate
import com.apagon.rhythm.ui.util.isScheduledForDate
import com.apagon.rhythm.ui.util.toChecklistProgressMap
import com.apagon.rhythm.ui.util.toDayStartEndMillis
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import com.apagon.rhythm.core.time.YearMonth
import com.apagon.rhythm.core.time.DateTimeFormatter

/** Type-safe container for the inner combine in indicatorDates. */
private data class IndicatorSources(
    val events: List<CalendarEvent>,
    val reminders: List<Reminder>,
    val completions: Set<LocalDate>,
    val displayMonth: YearMonth
)

@OptIn(ExperimentalCoroutinesApi::class)
class HabitListViewModel constructor(
    private val scheduler: ReminderScheduling,
    private val widgetRefresher: WidgetRefresher,
    private val repository: HabitRepository,
    private val calendarEventRepository: CalendarEventRepository,
    private val calendarIntegrationRepository: DeviceCalendarIntegration,
    private val reminderRepository: ReminderRepository,
    private val todoRepository: TodoRepository,
    private val journalRepository: JournalRepository,
    private val themePreferences: ThemePreferences,
    private val purchaseLauncher: PurchaseLauncher
) : ViewModel() {

    val isPro: StateFlow<Boolean> = themePreferences.isPro
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val calendarIntegrationEnabled: StateFlow<Boolean> = themePreferences.calendarIntegrationEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val enabledCalendars: StateFlow<Set<String>> = themePreferences.enabledCalendars
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun startBillingFlow(productId: String = PurchaseLauncher.PRO_MONTHLY_ID) {
        purchaseLauncher.launchPurchase(productId)
    }

    private val _ticker = flow {
        while (true) {
            emit(System.currentTimeMillis())
            kotlinx.coroutines.delay(10 * 60 * 1000L) // Refresh every 10 mins to catch midnight
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, System.currentTimeMillis())

    val todayDate: StateFlow<LocalDate> = _ticker.map { LocalDate.now() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, LocalDate.now())

    val today: StateFlow<String> = todayDate.map { it.format(DateTimeFormatter.ISO_LOCAL_DATE) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))

    val tomorrowDate: StateFlow<LocalDate> = todayDate.map { it.plusDays(1) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, LocalDate.now().plusDays(1))

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _displayMonth = MutableStateFlow(YearMonth.now())
    val displayMonth: StateFlow<YearMonth> = _displayMonth.asStateFlow()

    fun setSelectedDate(date: LocalDate) { 
        _selectedDate.value = date 
        _displayMonth.value = YearMonth.from(date)
    }

    fun setDisplayMonth(month: YearMonth) {
        _displayMonth.value = month
    }

    val habits = repository
        .getAllActiveHabits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // IDs of checklist habits currently active
    private val checklistHabitIds: StateFlow<List<Long>> = habits
        .map { list -> list.filter { it.isChecklist }.map { it.id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Items for all checklist habits, keyed by habitId
    private val allChecklistItems: StateFlow<Map<Long, List<ChecklistItem>>> =
        checklistHabitIds
            .flatMapToChecklistItems { repository.getItemsForHabit(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val habitsUiState: StateFlow<HabitsUiState> = _selectedDate
        .flatMapLatest { date ->
            val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val completionsFlow = repository.getCompletionsByDate(dateStr)
                .map { list -> list.map { it.habitId }.toSet() }
            val itemCompletionsFlow = checklistHabitIds.flatMapLatest { ids ->
                if (ids.isEmpty()) flowOf(emptySet())
                else repository.getItemCompletionsByHabitsOnDate(ids, dateStr)
                    .map { list -> list.map { it.itemId }.toSet() }
            }
            combine(
                habits,
                allChecklistItems,
                completionsFlow,
                itemCompletionsFlow
            ) { activeHabits, checklistItemsMap, completions, checkedItemIds ->
                val grouped = HabitFrequency.entries
                    .associateWith { freq -> activeHabits.filter { it.frequency == freq && it.isScheduledForDate(date) } }
                    .filter { (freq, habitsList) -> freq == HabitFrequency.DAILY || habitsList.isNotEmpty() }
                val pendingGrouped = HabitFrequency.entries
                    .associateWith { freq ->
                        activeHabits.filter {
                            it.frequency == freq && it.isScheduledForDate(date) && it.id !in completions
                        }
                    }
                    .filter { (freq, habitsList) -> freq == HabitFrequency.DAILY || habitsList.isNotEmpty() }
                val completed = activeHabits.filter { it.isScheduledForDate(date) && it.id in completions }
                val checklistProgress = checklistItemsMap.toChecklistProgressMap(checkedItemIds)
                HabitsUiState(
                    date = date,
                    groupedHabits = grouped,
                    pendingGroupedHabits = pendingGrouped,
                    completedHabits = completed,
                    completions = completions,
                    checklistProgress = checklistProgress
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HabitsUiState(
                date = LocalDate.now(),
                groupedHabits = mapOf(HabitFrequency.DAILY to emptyList()),
                pendingGroupedHabits = mapOf(HabitFrequency.DAILY to emptyList()),
                completedHabits = emptyList(),
                completions = emptySet(),
                checklistProgress = emptyMap()
            )
        )

    val todayCompletions: StateFlow<Set<Long>> = habitsUiState
        .map { it.completions }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val groupedHabits: StateFlow<Map<HabitFrequency, List<Habit>>> = habitsUiState
        .map { it.groupedHabits }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), mapOf(HabitFrequency.DAILY to emptyList()))

    val todayChecklistProgress: StateFlow<Map<Long, ChecklistProgress>> = habitsUiState
        .map { it.checklistProgress }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val upcomingHabits: StateFlow<List<Habit>> =
        combine(habits, _selectedDate) { list, date ->
            list.filter { habit ->
                !habit.isScheduledForDate(date) &&
                (1..7).any { days -> habit.isScheduledForDate(date.plusDays(days.toLong())) }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val selectedDateTodos: StateFlow<List<Todo>> = combine(
        _selectedDate,
        todoRepository.getPendingTodos()
    ) { date, todos ->
        todos.filter { todo ->
            if (todo.dueDate.isEmpty()) true
            else todo.getDueDateAsLocalDate() == date
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val selectedDateOverdueTodos: StateFlow<List<Todo>> = combine(
        _selectedDate,
        todoRepository.getPendingTodos()
    ) { date, todos ->
        todos.filter { todo ->
            if (todo.dueDate.isEmpty()) false
            else todo.getDueDateAsLocalDate()?.isBefore(date) == true
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val todayHabits: StateFlow<List<Habit>> = combine(habits, todayDate) { all, currentToday ->
        all.filter { it.isScheduledForDate(currentToday) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val todayCompletionsFixed: StateFlow<Set<Long>> = today
        .flatMapLatest { currentTodayStr ->
            repository.getCompletionsByDate(currentTodayStr)
                .map { list -> list.map { it.habitId }.toSet() }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val selectedDateCompletedTodos: StateFlow<List<Todo>> = _selectedDate.flatMapLatest { date ->
        val (dayStart, dayEnd) = date.toDayStartEndMillis()
        todoRepository.getCompletedTodosForDay(dayStart, dayEnd)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val selectedDateJournalEntries: StateFlow<List<JournalEntry>> = _selectedDate
        .flatMapLatest { date ->
            journalRepository.getEntriesForDate(date.format(DateTimeFormatter.ISO_LOCAL_DATE))
        }
        .map { list -> list.filter { it.deletedAt == null } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())


    private val _refreshTrigger = MutableStateFlow(0L)

    val selectedDayEvents: StateFlow<List<CalendarEvent>> = combine(
        _selectedDate.flatMapLatest { date ->
            calendarEventRepository.getEventsInRange(
                date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            )
        },
        _selectedDate,
        calendarIntegrationEnabled,
        enabledCalendars,
        _refreshTrigger
    ) { internalEvents, date, enabled, enabledIds, _ ->
        val externalEvents = if (enabled) {
            calendarIntegrationRepository.fetchEventsForDate(date, enabledIds)
        } else {
            emptyList()
        }
        (internalEvents + externalEvents).sortedBy { it.startTime ?: "00:00" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val selectedDayReminders: StateFlow<List<Reminder>> = combine(_selectedDate, _refreshTrigger) { date, _ -> 
        date
    }.flatMapLatest { date ->
        val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        reminderRepository.getAllActiveReminders().map { list ->
            list.filter { !it.isCompleted && it.dateTime.startsWith(dateStr) }.sortedBy { it.dateTime }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val overdueReminders: StateFlow<List<Reminder>> = combine(_selectedDate, _refreshTrigger) { date, _ -> 
        date
    }.flatMapLatest { date ->
        val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        reminderRepository.getAllActiveReminders().map { list ->
            list.filter { !it.isCompleted && it.dateTime.substringBefore(" ") < dateStr }.sortedByDescending { it.dateTime }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allActiveReminderCount: StateFlow<Int> = reminderRepository.getAllActiveReminders()
        .map { list -> list.count { !it.isCompleted } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val selectedDateCompletedReminders: StateFlow<List<Reminder>> = combine(_selectedDate, _refreshTrigger) { date, _ ->
        date
    }.flatMapLatest { date ->
        val (dayStart, dayEnd) = date.toDayStartEndMillis()
        reminderRepository.getCompletedRemindersForDay(dayStart, dayEnd)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val calendarCompletionDates: StateFlow<Set<LocalDate>> = todayDate.flatMapLatest { currentToday ->
        repository.getCompletionsBetweenDates(
            startDate = currentToday.minusDays(365).format(DateTimeFormatter.ISO_LOCAL_DATE),
            endDate   = currentToday.plusDays(365).format(DateTimeFormatter.ISO_LOCAL_DATE)
        ).map { completions ->
            completions.map { LocalDate.parse(it.dateCompleted) }.toSet()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val indicatorDates: StateFlow<Set<String>> = combine(
        combine(
            calendarEventRepository.getAllActiveEvents(),
            reminderRepository.getAllActiveReminders(),
            calendarCompletionDates,
            _displayMonth
        ) { events, reminders, completions, displayMonth ->
            IndicatorSources(events, reminders, completions, displayMonth)
        },
        calendarIntegrationEnabled,
        enabledCalendars,
        todoRepository.getAllActiveTodos()
    ) { sources, enabled, enabledIds, todos ->
        val dates = mutableSetOf<String>()
        val rangeStart = sources.displayMonth.minusMonths(1).atDay(1)
        val rangeEnd = sources.displayMonth.plusMonths(1).atEndOfMonth()

        sources.events.forEach { event ->
            try {
                val start = maxOf(LocalDate.parse(event.startDate), rangeStart)
                val end = minOf(LocalDate.parse(event.endDate), rangeEnd)
                var d = start
                while (!d.isAfter(end)) {
                    dates.add(d.format(DateTimeFormatter.ISO_LOCAL_DATE))
                    d = d.plusDays(1)
                }
            } catch (e: Exception) {}
        }
        sources.reminders.forEach { reminder ->
            val dStr = reminder.dateTime.substringBefore(" ")
            try {
                val d = LocalDate.parse(dStr)
                if (!d.isBefore(rangeStart) && !d.isAfter(rangeEnd)) {
                    dates.add(dStr)
                }
            } catch (e: Exception) {}
        }
        sources.completions.forEach { date ->
            if (!date.isBefore(rangeStart) && !date.isAfter(rangeEnd)) {
                dates.add(date.format(DateTimeFormatter.ISO_LOCAL_DATE))
            }
        }

        if (enabled) {
            val externalDates = calendarIntegrationRepository.fetchEventDatesInRange(rangeStart, rangeEnd, enabledIds)
            dates.addAll(externalDates)
        }

        val nowLocalDate = LocalDate.now()
        todos.forEach { todo ->
            val dStr = if (todo.dueDate.isNotEmpty()) {
                todo.dueDate.substringBefore(" ")
            } else {
                todo.getDueDateAsLocalDate()?.toString() ?: ""
            }
            try {
                val d = LocalDate.parse(dStr)
                // Only show indicators for today or future to-dos to keep calendar clean
                if (!d.isBefore(nowLocalDate) && !d.isBefore(rangeStart) && !d.isAfter(rangeEnd)) {
                    dates.add(dStr)
                }
            } catch (_: Exception) {}
        }

        dates
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    // Events + reminders only — used by the week strip so habit completions and todos don't clutter it
    val calendarEventDates: StateFlow<Set<String>> = combine(
        calendarEventRepository.getAllActiveEvents(),
        reminderRepository.getAllActiveReminders(),
        calendarIntegrationEnabled,
        enabledCalendars
    ) { events, reminders, enabled, enabledIds ->
        val dates = mutableSetOf<String>()
        val rangeStart = LocalDate.now().minusMonths(1)
        val rangeEnd = LocalDate.now().plusMonths(2)

        events.forEach { event ->
            try {
                val start = maxOf(LocalDate.parse(event.startDate), rangeStart)
                val end = minOf(LocalDate.parse(event.endDate), rangeEnd)
                var d = start
                while (!d.isAfter(end)) {
                    dates.add(d.format(DateTimeFormatter.ISO_LOCAL_DATE))
                    d = d.plusDays(1)
                }
            } catch (_: Exception) {}
        }
        reminders.forEach { reminder ->
            val dStr = reminder.dateTime.substringBefore(" ")
            try {
                val d = LocalDate.parse(dStr)
                if (!d.isBefore(rangeStart) && !d.isAfter(rangeEnd)) dates.add(dStr)
            } catch (_: Exception) {}
        }
        if (enabled) {
            val externalDates = calendarIntegrationRepository.fetchEventDatesInRange(rangeStart, rangeEnd, enabledIds)
            dates.addAll(externalDates)
        }
        dates
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun addReminder(title: String, note: String, dateTime: String, soundUri: String) {
        viewModelScope.launch {
            val id = reminderRepository.addReminder(
                Reminder(title = title, note = note, dateTime = dateTime, soundUri = soundUri)
            )
            scheduler.scheduleOneShot(Reminder(id = id, title = title, note = note, dateTime = dateTime, soundUri = soundUri)
            )
        }
    }

    fun updateReminder(reminder: Reminder) {
        viewModelScope.launch {
            reminderRepository.updateReminder(reminder)
            scheduler.scheduleOneShot(reminder)
        }
    }

    fun toggleReminderCompletion(reminder: Reminder) {
        viewModelScope.launch {
            val completing = !reminder.isCompleted
            val updated = reminder.copy(
                isCompleted = completing,
                completedAt = if (completing) System.currentTimeMillis() else null
            )
            reminderRepository.updateReminder(updated)
            if (completing) {
                scheduler.cancelOneShot(reminder.id)
            } else {
                scheduler.scheduleOneShot(updated)
            }
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            reminderRepository.deleteReminder(reminder)
            scheduler.cancelOneShot(reminder.id)
            _refreshTrigger.value = System.currentTimeMillis()
        }
    }

    fun addCalendarEvent(event: CalendarEvent, targetCalendarId: Long? = null) {
        viewModelScope.launch {
            if (targetCalendarId != null && targetCalendarId != 0L) {
                calendarIntegrationRepository.insertEventToNativeCalendar(event, targetCalendarId)
            } else {
                calendarEventRepository.addEvent(event)
            }
            _refreshTrigger.value = System.currentTimeMillis()
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

    fun updateCalendarEvent(event: CalendarEvent) {
        viewModelScope.launch {
            if (event.id < 0) {
                calendarIntegrationRepository.updateEventInNativeCalendar(event)
            } else {
                calendarEventRepository.updateEvent(event)
            }
            _refreshTrigger.value = System.currentTimeMillis()
        }
    }
    /** Today's habit completion rate (0–100) — always reflects today regardless of selected date. */
    val completionRate: StateFlow<Int> = combine(todayHabits, todayCompletionsFixed) { hList: List<Habit>, completions: Set<Long> ->
        if (hList.isEmpty()) 0 else (hList.count { it.id in completions } * 100 / hList.size)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /**
     * Consecutive days ending today (or yesterday if today has no completions yet)
     * on which the user completed at least one habit.
     */
    val dailyStreak: StateFlow<Int> = combine(todayDate, today) { currentToday, currentTodayStr ->
        currentToday to currentTodayStr
    }.flatMapLatest { (currentToday, currentTodayStr) ->
        repository.getCompletionsBetweenDates(
            startDate = currentToday.minusDays(365).format(DateTimeFormatter.ISO_LOCAL_DATE),
            endDate   = currentTodayStr
        ).map { completions ->
            val datesWithCompletion = completions
                .map { LocalDate.parse(it.dateCompleted) }
                .toSet()
            var streak = 0
            var checkDate = currentToday
            // If nothing done today, start checking from yesterday
            if (checkDate !in datesWithCompletion) checkDate = checkDate.minusDays(1)
            while (checkDate in datesWithCompletion) {
                streak++
                checkDate = checkDate.minusDays(1)
            }
            streak
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val tomorrowGroupedHabits: StateFlow<Map<HabitFrequency, List<Habit>>> = tomorrowDate.flatMapLatest { nextDay ->
        habits.map { list ->
            HabitFrequency.entries
                .associateWith { freq -> list.filter { it.frequency == freq && it.isScheduledForDate(nextDay) } }
                .filter { (_, habits) -> habits.isNotEmpty() }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    // Derived todayChecklistProgress moved to the top.

    fun toggleCompletion(habitId: Long, isDone: Boolean) {
        viewModelScope.launch {
            val dateStr = _selectedDate.value.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val habit = habits.value.firstOrNull { it.id == habitId }
            if (isDone) {
                repository.markIncomplete(habitId, dateStr)
                if (habit?.isChecklist == true) {
                    val items = todayChecklistProgress.value[habitId]?.items ?: emptyList()
                    items.forEach { repository.uncheckItem(it.id, dateStr) }
                }
            } else {
                repository.markComplete(habitId, dateStr)
            }
            widgetRefresher.refreshAll()
        }
    }

    fun toggleItemCompletion(habitId: Long, item: ChecklistItem, isCurrentlyDone: Boolean) {
        viewModelScope.launch {
            val dateStr = _selectedDate.value.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val progress = todayChecklistProgress.value[habitId]
            repository.toggleChecklistItem(
                habitId = habitId,
                item = item,
                isCurrentlyDone = isCurrentlyDone,
                date = dateStr,
                habitCompletedIds = todayCompletions.value,
                allItemsForHabit = progress?.items ?: emptyList(),
                checkedItemIds = progress?.checkedItemIds ?: emptySet()
            )
            widgetRefresher.refreshAll()
        }
    }

    fun getItemsForHabit(habitId: Long): Flow<List<ChecklistItem>> =
        repository.getItemsForHabit(habitId)

    fun getStreakForHabit(habitId: Long): Flow<Int> = combine(todayDate, repository.getCompletionsForHabit(habitId)) { currentToday, completions ->
        val dates = completions
            .map { LocalDate.parse(it.dateCompleted) }
            .toSet()
        var streak = 0
        var checkDate = currentToday
        if (!dates.contains(checkDate)) checkDate = checkDate.minusDays(1)
        while (dates.contains(checkDate)) {
            streak++
            checkDate = checkDate.minusDays(1)
        }
        streak
    }

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
        reminderTime: String? = null,
        soundUri: String = "",
        vibrationPatternId: String = "default"
    ) {
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
                reminderTime = reminderTime,
                soundUri = soundUri,
                vibrationPatternId = vibrationPatternId
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

    fun updateHabit(
        habit: Habit,
        name: String,
        description: String,
        frequency: HabitFrequency,
        weekDaysMask: Int,
        monthDaysMask: Int,
        isChecklist: Boolean = habit.isChecklist,
        checklistItems: List<String> = emptyList(),
        colorIndex: Int = habit.colorIndex,
        colorArgb: Int? = habit.colorArgb,
        durationDays: Int = habit.durationDays,
        iconIndex: Int = habit.iconIndex,
        reminderTime: String? = habit.reminderTime,
        soundUri: String = habit.soundUri,
        vibrationPatternId: String = habit.vibrationPatternId
    ) {
        viewModelScope.launch {
            val updated = habit.copy(
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
                reminderTime = reminderTime,
                soundUri = soundUri,
                vibrationPatternId = vibrationPatternId
            )
            if (isChecklist) {
                repository.updateHabitWithItems(updated, checklistItems)
            } else {
                repository.updateHabit(updated)
            }
            scheduler.cancelReminder(habit.id)
            if (reminderTime != null) {
                scheduler.scheduleReminder(updated)
            }
        }
    }

    fun updateHabitReminder(habit: Habit, reminderTime: String?) {
        viewModelScope.launch {
            val updated = habit.copy(reminderTime = reminderTime)
            repository.updateHabit(updated)
            scheduler.cancelReminder(habit.id)
            if (reminderTime != null) {
                scheduler.scheduleReminder(updated)
            }
        }
    }

    init {
        archiveOverdueTodos()
    }

    private fun archiveOverdueTodos() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val today = LocalDate.now()
            
            // 1. Move to Archive: Completed > 25h OR Overdue > 7 days
            val activeTodos = todoRepository.getAllActiveTodos().first()
            activeTodos.forEach { todo ->
                if (todo.isCompleted) {
                    val completedAt = todo.completedAt ?: todo.createdAt
                    if (now - completedAt > 25 * 60 * 60 * 1000L) {
                        todoRepository.archiveTodo(todo)
                    }
                } else {
                    val dueDate = todo.getDueDateAsLocalDate()
                    if (dueDate != null && dueDate.isBefore(today.minusDays(7))) {
                        todoRepository.archiveTodo(todo)
                    }
                }
            }

            // 2. Move to Deleted: Archived > 7 days
            val archivedTodos = todoRepository.getArchivedTodos().first()
            archivedTodos.forEach { todo ->
                // Since we don't have an "archivedAt" field, we'll use a heuristic or just 
                // check if the task is simply "old" and archived. 
                // For a strict pipeline, we'd need archivedAt. 
                // Let's use dueDate/createdAt as a proxy for age.
                val ageRef = todo.completedAt ?: todo.createdAt
                if (now - ageRef > 14 * 24 * 60 * 60 * 1000L) { // 7 days in archive + initial age
                    todoRepository.deleteTodo(todo)
                }
            }

            // 3. Purge Deleted > 7 days
            val purgeThreshold = now - (7 * 24 * 60 * 60 * 1000L)
            todoRepository.purgeOldDeletedItems(purgeThreshold)
        }
    }

    fun toggleTodoCompletion(todo: Todo) {
        viewModelScope.launch {
            if (todo.isCompleted) {
                todoRepository.updateTodo(todo.copy(isCompleted = false, completedAt = null))
            } else {
                todoRepository.markTodoComplete(todo)
            }
        }
    }

    fun archiveHabit(habit: Habit) {
        viewModelScope.launch { repository.archiveHabit(habit) }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch { repository.deleteHabit(habit) }
    }
}
