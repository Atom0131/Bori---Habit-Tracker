package com.apagon.rhythm.data.sync

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
import com.apagon.rhythm.data.model.NoteLink
import com.apagon.rhythm.data.model.Notebook
import com.apagon.rhythm.data.model.Reminder
import com.apagon.rhythm.data.model.Timer
import com.apagon.rhythm.data.model.Todo
import com.apagon.rhythm.data.model.TodoSubtask
import kotlinx.serialization.Serializable

/**
 * Wire shapes for the sync engine (Stage 2). Deliberately flat, plain data —
 * no Ktor/transport types here, so this file (and SyncEngine.kt) stay usable
 * from commonMain even though the actual transport (SyncServer/SyncClient)
 * lives in jvmMain only.
 *
 * Every DTO's local `id` is never serialized — `syncId` is the cross-device identity, and a local
 * autoincrement Room id is only ever meaningful within one device's own database. Any field that
 * references another row by local id on the entity (e.g. `HabitCompletion.habitId`,
 * `Note.notebookId`, `Reminder.noteId`, `JournalEntry.habitId`) is carried here as that row's
 * `syncId` instead (`habitSyncId`, `notebookSyncId`, `noteSyncId`, `habitSyncId`) — same principle
 * as the existing `HabitCompletionDto.habitSyncId`.
 *
 * The five full-replace-per-parent child tables (`TodoSubtask`, `EventReminder`, `ChecklistItem`,
 * `ChecklistItemCompletion`, `NoteLink`) are nested under their parent's DTO rather than flattened
 * into their own `SyncBatch` list — see SyncEngine's own KDoc for the replace semantics.
 */
@Serializable
data class HabitDto(
    val syncId: String,
    val name: String,
    val description: String,
    val frequency: String,
    val targetDaysPerWeek: Int,
    val targetDaysPerMonth: Int,
    val weekDaysMask: Int,
    val monthDaysMask: Int,
    val reminderTime: String?,
    val createdAt: Long,
    val isActive: Boolean,
    val isChecklist: Boolean,
    val colorIndex: Int,
    val colorArgb: Int?,
    val durationDays: Int,
    val iconIndex: Int,
    val deletedAt: Long?,
    val soundUri: String,
    val vibrationPatternId: String,
    val updatedAt: Long,
    /** Only meaningful when [isChecklist] — full-replace-per-parent, see SyncEngine. */
    val checklistItems: List<ChecklistItemDto> = emptyList()
)

fun Habit.toDto(checklistItems: List<ChecklistItemDto> = emptyList()) = HabitDto(
    syncId = syncId,
    name = name,
    description = description,
    frequency = frequency.name,
    targetDaysPerWeek = targetDaysPerWeek,
    targetDaysPerMonth = targetDaysPerMonth,
    weekDaysMask = weekDaysMask,
    monthDaysMask = monthDaysMask,
    reminderTime = reminderTime,
    createdAt = createdAt,
    isActive = isActive,
    isChecklist = isChecklist,
    colorIndex = colorIndex,
    colorArgb = colorArgb,
    durationDays = durationDays,
    iconIndex = iconIndex,
    deletedAt = deletedAt,
    soundUri = soundUri,
    vibrationPatternId = vibrationPatternId,
    updatedAt = updatedAt,
    checklistItems = checklistItems
)

/** id is left at Room's default (0 = autogenerate) — sync identity is syncId, never the local row id, which is only ever meaningful within one device's own database. */
fun HabitDto.toEntity() = Habit(
    name = name,
    description = description,
    frequency = HabitFrequency.valueOf(frequency),
    targetDaysPerWeek = targetDaysPerWeek,
    targetDaysPerMonth = targetDaysPerMonth,
    weekDaysMask = weekDaysMask,
    monthDaysMask = monthDaysMask,
    reminderTime = reminderTime,
    createdAt = createdAt,
    isActive = isActive,
    isChecklist = isChecklist,
    colorIndex = colorIndex,
    colorArgb = colorArgb,
    durationDays = durationDays,
    iconIndex = iconIndex,
    deletedAt = deletedAt,
    soundUri = soundUri,
    vibrationPatternId = vibrationPatternId,
    syncId = syncId,
    updatedAt = updatedAt
)

