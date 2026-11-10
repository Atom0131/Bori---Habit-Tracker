package com.apagon.rhythm.data.agent

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.apagon.rhythm.data.db.DesktopHabitDatabase
import com.apagon.rhythm.data.model.Habit
import com.apagon.rhythm.data.model.Notebook
import com.apagon.rhythm.data.repository.AlarmRepository
import com.apagon.rhythm.data.repository.CalendarEventRepository
import com.apagon.rhythm.data.repository.HabitRepository
import com.apagon.rhythm.data.repository.NotesRepository
import com.apagon.rhythm.data.repository.ReminderRepository
import com.apagon.rhythm.data.repository.TimerRepository
import com.apagon.rhythm.data.repository.TodoRepository
import com.apagon.rhythm.data.vault.DesktopNoteVault
import com.apagon.rhythm.platform.DesktopWidgetRefresher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.time.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AgentApiServerTest {

    companion object {
        val home: File by lazy {
            if (System.getProperty("bori.home") == null) System.setProperty("bori.home", Files.createTempDirectory("bori-home").toFile().path)
            com.apagon.rhythm.platform.AppHome.dir
        }
    }

    private lateinit var db: DesktopHabitDatabase
    private lateinit var api: AgentApiServer
    private val http = HttpClient.newHttpClient()
    private lateinit var token: String
    private val port = java.net.ServerSocket(0).use { it.localPort }

    @BeforeTest
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder<DesktopHabitDatabase>()
            .setDriver(BundledSQLiteDriver()).setQueryCoroutineContext(Dispatchers.IO).build()
        val habits = HabitRepository(db.habitDao(), DesktopWidgetRefresher(), db.activeDayDao())
        api = AgentApiServer(
            CalendarEventRepository(db.calendarEventDao(), db.eventReminderDao()), db.eventReminderDao(),
            TodoRepository(db.todoDao(), db.todoSubtaskDao()), ReminderRepository(db.reminderDao()), habits,
            NotesRepository(db.notesDao(), db.noteLinkDao()), db.notesDao(), DesktopNoteVault(db.notesDao()),
            AlarmRepository(db.alarmDao()), TimerRepository(db.timerDao()), port
        )
        api.setEnabled(true)
        assertTrue(api.state.value.running, api.state.value.lastError)
        token = File(home, "agent_token").readText().trim()
        runBlocking {
            habits.addHabit(Habit(name = "Read"))
            db.notesDao().insertNotebook(Notebook(name = "Inbox"))
        }
    }

    @AfterTest
    fun tearDown() { api.setEnabled(false); db.close() }

    private fun call(method: String, path: String, body: String? = null, auth: Boolean = true): Pair<Int, String> {
        val req = HttpRequest.newBuilder(URI("http://127.0.0.1:$port$path"))
            .method(method, body?.let { HttpRequest.BodyPublishers.ofString(it) } ?: HttpRequest.BodyPublishers.noBody())
            .apply { if (auth) header("Authorization", "Bearer $token") }
            .header("Content-Type", "application/json").build()
        val res = http.send(req, HttpResponse.BodyHandlers.ofString())
        return res.statusCode() to res.body()
    }

    private fun obj(s: String) = Json.parseToJsonElement(s).jsonObject
    private fun JsonObject.s(k: String) = this[k]!!.jsonPrimitive.content

    @Test
    fun tokenIsRequiredAndPrivate() {
        assertEquals(401, call("GET", "/health", auth = false).first)
        assertEquals(200, call("GET", "/health").first)
        val perms = Files.getPosixFilePermissions(File(home, "agent_token").toPath()).map { it.name }.toSet()
        assertEquals(setOf("OWNER_READ", "OWNER_WRITE"), perms)
    }

    @Test
    fun createsAndListsAgenda() {
        val day = LocalDate.now().plusDays(3).toString()
        val (c1, ev) = call("POST", "/events", """{"title":"Dentist","date":"$day","start_time":"15:00","remind_minutes_before":60}""")
        assertEquals(200, c1, ev)
        val event = obj(ev)
        assertEquals("16:00", event.s("end_time"))
        assertEquals("60", event["remind_minutes_before"]!!.jsonArray.single().jsonPrimitive.content)

        val (c2, td) = call("POST", "/todos", """{"title":"Buy milk","due":"$day","priority":"high","subtasks":["2%","oat"]}""")
        assertEquals(200, c2, td)
        val (c3, rm) = call("POST", "/reminders", """{"title":"Call mom","at":"$day 18:30"}""")
        assertEquals(200, c3, rm)

        val agenda = obj(call("GET", "/agenda?from=${LocalDate.now()}&to=$day").second)
        assertEquals("Dentist", agenda["events"]!!.jsonArray.single().jsonObject.s("title"))
        assertEquals(2, agenda["todos_due"]!!.jsonArray.single().jsonObject["subtasks"]!!.jsonArray.size)
        assertEquals("Call mom", agenda["reminders"]!!.jsonArray.single().jsonObject.s("title"))
        assertEquals("false", agenda["habits_today"]!!.jsonArray.single().jsonObject.s("done"))

        // Edits and completion.
        val id = obj(td).s("id")
        assertEquals("true", obj(call("POST", "/todos/$id/complete").second).s("done"))
        val moved = obj(call("PATCH", "/events/${event.s("id")}", """{"start_time":"16:00"}""").second)
        assertEquals("16:00", moved.s("start_time"))
        val habitId = agenda["habits_today"]!!.jsonArray.single().jsonObject.s("id")
        assertEquals(200, call("POST", "/habits/$habitId/check").first)
        assertEquals("true", obj(call("GET", "/agenda").second)["habits_today"]!!.jsonArray.single().jsonObject.s("done"))
    }

    @Test
    fun deletesNeedConfirmAndBadInputIsExplained() {
        val ev = obj(call("POST", "/events", """{"title":"X","date":"${LocalDate.now()}"}""").second)
        assertEquals(400, call("DELETE", "/events/${ev.s("id")}").first)
        assertEquals(200, call("DELETE", "/events/${ev.s("id")}?confirm=true").first)
        assertTrue(obj(call("GET", "/agenda").second)["events"]!!.jsonArray.isEmpty())
        val (code, body) = call("POST", "/reminders", """{"title":"x","at":"tomorrow 5pm"}""")
        assertEquals(400, code)
        assertTrue("yyyy-MM-dd HH:mm" in body)
    }

    @Test
    fun notesFromMarkdown() {
        val (code, body) = call("POST", "/notes", """{"title":"Ideas","markdown":"## Big\n\n- [ ] ship it\n","tags":["coqui"]}""")
        assertEquals(200, code, body)
        assertEquals("Inbox", obj(body).s("notebook"))
        val found = Json.parseToJsonElement(call("GET", "/notes/search?q=Ideas").second).jsonArray
        assertNotNull(found.singleOrNull())
    }

    @Test
    fun readsAndEditsNotes() {
        val id = obj(call("POST", "/notes", """{"title":"Groceries","markdown":"# Shop\n\n- [ ] eggs\n"}""").second).s("id")
        val read = obj(call("GET", "/notes/$id").second)
        assertEquals("## Shop\n\n- [ ] eggs\n", read.s("markdown"))
        // Append keeps what was there.
        val (c1, b1) = call("PATCH", "/notes/$id", """{"append":"- [ ] milk\n"}""")
        assertEquals(200, c1, b1)
        assertEquals("## Shop\n\n- [ ] eggs\n- [ ] milk\n", obj(b1).s("markdown"))
        // Replace, rename, tags.
        val b2 = obj(call("PATCH", "/notes/$id", """{"markdown":"Done shopping.","title":"Groceries (old)","tags":["#home"]}""").second)
        assertEquals("Done shopping.\n", b2.s("markdown"))
        assertEquals("Groceries (old)", b2.s("title"))
        assertEquals("home", b2["tags"]!!.jsonArray.single().jsonPrimitive.content)
        assertEquals(400, call("PATCH", "/notes/$id", """{"markdown":"a","append":"b"}""").first)
        assertEquals(400, call("PATCH", "/notes/$id", "{}").first)
        assertEquals(404, call("GET", "/notes/nope").first)
    }

    @Test
    fun timersAndAlarms() {
        val t = obj(call("POST", "/timers", """{"minutes":10,"label":"Tea"}""").second)
        assertEquals("true", t.s("running"))
        assertEquals("600", t.s("duration_seconds"))
        assertTrue(t.s("remaining_seconds").toInt() in 595..600)
        val stopped = obj(call("POST", "/timers/${t.s("id")}/stop").second)
        assertEquals("false", stopped.s("running"))
        assertEquals(1, Json.parseToJsonElement(call("GET", "/timers").second).jsonArray.size)
        assertEquals(400, call("POST", "/timers", """{"minutes":0}""").first)

        val a = obj(call("POST", "/alarms", """{"time":"6:30","label":"Meds","days":"weekdays"}""").second)
        assertEquals("06:30", a.s("time"))
        assertEquals("weekdays", a.s("days"))
        val once = obj(call("POST", "/alarms", """{"time":"07:00"}""").second)
        assertEquals("once", once.s("days"))
        val edited = obj(call("PATCH", "/alarms/${a.s("id")}", """{"days":["mon","wed"],"enabled":false}""").second)
        assertEquals(listOf("mon", "wed"), edited["days"]!!.jsonArray.map { it.jsonPrimitive.content })
        assertEquals("false", edited.s("enabled"))
        assertEquals(400, call("POST", "/alarms", """{"time":"07:00","days":["funday"]}""").first)
        assertEquals(400, call("DELETE", "/alarms/${once.s("id")}").first)
        assertEquals(200, call("DELETE", "/alarms/${once.s("id")}?confirm=true").first)
        assertEquals(200, call("DELETE", "/timers/${t.s("id")}?confirm=true").first)
        assertEquals(1, Json.parseToJsonElement(call("GET", "/alarms").second).jsonArray.size)
        assertTrue(Json.parseToJsonElement(call("GET", "/timers").second).jsonArray.isEmpty())
    }

    @Test
    fun repeats() {
        val mon = LocalDate.now().plusDays(((8 - LocalDate.now().dayOfWeek.value) % 7 + 7).toLong()) // a Monday 1–2 weeks out
        val (c, body) = call("POST", "/events", """{"title":"Class","date":"$mon","start_time":"10:00","repeat":{"every":1,"unit":"week","days":["mon","wed"]}}""")
        assertEquals(200, c, body)
        val ev = obj(body)
        assertEquals("Weekly on Mon, Wed", ev.s("repeat"))
        val agenda = obj(call("GET", "/agenda?from=$mon&to=${mon.plusDays(9)}").second)["events"]!!.jsonArray
        assertEquals(listOf(mon, mon.plusDays(2), mon.plusDays(7), mon.plusDays(9)).map { it.toString() },
            agenda.map { it.jsonObject.s("date") })
        assertEquals(mon.plusDays(2).toString(), agenda[1].jsonObject.s("occurrence_date"))
        // Skip one date, then move another on its own.
        assertEquals(200, call("DELETE", "/events/${ev.s("id")}?confirm=true&date=${mon.plusDays(2)}").first)
        val moved = obj(call("PATCH", "/events/${ev.s("id")}", """{"only_date":"${mon.plusDays(7)}","start_time":"12:00"}""").second)
        assertEquals("12:00", moved.s("start_time"))
        assertEquals("13:00", moved.s("end_time")) // kept its one-hour length
        val after = obj(call("GET", "/agenda?from=$mon&to=${mon.plusDays(9)}").second)["events"]!!.jsonArray
        assertEquals(listOf("10:00", "12:00", "10:00"), after.map { it.jsonObject.s("start_time") })

        val todo = obj(call("POST", "/todos", """{"title":"Trash","repeat":"weekly"}""").second)
        assertEquals(LocalDate.now().toString(), todo.s("due"))
        val done = obj(call("POST", "/todos/${todo.s("id")}/complete").second)
        assertEquals("false", done.s("done"))
        assertEquals(LocalDate.now().plusDays(7).toString(), done.s("due"))
        val rem = obj(call("POST", "/reminders", """{"title":"Meds","at":"$mon 08:00","repeat":"daily"}""").second)
        assertEquals("Daily", rem.s("repeat"))
        assertEquals(400, call("POST", "/reminders", """{"title":"x","at":"$mon 08:00","repeat":"hourly"}""").first)
    }

    /** `bori --mcp` end to end: JSON-RPC in, through the agent API, into the database. */
    @Test
    fun mcpBridgeAddsTodoAndRefusesUnconfirmedDelete() {
        BoriMcpServer.url = "http://127.0.0.1:$port"
        fun rpc(id: Int, method: String, params: String = "{}") =
            BoriMcpServer.handle(obj("""{"jsonrpc":"2.0","id":$id,"method":"$method","params":$params}"""))!!

        val init = rpc(1, "initialize", """{"protocolVersion":"2025-06-18"}""")["result"]!!.jsonObject
        assertEquals("bori", init["serverInfo"]!!.jsonObject.s("name"))
        assertTrue(init.s("instructions").contains("explicit yes"))
        assertEquals(null, BoriMcpServer.handle(obj("""{"jsonrpc":"2.0","method":"notifications/initialized"}""")))

        val names = rpc(2, "tools/list")["result"]!!.jsonObject["tools"]!!.jsonArray.map { it.jsonObject.s("name") }
        assertEquals(20, names.size)
        assertTrue("bori_add_todo" in names && "bori_delete" in names)

        val add = rpc(3, "tools/call", """{"name":"bori_add_todo","arguments":{"title":"Buy coffee","priority":"high"}}""")["result"]!!.jsonObject
        assertEquals("false", add["isError"]!!.jsonPrimitive.content, add.toString())
        val addText = add["content"]!!.jsonArray[0].jsonObject.s("text")
        assertTrue(addText.startsWith("Saved. Start your reply with \"✅ Updated in Bori Habit Tracker:\""), addText)
        val todo = obj(addText.substringAfter("\n"))
        assertEquals("Buy coffee", todo.s("title"))

        val del = rpc(4, "tools/call", """{"name":"bori_delete","arguments":{"kind":"todos","id":"${todo.s("id")}","confirm":false}}""")["result"]!!.jsonObject
        assertEquals("true", del["isError"]!!.jsonPrimitive.content)
        val pending = runBlocking { db.todoDao().getPendingTodos().first() }
        assertTrue(pending.any { it.title == "Buy coffee" }, "unconfirmed delete must not delete")

        val missing = rpc(5, "tools/call", """{"name":"bori_read_note","arguments":{}}""")["result"]!!.jsonObject
        assertEquals("true", missing["isError"]!!.jsonPrimitive.content)
        assertEquals(-32601, rpc(6, "bogus")["error"]!!.jsonObject["code"]!!.jsonPrimitive.content.toInt())
    }

    /** 11-09: "due 16:00 with a reminder at 15:30" — the early alert is stored, not silently dropped. */
    @Test
    fun todoEarlyAlertIsKeptAndIgnoredArgumentsAreReported() {
        val day = LocalDate.now().plusDays(2).toString()
        val (c, td) = call("POST", "/todos", """{"title":"Trash out","due":"$day 16:00","remind_minutes_before":"30"}""")
        assertEquals(200, c, td)
        val todo = obj(td)
        assertEquals("30", todo.s("remind_minutes_before"))
        val stored = runBlocking { db.todoDao().getPendingTodos().first() }.single { it.title == "Trash out" }
        assertEquals(30, stored.earlyAlertMinutes)
        // No due time: nothing to be early for.
        assertEquals(400, call("POST", "/todos", """{"title":"x","due":"$day","remind_minutes_before":30}""").first)
        // Removing the time drops the early alert; setting 0 clears it.
        val moved = obj(call("PATCH", "/todos/${todo.s("id")}", """{"due":"$day"}""").second)
        assertEquals(null, moved["remind_minutes_before"])

        BoriMcpServer.url = "http://127.0.0.1:$port"
        val res = BoriMcpServer.handle(obj("""{"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"bori_add_reminder",
            "arguments":{"title":"Call","at":"$day 09:00","remind_minutes_before":10}}}"""))!!["result"]!!.jsonObject
        val text = res["content"]!!.jsonArray[0].jsonObject.s("text")
        assertTrue(text.contains("NOT done: Bori ignored remind_minutes_before"), text)
        assertEquals(emptyList(), BoriMcpServer.ignoredArguments("bori_add_todo", obj("""{"title":"a","remind_minutes_before":5}""")))
    }

    @Test
    fun connectSetupEscapesWindowsPaths() {
        val win = com.apagon.rhythm.ui.settings.aiAppSetups("C:\\Users\\Ana Lee\\AppData\\Local\\Bori\\Bori.exe", true)
        val desktop = win.first { it.name.startsWith("Claude Desktop") }.text
        val parsed = obj(desktop)["mcpServers"]!!.jsonObject["bori"]!!.jsonObject
        assertEquals("C:\\Users\\Ana Lee\\AppData\\Local\\Bori\\Bori.exe", parsed.s("command"))
        assertEquals("--mcp", parsed["args"]!!.jsonArray[0].jsonPrimitive.content)
        assertTrue(win.first { it.name == "Claude Code" }.text.endsWith("\"C:\\Users\\Ana Lee\\AppData\\Local\\Bori\\Bori.exe\" --mcp"))
        val linux = com.apagon.rhythm.ui.settings.aiAppSetups("/usr/bin/bori", false)
        assertEquals("mcp_servers:\n  bori:\n    command: \"/usr/bin/bori\"\n    args: [\"--mcp\"]", linux.first { it.name == "Hermes" }.text)
    }
}
