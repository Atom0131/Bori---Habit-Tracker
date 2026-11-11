package com.apagon.rhythm.data.agent

import com.apagon.rhythm.platform.AppHome
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.io.File
import java.io.PrintStream
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * **`bori --mcp`**: an MCP server on stdin/stdout that lets an AI app on this computer (Hermes,
 * Claude Desktop, Claude Code, Cursor, …) use Bori. It forwards each tool call to the running Bori's
 * [AgentApiServer] on 127.0.0.1 with the token from `agent_token` in the data folder.
 *
 * Built into the app (since 1.1.9) so users need nothing else installed: an AI app's config just
 * runs Bori's own launcher with `--mcp`. It never opens a window, a database or the single-instance
 * lock; `main()` branches here before any of that. Replaces the Python `tools/bori-mcp` script, whose
 * tool list it mirrors.
 *
 * Environment overrides (same as the old script): `BORI_AGENT_URL`, `BORI_AGENT_TOKEN`, `BORI_HOME`.
 */
object BoriMcpServer {
    const val VERSION = "1.3.0"
    private const val PROTOCOL_VERSION = "2025-06-18"

    private val json = Json { ignoreUnknownKeys = true }
    private val pretty = Json { prettyPrint = true }
    /** Where the running Bori listens; tests point it at their own server. */
    internal var url = (System.getenv("BORI_AGENT_URL") ?: "http://127.0.0.1:${AgentApiServer.PORT}").trimEnd('/')
    private val http by lazy { HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build() }

    /** What an AI app reads once when it connects: the rules the old Hermes skill carried. */
    val INSTRUCTIONS = """
        Bori is the user's own habit tracker, calendar, to-do list, reminders, notes, alarms and timers app (computer + phone, synced).
        - Dates are yyyy-MM-dd and times HH:mm (24-hour), always the user's local time; never convert to UTC. If unsure of today's date, call bori_agenda first: it returns today.
        - Turn the user's words into absolute values yourself ("Tuesday" = the next Tuesday, "morning" = 09:00, "tonight" = 20:00).
        - Pick the right kind: something at a time with a duration = event; a task to get done = to-do (it only rings if it has a due time; a date alone is silent, which is right when the user didn't ask to be reminded); "remind me to X at T" = reminder (it rings); something to do regularly or a routine to build = habit (bori_add_habit); "I did my habit" = bori_list_habits then bori_check_habit; longer writing = note; "set a timer" = timer; "wake me" / "alarm" = alarm.
        - Only ask a follow-up question when a tool result tells you to, or when you truly can't tell which kind the user means: then ask one short question instead of guessing. Don't ask about extras the user didn't mention.
        - Anything that repeats goes on the item itself with `repeat` (daily, weekdays, weekly, biweekly, monthly, yearly, or {every, unit, days, until, count}).
        - Add and change things right away without asking "should I?". Then confirm by starting your reply with "✅ Updated in Bori Habit Tracker:" followed by one line of exactly what was saved, using the values the tool returned (e.g. "✅ Updated in Bori Habit Tracker: added to-do "Buy coffee", due tomorrow at 9:00"). Only say this after the tool succeeded; if it failed, say it was NOT saved and why.
        - To change or find something, look it up first (bori_agenda, bori_search_notes, bori_list_alarms_and_timers). Repeating events appear once per date with the same id and their own occurrence_date: use only_date to change a single date.
        - "An alarm with a puzzle / maths / shake to turn it off" = bori_add_alarm with puzzle set. Bori makes up a new problem every time it rings and keeps the answer hidden: never write a problem or its answer in the label, a note or your reply.
        - If the user asks what puzzles there are, answer from this list (no tool needed). Wake-up puzzles the phone asks for before an alarm stops: maths (easy: 1 single-digit sum, medium: 2 two-digit sums, hard: 3 problems with multiplication); shake (shake the phone 10 / 20 / 30 times); memory (repeat a 3 / 4 / 5-digit sequence); typing (type a short / medium / long phrase); pairs (match 3 / 4 / 6 pairs). Any alarm can have one, chosen when it's set or added later. They run on the phone; on the computer the alarm has a plain Dismiss button.
        - If a tool result says to ask whether they want a wake-up puzzle, ask once. "Yes, maths" = bori_update_alarm with puzzle (and difficulty); "no" = leave it; "stop asking" / "never ask" = bori_ai_preferences ask_puzzle_on_alarms=false ("always ask" turns it back on).
        - Turning an alarm off is bori_update_alarm enabled=false, stopping a timer is bori_stop_timer: neither is a delete.
        - DELETING: always say exactly what you will delete and wait for the user's explicit yes in this conversation, then call bori_delete with confirm=true. Never delete on your own initiative.
        - Don't create duplicates; check the agenda if unsure. Don't invent dates the user didn't give.
        - If a tool says Bori isn't running, tell the user to open Bori on the computer; don't retry in a loop.
    """.trimIndent()