@Serializable
data class HabitCompletionDto(
    val syncId: String,
    /** The owning habit's syncId, NOT its local Room id — ids are per-database and collide across two independent SQLite files. */
    val habitSyncId: String,
    val dateCompleted: String,
    val updatedAt: Long,
    val deletedAt: Long?
)

fun HabitCompletion.toDto(habitSyncId: String) = HabitCompletionDto(
    syncId = syncId,
    habitSyncId = habitSyncId,
    dateCompleted = dateCompleted,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)

// ── Checklist items / completions — nested under HabitDto, full-replace-per-parent ─────────────

@Serializable
data class ChecklistItemCompletionDto(
    val syncId: String,
    val dateCompleted: String,
    val updatedAt: Long
)

fun ChecklistItemCompletion.toDto() = ChecklistItemCompletionDto(
    syncId = syncId,
    dateCompleted = dateCompleted,
    updatedAt = updatedAt
)

@Serializable
data class ChecklistItemDto(
    val syncId: String,
    val label: String,
    val sortOrder: Int,
    val updatedAt: Long,
    val completions: List<ChecklistItemCompletionDto> = emptyList()
)

fun ChecklistItem.toDto(completions: List<ChecklistItemCompletionDto> = emptyList()) = ChecklistItemDto(
    syncId = syncId,
    label = label,
    sortOrder = sortOrder,
    updatedAt = updatedAt,
    completions = completions
)

// ── Todo / TodoSubtask — subtasks nested under TodoDto, full-replace-per-parent ─────────────────

@Serializable
data class TodoSubtaskDto(
    val syncId: String,
    val label: String,
    val isDone: Boolean,
    val sortOrder: Int,
    /** The parent subtask's syncId (one extra level of nesting), or null for a top-level row —
     * never a local id, which is only stable within one device's own database. */
    val parentSyncId: String?,
    val updatedAt: Long
)

fun TodoSubtask.toDto(parentSyncId: String?) = TodoSubtaskDto(
    syncId = syncId,
    label = label,
    isDone = isDone,
    sortOrder = sortOrder,
    parentSyncId = parentSyncId,
    updatedAt = updatedAt
)

@Serializable
data class TodoDto(
    val syncId: String,
    val title: String,
    val note: String,
    val dueDate: String,
    val priority: String,
    val isCompleted: Boolean,
    val completedAt: Long?,
    val iconIndex: Int,
    val createdAt: Long,
    val isArchived: Boolean,
    val deletedAt: Long?,
    val soundUri: String,
    val vibrationPatternId: String,
    val updatedAt: Long,
    val subtasks: List<TodoSubtaskDto> = emptyList()
)

fun Todo.toDto(subtasks: List<TodoSubtaskDto> = emptyList()) = TodoDto(
    syncId = syncId,
    title = title,
    note = note,
    dueDate = dueDate,
    priority = priority,
    isCompleted = isCompleted,
    completedAt = completedAt,
    iconIndex = iconIndex,
    createdAt = createdAt,
    isArchived = isArchived,
    deletedAt = deletedAt,
    soundUri = soundUri,
    vibrationPatternId = vibrationPatternId,
    updatedAt = updatedAt,
    subtasks = subtasks
)

fun TodoDto.toEntity() = Todo(
    title = title,
    note = note,
    dueDate = dueDate,
    priority = priority,
    isCompleted = isCompleted,
    completedAt = completedAt,
    iconIndex = iconIndex,
    createdAt = createdAt,
    isArchived = isArchived,
    deletedAt = deletedAt,
    soundUri = soundUri,
    vibrationPatternId = vibrationPatternId,
    syncId = syncId,
    updatedAt = updatedAt
)

