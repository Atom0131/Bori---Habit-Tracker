package com.apagon.rhythm.data.backup

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import com.apagon.rhythm.core.json.JSONArray
import com.apagon.rhythm.core.json.JSONObject
import com.apagon.rhythm.core.time.now
import kotlinx.datetime.LocalDate
import com.apagon.rhythm.data.db.DesktopHabitDatabase
import com.apagon.rhythm.data.model.Alarm
import com.apagon.rhythm.data.model.CalendarEvent
import com.apagon.rhythm.data.model.ChecklistItem
import com.apagon.rhythm.data.model.ChecklistItemCompletion
import com.apagon.rhythm.data.model.EventReminder
import com.apagon.rhythm.data.model.Habit
import com.apagon.rhythm.data.model.HabitCompletion
import com.apagon.rhythm.data.model.HabitFrequency
import com.apagon.rhythm.data.model.JournalEntry
import com.apagon.rhythm.data.model.Note
import com.apagon.rhythm.data.model.Notebook
import com.apagon.rhythm.data.model.NoteLink
import com.apagon.rhythm.data.model.Reminder
import com.apagon.rhythm.data.model.Timer
import com.apagon.rhythm.data.model.Todo
import com.apagon.rhythm.data.model.TodoSubtask
import com.apagon.rhythm.data.preferences.ThemePreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.crypto.AEADBadTagException