    // ---- tool list -------------------------------------------------------------------------------

    private fun str(description: String? = null) = buildJsonObject {
        put("type", "string"); description?.let { put("description", it) }
    }
    private val DATE = str("yyyy-MM-dd")
    private val TIME = str("HH:mm, 24-hour")
    private const val REPEAT_TEXT = "How it repeats: 'daily', 'weekdays', 'weekly', 'biweekly', 'monthly', 'yearly', 'never', " +
        "or an object {every: n, unit: day|week|month|year, days: ['mon','wed'], until: yyyy-MM-dd, count: n}. " +
        "Weekly repeats on the start date's weekday unless days are given."
    private fun repeat(extra: String = "") = buildJsonObject {
        put("description", REPEAT_TEXT + extra)
        putJsonArray("anyOf") { add(buildJsonObject { put("type", "string") }); add(buildJsonObject { put("type", "object") }) }
    }
    private val INT = buildJsonObject { put("type", "integer") }
    private val NUM = buildJsonObject { put("type", "number") }
    private val BOOL = buildJsonObject { put("type", "boolean") }
    private val STRINGS = buildJsonObject { put("type", "array"); putJsonObject("items") { put("type", "string") } }
    private fun enumOf(vararg values: String) = buildJsonObject {
        put("type", "string"); putJsonArray("enum") { values.forEach { add(JsonPrimitive(it)) } }
    }
    private val PRIORITY = enumOf("high", "medium", "low", "none")
    private val HABIT_COLOR = enumOf("purple", "green", "blue", "red", "orange")
    private val HABIT_ICON = str("An icon name like Running, Reading, Gym, Hydration, Meditation, Sleep, Study, Medication, " +
        "Walking, Cooking, Music, Journal, Savings, Cleaning. Leave it out and Bori picks one.")
    private val PUZZLE = withDescription(enumOf("none", "math", "shake", "memory", "typing", "pairs"),
        "Wake-up puzzle the phone asks for before the alarm stops: math (solve sums), shake (shake the phone), " +
            "memory (repeat a digit sequence), typing (type a phrase), pairs (match circles); 'none' turns it off. " +
            "Bori makes up a NEW problem each time the alarm rings and hides the answer, so never invent one yourself: " +
            "no problem or answer in the label, a note or your reply. On the computer the alarm still stops with a plain Dismiss.")
    private val DIFFICULTY = withDescription(INT,
        "Puzzle difficulty 1-3 (easy, medium, hard), default 1. math: 1 single-digit problem / 2 two-digit / 3 multiplication; " +
            "shake: 10/20/30 shakes; memory: 3/4/5 digits; typing: short/medium/long phrase; pairs: 3/4/6 pairs.")
    private fun days(description: String? = null) = buildJsonObject {
        description?.let { put("description", it) }
        putJsonArray("anyOf") { add(buildJsonObject { put("type", "string") }); add(STRINGS) }
    }
    private fun withDescription(base: JsonObject, description: String) = JsonObject(base + ("description" to JsonPrimitive(description)))

    private fun tool(name: String, description: String, props: Map<String, JsonElement>, required: List<String> = emptyList()) =
        buildJsonObject {
            put("name", name)
            put("description", description)
            putJsonObject("inputSchema") {
                put("type", "object")
                put("properties", JsonObject(props))
                putJsonArray("required") { required.forEach { add(JsonPrimitive(it)) } }
            }
        }