// ── CalendarEvent / EventReminder — reminders nested under CalendarEventDto, full-replace ──────

@Serializable
data class EventReminderDto(
    val syncId: String,
    val minutesBefore: Int?,
    val absoluteDateTime: String?,
    val soundUri: String,
    val vibrationPatternId: String,
    val createdAt: Long,
    val updatedAt: Long
)

fun EventReminder.toDto() = EventReminderDto(
    syncId = syncId,
    minutesBefore = minutesBefore,
    absoluteDateTime = absoluteDateTime,
    soundUri = soundUri,
    vibrationPatternId = vibrationPatternId,
    createdAt = createdAt,
    updatedAt = updatedAt
)

@Serializable
data class CalendarEventDto(
    val syncId: String,
    val title: String,
    val note: String,
    val startDate: String,
    val endDate: String,
    val startTime: String?,
    val endTime: String?,
    val colorIndex: Int,
    val colorArgb: Int?,
    val isActive: Boolean,
    val createdAt: Long,
    val deletedAt: Long?,
    val updatedAt: Long,
    val reminders: List<EventReminderDto> = emptyList()
)

fun CalendarEvent.toDto(reminders: List<EventReminderDto> = emptyList()) = CalendarEventDto(
    syncId = syncId,
    title = title,
    note = note,
    startDate = startDate,
    endDate = endDate,
    startTime = startTime,
    endTime = endTime,
    colorIndex = colorIndex,
    colorArgb = colorArgb,
    isActive = isActive,
    createdAt = createdAt,
    deletedAt = deletedAt,
    updatedAt = updatedAt,
    reminders = reminders
)

fun CalendarEventDto.toEntity() = CalendarEvent(
    title = title,
    note = note,
    startDate = startDate,
    endDate = endDate,
    startTime = startTime,
    endTime = endTime,
    colorIndex = colorIndex,
    colorArgb = colorArgb,
    isActive = isActive,
    createdAt = createdAt,
    deletedAt = deletedAt,
    syncId = syncId,
    updatedAt = updatedAt
)

// ── Alarm ────────────────────────────────────────────────────────────────────────────────────

@Serializable
data class AlarmDto(
    val syncId: String,
    val label: String,
    val hour: Int,
    val minute: Int,
    val repeatDaysMask: Int,
    val isEnabled: Boolean,
    val soundUri: String,
    val vibrationPatternId: String,
    val dismissMission: String,
    val missionDifficulty: Int,
    val createdAt: Long,
    val deletedAt: Long?,
    val updatedAt: Long
)

fun Alarm.toDto() = AlarmDto(
    syncId = syncId,
    label = label,
    hour = hour,
    minute = minute,
    repeatDaysMask = repeatDaysMask,
    isEnabled = isEnabled,
    soundUri = soundUri,
    vibrationPatternId = vibrationPatternId,
    dismissMission = dismissMission,
    missionDifficulty = missionDifficulty,
    createdAt = createdAt,
    deletedAt = deletedAt,
    updatedAt = updatedAt
)

fun AlarmDto.toEntity() = Alarm(
    label = label,
    hour = hour,
    minute = minute,
    repeatDaysMask = repeatDaysMask,
    isEnabled = isEnabled,
    soundUri = soundUri,
    vibrationPatternId = vibrationPatternId,
    dismissMission = dismissMission,
    missionDifficulty = missionDifficulty,
    createdAt = createdAt,
    deletedAt = deletedAt,
    syncId = syncId,
    updatedAt = updatedAt
)

// ── Timer ────────────────────────────────────────────────────────────────────────────────────