// Desktop-native counterpart to androidMain's BackupManager.kt — not a
// straight port. That class calls Android-only ReminderScheduler post-import
// (rescheduling AlarmManager entries), which desktop has no equivalent for —
// DesktopAlarmClockService (Stage 12) re-derives what's due by polling the DB
// on its own ticker, so there's nothing to explicitly reschedule after a
// restore. As of Stage 12, DesktopHabitDatabase carries all the same entities
// Android's exporter does (alarms/reminders/timers landed this stage), so the
// JSON shape now matches Android's field-for-field. Stage 2/3 of the
// backup/sync-prep plan added AES-256-GCM encryption (DesktopBackupCrypto,
// byte-compatible with Android's BackupCrypto) and the remaining
// EventReminder/NoteLink/TodoSubtask/field parity Android's BackupManager.kt
// already had.
class DesktopBackupManager(
    private val db: DesktopHabitDatabase,
    private val themePreferences: ThemePreferences
) : BackupManaging {

    // androidx.room.withTransaction (the KTX extension androidMain's BackupManager
    // uses) is Android-only — the multiplatform Room artifact only exposes the
    // lower-level Transactor API, so this rebuilds the same call shape on top of it.
    private suspend fun <R> withTransaction(block: suspend () -> R): R =
        db.useWriterConnection { transactor -> transactor.immediateTransaction { block() } }

    /** Exports and writes straight to an absolute file path — keeps java.io.File
     *  usage out of the commonMain ViewModel (iosArm64 targets are declared,
     *  even though dormant, and can't see java.io).
     *
     *  @param password when non-null, the written file is an encrypted envelope
     *  (see [DesktopBackupCrypto]) rather than plain JSON. */
    override suspend fun exportToPath(path: String, password: CharArray?) {
        val json = exportToJson(password)
        withContext(Dispatchers.IO) { File(path).writeText(json) }
    }

    /**
     * @return null on a clean restore, or a short warning/error message when part of it did not
     *   apply — matching [importFromJson]'s own contract.
     */
    override suspend fun importFromPath(path: String, password: CharArray?): String? {
        val json = withContext(Dispatchers.IO) { File(path).readText() }
        return importFromJson(json, password)
    }

    suspend fun exportToJson(password: CharArray? = null): String {
        val plain = buildBackupJson()
        return if (password == null) plain else DesktopBackupCrypto.encrypt(plain, password)
    }

    private suspend fun buildBackupJson(): String {
        val habitDao = db.habitDao()
        val todoDao = db.todoDao()
        val calendarDao = db.calendarEventDao()
        val notesDao = db.notesDao()
        val journalDao = db.journalDao()
        val alarmDao = db.alarmDao()
        val reminderDao = db.reminderDao()
        val timerDao = db.timerDao()
        val eventReminderDao = db.eventReminderDao()
        val noteLinkDao = db.noteLinkDao()
        val todoSubtaskDao = db.todoSubtaskDao()

        val habits = habitDao.getAllHabitsForBackup()
        val completions = habitDao.getAllCompletionsForBackup()
        val checklistItems = habitDao.getAllChecklistItemsForBackup()
        val checklistItemCompletions = habitDao.getAllChecklistItemCompletionsForBackup()
        val reminders = reminderDao.getAllRemindersForBackup()
        val calendarEvents = calendarDao.getAllEventsForBackup()
        val eventReminders = eventReminderDao.getAllForBackup()
        val alarms = alarmDao.getAllAlarmsForBackup()
        val timers = timerDao.getAllTimersForBackup()
        val todos = todoDao.getAllTodosForBackup()
        val todoSubtasks = todoSubtaskDao.getAllSubtasksForBackup()
        val notebooks = notesDao.getAllNotebooksForBackup()
        val notes = notesDao.getAllNotesForBackup()
        val journalEntries = journalDao.getAllForBackup()
        val noteLinks = noteLinkDao.getAllNoteLinksForBackup()
        val preferences = themePreferences.exportPreferences()

        // Desktop has no note-image/profile-picture feature at all (re-confirmed this stage via
        // grep across commonMain/desktopMain for "noteimage|profileimage|note_image|
        // profile_image" — zero matches). Emitting an empty object rather than omitting the key
        // entirely matches Android's own "absent means none" reading on import, and means
        // re-exporting an Android backup from desktop doesn't silently misrepresent images that
        // were never processed — they're preserved as "absent," not corrupted. profileImage is
        // omitted outright, matching what every pre-image-feature Android backup looks like.
        val noteImages = JSONObject()

        // Key order matches Android's BackupManager.kt exactly, for cross-platform diffability —
        // no functional effect since both sides read by key.
        return JSONObject().apply {
            put("version", 1)
            put("exportedAt", LocalDate.now().toString())
            put("habits", habitsToJson(habits))
            put("habitCompletions", completionsToJson(completions))
            put("checklistItems", checklistItemsToJson(checklistItems))
            put("checklistItemCompletions", checklistItemCompletionsToJson(checklistItemCompletions))
            put("reminders", remindersToJson(reminders))
            put("calendarEvents", calendarEventsToJson(calendarEvents))
            put("eventReminders", eventRemindersToJson(eventReminders))
            put("alarms", alarmsToJson(alarms))
            put("timers", timersToJson(timers))
            put("todos", todosToJson(todos))
            put("todoSubtasks", todoSubtasksToJson(todoSubtasks))
            put("notebooks", notebooksToJson(notebooks))
            put("notes", notesToJson(notes))
            put("journalEntries", journalEntriesToJson(journalEntries))
            put("noteLinks", noteLinksToJson(noteLinks))
            put("noteImages", noteImages)
            // No profileImage key — see comment above.
            put("preferences", preferences)
        }.toString(2)
    }

    suspend fun importFromJson(json: String, password: CharArray? = null): String? {
        val outer = JSONObject(json)

        // Decrypt BEFORE anything else touches the database — a wrong password must leave
        // nothing written. needsPassword-style detection is just isEncrypted() here; desktop
        // always prompts rather than remembering a password (no BackupKeyStore port).
        val root = if (DesktopBackupCrypto.isEncrypted(outer)) {
            if (password == null) {
                return "This backup is password protected. Please provide a password and try again."
            }
            try {
                JSONObject(DesktopBackupCrypto.decrypt(outer, password))
            } catch (e: AEADBadTagException) {
                return "Incorrect password."
            }
        } else outer

        withTransaction {
            val habitDao = db.habitDao()
            val todoDao = db.todoDao()
            val calendarDao = db.calendarEventDao()
            val notesDao = db.notesDao()
            val journalDao = db.journalDao()
            val alarmDao = db.alarmDao()
            val reminderDao = db.reminderDao()
            val timerDao = db.timerDao()
            val eventReminderDao = db.eventReminderDao()
            val noteLinkDao = db.noteLinkDao()
            val todoSubtaskDao = db.todoSubtaskDao()

            // Clear in FK-safe order (children before parents).
            habitDao.deleteAllChecklistItemCompletions()
            habitDao.deleteAllChecklistItems()
            habitDao.deleteAllCompletions()
            habitDao.deleteAllHabits()
            reminderDao.deleteAll()
            // Before the events themselves — event_reminders cascades off calendar_events, same
            // ordering reasoning as Android's BackupManager.
            eventReminderDao.deleteAll()
            calendarDao.deleteAll()
            alarmDao.deleteAll()
            timerDao.deleteAll()
            // Before the to-dos themselves, for the same cascade reason.
            todoSubtaskDao.deleteAllSubtasks()
            todoDao.deleteAll()

            // Insert in FK-safe order (parents before children).
            habitDao.insertAllHabits(jsonToHabits(root.optJSONArray("habits") ?: JSONArray()))
            habitDao.insertChecklistItems(jsonToChecklistItems(root.optJSONArray("checklistItems") ?: JSONArray()))
            habitDao.insertAllChecklistItemCompletions(jsonToChecklistItemCompletions(root.optJSONArray("checklistItemCompletions") ?: JSONArray()))
            habitDao.insertAllCompletions(jsonToCompletions(root.optJSONArray("habitCompletions") ?: JSONArray()))
            reminderDao.insertAll(jsonToReminders(root.optJSONArray("reminders") ?: JSONArray()))
            calendarDao.insertAll(jsonToCalendarEvents(root.optJSONArray("calendarEvents") ?: JSONArray()))
            // Strictly after the events — the foreign key would reject these otherwise. A backup
            // written before event reminders existed simply has no key here and restores fine.
            eventReminderDao.insertAll(jsonToEventReminders(root.optJSONArray("eventReminders") ?: JSONArray()))
            alarmDao.insertAll(jsonToAlarms(root.optJSONArray("alarms") ?: JSONArray()))
            timerDao.insertAll(jsonToTimers(root.optJSONArray("timers") ?: JSONArray()))
            todoDao.insertAll(jsonToTodos(root.optJSONArray("todos") ?: JSONArray()))
            // Strictly after the to-dos — same reasoning as eventReminders above.
            todoSubtaskDao.insertAllSubtasks(jsonToTodoSubtasks(root.optJSONArray("todoSubtasks") ?: JSONArray()))

            if (root.has("notebooks")) {
                notesDao.deleteAllNotes()
                notesDao.deleteAllNotebooks()
                // notes are being replaced; stale links must not survive.
                noteLinkDao.deleteAllNoteLinks()
                jsonToNotebooks(root.getJSONArray("notebooks")).forEach { notesDao.insertNotebook(it) }
                jsonToNotes(root.optJSONArray("notes") ?: JSONArray()).forEach { notesDao.insertNote(it) }
            }
            if (root.has("noteLinks")) {
                noteLinkDao.deleteAllNoteLinks()
                noteLinkDao.insertAllNoteLinks(jsonToNoteLinks(root.getJSONArray("noteLinks")))
            }
            if (root.has("journalEntries")) {
                journalDao.deleteAll()
                journalDao.insertAll(jsonToJournalEntries(root.getJSONArray("journalEntries")))
            }

            // noteImages/profileImage: desktop has no image-embedding feature to restore into.
            // Read tolerantly so importing a real Android backup with embedded images doesn't
            // crash — it just no-ops on them, same as every key this importer doesn't recognize.
            root.optJSONObject("noteImages")
            root.optJSONObject("profileImage")
        }
        // Applied after the transaction, same reasoning as Android's BackupManager: no DataStore
        // emission can race with the Room transaction and trigger a recomposition mid-write.
        //
        // Isolated from the entity import — a malformed preferences blob must not report "Import
        // failed" over entity data already committed above — but the failure is not swallowed,
        // matching Android's own fix for exactly that gap.
        var preferencesError: Exception? = null
        try {
            root.optJSONObject("preferences")?.let { themePreferences.importPreferences(it) }
        } catch (e: Exception) {
            preferencesError = e
        }

        return preferencesError?.let {
            "Your habits and notes were restored, but your settings could not be " +
                "(${it.message ?: it::class.simpleName})."
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
                put("syncId", h.syncId)
                put("updatedAt", h.updatedAt)
            })
        }
    }

    private fun completionsToJson(completions: List<HabitCompletion>) = JSONArray().also { arr ->
        completions.forEach { c ->
            arr.put(JSONObject().apply {
                put("id", c.id)
                put("habitId", c.habitId)
                put("dateCompleted", c.dateCompleted)
                put("syncId", c.syncId)
                put("updatedAt", c.updatedAt)
                put("deletedAt", c.deletedAt ?: JSONObject.NULL)
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

    private fun eventRemindersToJson(reminders: List<EventReminder>) = JSONArray().also { arr ->
        reminders.forEach { r ->
            arr.put(JSONObject().apply {
                put("id", r.id)
                put("eventId", r.eventId)
                put("minutesBefore", r.minutesBefore ?: JSONObject.NULL)
                put("absoluteDateTime", r.absoluteDateTime ?: JSONObject.NULL)
                put("soundUri", r.soundUri)
                put("vibrationPatternId", r.vibrationPatternId)
                put("createdAt", r.createdAt)
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

    private fun todoSubtasksToJson(subtasks: List<TodoSubtask>) = JSONArray().also { arr ->
        subtasks.forEach { s ->
            arr.put(JSONObject().apply {
                put("id", s.id)
                put("todoId", s.todoId)
                put("label", s.label)
                put("isDone", s.isDone)
                put("sortOrder", s.sortOrder)
                put("parentId", s.parentId ?: JSONObject.NULL)
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
                put("isPrivate", n.isPrivate)
                // vaultFolderName is portable and human-meaningful so it's carried across a
                // restore, matching Android exactly. folderDocUriCache is deliberately NOT
                // backed up, also matching Android: a per-document content/file-handle cache is
                // device-local and self-healing (re-resolved by name on next use), so carrying a
                // stale one across devices would only ever be wrong.
                put("vaultFolderName", n.vaultFolderName ?: JSONObject.NULL)
                put("parentId", n.parentId ?: JSONObject.NULL)
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
                // bodyPreview/filePointer/uuid carried for the same reason as notebooks' fields
                // above, matching Android's notesToJson exactly. fileDocUriCache/fileSyncedAt are
                // deliberately NOT backed up — device-local and self-healing, same exclusion
                // Android applies to those two fields.
                put("bodyPreview", n.bodyPreview)
                put("filePointer", n.filePointer ?: JSONObject.NULL)
                put("uuid", n.uuid ?: JSONObject.NULL)
            })
        }
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

    private fun noteLinksToJson(links: List<NoteLink>) = JSONArray().also { arr ->
        links.forEach { l ->
            arr.put(JSONObject().apply {
                put("id", l.id)
                put("sourceNoteId", l.sourceNoteId)
                put("targetNoteId", l.targetNoteId)
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
                put("dismissMission", a.dismissMission)
                put("missionDifficulty", a.missionDifficulty)
                put("createdAt", a.createdAt)
                put("deletedAt", a.deletedAt ?: JSONObject.NULL)
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
                put("noteId", r.noteId ?: JSONObject.NULL)
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
                put("vibrationPatternId", t.vibrationPatternId)
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
            vibrationPatternId = o.optString("vibrationPatternId", "default"),
            // A pre-sync-feature backup has neither key — mint a fresh syncId (there is no prior
            // one to recover) and fall back to createdAt for updatedAt, matching Android's own
            // jsonToHabits fallback exactly.
            syncId = if (o.has("syncId")) o.getString("syncId") else uuidString(),
            updatedAt = o.optLong("updatedAt", o.optLong("createdAt", System.currentTimeMillis()))
        )
    }

    private fun jsonToCompletions(arr: JSONArray) = (0 until arr.length()).map { i ->
        val o = arr.getJSONObject(i)
        HabitCompletion(
            id = o.getLong("id"),
            habitId = o.getLong("habitId"),
            dateCompleted = o.getString("dateCompleted"),
            syncId = if (o.has("syncId")) o.getString("syncId") else uuidString(),
            updatedAt = o.optLong("updatedAt", System.currentTimeMillis()),
            deletedAt = if (o.isNull("deletedAt")) null else o.optLong("deletedAt")
        )
    }

    @OptIn(kotlin.uuid.ExperimentalUuidApi::class)
    private fun uuidString(): String = kotlin.uuid.Uuid.random().toString()

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

    private fun jsonToEventReminders(arr: JSONArray) = (0 until arr.length()).map { i ->
        val o = arr.getJSONObject(i)
        EventReminder(
            id = o.getLong("id"),
            eventId = o.getLong("eventId"),
            minutesBefore = if (o.isNull("minutesBefore")) null else o.optInt("minutesBefore"),
            absoluteDateTime = if (o.isNull("absoluteDateTime")) null else o.optString("absoluteDateTime"),
            soundUri = o.optString("soundUri", ""),
            vibrationPatternId = o.optString("vibrationPatternId", "default"),
            createdAt = o.optLong("createdAt", System.currentTimeMillis())
        )
    }

    private fun jsonToTodos(arr: JSONArray) = (0 until arr.length()).map { i ->
        val o = arr.getJSONObject(i)
        Todo(
            id = o.getLong("id"),
            title = o.getString("title"),
            note = o.optString("note", ""),
            dueDate = o.optString("dueDate", ""),
            priority = normalizePriority(o.optString("priority", "")),
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

    // Android's own jsonToTodos reader (BackupManager.kt) defaults a missing priority key to
    // "LOW", and Android's Todo has no "NONE" value at all. Desktop's Todo keeps NONE (its own
    // default, no enum change), but to round-trip an Android-authored backup sensibly, a missing
    // key OR an explicit "NONE" (which Android would never write but desktop itself might have,
    // pre-parity) both read as LOW here — matching the tolerant-read behaviour Stage 1 called for,
    // without changing the entity's own default.
    private fun normalizePriority(raw: String): String =
        if (raw.isBlank() || raw == "NONE") "LOW" else raw

    private fun jsonToTodoSubtasks(arr: JSONArray) = (0 until arr.length()).map { i ->
        val o = arr.getJSONObject(i)
        TodoSubtask(
            id = o.getLong("id"),
            todoId = o.getLong("todoId"),
            label = o.optString("label", ""),
            isDone = o.optBoolean("isDone", false),
            sortOrder = o.optInt("sortOrder", 0),
            parentId = if (o.isNull("parentId")) null else o.optLong("parentId")
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
            deletedAt = if (o.isNull("deletedAt")) null else o.optLong("deletedAt"),
            // Absent in a pre-parity backup; false is the safe reading of a missing key.
            isPrivate = o.optBoolean("isPrivate", false),
            // Absent in a pre-parity backup; null forces a re-resolve/recreate by name rather
            // than trust a foreign device's stale folder reference.
            vaultFolderName = if (o.isNull("vaultFolderName")) null else o.optString("vaultFolderName"),
            // folderDocUriCache is never read from a backup — see notebooksToJson's comment;
            // every restored notebook gets null here and self-heals on next vault write.
            parentId = if (o.isNull("parentId")) null else o.optLong("parentId")
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
            fontSize = o.optString("fontSize", "normal"),
            bodyPreview = o.optString("bodyPreview", ""),
            filePointer = if (o.isNull("filePointer")) null else o.optString("filePointer"),
            // fileDocUriCache/fileSyncedAt are never read from a backup — see notesToJson's
            // comment; both stay at their entity defaults (null) and self-heal on next use.
            uuid = if (o.isNull("uuid")) null else o.optString("uuid")
        )
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

    private fun jsonToNoteLinks(arr: JSONArray) = (0 until arr.length()).map { i ->
        val o = arr.getJSONObject(i)
        NoteLink(
            id = o.getLong("id"),
            sourceNoteId = o.getLong("sourceNoteId"),
            targetNoteId = o.getLong("targetNoteId")
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
            // Defaults keep backups written before missions existed restoring cleanly, matching
            // Android's own jsonToAlarms defaults exactly.
            dismissMission = o.optString("dismissMission", "none"),
            missionDifficulty = o.optInt("missionDifficulty", 1),
            createdAt = o.optLong("createdAt", System.currentTimeMillis()),
            deletedAt = if (o.isNull("deletedAt")) null else o.optLong("deletedAt")
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
            deletedAt = if (o.isNull("deletedAt")) null else o.optLong("deletedAt"),
            noteId = if (o.isNull("noteId")) null else o.optLong("noteId")
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
            vibrationPatternId = o.optString("vibrationPatternId", "default"),
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
}