    val TOOLS: List<JsonObject> = listOf(
        tool("bori_agenda",
            "Read the user's Bori agenda: calendar events, to-dos due (and overdue), reminders and today's habits. " +
                "Also returns today's date. Call this first when you need to know what's planned or to find an item's id.",
            mapOf("from" to DATE, "to" to str("yyyy-MM-dd, defaults to 'from'"))),
        tool("bori_add_event",
            "Add a calendar event to Bori. Omit start_time for an all-day event. end_time defaults to one hour after start.",
            mapOf("title" to str(), "date" to DATE, "start_time" to TIME, "end_time" to TIME,
                "end_date" to str("yyyy-MM-dd, only for events spanning several days"), "note" to str(),
                "remind_minutes_before" to withDescription(INT, "Alert this many minutes before the start, e.g. 60"),
                "repeat" to repeat()),
            listOf("title", "date")),
        tool("bori_update_event",
            "Change an existing Bori event (only the fields you pass). Get the id from bori_agenda. For a repeating event, " +
                "pass only_date (its occurrence_date) to change just that one date; without it the whole series changes.",
            mapOf("id" to str(), "title" to str(), "date" to DATE, "start_time" to TIME, "end_time" to TIME,
                "end_date" to DATE, "note" to str(), "remind_minutes_before" to INT, "repeat" to repeat(),
                "only_date" to str("yyyy-MM-dd: change only this date of a repeating event")),
            listOf("id")),
        tool("bori_add_todo",
            "Add a to-do to Bori, optionally with a due date/time, priority and sub-steps. A to-do with a due time " +
                "rings at that time on the user's computer and phone; remind_minutes_before adds an earlier alert too.",
            mapOf("title" to str(), "due" to str("yyyy-MM-dd or 'yyyy-MM-dd HH:mm'"), "priority" to PRIORITY,
                "note" to str(), "subtasks" to STRINGS,
                "remind_minutes_before" to withDescription(INT, "Also alert this many minutes before the due time, " +
                    "e.g. 30 (needs a due time; 0 = only at the due time)"),
                "repeat" to repeat(" A repeating to-do moves to its next date when completed.")),
            listOf("title")),
        tool("bori_update_todo", "Change an existing Bori to-do (only the fields you pass).",
            mapOf("id" to str(), "title" to str(), "due" to str(), "priority" to PRIORITY, "note" to str(), "repeat" to repeat(),
                "remind_minutes_before" to withDescription(INT, "Early alert, minutes before the due time (0 = none)")),
            listOf("id")),
        tool("bori_complete_todo", "Mark a Bori to-do done (or not done with done=false).",
            mapOf("id" to str(), "done" to BOOL), listOf("id")),
        tool("bori_add_reminder",
            "Add a reminder to Bori. It rings on the user's computer and phone at that time; with repeat it rings again " +
                "on each next date (e.g. 'remind me every day at 8 to take meds').",
            mapOf("title" to str(), "at" to str("'yyyy-MM-dd HH:mm', 24-hour (the first time)"), "note" to str(), "repeat" to repeat()),
            listOf("title", "at")),
        tool("bori_update_reminder", "Change a Bori reminder (only the fields you pass); done=true marks it completed.",
            mapOf("id" to str(), "title" to str(), "at" to str(), "note" to str(), "done" to BOOL, "repeat" to repeat()),
            listOf("id")),
        tool("bori_list_habits", "List the user's habits and whether each is done on a day (default today).",
            mapOf("date" to DATE)),
        tool("bori_add_habit",
            "Create a habit in Bori (something the user wants to do regularly). Only name is needed; pass what the user said. " +
                "frequency: daily (default), weekly (with days, e.g. ['mon','wed','fri']) or monthly (with month_days). " +
                "steps makes it a routine/checklist. Leave icon out and Bori picks one from the name. In your reply, say which " +
                "icon the result shows and that they can change the icon and colour in Bori; if the result has no reminder_time " +
                "or goal_days, mention in a few words that it can have those too.",
            mapOf("name" to str(), "description" to str(), "frequency" to enumOf("daily", "weekly", "monthly"),
                "days" to days("Weekly habits: a list of mon..sun, or 'weekdays' / 'weekends'"),
                "month_days" to withDescription(buildJsonObject { put("type", "array"); putJsonObject("items") { put("type", "integer") } },
                    "Monthly habits: days of the month, e.g. [1, 15]"),
                "reminder_time" to withDescription(TIME, "HH:mm, 24-hour: a daily nudge on the phone"),
                "goal_days" to withDescription(INT, "Keep it up for this many days, e.g. 30, 66 or 100"),
                "color" to HABIT_COLOR, "icon" to HABIT_ICON, "steps" to withDescription(STRINGS, "Routine steps, in order")),
            listOf("name")),
        tool("bori_update_habit",
            "Change a Bori habit (only the fields you pass). Get the id from bori_list_habits. steps replaces the whole step list; " +
                "reminder_time 'none' removes the reminder.",
            mapOf("id" to str(), "name" to str(), "description" to str(), "frequency" to enumOf("daily", "weekly", "monthly"),
                "days" to days(), "month_days" to buildJsonObject { put("type", "array"); putJsonObject("items") { put("type", "integer") } },
                "reminder_time" to str("HH:mm, 24-hour, or 'none'"), "goal_days" to INT,
                "color" to HABIT_COLOR, "icon" to HABIT_ICON, "steps" to STRINGS),
            listOf("id")),
        tool("bori_check_habit", "Check in a habit for a day (default today), or undo it with done=false.",
            mapOf("id" to str(), "date" to DATE, "done" to BOOL), listOf("id")),
        tool("bori_search_notes", "Search the user's Bori notes by text. Returns ids, titles and previews.",
            mapOf("query" to str()), listOf("query")),
        tool("bori_add_note",
            "Create a note in Bori from Markdown. It goes into the Obsidian vault notebook unless another notebook is named.",
            mapOf("title" to str(), "markdown" to str(), "notebook" to str(), "tags" to STRINGS),
            listOf("title", "markdown")),
        tool("bori_read_note", "Read a whole Bori note as Markdown. Get the id from bori_search_notes.",
            mapOf("id" to str()), listOf("id")),
        tool("bori_edit_note",
            "Change a Bori note. Use 'append' to add Markdown to the end (e.g. a new '- [ ] milk' item); use 'markdown' " +
                "only to replace the whole body (read it first). title and tags change only if given.",
            mapOf("id" to str(), "append" to str(), "markdown" to str(), "title" to str(), "tags" to STRINGS),
            listOf("id")),
        tool("bori_start_timer", "Start a countdown timer in Bori now (it rings on the computer and phone when done).",
            mapOf("minutes" to NUM, "seconds" to NUM, "hours" to NUM, "label" to str())),
        tool("bori_stop_timer", "Pause/stop a running Bori timer. Get the id from bori_list_alarms_and_timers.",
            mapOf("id" to str()), listOf("id")),
        tool("bori_add_alarm",
            "Set an alarm clock in Bori. days: leave it out for a one-time alarm (the next time that clock time comes); " +
                "otherwise 'daily', 'weekdays', " +
                "'weekends', or a list like ['mon','thu']. Use a repeating alarm for things like 'every morning at 8'. " +
                "For 'wake me with a puzzle / maths / shake', set puzzle (and difficulty if the user says how hard); " +
                "otherwise leave puzzle out. If the result has ask_about_puzzle: true, end your reply by asking once whether " +
                "they'd like a wake-up puzzle on it (maths, shake, memory, typing or pairs; easy, medium or hard), unless they " +
                "already said no puzzle. 'Yes, maths' = bori_update_alarm with puzzle; 'stop asking' = bori_ai_preferences " +
                "ask_puzzle_on_alarms=false.",
            mapOf("time" to TIME, "label" to str(), "days" to days("'once', 'daily', 'weekdays', 'weekends' or a list of mon..sun"),
                "puzzle" to PUZZLE, "difficulty" to DIFFICULTY),
            listOf("time")),
        tool("bori_update_alarm", "Change a Bori alarm (only the fields you pass); enabled=false turns it off without deleting. " +
                "puzzle adds or changes its wake-up puzzle ('none' removes it).",
            mapOf("id" to str(), "time" to TIME, "label" to str(), "days" to days(), "enabled" to BOOL,
                "puzzle" to PUZZLE, "difficulty" to DIFFICULTY),
            listOf("id")),
        tool("bori_list_alarms_and_timers", "List the user's Bori alarms and timers with their ids.", emptyMap()),
        tool("bori_ai_preferences",
            "Read or change how you (the assistant) behave in Bori. ask_puzzle_on_alarms=false when the user says to stop " +
                "asking about wake-up puzzles on alarms; true when they want to be asked again. Call with no arguments to read.",
            mapOf("ask_puzzle_on_alarms" to BOOL)),
        tool("bori_delete",
            "Delete a Bori item (it goes to Recently Deleted). ONLY call this after the user has explicitly said yes to " +
                "deleting this exact item in this conversation; then pass confirm=true. For one date of a repeating event, " +
                "pass date (its occurrence_date) to skip only that date.",
            mapOf("kind" to enumOf("events", "todos", "reminders", "notes", "alarms", "timers", "habits"), "id" to str(),
                "confirm" to BOOL, "date" to str("yyyy-MM-dd, events only: delete just this date of a repeating event")),
            listOf("kind", "id", "confirm")),
    )