@Serializable
data class TimerDto(
    val syncId: String,
    val label: String,
    val durationSeconds: Int,
    val remainingSeconds: Int,
    val endTimeMillis: Long,
    val soundUri: String,
    val vibrationPatternId: String,
    val createdAt: Long,
    val isPomo: Boolean,
    val pomoWorkSecs: Int,
    val pomoShortBreakSecs: Int,
    val pomoLongBreakSecs: Int,
    val pomoSessionsPerRound: Int,
    val pomoCurrentSession: Int,
    val pomoPhase: String,
    val deletedAt: Long?,
    val updatedAt: Long
)

fun Timer.toDto() = TimerDto(
    syncId = syncId,
    label = label,
    durationSeconds = durationSeconds,
    remainingSeconds = remainingSeconds,
    endTimeMillis = endTimeMillis,
    soundUri = soundUri,
    vibrationPatternId = vibrationPatternId,
    createdAt = createdAt,
    isPomo = isPomo,
    pomoWorkSecs = pomoWorkSecs,
    pomoShortBreakSecs = pomoShortBreakSecs,
    pomoLongBreakSecs = pomoLongBreakSecs,
    pomoSessionsPerRound = pomoSessionsPerRound,
    pomoCurrentSession = pomoCurrentSession,
    pomoPhase = pomoPhase,
    deletedAt = deletedAt,
    updatedAt = updatedAt
)

fun TimerDto.toEntity() = Timer(
    label = label,
    durationSeconds = durationSeconds,
    remainingSeconds = remainingSeconds,
    endTimeMillis = endTimeMillis,
    soundUri = soundUri,
    vibrationPatternId = vibrationPatternId,
    createdAt = createdAt,
    isPomo = isPomo,
    pomoWorkSecs = pomoWorkSecs,
    pomoShortBreakSecs = pomoShortBreakSecs,
    pomoLongBreakSecs = pomoLongBreakSecs,
    pomoSessionsPerRound = pomoSessionsPerRound,
    pomoCurrentSession = pomoCurrentSession,
    pomoPhase = pomoPhase,
    deletedAt = deletedAt,
    syncId = syncId,
    updatedAt = updatedAt
)

// ── Reminder ─────────────────────────────────────────────────────────────────────────────────

@Serializable
data class ReminderDto(
    val syncId: String,
    val title: String,
    val note: String,
    val dateTime: String,
    val isCompleted: Boolean,
    val completedAt: Long?,
    val isActive: Boolean,
    val createdAt: Long,
    val soundUri: String,
    val deletedAt: Long?,
    /** The attached note's syncId, or null — NOT its local Room id (see this file's own KDoc). */
    val noteSyncId: String?,
    val updatedAt: Long
)

fun Reminder.toDto(noteSyncId: String?) = ReminderDto(
    syncId = syncId,
    title = title,
    note = note,
    dateTime = dateTime,
    isCompleted = isCompleted,
    completedAt = completedAt,
    isActive = isActive,
    createdAt = createdAt,
    soundUri = soundUri,
    deletedAt = deletedAt,
    noteSyncId = noteSyncId,
    updatedAt = updatedAt
)

/** [localNoteId] is resolved by the caller from [ReminderDto.noteSyncId] (null if unresolvable — the attached note hasn't arrived on this device yet). */
fun ReminderDto.toEntity(localNoteId: Long?) = Reminder(
    title = title,
    note = note,
    dateTime = dateTime,
    isCompleted = isCompleted,
    completedAt = completedAt,
    isActive = isActive,
    createdAt = createdAt,
    soundUri = soundUri,
    deletedAt = deletedAt,
    noteId = localNoteId,
    syncId = syncId,
    updatedAt = updatedAt
)

// ── Notebook / Note / NoteLink ──────────────────────────────────────────────────────────────────
// Privacy boundary: a Notebook/Note with isPrivate == true is never included in an outgoing batch
// and is never created/updated by an incoming one — see SyncEngine's buildOutgoingBatch/
// applyIncomingBatch for the enforcement; the DTOs themselves carry isPrivate only so the
// receiving side can make that defensive check.

