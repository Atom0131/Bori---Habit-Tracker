package com.apagon.rhythm.data.backup
import com.apagon.rhythm.core.time.*

import com.apagon.rhythm.data.db.HabitDatabase
import androidx.room.withTransaction
import com.apagon.rhythm.data.model.Alarm
import com.apagon.rhythm.data.model.CalendarEvent
import com.apagon.rhythm.data.model.ChecklistItem
import com.apagon.rhythm.data.model.ChecklistItemCompletion
import com.apagon.rhythm.data.model.Habit
import com.apagon.rhythm.data.model.HabitCompletion
import com.apagon.rhythm.data.model.HabitFrequency
import com.apagon.rhythm.data.model.JournalEntry
import com.apagon.rhythm.data.model.Note
import com.apagon.rhythm.data.model.Notebook
import com.apagon.rhythm.data.model.Reminder
import com.apagon.rhythm.data.model.Timer
import com.apagon.rhythm.data.model.Todo
import com.apagon.rhythm.data.preferences.ThemePreferences
import kotlinx.coroutines.flow.first
import com.apagon.rhythm.core.json.JSONArray
import com.apagon.rhythm.core.json.JSONObject
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import android.content.Context
import com.apagon.rhythm.notifications.ReminderScheduler
class BackupManager constructor(
    private val db: HabitDatabase,
    private val themePreferences: ThemePreferences,
    private val context: Context
) {

    suspend fun exportToJson(): String {
        val habitDao = db.habitDao()
        val reminderDao = db.reminderDao()
        val calendarDao = db.calendarEventDao()
        val alarmDao = db.alarmDao()
        val timerDao = db.timerDao()
        val todoDao = db.todoDao()
        val notesDao = db.notesDao()
        val journalDao = db.journalDao()

        // Collect all data
        val habits = habitDao.getAllHabitsForBackup()
        val completions = habitDao.getAllCompletionsForBackup()
        val checklistItems = habitDao.getAllChecklistItemsForBackup()
        val checklistItemCompletions = habitDao.getAllChecklistItemCompletionsForBackup()
        val reminders = reminderDao.getAllRemindersForBackup()
        val calendarEvents = calendarDao.getAllEventsForBackup()
        val alarms = alarmDao.getAllAlarmsForBackup()
        val timers = timerDao.getAllTimersForBackup()
        val todos = todoDao.getAllTodosForBackup()
        val notebooks = notesDao.getAllNotebooksForBackup()
        val notes = notesDao.getAllNotesForBackup()
        val journalEntries = journalDao.getAllForBackup()
        val preferences = themePreferences.exportPreferences()

        return JSONObject().apply {
            put("version", 1)
            put("exportedAt", LocalDate.now().toString())
            put("habits", habitsToJson(habits))
            put("habitCompletions", completionsToJson(completions))
            put("checklistItems", checklistItemsToJson(checklistItems))
            put("checklistItemCompletions", checklistItemCompletionsToJson(checklistItemCompletions))
            put("reminders", remindersToJson(reminders))
            put("calendarEvents", calendarEventsToJson(calendarEvents))
            put("alarms", alarmsToJson(alarms))
            put("timers", timersToJson(timers))
            put("todos", todosToJson(todos))
            put("notebooks", notebooksToJson(notebooks))
            put("notes", notesToJson(notes))
            put("journalEntries", journalEntriesToJson(journalEntries))
            put("preferences", preferences)
        }.toString(2)
    }

    suspend fun importFromJson(json: String) {
        val root = JSONObject(json)
        val version = root.optInt("version", 0)
        if (version < 1) {
             // In the future, we could add migrations here. 
             // For now, version 1 is the baseline.
        }

        db.withTransaction {
            val habitDao = db.habitDao()
            val reminderDao = db.reminderDao()
            val calendarDao = db.calendarEventDao()
            val alarmDao = db.alarmDao()
            val timerDao = db.timerDao()
            val todoDao = db.todoDao()
            val notesDao = db.notesDao()
            val journalDao = db.journalDao()

            // Clear in FK-safe order (children before parents).
            // Notes/notebooks/journals are cleared later, only when the backup contains those keys.
            habitDao.deleteAllChecklistItemCompletions()
            habitDao.deleteAllChecklistItems()
            habitDao.deleteAllCompletions()
            habitDao.deleteAllHabits()
            reminderDao.deleteAll()
            calendarDao.deleteAll()
            alarmDao.deleteAll()
            timerDao.deleteAll()
            todoDao.deleteAll()

            // Insert in FK-safe order (parents before children)
            habitDao.insertAllHabits(jsonToHabits(root.optJSONArray("habits") ?: JSONArray()))
            habitDao.insertChecklistItems(jsonToChecklistItems(root.optJSONArray("checklistItems") ?: JSONArray()))
            habitDao.insertAllChecklistItemCompletions(jsonToChecklistItemCompletions(root.optJSONArray("checklistItemCompletions") ?: JSONArray()))
            habitDao.insertAllCompletions(jsonToCompletions(root.optJSONArray("habitCompletions") ?: JSONArray()))
            reminderDao.insertAll(jsonToReminders(root.optJSONArray("reminders") ?: JSONArray()))
            calendarDao.insertAll(jsonToCalendarEvents(root.optJSONArray("calendarEvents") ?: JSONArray()))
            alarmDao.insertAll(jsonToAlarms(root.optJSONArray("alarms") ?: JSONArray()))
            timerDao.insertAll(jsonToTimers(root.optJSONArray("timers") ?: JSONArray()))
            todoDao.insertAll(jsonToTodos(root.optJSONArray("todos") ?: JSONArray()))
            // Only wipe and restore notes/notebooks/journals if the backup explicitly includes them.
            // Old backups omit these keys entirely — unconditionally deleting would erase the user's data.
            if (root.has("notebooks")) {
                notesDao.deleteAllNotes()
                notesDao.deleteAllNotebooks()
                jsonToNotebooks(root.getJSONArray("notebooks")).forEach { notesDao.insertNotebook(it) }
                jsonToNotes(root.optJSONArray("notes") ?: JSONArray()).forEach { notesDao.insertNote(it) }
            }
            if (root.has("journalEntries")) {
                journalDao.deleteAll()
                journalDao.insertAll(jsonToJournalEntries(root.getJSONArray("journalEntries")))
            }
        }
        // Apply preferences after the transaction so no DataStore emission can race with the
        // Room transaction and trigger a recomposition mid-write.
        root.optJSONObject("preferences")?.let { themePreferences.importPreferences(it) }

        // Reschedule alarms, habit reminders, and one-shot reminders post-import
        try {
            val habitDao = db.habitDao()
            val reminderDao = db.reminderDao()
            val alarmDao = db.alarmDao()

            val habits = habitDao.getAllActiveHabits().first()
            habits.forEach { habit ->
                ReminderScheduler.scheduleReminder(context, habit)
            }

            val now = LocalDateTime.now()
            val format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            val reminders = reminderDao.getAllActiveReminders().first()
            reminders.filter {
                try { LocalDateTime.parse(it.dateTime, format).isAfter(now) }
                catch (e: Exception) { false }
            }.forEach { reminder ->
                ReminderScheduler.scheduleOneShot(context, reminder)
            }

            val alarms = alarmDao.getEnabledAlarms()
            alarms.forEach { alarm ->
                ReminderScheduler.scheduleAlarm(context, alarm)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ── Serializers ───────────────────────────────────────────────────────────

    private fun habitsToJson(habits: List<Habit>) = JSONArray().also { arr ->
        habits.forEach { h ->
            arr.put(JSONObject().apply {
                put("id", h.id)
                put("name", h.name)
                put("description", h.description)
                put("frequency", h.frequency.name)
                put("targetDaysPerWeek", h.targetDaysPerWeek)
                put("targetDaysPerMonth", h.targetDaysPerMonth)
                put("weekDaysMask", h.weekDaysMask)
                put("monthDaysMask", h.monthDaysMask)
                put("reminderTime", h.reminderTime ?: JSONObject.NULL)
                put("createdAt", h.createdAt)
                put("isActive", h.isActive)
                put("isChecklist", h.isChecklist)
                put("colorIndex", h.colorIndex)
                put("durationDays", h.durationDays)
                put("iconIndex", h.iconIndex)
                put("colorArgb", h.colorArgb ?: JSONObject.NULL)
                put("deletedAt", h.deletedAt ?: JSONObject.NULL)
                put("soundUri", h.soundUri)
                put("vibrationPatternId", h.vibrationPatternId)
            })
        }
    }

    private fun completionsToJson(completions: List<HabitCompletion>) = JSONArray().also { arr ->
        completions.forEach { c ->
            arr.put(JSONObject().apply {
                put("id", c.id)
                put("habitId", c.habitId)
                put("dateCompleted", c.dateCompleted)
            })
        }
    }

    private fun checklistItemsToJson(items: List<ChecklistItem>) = JSONArray().also { arr ->
        items.forEach { i ->
            arr.put(JSONObject().apply {
                put("id", i.id)
                put("habitId", i.habitId)
                put("label", i.label)
                put("sortOrder", i.sortOrder)
            })
        }
    }

    private fun checklistItemCompletionsToJson(completions: List<ChecklistItemCompletion>) = JSONArray().also { arr ->
        completions.forEach { c ->
            arr.put(JSONObject().apply {
                put("id", c.id)
                put("itemId", c.itemId)
                put("dateCompleted", c.dateCompleted)
            })
        }
    }

    private fun remindersToJson(reminders: List<Reminder>) = JSONArray().also { arr ->
        reminders.forEach { r ->
            arr.put(JSONObject().apply {
                put("id", r.id)
                put("title", r.title)
                put("note", r.note)
                put("dateTime", r.dateTime)
                put("isCompleted", r.isCompleted)
                if (r.completedAt != null) put("completedAt", r.completedAt)
                put("isActive", r.isActive)
                put("createdAt", r.createdAt)
                put("soundUri", r.soundUri)
                put("deletedAt", r.deletedAt ?: JSONObject.NULL)
            })
        }
    }

    private fun calendarEventsToJson(events: List<CalendarEvent>) = JSONArray().also { arr ->
        events.forEach { e ->
            arr.put(JSONObject().apply {
                put("id", e.id)
                put("title", e.title)
                put("note", e.note)
                put("startDate", e.startDate)
                put("endDate", e.endDate)
                put("startTime", e.startTime ?: JSONObject.NULL)
                put("endTime", e.endTime ?: JSONObject.NULL)
                put("colorIndex", e.colorIndex)
                put("colorArgb", e.colorArgb ?: JSONObject.NULL)
                put("isActive", e.isActive)
                put("createdAt", e.createdAt)
                put("deletedAt", e.deletedAt ?: JSONObject.NULL)
            })
        }
    }

    private fun alarmsToJson(alarms: List<Alarm>) = JSONArray().also { arr ->
        alarms.forEach { a ->
            arr.put(JSONObject().apply {
                put("id", a.id)
                put("label", a.label)
                put("hour", a.hour)
                put("minute", a.minute)
                put("repeatDaysMask", a.repeatDaysMask)
                put("isEnabled", a.isEnabled)
                put("soundUri", a.soundUri)
                put("vibrationPatternId", a.vibrationPatternId)
                put("createdAt", a.createdAt)
                put("deletedAt", a.deletedAt ?: JSONObject.NULL)
            })
        }
    }

    private fun timersToJson(timers: List<Timer>) = JSONArray().also { arr ->
        timers.forEach { t ->
            arr.put(JSONObject().apply {
                put("id", t.id)
                put("label", t.label)
                put("durationSeconds", t.durationSeconds)
                put("remainingSeconds", t.remainingSeconds)
                put("endTimeMillis", 0L) // never restore a running state
                put("soundUri", t.soundUri)
                put("createdAt", t.createdAt)
                put("isPomo", t.isPomo)
                put("pomoWorkSecs", t.pomoWorkSecs)
                put("pomoShortBreakSecs", t.pomoShortBreakSecs)
                put("pomoLongBreakSecs", t.pomoLongBreakSecs)
                put("pomoSessionsPerRound", t.pomoSessionsPerRound)
                put("pomoCurrentSession", t.pomoCurrentSession)
                put("pomoPhase", t.pomoPhase)
                put("deletedAt", t.deletedAt ?: JSONObject.NULL)
            })
        }
    }

    private fun todosToJson(todos: List<Todo>) = JSONArray().also { arr ->
        todos.forEach { t ->
            arr.put(JSONObject().apply {
                put("id", t.id)
                put("title", t.title)
                put("note", t.note)
                put("dueDate", t.dueDate)
                put("priority", t.priority)
                put("isCompleted", t.isCompleted)
                if (t.completedAt != null) put("completedAt", t.completedAt)
                put("iconIndex", t.iconIndex)
                put("isArchived", t.isArchived)
                put("soundUri", t.soundUri)
                put("vibrationPatternId", t.vibrationPatternId)
                put("deletedAt", t.deletedAt ?: JSONObject.NULL)
                put("createdAt", t.createdAt)
            })
        }
    }

    private fun notebooksToJson(notebooks: List<Notebook>) = JSONArray().also { arr ->
        notebooks.forEach { n ->
            arr.put(JSONObject().apply {
                put("id", n.id)
                put("name", n.name)
                put("colorIndex", n.colorIndex)
                if (n.colorArgb != null) put("colorArgb", n.colorArgb) else put("colorArgb", JSONObject.NULL)
                put("createdAt", n.createdAt)
                put("updatedAt", n.updatedAt)
                put("deletedAt", n.deletedAt ?: JSONObject.NULL)
            })
        }
    }

    private fun notesToJson(notes: List<Note>) = JSONArray().also { arr ->
        notes.forEach { n ->
            arr.put(JSONObject().apply {
                put("id", n.id)
                put("notebookId", n.notebookId)
                put("title", n.title)
                put("content", n.content)
                put("createdAt", n.createdAt)
                put("updatedAt", n.updatedAt)
                put("isPinned", n.isPinned)
                put("deletedAt", n.deletedAt ?: JSONObject.NULL)
                put("tags", n.tags)
                put("fontFamily", n.fontFamily)
                put("fontSize", n.fontSize)
            })
        }
    }

    // ── Deserializers ─────────────────────────────────────────────────────────

    private fun jsonToHabits(arr: JSONArray) = (0 until arr.length()).map { i ->
        val o = arr.getJSONObject(i)
        Habit(
            id = o.getLong("id"),
            name = o.getString("name"),
            description = o.optString("description", ""),
            frequency = runCatching { 
                HabitFrequency.valueOf(o.optString("frequency", HabitFrequency.DAILY.name)) 
            }.getOrDefault(HabitFrequency.DAILY),
            targetDaysPerWeek = o.optInt("targetDaysPerWeek", 1),
            targetDaysPerMonth = o.optInt("targetDaysPerMonth", 1),
            weekDaysMask = o.optInt("weekDaysMask", 0),
            monthDaysMask = o.optInt("monthDaysMask", 0),
            reminderTime = if (o.isNull("reminderTime")) null else o.optString("reminderTime"),
            createdAt = o.optLong("createdAt", System.currentTimeMillis()),
            isActive = o.optBoolean("isActive", true),
            isChecklist = o.optBoolean("isChecklist", false),
            colorIndex = o.optInt("colorIndex", 0),
            durationDays = o.optInt("durationDays", 0),
            iconIndex = o.optInt("iconIndex", -1),
            colorArgb = if (o.isNull("colorArgb")) null else o.optInt("colorArgb"),
            deletedAt = if (o.isNull("deletedAt")) null else o.optLong("deletedAt"),
            soundUri = o.optString("soundUri", ""),
            vibrationPatternId = o.optString("vibrationPatternId", "default")
        )
    }

    private fun jsonToCompletions(arr: JSONArray) = (0 until arr.length()).map { i ->
        val o = arr.getJSONObject(i)
        HabitCompletion(
            id = o.getLong("id"),
            habitId = o.getLong("habitId"),
            dateCompleted = o.getString("dateCompleted")
        )
    }

    private fun jsonToChecklistItems(arr: JSONArray) = (0 until arr.length()).map { i ->
        val o = arr.getJSONObject(i)
        ChecklistItem(
            id = o.getLong("id"),
            habitId = o.getLong("habitId"),
            label = o.getString("label"),
            sortOrder = o.optInt("sortOrder", 0)
        )
    }

    private fun jsonToChecklistItemCompletions(arr: JSONArray) = (0 until arr.length()).map { i ->
        val o = arr.getJSONObject(i)
        ChecklistItemCompletion(
            id = o.getLong("id"),
            itemId = o.getLong("itemId"),
            dateCompleted = o.getString("dateCompleted")
        )
    }

    private fun jsonToReminders(arr: JSONArray) = (0 until arr.length()).map { i ->
        val o = arr.getJSONObject(i)
        Reminder(
            id = o.getLong("id"),
            title = o.getString("title"),
            note = o.optString("note", ""),
            dateTime = o.getString("dateTime"),
            isCompleted = o.optBoolean("isCompleted", false),
            completedAt = if (o.isNull("completedAt")) null else o.optLong("completedAt"),
            isActive = o.optBoolean("isActive", true),
            createdAt = o.optLong("createdAt", System.currentTimeMillis()),
            soundUri = o.optString("soundUri", ""),
            deletedAt = if (o.isNull("deletedAt")) null else o.optLong("deletedAt")
        )
    }

    private fun jsonToCalendarEvents(arr: JSONArray) = (0 until arr.length()).map { i ->
        val o = arr.getJSONObject(i)
        CalendarEvent(
            id = o.getLong("id"),
            title = o.getString("title"),
            note = o.optString("note", ""),
            startDate = o.getString("startDate"),
            endDate = o.getString("endDate"),
            startTime = if (o.isNull("startTime")) null else o.optString("startTime"),
            endTime = if (o.isNull("endTime")) null else o.optString("endTime"),
            colorIndex = o.optInt("colorIndex", 0),
            colorArgb = if (o.isNull("colorArgb")) null else o.optInt("colorArgb"),
            isActive = o.optBoolean("isActive", true),
            createdAt = o.optLong("createdAt", System.currentTimeMillis()),
            deletedAt = if (o.isNull("deletedAt")) null else o.optLong("deletedAt")
        )
    }

    private fun jsonToAlarms(arr: JSONArray) = (0 until arr.length()).map { i ->
        val o = arr.getJSONObject(i)
        Alarm(
            id = o.getLong("id"),
            label = o.optString("label", ""),
            hour = o.getInt("hour"),
            minute = o.getInt("minute"),
            repeatDaysMask = o.optInt("repeatDaysMask", 0),
            isEnabled = o.optBoolean("isEnabled", true),
            soundUri = o.optString("soundUri", ""),
            vibrationPatternId = o.optString("vibrationPatternId", "default"),
            createdAt = o.optLong("createdAt", System.currentTimeMillis()),
            deletedAt = if (o.isNull("deletedAt")) null else o.optLong("deletedAt")
        )
    }

    private fun jsonToTimers(arr: JSONArray) = (0 until arr.length()).map { i ->
        val o = arr.getJSONObject(i)
        Timer(
            id = o.getLong("id"),
            label = o.optString("label", ""),
            durationSeconds = o.getInt("durationSeconds"),
            remainingSeconds = o.optInt("remainingSeconds", o.getInt("durationSeconds")),
            endTimeMillis = 0L,
            soundUri = o.optString("soundUri", ""),
            createdAt = o.optLong("createdAt", System.currentTimeMillis()),
            isPomo = o.optBoolean("isPomo", false),
            pomoWorkSecs = o.optInt("pomoWorkSecs", 1500),
            pomoShortBreakSecs = o.optInt("pomoShortBreakSecs", 300),
            pomoLongBreakSecs = o.optInt("pomoLongBreakSecs", 900),
            pomoSessionsPerRound = o.optInt("pomoSessionsPerRound", 4),
            pomoCurrentSession = o.optInt("pomoCurrentSession", 1),
            pomoPhase = o.optString("pomoPhase", "WORK"),
            deletedAt = if (o.isNull("deletedAt")) null else o.optLong("deletedAt")
        )
    }

    private fun jsonToTodos(arr: JSONArray) = (0 until arr.length()).map { i ->
        val o = arr.getJSONObject(i)
        Todo(
            id = o.getLong("id"),
            title = o.getString("title"),
            note = o.optString("note", ""),
            dueDate = o.optString("dueDate", ""),
            priority = o.optString("priority", "NONE"),
            isCompleted = o.optBoolean("isCompleted", false),
            completedAt = if (o.isNull("completedAt")) null else o.optLong("completedAt"),
            iconIndex = o.optInt("iconIndex", 0),
            isArchived = o.optBoolean("isArchived", false),
            soundUri = o.optString("soundUri", ""),
            vibrationPatternId = o.optString("vibrationPatternId", "default"),
            deletedAt = if (o.isNull("deletedAt")) null else o.optLong("deletedAt"),
            createdAt = o.optLong("createdAt", System.currentTimeMillis())
        )
    }

    private fun jsonToNotebooks(arr: JSONArray) = (0 until arr.length()).map { i ->
        val o = arr.getJSONObject(i)
        Notebook(
            id = o.getLong("id"),
            name = o.getString("name"),
            colorIndex = o.optInt("colorIndex", 0),
            colorArgb = if (o.isNull("colorArgb")) null else o.optInt("colorArgb"),
            createdAt = o.optLong("createdAt", System.currentTimeMillis()),
            updatedAt = o.optLong("updatedAt", System.currentTimeMillis()),
            deletedAt = if (o.isNull("deletedAt")) null else o.optLong("deletedAt")
        )
    }

    private fun jsonToNotes(arr: JSONArray) = (0 until arr.length()).map { i ->
        val o = arr.getJSONObject(i)
        Note(
            id = o.getLong("id"),
            notebookId = o.getLong("notebookId"),
            title = o.getString("title"),
            content = o.optString("content", ""),
            createdAt = o.optLong("createdAt", System.currentTimeMillis()),
            updatedAt = o.optLong("updatedAt", System.currentTimeMillis()),
            isPinned = o.optBoolean("isPinned", false),
            deletedAt = if (o.isNull("deletedAt")) null else o.optLong("deletedAt"),
            tags = o.optString("tags", ""),
            fontFamily = o.optString("fontFamily", "default"),
            fontSize = o.optString("fontSize", "normal")
        )
    }

    private fun journalEntriesToJson(entries: List<JournalEntry>) = JSONArray().also { arr ->
        entries.forEach { e ->
            arr.put(JSONObject().apply {
                put("id", e.id)
                put("date", e.date)
                put("title", e.title)
                put("content", e.content)
                put("mood", e.mood)
                put("habitId", e.habitId ?: JSONObject.NULL)
                put("createdAt", e.createdAt)
                put("updatedAt", e.updatedAt)
                put("tags", e.tags)
                put("photoUris", e.photoUris)
                put("feelings", e.feelings)
                put("deletedAt", e.deletedAt ?: JSONObject.NULL)
            })
        }
    }

    private fun jsonToJournalEntries(arr: JSONArray) = (0 until arr.length()).map { i ->
        val o = arr.getJSONObject(i)
        JournalEntry(
            id = o.getLong("id"),
            date = o.getString("date"),
            title = o.optString("title", ""),
            content = o.optString("content", ""),
            mood = o.optInt("mood", 0),
            habitId = if (o.isNull("habitId")) null else o.optLong("habitId"),
            createdAt = o.optLong("createdAt", System.currentTimeMillis()),
            updatedAt = o.optLong("updatedAt", System.currentTimeMillis()),
            tags = o.optString("tags", ""),
            photoUris = o.optString("photoUris", ""),
            feelings = o.optString("feelings", ""),
            deletedAt = if (o.isNull("deletedAt")) null else o.optLong("deletedAt")
        )
    }
}