    // ---- calls to the running Bori ---------------------------------------------------------------

    class BoriError(message: String) : Exception(message)

    /** Tools that only read; every other successful call changed something in Bori. */
    private val READ_ONLY = setOf("bori_agenda", "bori_list_habits", "bori_search_notes", "bori_read_note", "bori_list_alarms_and_timers")

    private fun token(): String {
        System.getenv("BORI_AGENT_TOKEN")?.takeIf { it.isNotBlank() }?.let { return it.trim() }
        val home = System.getenv("BORI_HOME")?.let(::File) ?: AppHome.dir
        return runCatching { File(home, "agent_token").readText().trim() }.getOrNull()?.takeIf { it.isNotEmpty() }
            ?: throw BoriError("Bori's agent token wasn't found. In Bori on this computer, turn on " +
                "Settings > Obsidian & AI > Allow local AI agents.")
    }

    private fun enc(s: String) = URLEncoder.encode(s, Charsets.UTF_8).replace("+", "%20")

    private fun request(method: String, path: String, body: JsonObject? = null, query: Map<String, String?> = emptyMap()): JsonElement {
        val q = query.filterValues { !it.isNullOrEmpty() }.entries.joinToString("&") { "${enc(it.key)}=${enc(it.value!!)}" }
        val builder = HttpRequest.newBuilder(URI.create(url + path + if (q.isEmpty()) "" else "?$q"))
            .timeout(Duration.ofSeconds(20))
            .header("Authorization", "Bearer ${token()}")
            .header("Content-Type", "application/json")
            .method(method, body?.let { HttpRequest.BodyPublishers.ofString(it.toString()) } ?: HttpRequest.BodyPublishers.noBody())
        val res = try {
            http.send(builder.build(), HttpResponse.BodyHandlers.ofString())
        } catch (e: java.io.IOException) { // refused, reset or timed out
            throw BoriError("Bori desktop is not running (or 'Allow local AI agents' is off in Settings > Obsidian & AI).")
        }
        val parsed = runCatching { json.parseToJsonElement(res.body().ifBlank { "null" }) }.getOrNull()
        if (res.statusCode() !in 200..299) {
            val msg = (parsed as? JsonObject)?.get("error")?.jsonPrimitive?.contentOrNull
            throw BoriError(msg ?: "Bori answered HTTP ${res.statusCode()}")
        }
        return parsed ?: JsonNull
    }