@Serializable
data class NotebookDto(
    val syncId: String,
    val name: String,
    val colorIndex: Int,
    val colorArgb: Int?,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
    val isPrivate: Boolean,
    val vaultFolderName: String?,
    val folderDocUriCache: String?,
    /** The parent notebook's syncId, or null for a top-level notebook — NOT a local id. */
    val parentSyncId: String?
)

fun Notebook.toDto(parentSyncId: String?) = NotebookDto(
    syncId = syncId,
    name = name,
    colorIndex = colorIndex,
    colorArgb = colorArgb,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    isPrivate = isPrivate,
    vaultFolderName = vaultFolderName,
    folderDocUriCache = folderDocUriCache,
    parentSyncId = parentSyncId
)

/** [localParentId] is resolved by the caller from [NotebookDto.parentSyncId] (null if unresolvable). */
fun NotebookDto.toEntity(localParentId: Long?) = Notebook(
    name = name,
    colorIndex = colorIndex,
    colorArgb = colorArgb,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    isPrivate = isPrivate,
    vaultFolderName = vaultFolderName,
    folderDocUriCache = folderDocUriCache,
    parentId = localParentId,
    syncId = syncId
)

/** One LINK block, stored under the SOURCE note's DTO only (see this file's own KDoc on
 * cross-notebook-safe identity) — `targetNoteSyncId`, never the target's local id. */
@Serializable
data class NoteLinkDto(
    val syncId: String,
    val targetNoteSyncId: String
)

@Serializable
data class NoteDto(
    val syncId: String,
    /** The owning notebook's syncId — NOT its local Room id. */
    val notebookSyncId: String,
    val title: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isPinned: Boolean,
    val deletedAt: Long?,
    val tags: String,
    val fontFamily: String,
    val fontSize: String,
    val bodyPreview: String,
    val filePointer: String?,
    val fileDocUriCache: String?,
    val fileSyncedAt: Long?,
    val uuid: String?,
    val links: List<NoteLinkDto> = emptyList()
)

fun Note.toDto(notebookSyncId: String, links: List<NoteLinkDto> = emptyList()) = NoteDto(
    syncId = syncId,
    notebookSyncId = notebookSyncId,
    title = title,
    content = content,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isPinned = isPinned,
    deletedAt = deletedAt,
    tags = tags,
    fontFamily = fontFamily,
    fontSize = fontSize,
    bodyPreview = bodyPreview,
    filePointer = filePointer,
    fileDocUriCache = fileDocUriCache,
    fileSyncedAt = fileSyncedAt,
    uuid = uuid,
    links = links
)

fun NoteDto.toEntity(localNotebookId: Long) = Note(
    notebookId = localNotebookId,
    title = title,
    content = content,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isPinned = isPinned,
    deletedAt = deletedAt,
    tags = tags,
    fontFamily = fontFamily,
    fontSize = fontSize,
    bodyPreview = bodyPreview,
    filePointer = filePointer,
    fileDocUriCache = fileDocUriCache,
    fileSyncedAt = fileSyncedAt,
    uuid = uuid,
    syncId = syncId
)

// ── JournalEntry ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class JournalEntryDto(
    val syncId: String,
    val date: String,
    val title: String,
    val content: String,
    val mood: Int,
    /** The associated habit's syncId, or null for a general daily entry — NOT its local Room id. */
    val habitSyncId: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val tags: String,
    val photoUris: String,
    val feelings: String,
    val deletedAt: Long?
)

fun JournalEntry.toDto(habitSyncId: String?) = JournalEntryDto(
    syncId = syncId,
    date = date,
    title = title,
    content = content,
    mood = mood,
    habitSyncId = habitSyncId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    tags = tags,
    photoUris = photoUris,
    feelings = feelings,
    deletedAt = deletedAt
)

