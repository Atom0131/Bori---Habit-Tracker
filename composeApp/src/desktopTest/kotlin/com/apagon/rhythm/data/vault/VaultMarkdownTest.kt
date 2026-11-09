package com.apagon.rhythm.data.vault

import com.apagon.rhythm.ui.notes.BlockType
import com.apagon.rhythm.ui.notes.NoteBlock
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VaultMarkdownTest {

    private val obsidianNote = """
        ---
        aliases: [Plan]
        tags:
          - project
          - bori
        cssclasses: wide
        ---
        # Launch plan

        Intro paragraph with **bold**, a [[Wikilink]], ==highlight== and #inline-tag.
        Second line of the same paragraph.

        ## Tasks
        - [ ] Write copy
        - [x] Pick date
          - nested bullet
            continuation line
        1. first
        2. second

        > [!note] Callout title
        > callout body

        | Col A | Col B |
        |-------|------:|
        | 1     | 2     |

        ```kotlin
        val x = 1
        ```

        ![[diagram.png]]

        ***

        Setext heading
        ==============

        %%
        hidden comment
        %%

        $$
        e = mc^2
        $$
        <div>html</div>

        ### Closing #
    """.trimIndent() + "\n"

    @Test
    fun unchangedFileIsByteIdentical() {
        val parsed = VaultMarkdown.parse(obsidianNote)
        assertEquals(obsidianNote, VaultMarkdown.write(obsidianNote, parsed.tags, parsed.toBlocks()))
    }

    @Test
    fun tagsComeFromFrontmatter() {
        assertEquals(listOf("project", "bori"), VaultMarkdown.parse(obsidianNote).tags)
        assertEquals(listOf("a", "b"), VaultMarkdown.parse("---\ntags: [a, \"#b\"]\n---\nx\n").tags)
    }

    @Test
    fun editingOneBlockKeepsTheRestAndItsShape() {
        val parsed = VaultMarkdown.parse(obsidianNote)
        val blocks = parsed.toBlocks().map {
            when {
                it.type == BlockType.HEADER && it.content == "Tasks" -> it.copy(content = "To do")
                it.type == BlockType.CHECKLIST && it.content == "Write copy" -> it.copy(isChecked = true)
                it.type == BlockType.BULLET_LIST -> it.copy(content = "nested bullet edited\ncontinuation line")
                else -> it
            }
        }
        val out = VaultMarkdown.write(obsidianNote, parsed.tags, blocks)
        val expected = obsidianNote
            .replace("## Tasks", "## To do")
            .replace("- [ ] Write copy", "- [x] Write copy")
            .replace("  - nested bullet\n", "  - nested bullet edited\n")
        assertEquals(expected, out)
    }

    @Test
    fun insertedBlocksGetObsidianMarkdown() {
        val text = "# Title\n\n- [ ] one\n\nEnd.\n"
        val parsed = VaultMarkdown.parse(text)
        val blocks = parsed.toBlocks().toMutableList()
        blocks.add(2, NoteBlock(type = BlockType.CHECKLIST, content = "two"))
        blocks.add(NoteBlock(type = BlockType.DIVIDER))
        blocks.add(NoteBlock(type = BlockType.HEADER, content = "New"))
        assertEquals("# Title\n\n- [ ] one\n- [ ] two\n\nEnd.\n\n---\n\n## New\n", VaultMarkdown.write(text, parsed.tags, blocks))
    }

    @Test
    fun deletingAndRetaggingTouchesOnlyThose() {
        val parsed = VaultMarkdown.parse(obsidianNote)
        val blocks = parsed.toBlocks().filterNot { it.type == BlockType.NUMBERED_LIST }
        val out = VaultMarkdown.write(obsidianNote, listOf("project"), blocks)
        assertTrue("1. first" !in out && "2. second" !in out)
        assertTrue(out.startsWith("---\naliases: [Plan]\ntags:\n  - project\ncssclasses: wide\n---\n# Launch plan\n"))
        assertTrue("| Col A | Col B |" in out && "%%\nhidden comment\n%%" in out)
    }

    @Test
    fun newFileWithoutTagsHasNoFrontmatter() {
        val out = VaultMarkdown.write(null, emptyList(), listOf(NoteBlock(content = "hello"), NoteBlock(type = BlockType.BULLET_LIST, content = "x")))
        assertEquals("hello\n\n- x\n", out)
        assertEquals("---\ntags:\n  - a\n---\nhi\n", VaultMarkdown.write(null, listOf("a"), listOf(NoteBlock(content = "hi"))))
    }

    @Test
    fun rawConstructsAreEditableVerbatim() {
        val text = "| a |\n|---|\n| 1 |\n"
        val parsed = VaultMarkdown.parse(text)
        assertEquals(BlockType.CODE, parsed.segments.single().type)
        val edited = parsed.toBlocks().map { it.copy(content = "| a |\n|---|\n| 2 |") }
        assertEquals("| a |\n|---|\n| 2 |\n", VaultMarkdown.write(text, emptyList(), edited))
    }

    /** -PvaultFixtures=/dir runs every .md under it through parse → write, read only. */
    @Test
    fun realVaultRoundTrips() {
        val dir = System.getProperty("vaultFixtures")?.let(::File) ?: return
        val files = dir.walkTopDown().onEnter { !it.name.startsWith(".") }.filter { it.isFile && it.extension == "md" }.toList()
        assertTrue(files.isNotEmpty())
        files.forEach { f ->
            val text = f.readText()
            val p = VaultMarkdown.parse(text)
            assertEquals(text.replace("\r\n", "\n"), VaultMarkdown.write(text, p.tags, p.toBlocks()), "round trip of ${f.name}")
        }
        println("vault round trip OK: ${files.size} files")
    }
}