    private val HABIT_FIELDS = arrayOf("name", "description", "frequency", "days", "month_days", "reminder_time", "goal_days", "color", "icon", "steps")

    private fun JsonObject.pick(vararg keys: String) = JsonObject(filterKeys { it in keys }.filterValues { it !is JsonNull })
    private fun JsonObject.s(key: String): String? = (get(key) as? JsonPrimitive)?.contentOrNull
    private fun JsonObject.id(): String = s("id") ?: throw NoSuchElementException("id")

    fun callTool(name: String?, a: JsonObject): JsonElement = when (name) {
        "bori_agenda" -> request("GET", "/agenda", query = mapOf("from" to a.s("from"), "to" to a.s("to")))
        "bori_add_event" -> request("POST", "/events", a.pick("title", "date", "start_time", "end_time", "end_date", "note", "remind_minutes_before", "repeat"))
        "bori_update_event" -> request("PATCH", "/events/${enc(a.id())}", a.pick("title", "date", "start_time", "end_time", "end_date", "note", "remind_minutes_before", "repeat", "only_date"))
        "bori_add_todo" -> request("POST", "/todos", a.pick("title", "due", "priority", "note", "subtasks", "repeat", "remind_minutes_before"))
        "bori_update_todo" -> request("PATCH", "/todos/${enc(a.id())}", a.pick("title", "due", "priority", "note", "repeat", "remind_minutes_before"))
        "bori_complete_todo" -> request("POST", "/todos/${enc(a.id())}/complete", a.pick("done"))
        "bori_add_reminder" -> request("POST", "/reminders", a.pick("title", "at", "note", "repeat"))
        "bori_update_reminder" -> request("PATCH", "/reminders/${enc(a.id())}", a.pick("title", "at", "note", "done", "repeat"))
        "bori_list_habits" -> request("GET", "/habits", query = mapOf("date" to a.s("date")))
        "bori_add_habit" -> request("POST", "/habits", a.pick(*HABIT_FIELDS))
        "bori_update_habit" -> request("PATCH", "/habits/${enc(a.id())}", a.pick(*HABIT_FIELDS))
        "bori_ai_preferences" -> if (a.containsKey("ask_puzzle_on_alarms")) request("PATCH", "/preferences", a.pick("ask_puzzle_on_alarms"))
            else request("GET", "/preferences")
        "bori_check_habit" -> request("POST", "/habits/${enc(a.id())}/check", a.pick("date", "done"))
        "bori_search_notes" -> request("GET", "/notes/search", query = mapOf("q" to (a.s("query") ?: "")))
        "bori_add_note" -> request("POST", "/notes", a.pick("title", "markdown", "notebook", "tags"))
        "bori_read_note" -> request("GET", "/notes/${enc(a.id())}")
        "bori_edit_note" -> request("PATCH", "/notes/${enc(a.id())}", a.pick("append", "markdown", "title", "tags"))
        "bori_start_timer" -> request("POST", "/timers", a.pick("hours", "minutes", "seconds", "label"))
        "bori_stop_timer" -> request("POST", "/timers/${enc(a.id())}/stop")
        "bori_add_alarm" -> request("POST", "/alarms", a.pick("time", "label", "days", "puzzle", "difficulty"))
        "bori_update_alarm" -> request("PATCH", "/alarms/${enc(a.id())}", a.pick("time", "label", "days", "enabled", "puzzle", "difficulty"))
        "bori_list_alarms_and_timers" -> buildJsonObject {
            put("alarms", request("GET", "/alarms")); put("timers", request("GET", "/timers"))
        }
        "bori_delete" -> {
            if ((a["confirm"] as? JsonPrimitive)?.booleanOrNull != true) {
                throw BoriError("Not deleted. Ask the user to confirm deleting this item, then call again with confirm=true.")
            }
            val kind = a.s("kind") ?: throw NoSuchElementException("kind")
            request("DELETE", "/${enc(kind)}/${enc(a.id())}", query = mapOf("confirm" to "true", "date" to a.s("date")))
        }
        else -> throw BoriError("Unknown tool $name")
    }

