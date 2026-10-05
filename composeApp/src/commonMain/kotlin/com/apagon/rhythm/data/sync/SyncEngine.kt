package com.apagon.rhythm.data.sync

import com.apagon.rhythm.core.json.JSONObject
import com.apagon.rhythm.core.time.System
import com.apagon.rhythm.data.model.ChecklistItem
import com.apagon.rhythm.data.model.ChecklistItemCompletion
import com.apagon.rhythm.data.model.EventReminder
import com.apagon.rhythm.data.model.HabitCompletion
import com.apagon.rhythm.data.model.NoteLink
import com.apagon.rhythm.data.model.TodoSubtask
import com.apagon.rhythm.data.preferences.ThemePreferences
import com.apagon.rhythm.platform.VaultFileSync
import com.apagon.rhythm.data.repository.AlarmRepository
import com.apagon.rhythm.data.repository.CalendarEventRepository
import com.apagon.rhythm.data.repository.HabitRepository
import com.apagon.rhythm.data.repository.JournalRepository
import com.apagon.rhythm.data.repository.NotesRepository
import com.apagon.rhythm.data.repository.ReminderRepository
import com.apagon.rhythm.data.repository.TimerRepository
import com.apagon.rhythm.data.repository.TodoRepository
import kotlinx.coroutines.flow.first

/**
 * Transport-agnostic sync logic — SyncServer/SyncClient (jvmMain, Ktor) just
 * exchange the SyncBatch this produces/consumes over a WebSocket. Conflict
 * policy is plain last-write-wins by updatedAt (habits are flat structured
 * records, not freeform text — little to lose by not forking, and the usage
 * model — manual trigger, both devices foregrounded — makes same-window
 * concurrent edits rare; see the Stage 4 plan for the full reasoning).
 *
 * Stage 2 extends the same insert-or-update-by-syncId/last-write-wins-by-updatedAt pattern to every
 * top-level entity (Todo, CalendarEvent, Alarm, Timer, Reminder, Notebook, Note, JournalEntry).
 * Five child tables (TodoSubtask, EventReminder, ChecklistItem, ChecklistItemCompletion, NoteLink)
 * have no `deletedAt` of their own by design and sync via **full-replace-per-parent** instead:
 * whenever a parent is freshly inserted or wins last-write-wins, its complete current child set is
 * sent/applied wholesale (existing children deleted, incoming set inserted) — the same operation
 * backup restore already performs for these tables. A parent that loses last-write-wins (local copy
 * is newer or tied) keeps its local children untouched — no independent "did only a child change"
 * tracking, matching the plan's instruction not to over-engineer this for v1.
 *
 * **Privacy boundary**: a Notebook/Note with `isPrivate == true` is excluded from
 * [buildOutgoingBatch] at the query itself (see NotesDao/NotesRepository) and is defensively
 * skipped again in [applyIncomingBatch] even if somehow present on the wire — this is a privacy
 * rule, not an optimization.
 *
 * Processing order in [applyIncomingBatch] matters where one entity resolves another by syncId:
 * Habits (+ nested checklist items/completions) before Completions; Notebooks before Notes (+
 * nested links) before Reminders (which may reference a note) and before JournalEntries (which may
 * reference a habit, already applied in the first step).
 */
