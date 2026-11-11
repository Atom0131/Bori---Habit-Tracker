package com.apagon.rhythm.data.agent

import com.apagon.rhythm.core.app.AppInfo
import com.apagon.rhythm.data.db.EventReminderDao
import com.apagon.rhythm.data.db.NotesDao
import com.apagon.rhythm.data.model.Alarm
import com.apagon.rhythm.data.model.CalendarEvent
import com.apagon.rhythm.data.model.EventReminder
import com.apagon.rhythm.data.model.Habit
import com.apagon.rhythm.data.model.HabitFrequency
import com.apagon.rhythm.data.model.Note
import com.apagon.rhythm.data.model.Recurrence
import com.apagon.rhythm.data.model.Reminder
import com.apagon.rhythm.data.model.occurrencesIn
import com.apagon.rhythm.data.model.Timer
import com.apagon.rhythm.data.model.Todo
import com.apagon.rhythm.data.model.TodoPriority
import com.apagon.rhythm.data.model.TodoSubtask
import com.apagon.rhythm.data.repository.AlarmRepository
import com.apagon.rhythm.data.repository.CalendarEventRepository
import com.apagon.rhythm.data.repository.ChecklistDraft
import com.apagon.rhythm.data.repository.HabitRepository
import com.apagon.rhythm.data.repository.NotesRepository
import com.apagon.rhythm.data.repository.ReminderRepository
import com.apagon.rhythm.data.repository.TimerRepository
import com.apagon.rhythm.data.repository.TodoRepository
import com.apagon.rhythm.data.vault.DesktopNoteVault
import com.apagon.rhythm.data.vault.NoteBlocksJson
import com.apagon.rhythm.data.vault.VaultMarkdown
import com.apagon.rhythm.platform.AppHome
import com.apagon.rhythm.ui.notes.BlockType
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.withCharset
import io.ktor.server.application.ApplicationCall
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.AclEntry
import java.nio.file.attribute.AclEntryPermission
import java.nio.file.attribute.AclEntryType
import java.nio.file.attribute.AclFileAttributeView
import java.nio.file.attribute.PosixFilePermissions
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * **The local AI agent API.** Lets an agent on this computer (through `bori --mcp`, see BoriMcpServer) read the
 * agenda and add or change events, to-dos, reminders, habit check-ins, notes, timers and alarms, through the same
 * repositories the screens use, so the UI updates live, alarms fire and everything syncs to the phone.
 *
 * - Off until switched on in Settings → Obsidian & AI.
 * - Listens on **127.0.0.1 only** ([PORT]), never the network.
 * - Every request needs `Authorization: Bearer <token>`; the token is in `~/.bori/agent_token`
 *   (owner-only permissions), made on first use.
 * - Items are addressed by their sync id (stable across devices). Deleting needs `?confirm=true`,
 *   so an agent has to mean it; deletes are soft (Recently Deleted), never purges.
 *
 * Dates are `yyyy-MM-dd`, times `HH:mm`, local time — the same strings the database stores.
 */