/** [localHabitId] is resolved by the caller from [JournalEntryDto.habitSyncId] (null if unresolvable). */
fun JournalEntryDto.toEntity(localHabitId: Long?) = JournalEntry(
    date = date,
    title = title,
    content = content,
    mood = mood,
    habitId = localHabitId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    tags = tags,
    photoUris = photoUris,
    feelings = feelings,
    deletedAt = deletedAt,
    syncId = syncId
)

// ── Theme/appearance preferences (Stage 3) ──────────────────────────────────────────────────────
// Whole-blob last-write-wins, not per-key merging: `values` is the JSON text produced by
// ThemePreferences.exportPreferencesForSync() (device-local-only keys already stripped — desktop's
// runInBackground, Android's isPro/isLifetimePro) and applied verbatim via
// ThemePreferences.importPreferencesForSync() on the receiving side when `updatedAt` is newer than
// the local PREFERENCES_UPDATED_AT_KEY. A plain Map<String, String> can't model exportPreferences()'s
// actual JSON (mixed string/int/boolean/double values), and this is a blob, not a structured entity —
// so the JSON text itself is carried as a string rather than modeled field-by-field here.
@Serializable
data class PreferencesDto(
    val updatedAt: Long,
    val values: String,
    /** Profile picture, base64 JPEG — see ProfileImageStore. null = no information (older peer),
     * "" = no picture. Defaulted so either side can be the older version. */
    val profileImage: String? = null
)

// ── Vault file sync (Stage 3.5, optional, default-OFF) ──────────────────────────────────────────
// Separate from Note/Notebook *row* sync above (always on). Base64-encodes each vault file's bytes
// and ships it as an additional, OPTIONAL part of the sync exchange — the simplest correct
// transport given the existing single-WebSocket/single-JSON-blob design. Built/sent ONLY when
// `ThemePreferences.vaultFileSyncEnabled` is true on the SENDING device (see
// SyncEngine.buildOutgoingBatch), and applied ONLY when it is ALSO true on the RECEIVING device
// (see SyncEngine.applyIncomingBatch) — both ends must opt in independently, not just the sender.
// Conflict handling is last-write-wins by [mtime], matching the row-sync philosophy. A private
// notebook's vault folder is excluded from this list the same way it's excluded from `notebooks`/
// `notes` above. Known limitation: base64-over-one-JSON-blob is not an efficient transport for
// large binaries, so a file above the sync size cap is skipped rather than included — a real
// file-transfer protocol is future work, not attempted in this pass.
@Serializable
data class VaultFileDto(
    /** Path relative to the vault root, forward-slash separated (e.g. "My Notebook/Note.md" or
     * "_assets/image.jpg" or the root-level "_rhythm-vault-index.json" manifest). */
    val relativePath: String,
    val contentBase64: String,
    /** Last-modified time of the file at the sender, used for last-write-wins conflict resolution
     * on the receiving side — never overwrite a local file whose own mtime is >= this value. */
    val mtime: Long
)

@Serializable
data class SyncBatch(
    val deviceId: String,
    val habits: List<HabitDto>,
    val completions: List<HabitCompletionDto>,
    val todos: List<TodoDto> = emptyList(),
    val calendarEvents: List<CalendarEventDto> = emptyList(),
    val alarms: List<AlarmDto> = emptyList(),
    val timers: List<TimerDto> = emptyList(),
    val reminders: List<ReminderDto> = emptyList(),
    val notebooks: List<NotebookDto> = emptyList(),
    val notes: List<NoteDto> = emptyList(),
    val journalEntries: List<JournalEntryDto> = emptyList(),
    /** Null when preferences haven't changed locally since the last sync (see
     * SyncEngine.buildOutgoingBatch) — omitted from the batch entirely rather than sent as an
     * unchanged value, same "only include what's newer than `since`" pattern as every entity list
     * above. */
    val preferences: PreferencesDto? = null,
    /** Null when vault file sync is off on the sending device (see this file's own KDoc above
     * [VaultFileDto]) — distinct from an empty list, which means "sync is on, nothing changed." */
    val vaultFiles: List<VaultFileDto>? = null
)