class SyncEngine(
    private val habitRepository: HabitRepository,
    private val todoRepository: TodoRepository,
    private val calendarEventRepository: CalendarEventRepository,
    private val alarmRepository: AlarmRepository,
    private val timerRepository: TimerRepository,
    private val reminderRepository: ReminderRepository,
    private val notesRepository: NotesRepository,
    private val journalRepository: JournalRepository,
    private val preferences: SyncPreferences,
    private val themePreferences: ThemePreferences,
    private val vaultFileSync: VaultFileSync
) {
    /**
     * Always requeried fresh against [SyncPreferences.getLastSyncedAt] — never
     * a cached delta carried across calls. Caching a "what changed" snapshot
     * across a reconnect is exactly the class of bug that made rows silently
     * vanish in the live app's vault-sync feature (rhythm-stage3-reconcile-
     * vanish-bug.md) — the fix there was the same lesson: always re-resolve
     * from source, never trust a stale handle.
     */
    suspend fun buildOutgoingBatch(deviceId: String): SyncBatch {
        val since = preferences.getLastSyncedAt()

        val habits = habitRepository.getHabitsUpdatedSince(since)
        val habitDtos = habits.map { habit ->
            val items = habitRepository.getItemsForHabitSync(habit.id)
            val itemDtos = items.map { item ->
                val completions = habitRepository.getCompletionsForItemSync(item.id)
                item.toDto(completions.map { it.toDto() })
            }
            habit.toDto(itemDtos)
        }

        val completions = habitRepository.getCompletionsUpdatedSince(since)
        val completionDtos = completions.mapNotNull { completion ->
            val habit = habitRepository.getHabitByIdRaw(completion.habitId) ?: return@mapNotNull null
            completion.toDto(habitSyncId = habit.syncId)
        }

        val todos = todoRepository.getTodosUpdatedSince(since)
        val todoDtos = todos.map { todo ->
            val subtasks = todoRepository.getSubtasksForTodoSync(todo.id)
            val syncIdById = subtasks.associate { it.id to it.syncId }
            val subtaskDtos = subtasks.map { sub -> sub.toDto(parentSyncId = sub.parentId?.let { syncIdById[it] }) }
            todo.toDto(subtaskDtos)
        }

        val events = calendarEventRepository.getEventsUpdatedSince(since)
        val eventDtos = events.map { event ->
            val reminders = calendarEventRepository.getRemindersForEventSync(event.id)
            event.toDto(reminders.map { it.toDto() })
        }

        val alarmDtos = alarmRepository.getAlarmsUpdatedSince(since).map { it.toDto() }
        val timerDtos = timerRepository.getTimersUpdatedSince(since).map { it.toDto() }

        val reminders = reminderRepository.getRemindersUpdatedSince(since)
        val reminderDtos = reminders.map { reminder ->
            val noteSyncId = reminder.noteId?.let { notesRepository.getNoteById(it)?.syncId }
            reminder.toDto(noteSyncId)
        }

        // Private notebooks/notes are already excluded by the repository query — not re-filtered
        // here, so there's exactly one place that decision is made for the outgoing side.
        val notebooks = notesRepository.getNonPrivateNotebooksUpdatedSince(since)
        val notebookDtos = notebooks.map { nb ->
            // Stamp firstSyncedAt the moment a notebook actually crosses the wire — this is what
            // takes it out of eligibility for the unsynced-notebook-by-name dedup below (see
            // Notebook.firstSyncedAt's KDoc and applyIncomingBatch's notebook-matching block).
            val marked = if (nb.firstSyncedAt == null) {
                nb.copy(firstSyncedAt = System.currentTimeMillis()).also { notesRepository.updateNotebookFromSync(it) }
            } else nb
            val parentSyncId = marked.parentId?.let { notesRepository.getNotebookById(it)?.syncId }
            marked.toDto(parentSyncId)
        }

        val notes = notesRepository.getNonPrivateNotesUpdatedSince(since)
        val noteDtos = notes.mapNotNull { note ->
            val notebook = notesRepository.getNotebookById(note.notebookId) ?: return@mapNotNull null
            val links = notesRepository.getLinksForSourceNoteSync(note.id)
            val linkDtos = links.mapNotNull { link ->
                val target = notesRepository.getNoteById(link.targetNoteId) ?: return@mapNotNull null
                NoteLinkDto(syncId = link.syncId, targetNoteSyncId = target.syncId)
            }
            note.toDto(notebookSyncId = notebook.syncId, links = linkDtos)
        }

        val journalEntries = journalRepository.getEntriesUpdatedSince(since)
        val journalDtos = journalEntries.map { entry ->
            val habitSyncId = entry.habitId?.let { habitRepository.getHabitByIdRaw(it)?.syncId }
            entry.toDto(habitSyncId)
        }

        // Theme/appearance preferences (Stage 3) — whole-blob, only included when something
        // exportable changed locally since the last sync, same "newer than `since`" gate as every
        // entity list above.
        val preferencesUpdatedAt = themePreferences.preferencesUpdatedAt.first()
        val preferencesDto = if (preferencesUpdatedAt > since) {
            PreferencesDto(
                updatedAt = preferencesUpdatedAt,
                values = themePreferences.exportPreferencesForSync().toString()
            )
        } else {
            null
        }

        // Vault file sync (Stage 3.5) — optional, default-OFF, independently opted into per device:
        // only populated when THIS device has the toggle on. The receiving side makes its own
        // independent check before acting on it (see applyIncomingBatch) — both ends must opt in.
        // Desktop's own exportFiles() always returns empty today (see DesktopVaultFileSync's own
        // KDoc) — a documented, intentional asymmetry with Android, which has a real vault to
        // export from.
        val vaultFilesDto: List<VaultFileDto>? = if (themePreferences.vaultFileSyncEnabled.first()) {
            vaultFileSync.exportFiles().files
        } else {
            null
        }

        return SyncBatch(
            deviceId = deviceId,
            habits = habitDtos,
            completions = completionDtos,
            todos = todoDtos,
            calendarEvents = eventDtos,
            alarms = alarmDtos,
            timers = timerDtos,
            reminders = reminderDtos,
            notebooks = notebookDtos,
            notes = noteDtos,
            journalEntries = journalDtos,
            preferences = preferencesDto,
            vaultFiles = vaultFilesDto
        )
    }

    suspend fun applyIncomingBatch(batch: SyncBatch): SyncResult {
        var habitsInserted = 0
        var habitsUpdated = 0
        var completionsInserted = 0
        var completionsUpdated = 0
        var otherInserted = 0
        var otherUpdated = 0

        // ── Habits (+ nested checklist items/completions) ───────────────────────────────────────
        for (dto in batch.habits) {
            val existing = habitRepository.getHabitBySyncId(dto.syncId)
            val localHabitId: Long
            val applied: Boolean
            when {
                existing == null -> {
                    localHabitId = habitRepository.insertHabitFromSync(dto.toEntity())
                    habitsInserted++
                    applied = true
                }
                dto.updatedAt > existing.updatedAt -> {
                    habitRepository.updateHabitFromSync(dto.toEntity().copy(id = existing.id))
                    localHabitId = existing.id
                    habitsUpdated++
                    applied = true
                }
                else -> {
                    localHabitId = existing.id
                    applied = false
                }
            }
            if (applied) {
                habitRepository.deleteAllItemsForHabitSync(localHabitId)
                for (itemDto in dto.checklistItems) {
                    val itemId = habitRepository.insertChecklistItemFromSync(
                        ChecklistItem(
                            habitId = localHabitId,
                            label = itemDto.label,
                            sortOrder = itemDto.sortOrder,
                            syncId = itemDto.syncId,
                            updatedAt = itemDto.updatedAt
                        )
                    )
                    for (compDto in itemDto.completions) {
                        habitRepository.insertItemCompletionFromSync(
                            ChecklistItemCompletion(
                                itemId = itemId,
                                dateCompleted = compDto.dateCompleted,
                                syncId = compDto.syncId,
                                updatedAt = compDto.updatedAt
                            )
                        )
                    }
                }
            }
        }

        // ── Habit completions ─────────────────────────────────────────────────────────────────
        for (dto in batch.completions) {
            val habit = habitRepository.getHabitBySyncId(dto.habitSyncId)
            if (habit == null) {
                // The owning habit isn't known on this device yet. Shouldn't happen in the normal
                // two-device flow (habits are applied above, in the same batch, before completions
                // are ever processed) — defensive drop, not a silent data loss: this completion is
                // still present in the peer's own database and will be resent on any future round.
                continue
            }
            val existing = habitRepository.getCompletionBySyncId(dto.syncId)
            when {
                existing == null -> {
                    habitRepository.insertCompletionFromSync(
                        HabitCompletion(
                            habitId = habit.id,
                            dateCompleted = dto.dateCompleted,
                            syncId = dto.syncId,
                            updatedAt = dto.updatedAt,
                            deletedAt = dto.deletedAt
                        )
                    )
                    completionsInserted++
                }
                dto.updatedAt > existing.updatedAt -> {
                    habitRepository.updateCompletionFromSync(existing, existing.copy(updatedAt = dto.updatedAt, deletedAt = dto.deletedAt))
                    completionsUpdated++
                }
            }
        }

        // ── Notebooks (privacy boundary enforced here, defensively, in addition to the outgoing
        //    side already excluding them) ────────────────────────────────────────────────────────
        for (dto in batch.notebooks) {
            if (dto.isPrivate) continue
            val existing = notesRepository.getNotebookBySyncId(dto.syncId)
            if (existing != null && existing.isPrivate) continue // never let sync touch a private notebook
            val localParentId = dto.parentSyncId?.let { notesRepository.getNotebookBySyncId(it)?.id }
            when {
                existing == null -> {
                    // First-merge dedup: before inserting a new notebook, check for a local one
                    // with the same name that's never synced before (firstSyncedAt still null) —
                    // plain syncId matching can't tell "these are the same notebook" apart from
                    // "these are coincidentally named alike" when neither side has a shared
                    // history yet. Adopting the incoming syncId onto the existing row instead of
                    // inserting a duplicate is safe specifically because it only matches a row
                    // that's never synced — once a notebook has synced, a same-named-but-different
                    // notebook created later must stay distinct. Ported from Android's SyncEngine
                    // (commit 6f58f5d); see Notebook.firstSyncedAt's KDoc for why this is gated on
                    // that field rather than `syncId IS NULL` the way Android's fix is.
                    val unsyncedMatch = notesRepository.getUnsyncedNotebookByName(dto.name)
                    when {
                        unsyncedMatch == null -> {
                            notesRepository.insertNotebookFromSync(
                                dto.toEntity(localParentId).copy(firstSyncedAt = System.currentTimeMillis())
                            )
                            otherInserted++
                        }
                        // Still last-write-wins for content — a first merge only decides identity
                        // (which row owns this syncId going forward), not which side's edits win.
                        dto.updatedAt > unsyncedMatch.updatedAt -> {
                            notesRepository.updateNotebookFromSync(
                                dto.toEntity(localParentId).copy(id = unsyncedMatch.id, firstSyncedAt = System.currentTimeMillis())
                            )
                            otherUpdated++
                        }
                        else -> {
                            // Local copy is newer or tied — keep its content, but it must adopt
                            // the shared syncId now or the next sync round duplicates it again.
                            notesRepository.updateNotebookFromSync(
                                unsyncedMatch.copy(syncId = dto.syncId, firstSyncedAt = System.currentTimeMillis())
                            )
                            otherUpdated++
                        }
                    }
                }
                dto.updatedAt > existing.updatedAt -> {
                    notesRepository.updateNotebookFromSync(
                        dto.toEntity(localParentId).copy(
                            id = existing.id,
                            firstSyncedAt = existing.firstSyncedAt ?: System.currentTimeMillis()
                        )
                    )
                    otherUpdated++
                }
            }
        }

        // ── Notes (+ nested links; privacy boundary via the owning notebook) ──────────────────────
        // Two passes: a link's target may be a note elsewhere in this very batch, which doesn't
        // exist on this device until ITS OWN upsert runs — resolving links inline in a single pass
        // would silently drop any link whose target note happens to sort later in batch.notes.
        val notesNeedingLinkReplace = mutableListOf<Pair<NoteDto, Long>>()
        for (dto in batch.notes) {
            val notebook = notesRepository.getNotebookBySyncId(dto.notebookSyncId) ?: continue
            if (notebook.isPrivate) continue
            val existing = notesRepository.getNoteBySyncId(dto.syncId)
            when {
                existing == null -> {
                    val localNoteId = notesRepository.insertNoteFromSync(dto.toEntity(notebook.id))
                    otherInserted++
                    notesNeedingLinkReplace.add(dto to localNoteId)
                }
                dto.updatedAt > existing.updatedAt -> {
                    notesRepository.updateNoteFromSync(dto.toEntity(notebook.id).copy(id = existing.id))
                    otherUpdated++
                    notesNeedingLinkReplace.add(dto to existing.id)
                }
                // else: local copy is newer or tied — keep local note AND local links untouched.
            }
        }
        for ((dto, localNoteId) in notesNeedingLinkReplace) {
            val linkEntities = dto.links.mapNotNull { linkDto ->
                val target = notesRepository.getNoteBySyncId(linkDto.targetNoteSyncId) ?: return@mapNotNull null
                NoteLink(sourceNoteId = localNoteId, targetNoteId = target.id, syncId = linkDto.syncId)
            }
            notesRepository.replaceLinksForSourceNoteFromSync(localNoteId, linkEntities)
        }

        // ── Todos (+ nested subtasks, two-pass by syncId to resolve the one extra nesting level) ──
        for (dto in batch.todos) {
            val existing = todoRepository.getTodoBySyncId(dto.syncId)
            val localTodoId: Long
            val applied: Boolean
            when {
                existing == null -> {
                    localTodoId = todoRepository.insertTodoFromSync(dto.toEntity())
                    otherInserted++
                    applied = true
                }
                dto.updatedAt > existing.updatedAt -> {
                    todoRepository.updateTodoFromSync(dto.toEntity().copy(id = existing.id))
                    localTodoId = existing.id
                    otherUpdated++
                    applied = true
                }
                else -> {
                    localTodoId = existing.id
                    applied = false
                }
            }
            if (applied) {
                todoRepository.deleteSubtasksForTodoSync(localTodoId)
                val syncIdToLocalId = mutableMapOf<String, Long>()
                for (sub in dto.subtasks.filter { it.parentSyncId == null }) {
                    val id = todoRepository.insertSubtaskFromSync(
                        TodoSubtask(
                            todoId = localTodoId,
                            label = sub.label,
                            isDone = sub.isDone,
                            sortOrder = sub.sortOrder,
                            parentId = null,
                            syncId = sub.syncId,
                            updatedAt = sub.updatedAt
                        )
                    )
                    syncIdToLocalId[sub.syncId] = id
                }
                for (sub in dto.subtasks.filter { it.parentSyncId != null }) {
                    val parentId = syncIdToLocalId[sub.parentSyncId]
                    todoRepository.insertSubtaskFromSync(
                        TodoSubtask(
                            todoId = localTodoId,
                            label = sub.label,
                            isDone = sub.isDone,
                            sortOrder = sub.sortOrder,
                            parentId = parentId,
                            syncId = sub.syncId,
                            updatedAt = sub.updatedAt
                        )
                    )
                }
            }
        }

        // ── Calendar events (+ nested reminders) ─────────────────────────────────────────────────
        for (dto in batch.calendarEvents) {
            val existing = calendarEventRepository.getEventBySyncId(dto.syncId)
            val localEventId: Long
            val applied: Boolean
            when {
                existing == null -> {
                    localEventId = calendarEventRepository.insertEventFromSync(dto.toEntity())
                    otherInserted++
                    applied = true
                }
                dto.updatedAt > existing.updatedAt -> {
                    calendarEventRepository.updateEventFromSync(dto.toEntity().copy(id = existing.id))
                    localEventId = existing.id
                    otherUpdated++
                    applied = true
                }
                else -> {
                    localEventId = existing.id
                    applied = false
                }
            }
            if (applied) {
                val reminders = dto.reminders.map { r ->
                    EventReminder(
                        eventId = localEventId,
                        minutesBefore = r.minutesBefore,
                        absoluteDateTime = r.absoluteDateTime,
                        soundUri = r.soundUri,
                        vibrationPatternId = r.vibrationPatternId,
                        createdAt = r.createdAt,
                        syncId = r.syncId,
                        updatedAt = r.updatedAt
                    )
                }
                calendarEventRepository.replaceRemindersForEventFromSync(localEventId, reminders)
            }
        }

        // ── Alarms ────────────────────────────────────────────────────────────────────────────
        for (dto in batch.alarms) {
            val existing = alarmRepository.getAlarmBySyncId(dto.syncId)
            when {
                existing == null -> {
                    alarmRepository.insertAlarmFromSync(dto.toEntity())
                    otherInserted++
                }
                dto.updatedAt > existing.updatedAt -> {
                    alarmRepository.updateAlarmFromSync(dto.toEntity().copy(id = existing.id))
                    otherUpdated++
                }
            }
        }

        // ── Timers ────────────────────────────────────────────────────────────────────────────
        for (dto in batch.timers) {
            val existing = timerRepository.getTimerBySyncId(dto.syncId)
            when {
                existing == null -> {
                    timerRepository.insertTimerFromSync(dto.toEntity())
                    otherInserted++
                }
                dto.updatedAt > existing.updatedAt -> {
                    timerRepository.updateTimerFromSync(dto.toEntity().copy(id = existing.id))
                    otherUpdated++
                }
            }
        }

        // ── Reminders (may reference a note, already applied above) ─────────────────────────────
        for (dto in batch.reminders) {
            val localNoteId = dto.noteSyncId?.let { notesRepository.getNoteBySyncId(it)?.id }
            val existing = reminderRepository.getReminderBySyncId(dto.syncId)
            when {
                existing == null -> {
                    reminderRepository.insertReminderFromSync(dto.toEntity(localNoteId))
                    otherInserted++
                }
                dto.updatedAt > existing.updatedAt -> {
                    reminderRepository.updateReminderFromSync(dto.toEntity(localNoteId).copy(id = existing.id))
                    otherUpdated++
                }
            }
        }

        // ── Journal entries (may reference a habit, already applied above) ──────────────────────
        for (dto in batch.journalEntries) {
            val localHabitId = dto.habitSyncId?.let { habitRepository.getHabitBySyncId(it)?.id }
            val existing = journalRepository.getEntryBySyncId(dto.syncId)
            when {
                existing == null -> {
                    journalRepository.insertEntryFromSync(dto.toEntity(localHabitId))
                    otherInserted++
                }
                dto.updatedAt > existing.updatedAt -> {
                    journalRepository.updateEntryFromSync(dto.toEntity(localHabitId).copy(id = existing.id))
                    otherUpdated++
                }
            }
        }

        // ── Theme/appearance preferences (whole-blob last-write-wins, not per-key merging) ───────
        val incomingPreferences = batch.preferences
        if (incomingPreferences != null &&
            incomingPreferences.updatedAt > themePreferences.preferencesUpdatedAt.first()
        ) {
            themePreferences.importPreferencesForSync(JSONObject(incomingPreferences.values))
            themePreferences.setPreferencesUpdatedAtFromSync(incomingPreferences.updatedAt)
            otherUpdated++
        }

        // ── Vault file sync (Stage 3.5) — BOTH ends must opt in, not just the sender ─────────────
        val incomingVaultFiles = batch.vaultFiles
        if (!incomingVaultFiles.isNullOrEmpty() && themePreferences.vaultFileSyncEnabled.first()) {
            otherUpdated += vaultFileSync.writeIncomingFiles(incomingVaultFiles)
        }

        return SyncResult(
            habitsInserted = habitsInserted,
            habitsUpdated = habitsUpdated,
            completionsInserted = completionsInserted,
            completionsUpdated = completionsUpdated,
            otherInserted = otherInserted,
            otherUpdated = otherUpdated
        )
    }

    suspend fun markSynced() {
        preferences.setLastSyncedAt(System.currentTimeMillis())
    }
}