    // ---- JSON-RPC over stdio ---------------------------------------------------------------------

    fun handle(msg: JsonObject): JsonObject? {
        val method = msg.s("method")
        val id = msg["id"] ?: return null // a notification (initialized, cancelled, ...)
        val params = msg["params"] as? JsonObject ?: JsonObject(emptyMap())
        val result: JsonElement = when (method) {
            "initialize" -> buildJsonObject {
                put("protocolVersion", params.s("protocolVersion") ?: PROTOCOL_VERSION)
                putJsonObject("capabilities") { putJsonObject("tools") {} }
                putJsonObject("serverInfo") { put("name", "bori"); put("version", VERSION) }
                put("instructions", INSTRUCTIONS)
            }
            "ping" -> JsonObject(emptyMap())
            "tools/list" -> buildJsonObject { put("tools", JsonArray(TOOLS)) }
            "tools/call" -> {
                val args = params["arguments"] as? JsonObject ?: JsonObject(emptyMap())
                val name = params.s("name")
                val (text, isError) = try {
                    resultText(name, args, callTool(name, args)) to false
                } catch (e: BoriError) {
                    e.message.orEmpty() to true
                } catch (e: NoSuchElementException) {
                    "Missing argument '${e.message}'" to true
                } catch (e: Exception) { // never let one bad call end the server and drop every tool
                    "Bori tool failed: ${e.message ?: e.javaClass.simpleName}" to true
                }
                buildJsonObject {
                    put("content", buildJsonArray { add(buildJsonObject { put("type", "text"); put("text", text) }) })
                    put("isError", isError)
                }
            }
            else -> return buildJsonObject {
                put("jsonrpc", "2.0"); put("id", id)
                putJsonObject("error") { put("code", -32601); put("message", "Method not found: $method") }
            }
        }
        return buildJsonObject { put("jsonrpc", "2.0"); put("id", id); put("result", result) }
    }

