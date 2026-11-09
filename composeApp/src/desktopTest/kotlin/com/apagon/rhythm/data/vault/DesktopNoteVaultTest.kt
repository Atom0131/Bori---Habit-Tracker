package com.apagon.rhythm.data.vault

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.apagon.rhythm.core.json.JSONArray
import com.apagon.rhythm.data.db.DesktopHabitDatabase
import com.apagon.rhythm.data.db.NotesDao
import com.apagon.rhythm.data.model.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopNoteVaultTest {

    companion object {
        // AppHome reads this once; every test shares one throwaway home (the index is reset per test).
        val home: File by lazy {
            if (System.getProperty("bori.home") == null) System.setProperty("bori.home", Files.createTempDirectory("bori-home").toFile().path)
            com.apagon.rhythm.platform.AppHome.dir
        }
    }

    private lateinit var db: DesktopHabitDatabase
    private lateinit var dao: NotesDao
    private lateinit var vaultDir: File
    private lateinit var vault: DesktopNoteVault

    @BeforeTest
    fun setUp() {
        File(home, "markdown_vault.json").delete()
        db = Room.inMemoryDatabaseBuilder<DesktopHabitDatabase>()
            .setDriver(BundledSQLiteDriver()).setQueryCoroutineContext(Dispatchers.IO).build()
        dao = db.notesDao()
        vaultDir = Files.createTempDirectory("vault").toFile()
        File(vaultDir, "Plan.md").writeText("---\ntags: [work]\nstatus: draft\n---\n# Plan\n\n- [ ] one\n  - nested\n\n| a | b |\n|---|---|\n| 1 | 2 |\n")
        File(vaultDir, "Sub").mkdirs()
        File(vaultDir, "Sub/Inner.md").writeText("Just text.\n")
        File(vaultDir, ".obsidian").mkdirs()
        File(vaultDir, ".obsidian/app.json").writeText("{}")
        System.getProperty("vaultFixtures")?.let { src -> File(src).copyRecursively(File(vaultDir, "Real")) }
        vault = DesktopNoteVault(dao)
    }

    @AfterTest
    fun tearDown() { db.close(); vaultDir.deleteRecursively() }

    private fun snapshot() = vaultDir.walkTopDown().filter { it.isFile && "/.trash/" !in it.path }
        .associate { it.relativeTo(vaultDir).path to it.readText() }

    private suspend fun note(title: String) = dao.getAllNotesForBackup().single { it.title == title }

    private fun blocks(n: Note): List<String> {
        val a = JSONArray(n.content)
        return (0 until a.length()).map { a.getJSONObject(it).getString("content") }
    }

    private suspend fun edit(n: Note, change: (String) -> String) {
        dao.updateNote(n.copy(content = change(n.content), updatedAt = n.updatedAt + 10_000))
    }

    @Test
    fun firstSyncImportsWithoutTouchingFiles() = runBlocking {
        val before = snapshot()
        val st = vault.syncOnceForTest(vaultDir)
        assertEquals(before, snapshot(), "importing must not change any file")
        val mdCount = before.keys.count { it.endsWith(".md") && !it.startsWith(".") && "/." !in it }
        assertEquals(mdCount, st.noteCount)
        val books = dao.getAllNotebooksForBackup()
        val root = books.single { it.name == vaultDir.name }
        assertEquals(root.id, books.single { it.name == "Sub" }.parentId)
        val plan = note("Plan")
        assertEquals("work", plan.tags)
        assertEquals(listOf("Plan", "one", "nested", "| a | b |\n|---|---|\n| 1 | 2 |"), blocks(plan))
        // A second pass changes nothing.
        val notesBefore = dao.getAllNotesForBackup()
        vault.syncOnceForTest(null)
        assertEquals(before, snapshot())
        assertEquals(notesBefore, dao.getAllNotesForBackup())
    }

    @Test
    fun editsFlowBothWays() = runBlocking {
        vault.syncOnceForTest(vaultDir)
        // Bori → file: only the edited line changes.
        edit(note("Plan")) { it.replace("\"nested\"", "\"nested edited\"") }
        vault.syncOnceForTest(null)
        assertEquals(
            "---\ntags: [work]\nstatus: draft\n---\n# Plan\n\n- [ ] one\n  - nested edited\n\n| a | b |\n|---|---|\n| 1 | 2 |\n",
            File(vaultDir, "Plan.md").readText()
        )
        // File → Bori.
        File(vaultDir, "Sub/Inner.md").writeText("Changed by Coqui.\n\n- [x] done\n")
        vault.syncOnceForTest(null)
        assertEquals(listOf("Changed by Coqui.", "done"), blocks(note("Inner")))
        // Rename in Bori → file renamed; tags → frontmatter.
        val inner = note("Inner")
        dao.updateNote(inner.copy(title = "Renamed", tags = "a,b", updatedAt = inner.updatedAt + 10_000))
        vault.syncOnceForTest(null)
        assertTrue(!File(vaultDir, "Sub/Inner.md").exists())
        assertEquals("---\ntags:\n  - a\n  - b\n---\nChanged by Coqui.\n\n- [x] done\n", File(vaultDir, "Sub/Renamed.md").readText())
    }

    @Test
    fun createsMovesAndDeletes() = runBlocking {
        vault.syncOnceForTest(vaultDir)
        val sub = dao.getAllNotebooksForBackup().single { it.name == "Sub" }
        // New note in Bori → new file.
        dao.insertNote(Note(notebookId = sub.id, title = "From Bori", content = """[{"id":"1","type":"CHECKLIST","content":"milk","isChecked":false}]"""))
        vault.syncOnceForTest(null)
        assertEquals("- [ ] milk\n", File(vaultDir, "Sub/From Bori.md").readText())
        // Blank new notes (the editor inserts one the moment it opens) get no file.
        dao.insertNote(Note(notebookId = sub.id, title = "", content = """[{"id":"2","type":"TEXT","content":"","isChecked":false}]"""))
        vault.syncOnceForTest(null)
        assertTrue(File(vaultDir, "Sub").list()!!.none { it.startsWith("Untitled") })
        // Deleted in Bori → vault .trash.
        val fb = note("From Bori")
        dao.updateNote(fb.copy(deletedAt = fb.updatedAt + 1, updatedAt = fb.updatedAt + 10_000))
        vault.syncOnceForTest(null)
        assertTrue(!File(vaultDir, "Sub/From Bori.md").exists() && File(vaultDir, ".trash/From Bori.md").exists())
        // Moved outside Bori → same note, new notebook and title.
        val innerSync = note("Inner").syncId
        Files.move(File(vaultDir, "Sub/Inner.md").toPath(), File(vaultDir, "Moved.md").toPath())
        vault.syncOnceForTest(null)
        val moved = dao.getNoteBySyncId(innerSync)!!
        assertEquals("Moved", moved.title)
        assertNull(moved.deletedAt)
        // Deleted outside Bori → Recently Deleted.
        File(vaultDir, "Moved.md").delete()
        vault.syncOnceForTest(null)
        assertNotNull(dao.getNoteBySyncId(innerSync)!!.deletedAt)
        // New file → new note.
        File(vaultDir, "Sub/Fresh.md").writeText("hi\n")
        vault.syncOnceForTest(null)
        assertEquals(listOf("hi"), blocks(note("Fresh")))
    }

    @Test
    fun conflictKeepsBothVersions() = runBlocking {
        vault.syncOnceForTest(vaultDir)
        val inner = note("Inner")
        edit(inner) { it.replace("Just text.", "Bori version") }
        File(vaultDir, "Sub/Inner.md").apply { writeText("File version\n"); setLastModified(inner.updatedAt + 60_000) }
        vault.syncOnceForTest(null)
        assertEquals(listOf("File version"), blocks(note("Inner")))
        val saved = File(home, "vault_conflicts").walkTopDown().filter { it.isFile && it.path.endsWith("Inner.md") }.toList()
        assertTrue(saved.any { it.readText().contains("Bori version") })
    }

    @Test
    fun emptyFolderLikeAnUnmountedDriveDeletesNothing() = runBlocking {
        vault.syncOnceForTest(vaultDir)
        val before = dao.getAllNotesForBackup()
        // The drive is gone and its mountpoint is an empty folder.
        vaultDir.listFiles()!!.forEach { it.deleteRecursively() }
        val failed = runCatching { vault.syncOnceForTest(null) }.exceptionOrNull()
        assertNotNull(failed, "an empty vault folder must stop the pass")
        assertEquals(before, dao.getAllNotesForBackup())
        assertTrue(dao.getAllNotebooksForBackup().all { it.deletedAt == null })
    }

    @Test
    fun lockingANotebookRemovesItsPlainTextCopies() = runBlocking {
        vault.syncOnceForTest(vaultDir)
        val sub = dao.getAllNotebooksForBackup().single { it.name == "Sub" }
        dao.updateNotebook(sub.copy(isPrivate = true, updatedAt = sub.updatedAt + 10_000))
        vault.syncOnceForTest(null)
        assertTrue(!File(vaultDir, "Sub/Inner.md").exists(), "a locked notebook's note must leave the vault")
        assertTrue(vaultDir.walkTopDown().none { it.name == "Inner.md" }, "not even in .trash")
        assertNull(note("Inner").deletedAt, "the note itself stays in Bori")
        assertTrue(File(vaultDir, "Plan.md").exists())
        // Unlocked again → back in the vault.
        val locked = dao.getAllNotebooksForBackup().single { it.name == "Sub" }
        dao.updateNotebook(locked.copy(isPrivate = false, updatedAt = locked.updatedAt + 10_000))
        vault.syncOnceForTest(null)
        assertTrue(File(vaultDir, "Sub/Inner.md").exists())
    }

    @Test
    fun parentAndChildRenamedInOnePass() = runBlocking {
        File(vaultDir, "Sub/Deep").mkdirs()
        File(vaultDir, "Sub/Deep/Leaf.md").writeText("leaf\n")
        vault.syncOnceForTest(vaultDir)
        val books = dao.getAllNotebooksForBackup()
        books.single { it.name == "Sub" }.let { dao.updateNotebook(it.copy(name = "Sub2", updatedAt = it.updatedAt + 10_000)) }
        books.single { it.name == "Deep" }.let { dao.updateNotebook(it.copy(name = "Deep2", updatedAt = it.updatedAt + 10_000)) }
        vault.syncOnceForTest(null)
        assertTrue(File(vaultDir, "Sub2/Deep2/Leaf.md").exists())
        assertTrue(dao.getAllNotesForBackup().all { it.deletedAt == null })
    }
}