class AgentApiServer(
    private val events: CalendarEventRepository,
    private val eventReminders: EventReminderDao,
    private val todos: TodoRepository,
    private val reminders: ReminderRepository,
    private val habits: HabitRepository,
    private val notes: NotesRepository,
    private val notesDao: NotesDao,
    private val vault: DesktopNoteVault,
    private val alarms: AlarmRepository,
    private val timers: TimerRepository,
    /** Tests pass a free port so they can run next to a real Bori. */
    private val port: Int = PORT
) : AgentApiControl {

    companion object {
        const val PORT = 47891
        /** Say UTF-8 outright: clients like Windows PowerShell read a bare application/json as Latin-1. */
        private val JSON_UTF8 = ContentType.Application.Json.withCharset(Charsets.UTF_8)
        private val DATE = Regex("^\\d{4}-\\d{2}-\\d{2}$")
        private val TIME = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")
        /** Alarm.repeatDaysMask bit order: bit0 = Sunday … bit6 = Saturday. */
        private val DAYS = listOf("sun", "mon", "tue", "wed", "thu", "fri", "sat")
        /** Wake-up puzzle ids, as stored in Alarm.dismissMission (the phone's WakeUpCheck ids). */
        private val PUZZLES = listOf("none", "math", "shake", "memory", "typing", "pairs")
        const val ASK_PUZZLE = "Want a wake-up puzzle on this alarm? Maths, shake, memory, typing or pairs; easy, medium or hard. " +
            "(Say 'stop asking' and I won't ask again.)"
        /** Habit.weekDaysMask bit order: bit0 = Monday … bit6 = Sunday (not the alarms' Sunday-first order). */
        private val HABIT_DAYS = listOf("mon", "tue", "wed", "thu", "fri", "sat", "sun")
        /** Names for habitColorPalette's five colours, by colorIndex. */
        private val HABIT_COLORS = listOf("purple", "green", "blue", "red", "orange")
        /** A sum written into an alarm label: "8+6=?", "7 * 4", "12 x 3 = ?". Ranges like "6-7" or "24/7" and
         * doses like "2x500" only count with an "=". */
        private val SUM_IN_LABEL = Regex("\\d+\\s*[+×*÷]\\s*\\d+(\\s*=\\s*(\\?|\\d+))?|\\d+\\s*[-x/]\\s*\\d+\\s*=\\s*(\\?|\\d+)")
    }

    /**
     * The installed launcher, for the "Connect an AI app" setup text. jpackage sets `jpackage.app-path`
     * (`/opt/bori/bin/bori`, or `…\AppData\Local\Bori\Bori.exe` on Windows). On Linux the packages also
     * put `bori` on PATH, so prefer `/usr/bin/bori` when it points at the same app. Dev runs have no
     * launcher; fall back to plain `bori`.
     */
    private fun launcherPath(): String {
        val app = System.getProperty("jpackage.app-path") ?: return "bori"
        val onPath = File("/usr/bin/bori")
        return if (runCatching { onPath.canonicalPath == File(app).canonicalPath }.getOrDefault(false)) onPath.path else app
    }

    @Serializable
    private data class Prefs(val enabled: Boolean = false, val askPuzzleOnAlarms: Boolean = true)

    private class BadRequest(message: String) : Exception(message)
    private class NotFound(message: String) : Exception(message)

    private val json = Json { prettyPrint = false }
    private val prefsFile = File(AppHome.dir, "agent_api.json")
    private val tokenFile = File(AppHome.dir, "agent_token")
    private var server: EmbeddedServer<*, *>? = null
    private val _state = MutableStateFlow(AgentApiState(
        port = port,
        tokenFile = tokenFile.path,
        mcpCommand = launcherPath(),
        isWindows = System.getProperty("os.name").orEmpty().lowercase().contains("windows")
    ))
    override val state: StateFlow<AgentApiState> = _state.asStateFlow()

    // ── Lifecycle ───────────────────────────────────────────────────────────────────────────────

    private val prefsJson = Json { ignoreUnknownKeys = true }

    private fun readPrefs(): Prefs = runCatching { prefsJson.decodeFromString(Prefs.serializer(), prefsFile.readText()) }.getOrDefault(Prefs())

    init {
        _state.value = _state.value.copy(askPuzzleOnAlarms = readPrefs().askPuzzleOnAlarms)
    }

    fun start() {
        val p = readPrefs()
        _state.value = _state.value.copy(enabled = p.enabled, askPuzzleOnAlarms = p.askPuzzleOnAlarms)
        if (p.enabled) startServer()
    }

    override fun setEnabled(enabled: Boolean) {
        prefsFile.writeText(Json.encodeToString(Prefs.serializer(), readPrefs().copy(enabled = enabled)))
        _state.value = _state.value.copy(enabled = enabled)
        if (enabled) startServer() else stopServer()
    }

    override fun setAskPuzzleOnAlarms(ask: Boolean) {
        prefsFile.writeText(Json.encodeToString(Prefs.serializer(), readPrefs().copy(askPuzzleOnAlarms = ask)))
        _state.value = _state.value.copy(askPuzzleOnAlarms = ask)
    }

    override fun regenerateToken() {
        writeToken(newToken())
    }

    private fun startServer() {
        if (server != null) return
        token() // make sure there is one before anyone needs it
        runCatching {
            server = embeddedServer(CIO, host = "127.0.0.1", port = port) { routing { routes() } }.start(wait = false)
        }.onSuccess {
            _state.value = _state.value.copy(running = true, lastError = null)
        }.onFailure {
            server = null
            _state.value = _state.value.copy(running = false, lastError = "Couldn't open port $port: ${it.message}")
        }
    }

    private fun stopServer() {
        server?.stop(gracePeriodMillis = 200, timeoutMillis = 1000)
        server = null
        _state.value = _state.value.copy(running = false)
    }

    // ── Token ───────────────────────────────────────────────────────────────────────────────────

    private fun newToken(): String = ByteArray(32).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }

    private fun token(): String {
        val existing = runCatching { tokenFile.readText().trim() }.getOrNull()
        if (!existing.isNullOrEmpty()) return existing
        return newToken().also { writeToken(it) }
    }

    private fun writeToken(t: String) {
        tokenFile.delete()
        runCatching {
            Files.createFile(tokenFile.toPath(), PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")))
        }
        tokenFile.writeText(t + "\n")
        tokenFile.setReadable(false, false); tokenFile.setReadable(true, true)
        tokenFile.setWritable(false, false); tokenFile.setWritable(true, true)
        restrictToOwnerOnWindows(tokenFile)
    }

    /**
     * Windows has no POSIX modes: the `rw-------` above is silently skipped and the file just inherits
     * `%APPDATA%\Bori`'s ACL (the user, SYSTEM and Administrators). Replace it with a single entry for
     * the file's owner, the Windows equivalent of `chmod 600`.
     */
    private fun restrictToOwnerOnWindows(file: File) {
        if (!System.getProperty("os.name").orEmpty().lowercase().contains("windows")) return
        runCatching {
            val view = Files.getFileAttributeView(file.toPath(), AclFileAttributeView::class.java) ?: return
            val entry = AclEntry.newBuilder()
                .setType(AclEntryType.ALLOW)
                .setPrincipal(view.owner)
                .setPermissions(
                    AclEntryPermission.READ_DATA, AclEntryPermission.WRITE_DATA, AclEntryPermission.APPEND_DATA,
                    AclEntryPermission.READ_ATTRIBUTES, AclEntryPermission.WRITE_ATTRIBUTES,
                    AclEntryPermission.READ_NAMED_ATTRS, AclEntryPermission.WRITE_NAMED_ATTRS,
                    AclEntryPermission.READ_ACL, AclEntryPermission.WRITE_ACL,
                    AclEntryPermission.DELETE, AclEntryPermission.SYNCHRONIZE
                )
                .build()
            view.acl = listOf(entry)
        }
    }

    private fun authorized(call: ApplicationCall): Boolean {
        val given = call.request.headers["Authorization"]?.removePrefix("Bearer ")?.trim() ?: return false
        return MessageDigest.isEqual(given.toByteArray(), token().toByteArray())
    }

    // ── Routing ─────────────────────────────────────────────────────────────────────────────────

    private suspend fun handle(call: ApplicationCall, block: suspend () -> JsonElement) {
        if (!authorized(call)) {
            call.respondText(err("Missing or wrong token. It is in ${tokenFile.path}."), JSON_UTF8, HttpStatusCode.Unauthorized)
            return
        }
        _state.value = _state.value.copy(lastRequestAt = System.currentTimeMillis())
        val (status, body) = try {
            HttpStatusCode.OK to block().toString()
        } catch (e: BadRequest) {
            HttpStatusCode.BadRequest to err(e.message ?: "Bad request")
        } catch (e: NotFound) {
            HttpStatusCode.NotFound to err(e.message ?: "Not found")
        } catch (e: Exception) {
            HttpStatusCode.InternalServerError to err(e.message ?: e.javaClass.simpleName)
        }
        call.respondText(body, JSON_UTF8, status)
    }

    private fun err(message: String) = buildJsonObject { put("error", message) }.toString()

    private suspend fun body(call: ApplicationCall): JsonObject {
        val text = call.receiveText()
        if (text.isBlank()) return JsonObject(emptyMap())
        return runCatching { Json.parseToJsonElement(text).jsonObject }.getOrElse { throw BadRequest("Body must be a JSON object") }
    }

    private fun io.ktor.server.routing.Routing.routes() {
        get("/health") { handle(call) { buildJsonObject { put("ok", true); put("app", "Bori"); put("version", AppInfo.VERSION); put("today", LocalDate.now().toString()) } } }

        get("/agenda") {
            handle(call) {
                val from = call.request.queryParameters["from"]?.let(::date) ?: LocalDate.now().toString()
                val to = call.request.queryParameters["to"]?.let(::date) ?: from
                agenda(from, to)
            }
        }

        post("/events") { handle(call) { eventJson(addEvent(body(call))) } }
        patch("/events/{id}") { handle(call) { eventJson(editEvent(call.parameters["id"]!!, body(call))) } }

        post("/todos") { handle(call) { todoJson(addTodo(body(call))) } }
        patch("/todos/{id}") { handle(call) { todoJson(editTodo(call.parameters["id"]!!, body(call))) } }
        post("/todos/{id}/complete") {
            handle(call) {
                val t = todo(call.parameters["id"]!!)
                val done = body(call).bool("done") ?: true
                todos.setCompleted(t, done)
                todoJson(todos.getTodoBySyncId(t.syncId)!!)
            }
        }

        post("/reminders") { handle(call) { reminderJson(addReminder(body(call))) } }
        patch("/reminders/{id}") { handle(call) { reminderJson(editReminder(call.parameters["id"]!!, body(call))) } }

        get("/preferences") { handle(call) { preferencesJson() } }
        patch("/preferences") {
            handle(call) {
                val b = body(call)
                if (b.containsKey("ask_puzzle_on_alarms")) {
                    setAskPuzzleOnAlarms(b.bool("ask_puzzle_on_alarms") ?: throw BadRequest("ask_puzzle_on_alarms must be true or false"))
                }
                preferencesJson()
            }
        }

        post("/habits") { handle(call) { addHabit(body(call)) } }
        patch("/habits/{id}") { handle(call) { editHabit(call.parameters["id"]!!, body(call)) } }
        get("/habits") {
            handle(call) {
                val day = call.request.queryParameters["date"]?.let(::date) ?: LocalDate.now().toString()
                habitsJson(day)
            }
        }
        post("/habits/{id}/check") {
            handle(call) {
                val b = body(call)
                val h = habit(call.parameters["id"]!!)
                val day = b.str("date")?.let(::date) ?: LocalDate.now().toString()
                if (b.bool("done") == false) habits.markIncomplete(h.id, day) else habits.markComplete(h.id, day)
                buildJsonObject { put("habit", h.name); put("date", day); put("done", b.bool("done") != false) }
            }
        }

        get("/notes/search") {
            handle(call) {
                val q = call.request.queryParameters["q"]?.trim().orEmpty()
                if (q.isEmpty()) throw BadRequest("q is required")
                val books = notesDao.getAllNotebooksForBackup().associateBy { it.id }
                buildJsonArray {
                    notes.searchAllNotes(q).first().filter { books[it.notebookId]?.isPrivate != true }.take(20).forEach { n ->
                        add(buildJsonObject {
                            put("id", n.syncId); put("title", n.title); put("notebook", books[n.notebookId]?.name)
                            put("tags", n.tags); put("preview", n.bodyPreview.ifBlank { n.content }.take(300))
                        })
                    }
                }
            }
        }
        post("/notes") { handle(call) { addNote(body(call)) } }
        get("/notes/{id}") { handle(call) { noteJson(openNote(call.parameters["id"]!!)) } }
        patch("/notes/{id}") { handle(call) { noteJson(editNote(call.parameters["id"]!!, body(call))) } }

        get("/timers") { handle(call) { JsonArray(timers.getAllTimers().first().filter { it.deletedAt == null }.map { timerJson(it) }) } }
        post("/timers") { handle(call) { timerJson(addTimer(body(call))) } }
        post("/timers/{id}/stop") {
            handle(call) {
                val t = timer(call.parameters["id"]!!)
                val left = if (t.endTimeMillis > 0) maxOf(0, ((t.endTimeMillis - System.currentTimeMillis()) / 1000).toInt()) else t.remainingSeconds
                timers.updateTimer(t.copy(remainingSeconds = left, endTimeMillis = 0))
                timerJson(timer(t.syncId))
            }
        }

        get("/alarms") { handle(call) { JsonArray(alarms.getAllAlarms().first().filter { it.deletedAt == null }.map { alarmJson(it) }) } }
        post("/alarms") {
            handle(call) {
                val b = body(call)
                val a = addAlarm(b)
                val out = alarmJson(a)
                // No puzzle asked for: the agent asks the user whether they want one (unless they said stop asking).
                if (a.dismissMission == "none" && b.str("puzzle") == null && _state.value.askPuzzleOnAlarms) {
                    JsonObject(out + ("ask_user" to JsonPrimitive(ASK_PUZZLE)))
                } else out
            }
        }
        patch("/alarms/{id}") { handle(call) { alarmJson(editAlarm(call.parameters["id"]!!, body(call))) } }

        delete("/{kind}/{id}") {
            handle(call) {
                if (call.request.queryParameters["confirm"] != "true") {
                    throw BadRequest("Deleting needs confirm=true. Ask the user first.")
                }
                val id = call.parameters["id"]!!
                when (call.parameters["kind"]) {
                    "events" -> {
                        // ?date=yyyy-MM-dd on a repeating event skips just that date.
                        val day = call.request.queryParameters["date"]?.let(::date)
                        val e = event(id)
                        if (day != null) events.deleteFromEditor(e.copy(occurrenceDate = day), onlyThis = true)
                        else events.deleteEvent(e)
                    }
                    "todos" -> todos.deleteTodo(todo(id))
                    "reminders" -> reminders.deleteReminder(reminder(id))
                    "notes" -> notes.deleteNote(openNote(id)) // same lock check as read/edit
                    "timers" -> timers.deleteTimer(timer(id))
                    "alarms" -> alarms.deleteAlarm(alarm(id))
                    "habits" -> habits.deleteHabit(habit(id))
                    else -> throw NotFound("Unknown kind")
                }
                val days = if (call.parameters["kind"] == "habits") 14 else 30
                buildJsonObject { put("deleted", id); put("note", "Moved to Recently Deleted in Bori; restorable for $days days.") }
            }
        }
    }

    // ── Agenda ──────────────────────────────────────────────────────────────────────────────────

    private suspend fun agenda(from: String, to: String): JsonElement {
        if (to < from) throw BadRequest("to is before from")
        val evs = events.getEventsInRange(from, to).first().filter { it.deletedAt == null }
            .sortedWith(compareBy({ it.startDate }, { it.startTime ?: "" }))
        val pending = todos.getPendingTodos().first().filter { it.deletedAt == null && !it.isArchived }
        val dueInRange = pending.filter { it.dueDate.isNotBlank() && it.dueDate.take(10) in from..to }
        val overdue = pending.filter { it.dueDate.isNotBlank() && it.dueDate.take(10) < from }
        val undated = pending.filter { it.dueDate.isBlank() }
        val rems = reminders.getAllActiveReminders().first()
            .filter { it.deletedAt == null && !it.isCompleted && it.dateTime.take(10) in from..to }
            .sortedBy { it.dateTime }
        return buildJsonObject {
            put("from", from); put("to", to); put("today", LocalDate.now().toString())
            put("events", JsonArray(evs.map { eventJson(it) }))
            put("todos_due", JsonArray(dueInRange.map { todoJson(it) }))
            put("todos_overdue", JsonArray(overdue.map { todoJson(it) }))
            put("todos_without_date", JsonArray(undated.take(30).map { todoJson(it) }))
            put("reminders", JsonArray(rems.map { reminderJson(it) }))
            if (LocalDate.now().toString() in from..to) put("habits_today", habitsJson(LocalDate.now().toString()))
        }
    }

    private suspend fun habitsJson(day: String): JsonElement {
        val done = habits.getCompletionsByDate(day).first().map { it.habitId }.toSet()
        return buildJsonArray {
            habits.getAllActiveHabits().first().forEach { h -> add(JsonObject(habitJson(h) + ("done" to JsonPrimitive(h.id in done)))) }
        }
    }

    // ── Habits ──────────────────────────────────────────────────────────────────────────────────

    private suspend fun habit(id: String) =
        habits.getHabitBySyncId(id)?.takeIf { it.deletedAt == null } ?: throw NotFound("No habit with that id")

    private suspend fun habitJson(h: Habit) = buildJsonObject {
        put("id", h.syncId); put("name", h.name)
        if (h.description.isNotBlank()) put("description", h.description)
        put("frequency", h.frequency.name.lowercase())
        if (h.frequency == HabitFrequency.WEEKLY) {
            put("days", JsonArray(HABIT_DAYS.indices.filter { h.weekDaysMask and (1 shl it) != 0 }.map { JsonPrimitive(HABIT_DAYS[it]) }))
        }
        if (h.frequency == HabitFrequency.MONTHLY) {
            put("month_days", JsonArray((0..30).filter { h.monthDaysMask and (1 shl it) != 0 }.map { JsonPrimitive(it + 1) }))
        }
        put("reminder_time", h.reminderTime)
        if (h.durationDays > 0) put("goal_days", h.durationDays)
        put("icon", HabitIconGuess.labels.getOrNull(h.iconIndex) ?: "default")
        put("color", if (h.colorArgb != null) "custom" else HABIT_COLORS.getOrElse(h.colorIndex) { HABIT_COLORS[0] })
        if (h.isChecklist) {
            put("steps", JsonArray(habits.getItemsForHabitSync(h.id).sortedBy { it.sortOrder }.map { JsonPrimitive(it.label) }))
        }
    }

    /** `days` for a weekly habit: a list of mon..sun, "weekdays", "weekends" or "daily". Monday-first mask. */
    private fun habitDaysMask(v: JsonElement?): Int? {
        // Alarm parsing gives a Sunday-first mask; rotate it so Monday is bit 0.
        val sunFirst = daysMask(v) ?: return null
        return (sunFirst shr 1) or ((sunFirst and 1) shl 6)
    }

    private fun monthDaysMask(v: JsonElement?): Int? {
        if (v == null || v is JsonNull) return null
        val items = when (v) {
            is JsonArray -> v.map { it.jsonPrimitive.content }
            is JsonPrimitive -> v.content.trim('[', ']', ' ').split(',')
            else -> throw BadRequest("month_days must be a list of days of the month, like [1, 15]")
        }.map { it.trim(' ', '\'', '"') }.filter { it.isNotEmpty() }
        return items.fold(0) { m, d ->
            val n = d.toIntOrNull()?.takeIf { it in 1..31 } ?: throw BadRequest("month_days must be numbers 1-31, got '$d'")
            m or (1 shl (n - 1))
        }
    }

    private fun habitFrequency(b: JsonObject): HabitFrequency? = when (b.str("frequency")?.trim()?.lowercase()) {
        null -> null
        "daily", "day", "every day", "everyday" -> HabitFrequency.DAILY
        "weekly", "week" -> HabitFrequency.WEEKLY
        "monthly", "month" -> HabitFrequency.MONTHLY
        else -> throw BadRequest("frequency must be daily, weekly or monthly")
    }

    private fun habitColor(b: JsonObject): Int? {
        val v = b.str("color")?.trim()?.lowercase() ?: return null
        val i = HABIT_COLORS.indexOf(when (v) { "violet" -> "purple"; "teal", "emerald" -> "green"; "amber", "yellow" -> "orange"; else -> v })
        if (i < 0) throw BadRequest("color must be one of ${HABIT_COLORS.joinToString()}")
        return i
    }

    private fun steps(b: JsonObject): List<ChecklistDraft>? {
        val v = b["steps"] ?: return null
        if (v is JsonNull) return null
        val items = when (v) {
            is JsonArray -> v.map { it.jsonPrimitive.content }
            is JsonPrimitive -> v.content.split('\n', ';')
            else -> throw BadRequest("steps must be a list of strings")
        }
        return items.map { it.trim().removePrefix("- ").trim() }.filter { it.isNotEmpty() }.map { ChecklistDraft(it) }
    }

    private fun goalDays(b: JsonObject): Int? = b.num("goal_days")?.toInt()

    /** Applies weekly/monthly days. A weekly habit with no days yet gets every day; a monthly one the 1st. */
    private fun withSchedule(h: Habit, b: JsonObject): Habit {
        val freq = habitFrequency(b) ?: if (b.containsKey("days") && b.str("days")?.lowercase() !in setOf("daily", "every day", "everyday"))
            HabitFrequency.WEEKLY else if (b.containsKey("month_days")) HabitFrequency.MONTHLY else h.frequency
        val week = habitDaysMask(b["days"]) ?: h.weekDaysMask
        val month = monthDaysMask(b["month_days"]) ?: h.monthDaysMask
        return when (freq) {
            HabitFrequency.DAILY -> h.copy(frequency = freq)
            HabitFrequency.WEEKLY -> (if (week == 0) 0b1111111 else week).let { m ->
                h.copy(frequency = freq, weekDaysMask = m, targetDaysPerWeek = m.countOneBits())
            }
            HabitFrequency.MONTHLY -> (if (month == 0) 1 else month).let { m ->
                h.copy(frequency = freq, monthDaysMask = m, targetDaysPerMonth = m.countOneBits())
            }
        }
    }

    private suspend fun addHabit(b: JsonObject): JsonElement {
        val name = b.str("name")?.trim()?.takeIf { it.isNotEmpty() } ?: throw BadRequest("name is required")
        val description = b.str("description")?.trim().orEmpty()
        val asked = b.str("icon")?.trim()?.takeIf { it.isNotEmpty() }
        val named = asked?.let(HabitIconGuess::byName)
        val icon = named ?: HabitIconGuess.guess(name, description)
        val steps = steps(b).orEmpty()
        var h = Habit(
            name = name, description = description,
            reminderTime = b.str("reminder_time")?.takeIf { it.isNotBlank() }?.let(::time),
            durationDays = goalDays(b) ?: 0,
            colorIndex = habitColor(b) ?: 0,
            iconIndex = icon,
            isChecklist = steps.isNotEmpty()
        )
        h = withSchedule(h, b)
        val id = if (steps.isNotEmpty()) habits.addHabitWithItems(h, steps) else habits.addHabit(h)
        val saved = habits.getHabitByIdRaw(id) ?: throw IllegalStateException("Habit wasn't saved")
        val iconName = HabitIconGuess.labels.getOrNull(icon)
        val colorName = HABIT_COLORS[saved.colorIndex.coerceIn(0, 4)]
        val tell = buildString {
            if (asked != null && named == null) append("There's no '$asked' icon, so ")
            append(if (iconName != null) "I gave it the $iconName icon ($colorName). " else "It has the default icon ($colorName). ")
            append("They can change the icon and colour in Bori.")
            val extras = listOfNotNull(
                "a reminder time".takeIf { saved.reminderTime == null },
                "a goal of a number of days".takeIf { saved.durationDays == 0 }
            )
            if (extras.isNotEmpty()) append(" It can also have ${extras.joinToString(" or ")} if they'd like.")
        }
        return JsonObject(habitJson(saved) + ("tell_user" to JsonPrimitive(tell)))
    }

    private suspend fun editHabit(id: String, b: JsonObject): JsonElement {
        val old = habit(id)
        val asked = b.str("icon")?.trim()?.takeIf { it.isNotEmpty() }
        val icon = asked?.let { HabitIconGuess.byName(it) ?: throw BadRequest("No icon called '$it'. Try a simple word like Running, Reading, Gym, Hydration, Meditation, Sleep.") }
        val steps = steps(b)
        var h = old.copy(
            name = b.str("name")?.trim()?.takeIf { it.isNotEmpty() } ?: old.name,
            description = if (b.containsKey("description")) b.str("description")?.trim().orEmpty() else old.description,
            reminderTime = if (b.containsKey("reminder_time")) b.str("reminder_time")?.takeIf { it.isNotBlank() && it.lowercase() != "none" }?.let(::time) else old.reminderTime,
            durationDays = goalDays(b) ?: old.durationDays,
            colorIndex = habitColor(b) ?: old.colorIndex,
            colorArgb = if (b.containsKey("color")) null else old.colorArgb,
            iconIndex = icon ?: old.iconIndex,
            isChecklist = steps?.isNotEmpty() ?: old.isChecklist
        )
        h = withSchedule(h, b)
        if (steps != null) habits.updateHabitWithItems(h, steps) else habits.updateHabit(h)
        return habitJson(habit(id))
    }

    private fun preferencesJson() = buildJsonObject { put("ask_puzzle_on_alarms", _state.value.askPuzzleOnAlarms) }

    // ── Events ──────────────────────────────────────────────────────────────────────────────────

    private suspend fun event(id: String) =
        events.getEventBySyncId(id)?.takeIf { it.deletedAt == null } ?: throw NotFound("No event with that id")

    private suspend fun addEvent(b: JsonObject): CalendarEvent {
        val title = b.str("title")?.trim().takeUnless { it.isNullOrEmpty() } ?: throw BadRequest("title is required")
        val start = date(b.str("date") ?: throw BadRequest("date is required (yyyy-MM-dd)"))
        val end = b.str("end_date")?.let(::date) ?: start
        val startTime = b.str("start_time")?.let(::time)
        val endTime = b.str("end_time")?.let(::time) ?: startTime?.let { plusHour(it) }
        if (end < start) throw BadRequest("end_date is before date")
        val e = CalendarEvent(title = title, note = b.str("note").orEmpty(), startDate = start, endDate = end,
            startTime = startTime, endTime = endTime, recurrence = repeatRule(b["repeat"]) ?: "")
        val id = events.addEvent(e)
        alerts(b)?.let { mins -> eventReminders.insertAll(mins.map { EventReminder(eventId = id, minutesBefore = it) }) }
        return events.getEventBySyncId(e.syncId)!!
    }

    private suspend fun editEvent(id: String, b: JsonObject): CalendarEvent {
        b.str("only_date")?.let { return editOneDate(id, date(it), b) }
        val e = event(id)
        val start = b.str("date")?.let(::date) ?: e.startDate
        var updated = e.copy(
            title = b.str("title")?.trim()?.ifEmpty { null } ?: e.title,
            note = b.str("note") ?: e.note,
            startDate = start,
            endDate = b.str("end_date")?.let(::date) ?: if (b.str("date") != null && e.endDate == e.startDate) start else e.endDate,
            startTime = if (b.containsKey("start_time")) b.str("start_time")?.let(::time) else e.startTime,
            endTime = newEndTime(b, e.startTime, e.endTime),
            recurrence = repeatRule(b["repeat"]) ?: e.recurrence
        )
        if (updated.startTime != null && updated.endTime == null) updated = updated.copy(endTime = plusHour(updated.startTime!!))
        if (updated.endDate < updated.startDate) throw BadRequest("end_date is before date")
        events.updateEvent(updated)
        alerts(b)?.let { mins ->
            eventReminders.deleteForEvent(e.id)
            eventReminders.insertAll(mins.map { EventReminder(eventId = e.id, minutesBefore = it) })
            events.updateEvent(event(id)) // bump updatedAt so sync carries the new alerts
        }
        return event(id)
    }

    /**
     * One date of a repeating event changed on its own ("only this event"): that date is skipped in the
     * series and becomes a separate event with the changes. Returns the new event.
     */
    private suspend fun editOneDate(id: String, day: String, b: JsonObject): CalendarEvent {
        val series = event(id)
        val d = LocalDate.parse(day)
        val occ = series.occurrencesIn(
            kotlinx.datetime.LocalDate.parse(day), kotlinx.datetime.LocalDate.parse(day)
        ).firstOrNull { it.occurrenceDate == day } ?: throw NotFound("That event doesn't happen on $day")
        val newStart = b.str("date")?.let(::date) ?: day
        val span = java.time.temporal.ChronoUnit.DAYS.between(d, LocalDate.parse(occ.endDate))
        val edited = occ.copy(
            title = b.str("title")?.trim()?.ifEmpty { null } ?: occ.title,
            note = b.str("note") ?: occ.note,
            startDate = newStart, endDate = LocalDate.parse(newStart).plusDays(span).toString(),
            startTime = if (b.containsKey("start_time")) b.str("start_time")?.let(::time) else occ.startTime,
            endTime = newEndTime(b, occ.startTime, occ.endTime),
            recurrence = ""
        )
        val saved = events.saveEdit(edited.copy(recurrence = series.recurrence), onlyThis = true)
        return events.getEventBySyncId(saved.syncId) ?: saved
    }

    /**
     * `repeat`: "daily", "weekdays", "weekly", "monthly", "yearly", "never", an RRULE, or an object
     * `{every, unit: day|week|month|year, days: [mon..sun], until: yyyy-MM-dd, count}`. Null when absent.
     */
    private fun repeatRule(v: JsonElement?): String? {
        if (v == null) return null
        if (v is JsonNull) return ""
        if (v is JsonPrimitive) {
            val w = v.content.trim().lowercase()
            return when (w) {
                "", "never", "none", "no", "once" -> ""
                "daily", "every day" -> "FREQ=DAILY"
                "weekdays", "every weekday" -> "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR"
                "weekly", "every week" -> "FREQ=WEEKLY"
                "biweekly", "every 2 weeks", "every other week" -> "FREQ=WEEKLY;INTERVAL=2"
                "monthly", "every month" -> "FREQ=MONTHLY"
                "yearly", "annually", "every year" -> "FREQ=YEARLY"
                else -> Recurrence.parse(v.content)?.format()
                    ?: throw BadRequest("repeat must be daily, weekdays, weekly, monthly, yearly, never, or {every, unit, days, until, count}")
            }
        }
        val o = v as? JsonObject ?: throw BadRequest("repeat must be a word or an object")
        val unit = o.str("unit")?.trim()?.lowercase()?.removeSuffix("s") ?: "week"
        val freq = when (unit) {
            "day" -> Recurrence.Freq.DAILY; "week" -> Recurrence.Freq.WEEKLY
            "month" -> Recurrence.Freq.MONTHLY; "year" -> Recurrence.Freq.YEARLY
            else -> throw BadRequest("repeat.unit is day, week, month or year")
        }
        val every = o.num("every")?.toInt() ?: 1
        val days = daysMask(o["days"])?.let { m -> (0..6).filter { m and (1 shl it) != 0 }.map { if (it == 0) 7 else it }.toSet() } ?: emptySet()
        val count = o.num("count")?.toInt()
        val until = o.str("until")?.let { date(it) }?.let { kotlinx.datetime.LocalDate.parse(it) }
        if (count != null && until != null) throw BadRequest("Give repeat.until or repeat.count, not both")
        return runCatching {
            Recurrence(freq, interval = every, byDay = if (freq == Recurrence.Freq.WEEKLY) days else emptySet(), until = until, count = count).format()
        }.getOrElse { throw BadRequest("repeat: ${it.message}") }
    }

    private fun repeatText(rule: String, start: String): String? =
        Recurrence.parse(rule)?.describe(runCatching { kotlinx.datetime.LocalDate.parse(start.take(10)) }.getOrNull())

    /** `remind_minutes_before`: a number or a list of numbers. */
    private fun alerts(b: JsonObject): List<Int>? {
        val v = b["remind_minutes_before"] ?: return null
        val list = when (v) {
            is JsonArray -> v.map { it.jsonPrimitive.intOrNull ?: throw BadRequest("remind_minutes_before must be numbers") }
            is JsonPrimitive -> listOf(v.intOrNull ?: throw BadRequest("remind_minutes_before must be a number"))
            else -> throw BadRequest("remind_minutes_before must be a number or a list")
        }
        if (list.any { it < 0 || it > 60 * 24 * 60 }) throw BadRequest("remind_minutes_before out of range")
        return list.distinct()
    }

    private suspend fun eventJson(e: CalendarEvent) = buildJsonObject {
        put("id", e.syncId); put("title", e.title); put("date", e.startDate)
        if (e.endDate != e.startDate) put("end_date", e.endDate)
        put("start_time", e.startTime); put("end_time", e.endTime); put("all_day", e.startTime == null)
        if (e.note.isNotBlank()) put("note", e.note)
        repeatText(e.recurrence, e.startDate)?.let { put("repeat", it) }
        // One date of a repeating event (the agenda lists each); pass it as only_date / date to change
        // or delete just that date.
        e.occurrenceDate?.let { put("occurrence_date", it) }
        val alerts = eventReminders.getForEventOnce(e.id).mapNotNull { it.minutesBefore }
        if (alerts.isNotEmpty()) put("remind_minutes_before", JsonArray(alerts.map { JsonPrimitive(it) }))
    }

    // ── To-dos ──────────────────────────────────────────────────────────────────────────────────

    private suspend fun todo(id: String) =
        todos.getTodoBySyncId(id)?.takeIf { it.deletedAt == null } ?: throw NotFound("No to-do with that id")

    private fun due(s: String?): String? = s?.trim()?.let {
        when {
            it.isEmpty() -> ""
            DATE.matches(it) -> date(it)
            it.length == 16 && it[10] == ' ' -> "${date(it.take(10))} ${time(it.substring(11))}"
            else -> throw BadRequest("due must be yyyy-MM-dd or yyyy-MM-dd HH:mm")
        }
    }

    private fun priority(s: String?): String? = s?.let {
        runCatching { TodoPriority.valueOf(it.trim().uppercase()).name }.getOrElse { throw BadRequest("priority is high, medium, low or none") }
    }

    /** A to-do's `remind_minutes_before` (one number; "30" works too). Null = not given. */
    private fun earlyAlert(b: JsonObject, dueDate: String): Int? {
        val m = alerts(b)?.let { it.firstOrNull() ?: 0 } ?: return null
        if (m > 0 && !(dueDate.length >= 16 && dueDate[10] == ' '))
            throw BadRequest("remind_minutes_before needs a due time: pass due as 'yyyy-MM-dd HH:mm'")
        return m
    }

    private suspend fun addTodo(b: JsonObject): Todo {
        val title = b.str("title")?.trim().takeUnless { it.isNullOrEmpty() } ?: throw BadRequest("title is required")
        val rule = repeatRule(b["repeat"]) ?: ""
        // A repeating to-do needs a date to move forward from: today when none was given.
        val dueDate = due(b.str("due"))?.ifEmpty { null } ?: if (rule.isNotEmpty()) LocalDate.now().toString() else ""
        val t = Todo(title = title, note = b.str("note").orEmpty(), dueDate = dueDate,
            priority = priority(b.str("priority")) ?: TodoPriority.NONE.name, recurrence = rule,
            earlyAlertMinutes = earlyAlert(b, dueDate) ?: 0)
        val id = todos.addTodo(t)
        b["subtasks"]?.let { s ->
            val labels = (s as? JsonArray)?.mapNotNull { it.jsonPrimitive.contentOrNull?.trim()?.ifEmpty { null } }
                ?: throw BadRequest("subtasks must be a list of strings")
            todos.replaceSubtasks(id, labels.mapIndexed { i, l -> TodoSubtask(todoId = id, label = l, sortOrder = i) })
        }
        return todos.getTodoBySyncId(t.syncId)!!
    }

    private suspend fun editTodo(id: String, b: JsonObject): Todo {
        val t = todo(id)
        val newDue = if (b.containsKey("due")) due(b.str("due")) ?: "" else t.dueDate
        // A moved date restarts the series there (drops the roll-forward anchor).
        val rule = (repeatRule(b["repeat"]) ?: t.recurrence).let { r ->
            if (newDue != t.dueDate) Recurrence.parse(r)?.copy(anchor = null)?.format() ?: r else r
        }
        val finalDue = if (rule.isNotEmpty() && newDue.isEmpty()) LocalDate.now().toString() else newDue
        // A due time removed takes the early alert with it.
        val early = earlyAlert(b, finalDue) ?: if (finalDue.length >= 16) t.earlyAlertMinutes else 0
        todos.updateTodo(t.copy(
            title = b.str("title")?.trim()?.ifEmpty { null } ?: t.title,
            note = b.str("note") ?: t.note,
            dueDate = finalDue,
            priority = priority(b.str("priority")) ?: t.priority,
            recurrence = rule,
            earlyAlertMinutes = early
        ))
        return todo(id)
    }

    private suspend fun todoJson(t: Todo) = buildJsonObject {
        put("id", t.syncId); put("title", t.title)
        if (t.dueDate.isNotBlank()) put("due", t.dueDate)
        if (t.earlyAlertMinutes > 0) put("remind_minutes_before", t.earlyAlertMinutes)
        if (t.priority != TodoPriority.NONE.name) put("priority", t.priority.lowercase())
        if (t.note.isNotBlank()) put("note", t.note)
        repeatText(t.recurrence, t.dueDate)?.let { put("repeat", it) }
        put("done", t.isCompleted)
        val subs = todos.getSubtasksForTodo(t.id)
        if (subs.isNotEmpty()) put("subtasks", buildJsonArray {
            subs.forEach { s -> add(buildJsonObject { put("label", s.label); put("done", s.isDone) }) }
        })
    }

    // ── Reminders ───────────────────────────────────────────────────────────────────────────────

    private suspend fun reminder(id: String) =
        reminders.getReminderBySyncId(id)?.takeIf { it.deletedAt == null } ?: throw NotFound("No reminder with that id")

    private fun at(s: String): String {
        val v = s.trim().replace('T', ' ')
        if (v.length != 16 || v[10] != ' ') throw BadRequest("at must be yyyy-MM-dd HH:mm")
        return "${date(v.take(10))} ${time(v.substring(11))}"
    }

    private suspend fun addReminder(b: JsonObject): Reminder {
        val title = b.str("title")?.trim().takeUnless { it.isNullOrEmpty() } ?: throw BadRequest("title is required")
        val r = Reminder(title = title, note = b.str("note").orEmpty(), dateTime = at(b.str("at") ?: throw BadRequest("at is required")),
            recurrence = repeatRule(b["repeat"]) ?: "")
        reminders.addReminder(r)
        return reminders.getReminderBySyncId(r.syncId)!!
    }

    private suspend fun editReminder(id: String, b: JsonObject): Reminder {
        val r = reminder(id)
        val newAt = b.str("at")?.let(::at) ?: r.dateTime
        val rule = (repeatRule(b["repeat"]) ?: r.recurrence).let { x ->
            if (newAt != r.dateTime) Recurrence.parse(x)?.copy(anchor = null)?.format() ?: x else x
        }
        reminders.updateReminder(r.copy(
            title = b.str("title")?.trim()?.ifEmpty { null } ?: r.title,
            note = b.str("note") ?: r.note,
            dateTime = newAt,
            isCompleted = b.bool("done") ?: r.isCompleted,
            recurrence = rule
        ))
        return reminder(id)
    }

    private fun reminderJson(r: Reminder) = buildJsonObject {
        put("id", r.syncId); put("title", r.title); put("at", r.dateTime); put("done", r.isCompleted)
        repeatText(r.recurrence, r.dateTime)?.let { put("repeat", it) }
        if (r.note.isNotBlank()) put("note", r.note)
    }

    // ── Notes ───────────────────────────────────────────────────────────────────────────────────

    /** A note from Markdown. Default notebook: the Markdown vault's, so it also lands in Obsidian. */
    private suspend fun addNote(b: JsonObject): JsonElement {
        val title = b.str("title")?.trim().takeUnless { it.isNullOrEmpty() } ?: throw BadRequest("title is required")
        val books = notesDao.getAllNotebooksForBackup().filter { it.deletedAt == null && !it.isPrivate }
        val book = b.str("notebook")?.let { name -> books.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) } ?: throw NotFound("No notebook named $name") }
            ?: vault.rootNotebookSyncId()?.let { id -> books.firstOrNull { it.syncId == id } }
            ?: books.firstOrNull()
            ?: throw NotFound("No notebook to put it in")
        val parsed = VaultMarkdown.parse(b.str("markdown").orEmpty())
        val blocks = parsed.toBlocks()
        val tags = tagList(b) ?: parsed.tags
        val n = Note(notebookId = book.id, title = title, content = NoteBlocksJson.encode(blocks), tags = tags.joinToString(","),
            bodyPreview = NoteBlocksJson.preview(blocks))
        notes.insertNote(n)
        return buildJsonObject { put("id", n.syncId); put("title", title); put("notebook", book.name) }
    }

    private fun tagList(b: JsonObject): List<String>? = b["tags"]?.let { v ->
        (v as? JsonArray)?.mapNotNull { it.jsonPrimitive.contentOrNull?.trim()?.removePrefix("#")?.ifEmpty { null } }
            ?: throw BadRequest("tags must be a list of strings")
    }

    /** A note an agent may see: not deleted, not in a private (locked) notebook. */
    private suspend fun openNote(id: String): Note {
        val n = notesDao.getNoteBySyncId(id)?.takeIf { it.deletedAt == null } ?: throw NotFound("No note with that id")
        val book = notesDao.getAllNotebooksForBackup().firstOrNull { it.id == n.notebookId }
        if (book?.isPrivate == true) throw NotFound("No note with that id")
        return n
    }

    /** The whole note as Markdown, the same text the vault file holds (minus frontmatter). */
    private suspend fun noteJson(n: Note): JsonElement {
        val book = notesDao.getAllNotebooksForBackup().firstOrNull { it.id == n.notebookId }
        val blocks = NoteBlocksJson.decode(n.content)
        return buildJsonObject {
            put("id", n.syncId); put("title", n.title); put("notebook", book?.name)
            put("tags", JsonArray(n.tags.split(',').map { it.trim() }.filter { it.isNotEmpty() }.map { JsonPrimitive(it) }))
            // Handwriting (stroke data) and pictures would only flood the agent; they are flagged instead.
            put("markdown", VaultMarkdown.write(null, emptyList(), blocks.filter { it.type != BlockType.INK && it.type != BlockType.IMAGE }))
            if (blocks.any { it.type == BlockType.INK }) put("has_handwriting", true)
            if (blocks.any { it.type == BlockType.IMAGE }) put("has_pictures", true)
        }
    }

    /** `markdown` replaces the body, `append` adds to the end; title and tags change only when given. */
    private suspend fun editNote(id: String, b: JsonObject): Note {
        val n = openNote(id)
        val replace = b.str("markdown")
        val append = b.str("append")
        if (replace != null && append != null) throw BadRequest("Pass markdown (replace) or append, not both")
        val blocks = when {
            replace != null -> {
                val kept = NoteBlocksJson.decode(n.content).filter { it.type == BlockType.INK || it.type == BlockType.IMAGE }
                if (kept.isNotEmpty() && b.bool("drop_handwriting_and_pictures") != true) {
                    throw BadRequest("This note has handwriting or pictures that Markdown can't hold. Use append, " +
                        "or pass drop_handwriting_and_pictures=true after the user agrees.")
                }
                VaultMarkdown.parse(replace).toBlocks()
            }
            append != null -> {
                val old = NoteBlocksJson.decode(n.content).dropLastWhile { it.type == BlockType.TEXT && it.content.isBlank() }
                old + VaultMarkdown.parse(append).toBlocks()
            }
            else -> null
        }
        val title = b.str("title")?.trim()?.ifEmpty { null }
        val tags = tagList(b)
        if (blocks == null && title == null && tags == null) throw BadRequest("Nothing to change: pass markdown, append, title or tags")
        notes.updateNote(n.copy(
            title = title ?: n.title,
            tags = tags?.joinToString(",") ?: n.tags,
            content = blocks?.let { NoteBlocksJson.encode(it) } ?: n.content,
            bodyPreview = blocks?.let { NoteBlocksJson.preview(it) } ?: n.bodyPreview
        ))
        return openNote(id)
    }

    // ── Timers ──────────────────────────────────────────────────────────────────────────────────

    private suspend fun timer(id: String) =
        timers.getTimerBySyncId(id)?.takeIf { it.deletedAt == null } ?: throw NotFound("No timer with that id")

    /** A timer that starts counting down right away. */
    private suspend fun addTimer(b: JsonObject): Timer {
        val secs = (b.num("minutes") ?: 0.0) * 60 + (b.num("seconds") ?: 0.0) + (b.num("hours") ?: 0.0) * 3600
        val total = secs.toInt()
        if (total < 1 || total > 24 * 3600) throw BadRequest("Give a duration of 1 second to 24 hours (hours, minutes, seconds)")
        val t = Timer(label = b.str("label")?.trim().orEmpty(), durationSeconds = total, remainingSeconds = total,
            endTimeMillis = System.currentTimeMillis() + total * 1000L)
        timers.addTimer(t)
        return timer(t.syncId)
    }

    private fun timerJson(t: Timer) = buildJsonObject {
        put("id", t.syncId); put("label", t.label); put("duration_seconds", t.durationSeconds)
        val running = t.endTimeMillis > System.currentTimeMillis()
        put("running", running)
        if (running) {
            put("remaining_seconds", ((t.endTimeMillis - System.currentTimeMillis()) / 1000).toInt())
            put("ends_at", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(Instant.ofEpochMilli(t.endTimeMillis).atZone(ZoneId.systemDefault())))
        } else put("remaining_seconds", t.remainingSeconds)
    }

    // ── Alarms ──────────────────────────────────────────────────────────────────────────────────

    private suspend fun alarm(id: String) =
        alarms.getAlarmBySyncId(id)?.takeIf { it.deletedAt == null } ?: throw NotFound("No alarm with that id")

    /** `days`: "once" (default), "daily", "weekdays", "weekends", or a list like ["mon","wed"]. */
    private fun daysMask(v: JsonElement?): Int? {
        if (v == null || v is JsonNull) return null
        fun bit(d: String): Int {
            val i = DAYS.indexOf(d.trim().lowercase().take(3))
            if (i < 0) throw BadRequest("Unknown day '$d' (use mon, tue, wed, thu, fri, sat, sun)")
            return 1 shl i
        }
        return when (v) {
            is JsonArray -> v.fold(0) { m, d -> m or bit(d.jsonPrimitive.content) }
            is JsonPrimitive -> when (v.content.trim().lowercase()) {
                "once", "", "none" -> 0
                "daily", "every day", "everyday" -> 0b1111111
                "weekdays" -> 0b0111110
                "weekends" -> 0b1000001
                // Small models sometimes send a list as text: "['sun', 'mon']".
                else -> v.content.trim('[', ']', ' ').split(',').map { it.trim(' ', '\'', '"') }.filter { it.isNotEmpty() }
                    .ifEmpty { return 0 }.fold(0) { m, d -> m or bit(d) }
            }
            else -> throw BadRequest("days must be a word or a list of days")
        }
    }

    /** `puzzle`: a wake-up check the phone asks for before the alarm stops (none, math, shake, memory, typing, pairs). */
    private fun puzzle(b: JsonObject): String? {
        val v = b.str("puzzle")?.trim()?.lowercase() ?: return null
        return when (v) {
            "", "off", "no", "false" -> "none"
            "maths", "arithmetic" -> "math"
            "sequence" -> "memory"
            "type" -> "typing"
            "match", "matching" -> "pairs"
            in PUZZLES -> v
            else -> throw BadRequest("Unknown puzzle '$v' (use ${PUZZLES.joinToString()})")
        }
    }

    /** `difficulty`: 1-3 or easy/medium/hard. */
    private fun difficulty(b: JsonObject): Int? {
        val v = b.str("difficulty")?.trim()?.lowercase() ?: return null
        return when (v) {
            "easy" -> 1
            "medium" -> 2
            "hard" -> 3
            else -> v.toDoubleOrNull()?.takeIf { it == Math.floor(it) && it in 1.0..3.0 }?.toInt()
                ?: throw BadRequest("difficulty must be 1, 2 or 3 (easy, medium, hard)")
        }
    }

    /**
     * Safety net for small models that write the puzzle into the label instead of setting `puzzle`
     * (seen 2026-11-10: label "Math Puzzle Alarm - 8+6=?", puzzle unset). A sum in the label gives the
     * answer away, so it is cut out, and a label naming a puzzle turns that puzzle on.
     */
    private fun cleanAlarmLabel(label: String): String =
        label.replace(SUM_IN_LABEL, " ").replace(Regex("\\s+"), " ").trim().trimEnd('-', ':', '–', '—', ',').trim()

    private fun puzzleFromLabel(label: String): String? {
        // Only a label that says "puzzle" or holds a sum: "Protein shake" or "Typing class" stay plain alarms.
        val l = label.lowercase()
        if ("puzzle" !in l && !SUM_IN_LABEL.containsMatchIn(label)) return null
        return listOf("shake", "memory", "typing", "pairs").firstOrNull { it in l } ?: "math"
    }

    private suspend fun addAlarm(b: JsonObject): Alarm {
        val t = time(b.str("time") ?: throw BadRequest("time is required (HH:mm, 24-hour)"))
        val rawLabel = b.str("label")?.trim().orEmpty()
        val chosen = puzzle(b)
        val a = Alarm(label = cleanAlarmLabel(rawLabel), hour = t.take(2).toInt(), minute = t.substring(3).toInt(),
            repeatDaysMask = daysMask(b["days"]) ?: 0, dismissMission = chosen ?: puzzleFromLabel(rawLabel) ?: "none",
            missionDifficulty = difficulty(b) ?: 1)
        alarms.addAlarm(a)
        return alarm(a.syncId)
    }

    private suspend fun editAlarm(id: String, b: JsonObject): Alarm {
        val a = alarm(id)
        val t = b.str("time")?.let(::time)
        alarms.updateAlarm(a.copy(
            label = b.str("label")?.trim()?.let(::cleanAlarmLabel) ?: a.label,
            hour = t?.take(2)?.toInt() ?: a.hour,
            minute = t?.substring(3)?.toInt() ?: a.minute,
            repeatDaysMask = daysMask(b["days"]) ?: a.repeatDaysMask,
            dismissMission = puzzle(b) ?: a.dismissMission,
            missionDifficulty = difficulty(b) ?: a.missionDifficulty,
            isEnabled = b.bool("enabled") ?: if (t != null) true else a.isEnabled
        ))
        return alarm(id)
    }

    private fun alarmJson(a: Alarm) = buildJsonObject {
        put("id", a.syncId); put("label", a.label); put("time", "%02d:%02d".format(a.hour, a.minute))
        put("days", when (a.repeatDaysMask) {
            0 -> JsonPrimitive("once")
            0b1111111 -> JsonPrimitive("daily")
            0b0111110 -> JsonPrimitive("weekdays")
            0b1000001 -> JsonPrimitive("weekends")
            else -> JsonArray(DAYS.indices.filter { a.repeatDaysMask and (1 shl it) != 0 }.map { JsonPrimitive(DAYS[it]) })
        })
        put("enabled", a.isEnabled)
        put("puzzle", a.dismissMission.takeIf { it in PUZZLES } ?: "none"); put("difficulty", a.missionDifficulty)
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────────────────────

    private fun date(s: String): String {
        val v = s.trim()
        if (!DATE.matches(v) || runCatching { LocalDate.parse(v) }.isFailure) throw BadRequest("Bad date '$s' (want yyyy-MM-dd)")
        return v
    }

    private fun time(s: String): String {
        val v = s.trim().let { if (it.length == 4 && it[1] == ':') "0$it" else it }
        if (!TIME.matches(v)) throw BadRequest("Bad time '$s' (want HH:mm, 24-hour)")
        return v
    }

    /**
     * The end time after an edit. Only a new start given: the event keeps its length (10–11 moved to 14:00
     * becomes 14–15), instead of keeping the old end and ending before it starts.
     */
    private fun newEndTime(b: JsonObject, oldStart: String?, oldEnd: String?): String? {
        if (b.containsKey("end_time")) return b.str("end_time")?.let(::time)
        if (!b.containsKey("start_time")) return oldEnd
        val start = b.str("start_time")?.let(::time) ?: return null
        if (oldStart == null || oldEnd == null) return plusHour(start)
        val mins = java.time.Duration.between(java.time.LocalTime.parse(oldStart), java.time.LocalTime.parse(oldEnd)).toMinutes()
        if (mins <= 0) return plusHour(start)
        val end = java.time.LocalTime.parse(start).toSecondOfDay() / 60 + mins
        return if (end >= 24 * 60) "23:59" else "%02d:%02d".format(end / 60, end % 60)
    }

    private fun plusHour(t: String): String {
        val h = t.take(2).toInt()
        return if (h >= 23) "23:59" else "%02d:%s".format(h + 1, t.substring(3))
    }

    private fun JsonObject.str(k: String): String? = (this[k] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.contentOrNull
    private fun JsonObject.bool(k: String): Boolean? = (this[k] as? JsonPrimitive)?.contentOrNull?.toBooleanStrictOrNull()
    private fun JsonObject.num(k: String): Double? = (this[k] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.contentOrNull?.let {
        it.toDoubleOrNull()?.takeIf { d -> d >= 0 } ?: throw BadRequest("$k must be a positive number")
    }
}