    /** The argument names [args] passes that tool [name]'s schema doesn't list (they are dropped). */
    /**
     * What the model reads back from a successful call. Small local models follow the tool result more reliably
     * than the instructions, so the reply format and any follow-up (Bori's `ask_user` / `tell_user`) are spelled
     * out here, and those two fields are taken out of the JSON so they aren't repeated word for word.
     */
    internal fun resultText(name: String?, args: JsonObject, result: JsonElement): String {
        val obj = result as? JsonObject
        val ask = (obj?.get("ask_user") as? JsonPrimitive)?.contentOrNull
        val tell = (obj?.get("tell_user") as? JsonPrimitive)?.contentOrNull
        val shown = if (obj != null && (ask != null || tell != null)) JsonObject(obj - "ask_user" - "tell_user") else result
        val out = pretty.encodeToString(JsonElement.serializer(), shown)
        // Arguments this tool doesn't take were dropped. Say so, so the model can't tell the user
        // something happened that didn't (11-09: a to-do's "remind 30 min before" vanished and
        // Coqui confirmed a 15:30 alert anyway).
        val ignored = ignoredArguments(name, args)
        val note = if (ignored.isEmpty()) "" else "\nNOT done: Bori ignored ${ignored.joinToString()} " +
            "(this tool doesn't support it). Tell the user that part wasn't set."
        val follow = (tell?.let { "\nThen tell the user: $it" } ?: "") +
            (ask?.let { "\nThen ask the user, in one short line: $it" } ?: "")
        val readOnly = name in READ_ONLY || (name == "bori_ai_preferences" && args.isEmpty())
        return if (readOnly) out + note + follow
        else "Saved. Start your reply with \"✅ Updated in Bori Habit Tracker:\" and say what changed.$note$follow\n$out"
    }

    internal fun ignoredArguments(name: String?, args: JsonObject): List<String> {
        val tool = TOOLS.firstOrNull { it["name"]?.jsonPrimitive?.contentOrNull == name } ?: return emptyList()
        val known = ((tool["inputSchema"] as? JsonObject)?.get("properties") as? JsonObject)?.keys ?: return emptyList()
        return args.filter { (k, v) -> k !in known && v !is JsonNull }.keys.sorted()
    }

    /** [handle], but an unexpected failure becomes a JSON-RPC error instead of ending the process. */
    private fun safeHandle(msg: JsonObject): JsonObject? = try {
        handle(msg)
    } catch (e: Exception) {
        msg["id"]?.let { id ->
            buildJsonObject {
                put("jsonrpc", "2.0"); put("id", id)
                putJsonObject("error") { put("code", -32603); put("message", "Internal error: ${e.message ?: e.javaClass.simpleName}") }
            }
        }
    }

    /** Reads JSON-RPC lines until stdin closes. Only protocol messages go to stdout. */
    fun run() {
        val out = PrintStream(java.io.FileOutputStream(java.io.FileDescriptor.out), true, Charsets.UTF_8)
        System.setOut(System.err) // anything else that prints must not corrupt the protocol stream
        val reader = System.`in`.bufferedReader(Charsets.UTF_8)
        while (true) {
            val line = reader.readLine() ?: break
            if (line.isBlank()) continue
            val msg = runCatching { json.parseToJsonElement(line) }.getOrNull() ?: continue
            val replies = when (msg) {
                is JsonArray -> msg.mapNotNull { (it as? JsonObject)?.let(::safeHandle) }
                is JsonObject -> listOfNotNull(safeHandle(msg))
                else -> emptyList()
            }
            replies.forEach { out.println(it.toString()) }
        }
    }
}
