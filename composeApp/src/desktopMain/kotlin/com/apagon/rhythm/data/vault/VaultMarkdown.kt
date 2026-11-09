package com.apagon.rhythm.data.vault

import com.apagon.rhythm.ui.notes.BlockType
import com.apagon.rhythm.ui.notes.NoteBlock

/**
 * The desktop Markdown vault's file format: plain Obsidian Markdown, read and written so that a file
 * Bori didn't change stays **byte for byte** what Obsidian (or Coqui) wrote.
 *
 * Android's `NoteMarkdown.kt` re-renders the whole note from blocks, which is fine for a vault only
 * Bori writes but would flatten a shared Obsidian vault (every heading becomes `##`, nested lists lose
 * their indent, tables become text, unknown frontmatter disappears). So this is line based instead:
 *
 * - [parse] splits the body into [Segment]s, each remembering its exact source lines. Constructs the
 *   block model can't express (tables, HTML, math, comments, setext headings) become a CODE block
 *   marked [Segment.raw], shown in Bori as-is.
 * - [write] lines the edited blocks up against the file's current segments (longest common
 *   subsequence). Unchanged blocks are copied from the file verbatim; a changed block keeps the shape
 *   of the segment it replaced (heading level, list indent and marker, fence, raw-ness); only new
 *   blocks get default Markdown.
 *
 * The note title is the file name, as in Obsidian, so the body is never given a `# Title` line.
 * Tags live in the YAML frontmatter `tags` key; every other frontmatter line is kept untouched.
 */
object VaultMarkdown {

    /** One block of the file. [lines] are its exact source lines; [gapBefore] blank lines precede it. */
    data class Segment(
        val type: BlockType,
        val content: String,
        val checked: Boolean = false,
        val lines: List<String>,
        val gapBefore: Int,
        /** First line's text before [content] (`"### "`, `"  - [ ] "`, an opening fence). */
        val prefix: String = "",
        /** Closing fence line for fenced code. */
        val suffix: String? = null,
        /** True when the block model can't express it: written back exactly as [content]. */
        val raw: Boolean = false,
        /** The exact blank lines before it (they may hold spaces); [gapBefore] counts them. */
        val gapLines: List<String> = emptyList()
    ) {
        val key get() = Key(type, content, if (type == BlockType.CHECKLIST) checked else false)
    }

    data class Key(val type: BlockType, val content: String, val checked: Boolean)

    data class Parsed(
        /** Lines between the `---` fences, or null when the file has none. */
        val frontmatter: List<String>?,
        val tags: List<String>,
        val segments: List<Segment>,
        /** Blank lines after the last segment. */
        val trailingGap: List<String>,
        val endsWithNewline: Boolean
    ) {
        fun toBlocks(): List<NoteBlock> =
            segments.map { NoteBlock(type = it.type, content = it.content, isChecked = it.checked) }
                .ifEmpty { listOf(NoteBlock()) }
    }

    // ── Reading ─────────────────────────────────────────────────────────────────────────────────

    private val HEADING = Regex("^ {0,3}(#{1,6})(?:[ \\t]+(.*?))?(?:[ \\t]+#+)?[ \\t]*$")
    private val FENCE_OPEN = Regex("^( {0,3})(`{3,}|~{3,})(.*)$")
    private val HR = Regex("^ {0,3}(?:(?:-[ \\t]*){3,}|(?:\\*[ \\t]*){3,}|(?:_[ \\t]*){3,})$")
    private val LIST_ITEM = Regex("^([ \\t]*)([-*+]|\\d{1,9}[.)])([ \\t]+)(?:\\[([ xX])\\][ \\t]+)?(.*)$")
    private val EMPTY_LIST_ITEM = Regex("^([ \\t]*)([-*+]|\\d{1,9}[.)])[ \\t]*$")
    private val QUOTE = Regex("^ {0,3}>")
    private val TABLE_DELIM = Regex("^[ \\t]*\\|?[ \\t]*:?-+:?[ \\t]*(\\|[ \\t]*:?-+:?[ \\t]*)*\\|?[ \\t]*$")
    private val HTML_START = Regex("^ {0,3}<(?:[a-zA-Z][a-zA-Z0-9-]*|/[a-zA-Z]|!--|!\\[CDATA|\\?)")
    private val SETEXT = Regex("^ {0,3}(?:=+|-+)[ \\t]*$")

    fun parse(text: String): Parsed {
        val normalized = text.replace("\r\n", "\n")
        val endsWithNewline = normalized.endsWith("\n")
        var lines = normalized.split("\n")
        if (endsWithNewline) lines = lines.dropLast(1)

        var frontmatter: List<String>? = null
        var start = 0
        if (lines.isNotEmpty() && lines[0].trimEnd() == "---") {
            val close = (1 until lines.size).firstOrNull { lines[it].trimEnd() == "---" || lines[it].trimEnd() == "..." }
            if (close != null) {
                frontmatter = lines.subList(1, close).toList()
                start = close + 1
            }
        }
        val body = lines.subList(start, lines.size)
        val (segments, trailing) = parseBody(body)
        return Parsed(frontmatter, frontmatter?.let { parseTags(it) } ?: emptyList(), segments, trailing, endsWithNewline)
    }

    private fun isBlank(s: String) = s.isBlank()

    /** Does [line] start a block that ends a paragraph? */
    private fun interrupts(line: String): Boolean =
        HEADING.matches(line) || FENCE_OPEN.matches(line) || QUOTE.containsMatchIn(line) ||
            LIST_ITEM.matches(line) || HTML_START.containsMatchIn(line) || line.trimStart().startsWith("$$") ||
            (HR.matches(line) && !line.trim().startsWith("-"))

    private fun parseBody(lines: List<String>): Pair<List<Segment>, List<String>> {
        val out = mutableListOf<Segment>()
        var i = 0
        var gap = 0
        val gapLines = mutableListOf<String>()
        fun add(seg: Segment) { out.add(seg.copy(gapLines = gapLines.toList())); gap = 0; gapLines.clear() }

        while (i < lines.size) {
            val line = lines[i]
            if (isBlank(line)) { gap++; gapLines.add(line); i++; continue }

            // Fenced code (and Bori's handwriting fence). A ``` line whose info has a backtick is inline code.
            val fm = FENCE_OPEN.matchEntire(line)
            if (fm != null && !(fm.groupValues[2][0] == '`' && fm.groupValues[3].contains('`'))) {
                val fence = fm.groupValues[2]
                val info = fm.groupValues[3].trim()
                val closeRe = Regex("^ {0,3}${Regex.escape(fence.substring(0, 1))}{${fence.length},}[ \\t]*$")
                var j = i + 1
                while (j < lines.size && !closeRe.matches(lines[j])) j++
                val closed = j < lines.size
                val inner = lines.subList(i + 1, j)
                val segLines = lines.subList(i, if (closed) j + 1 else j).toList()
                val type = if (info == INK_FENCE) BlockType.INK else BlockType.CODE
                add(
                    if (closed) Segment(type, inner.joinToString("\n"), lines = segLines, gapBefore = gap, prefix = line, suffix = lines[j])
                    else rawSegment(segLines, gap)
                )
                i = if (closed) j + 1 else j
                continue
            }

            // `$$` math and `%%` comment blocks: kept exactly.
            val trimmed = line.trimStart()
            if (trimmed.startsWith("$$") || trimmed.startsWith("%%")) {
                val marker = trimmed.substring(0, 2)
                var j = i
                val singleLine = trimmed.length > 2 && trimmed.trimEnd().endsWith(marker) && trimmed.trimEnd().length >= 4
                if (!singleLine) {
                    j = i + 1
                    while (j < lines.size && !lines[j].contains(marker)) j++
                    if (j >= lines.size) j = lines.size - 1
                }
                val segLines = lines.subList(i, j + 1).toList()
                add(rawSegment(segLines, gap)); i = j + 1; continue
            }

            val hm = HEADING.matchEntire(line)
            if (hm != null) {
                val content = hm.groupValues[2]
                val prefix = line.substring(0, line.indexOf('#') + hm.groupValues[1].length) + " "
                // A plain heading only when it re-renders to the same line (no closing #s, odd spacing).
                add(
                    if (prefix + content == line) Segment(BlockType.HEADER, content, lines = listOf(line), gapBefore = gap, prefix = prefix)
                    else rawSegment(listOf(line), gap)
                )
                i++
                continue
            }

            if (HR.matches(line)) {
                add(Segment(BlockType.DIVIDER, "", lines = listOf(line), gapBefore = gap, prefix = line)); i++; continue
            }

            // Tables: a header row followed by a delimiter row, then every non-blank row with a pipe.
            if (line.contains('|') && i + 1 < lines.size && TABLE_DELIM.matches(lines[i + 1]) && lines[i + 1].contains('-')) {
                var j = i + 2
                while (j < lines.size && !isBlank(lines[j]) && lines[j].contains('|')) j++
                add(rawSegment(lines.subList(i, j).toList(), gap)); i = j; continue
            }

            if (HTML_START.containsMatchIn(line)) {
                var j = i + 1
                while (j < lines.size && !isBlank(lines[j])) j++
                add(rawSegment(lines.subList(i, j).toList(), gap)); i = j; continue
            }

            if (QUOTE.containsMatchIn(line)) {
                var j = i
                while (j < lines.size && QUOTE.containsMatchIn(lines[j])) j++
                val segLines = lines.subList(i, j).toList()
                val content = segLines.joinToString("\n") { it.trimStart().removePrefix(">").removePrefix(" ") }
                add(Segment(BlockType.QUOTE, content, lines = segLines, gapBefore = gap, prefix = "> "))
                i = j; continue
            }

            val item = LIST_ITEM.matchEntire(line)
            if (item != null || EMPTY_LIST_ITEM.matches(line)) {
                val indent: String
                val marker: String
                val spacing: String
                val box: String?
                val text: String
                if (item != null) {
                    indent = item.groupValues[1]; marker = item.groupValues[2]; spacing = item.groupValues[3]
                    box = item.groups[4]?.value; text = item.groupValues[5]
                } else {
                    val m = EMPTY_LIST_ITEM.matchEntire(line)!!
                    indent = m.groupValues[1]; marker = m.groupValues[2]; spacing = " "; box = null; text = ""
                }
                val prefix = line.substring(0, line.length - text.length)
                // Continuation lines: indented past the marker, not themselves another block.
                val contIndent = indent.length + marker.length + spacing.length
                var j = i + 1
                val cont = mutableListOf<String>()
                while (j < lines.size && !isBlank(lines[j]) && !LIST_ITEM.matches(lines[j]) &&
                    !EMPTY_LIST_ITEM.matches(lines[j]) && !FENCE_OPEN.matches(lines[j].trimStart()) &&
                    !HEADING.matches(lines[j].trimStart()) && !QUOTE.containsMatchIn(lines[j].trimStart())
                ) {
                    cont.add(lines[j]); j++
                }
                val contStripped = cont.map { if (it.startsWith(" ".repeat(contIndent))) it.substring(contIndent) else it.trimStart() }
                val content = (listOf(text) + contStripped).joinToString("\n")
                val type = when {
                    box != null -> BlockType.CHECKLIST
                    marker[0].isDigit() -> BlockType.NUMBERED_LIST
                    else -> BlockType.BULLET_LIST
                }
                add(Segment(type, content, checked = box != null && box != " ", lines = lines.subList(i, j).toList(),
                    gapBefore = gap, prefix = prefix))
                i = j; continue
            }

            // Paragraph.
            var j = i + 1
            while (j < lines.size && !isBlank(lines[j]) && !interrupts(lines[j]) &&
                !(lines[j].contains('|') && j + 1 < lines.size && TABLE_DELIM.matches(lines[j + 1]))
            ) {
                if (SETEXT.matches(lines[j])) { j++; break }
                j++
            }
            val segLines = lines.subList(i, j).toList()
            add(if (segLines.size > 1 && SETEXT.matches(segLines.last())) rawSegment(segLines, gap)
                else Segment(BlockType.TEXT, segLines.joinToString("\n"), lines = segLines, gapBefore = gap))
            i = j
        }
        return out to gapLines.toList()
    }

    private fun rawSegment(lines: List<String>, gap: Int) =
        Segment(BlockType.CODE, lines.joinToString("\n"), lines = lines, gapBefore = gap, raw = true)

    // ── Frontmatter tags ────────────────────────────────────────────────────────────────────────

    private val TAGS_KEY = Regex("^(tags|tag)[ \\t]*:(.*)$", RegexOption.IGNORE_CASE)

    fun parseTags(frontmatter: List<String>): List<String> {
        val idx = frontmatter.indexOfFirst { TAGS_KEY.matches(it) }
        if (idx < 0) return emptyList()
        val inline = TAGS_KEY.matchEntire(frontmatter[idx])!!.groupValues[2].trim()
        val values = if (inline.isNotEmpty()) {
            inline.removePrefix("[").removeSuffix("]").split(",")
        } else {
            frontmatter.drop(idx + 1).takeWhile { it.trimStart().startsWith("-") || it.isBlank() }
                .filter { it.isNotBlank() }.map { it.trimStart().removePrefix("-") }
        }
        return values.map { it.trim().trim('"', '\'').removePrefix("#") }.filter { it.isNotEmpty() }
    }

    /** [frontmatter] with its `tags` property replaced by [tags] in Obsidian's list form; null if nothing is left. */
    fun withTags(frontmatter: List<String>?, tags: List<String>): List<String>? {
        val lines = frontmatter?.toMutableList() ?: mutableListOf()
        val idx = lines.indexOfFirst { TAGS_KEY.matches(it) }
        if (idx >= 0) {
            var end = idx + 1
            if (TAGS_KEY.matchEntire(lines[idx])!!.groupValues[2].isBlank()) {
                while (end < lines.size && (lines[end].trimStart().startsWith("-") || lines[end].isBlank())) end++
            }
            repeat(end - idx) { lines.removeAt(idx) }
        }
        if (tags.isNotEmpty()) {
            val block = listOf("tags:") + tags.map { "  - $it" }
            lines.addAll(if (idx >= 0) idx else lines.size, block)
        }
        return if (lines.isEmpty() && frontmatter == null) null else lines
    }

    // ── Writing ─────────────────────────────────────────────────────────────────────────────────

    /**
     * The file text for [blocks] and [tags], keeping everything of [previous] (the file as it is on
     * disk now, or null for a new file) that the edit didn't touch.
     */
    fun write(previous: String?, tags: List<String>, blocks: List<NoteBlock>): String {
        val old = previous?.let { parse(it) }
        val oldSegs = old?.segments ?: emptyList()
        val newBlocks = blocks.filter { !(it.type == BlockType.TEXT && it.content.isBlank()) }
        val newKeys = newBlocks.map { Key(it.type, it.content, if (it.type == BlockType.CHECKLIST) it.isChecked else false) }

        // Longest common subsequence of keys.
        val n = oldSegs.size
        val m = newKeys.size
        val lcs = Array(n + 1) { IntArray(m + 1) }
        for (a in n - 1 downTo 0) for (b in m - 1 downTo 0) {
            lcs[a][b] = if (oldSegs[a].key == newKeys[b]) lcs[a + 1][b + 1] + 1 else maxOf(lcs[a + 1][b], lcs[a][b + 1])
        }

        val outLines = mutableListOf<String>()
        var prev: Segment? = null // what was last written, as a shape reference
        var a = 0
        var b = 0
        val pendingOld = ArrayDeque<Segment>() // deleted old segments, reused as templates for inserts

        fun emit(seg: Segment, gap: Int) {
            if (outLines.isNotEmpty()) {
                val count = maxOf(gap, minGap(prev, seg))
                if (count == seg.gapLines.size) outLines.addAll(seg.gapLines) else repeat(count) { outLines.add("") }
            }
            outLines.addAll(seg.lines)
            prev = seg
        }

        while (a < n || b < m) {
            if (a < n && b < m && oldSegs[a].key == newKeys[b]) {
                pendingOld.clear()
                emit(oldSegs[a], oldSegs[a].gapBefore); a++; b++
            } else if (a < n && (b >= m || lcs[a + 1][b] >= lcs[a][b + 1])) {
                pendingOld.addLast(oldSegs[a]); a++
            } else {
                val block = newBlocks[b]
                val template = pendingOld.firstOrNull { compatible(it, block.type) }?.also { pendingOld.remove(it) }
                val seg = render(block, template, prev)
                val gap = template?.gapBefore ?: defaultGap(prev, seg)
                emit(seg, gap); b++
            }
        }

        val sb = StringBuilder()
        val fm = if (old != null && tags == old.tags) old.frontmatter else withTags(old?.frontmatter, tags)
        if (fm != null) {
            sb.append("---\n")
            fm.forEach { sb.append(it).append('\n') }
            sb.append("---\n")
        }
        sb.append(outLines.joinToString("\n"))
        if (outLines.isNotEmpty()) {
            old?.trailingGap?.forEach { sb.append('\n').append(it) }
            if (old?.endsWithNewline != false) sb.append('\n')
        }
        return sb.toString()
    }

    private fun isList(t: BlockType) = t == BlockType.BULLET_LIST || t == BlockType.NUMBERED_LIST || t == BlockType.CHECKLIST

    private fun compatible(old: Segment, newType: BlockType) =
        old.type == newType || (isList(old.type) && isList(newType))

    /** Paragraph then `---` would become a heading, and two paragraphs with no blank line merge. */
    private fun minGap(prev: Segment?, seg: Segment): Int =
        if (prev?.type == BlockType.TEXT && (seg.type == BlockType.DIVIDER || seg.type == BlockType.TEXT)) 1 else 0

    private fun defaultGap(prev: Segment?, seg: Segment): Int = when {
        prev == null -> 0
        isList(prev.type) && isList(seg.type) -> 0
        else -> 1
    }

    private val LIST_PREFIX = Regex("^([ \\t]*)([-*+]|\\d{1,9}[.)])([ \\t]+)(\\[[ xX]\\][ \\t]+)?$")

    private fun render(block: NoteBlock, tpl: Segment?, prev: Segment?): Segment {
        val content = block.content
        val lines: List<String>
        var prefix = ""
        var suffix: String? = null
        when (block.type) {
            BlockType.HEADER -> {
                prefix = if (tpl?.type == BlockType.HEADER) tpl.prefix else "## "
                lines = listOf(prefix + content.replace('\n', ' '))
            }
            BlockType.TEXT -> lines = content.split("\n")
            BlockType.BULLET_LIST, BlockType.NUMBERED_LIST, BlockType.CHECKLIST -> {
                // Indent and marker from the template, or from the list item just written.
                val ref = tpl ?: prev?.takeIf { isList(it.type) }
                val pm = ref?.let { LIST_PREFIX.matchEntire(it.prefix) }
                val indent = pm?.groupValues?.get(1) ?: ""
                val refMarker = pm?.groupValues?.get(2)
                val marker = when (block.type) {
                    BlockType.NUMBERED_LIST -> when {
                        tpl != null && refMarker != null && refMarker[0].isDigit() -> refMarker
                        refMarker != null && refMarker[0].isDigit() ->
                            "${refMarker.dropLast(1).toInt() + 1}${refMarker.last()}"
                        else -> "1."
                    }
                    else -> if (refMarker != null && !refMarker[0].isDigit()) refMarker else "-"
                }
                val box = if (block.type == BlockType.CHECKLIST) (if (block.isChecked) "[x] " else "[ ] ") else ""
                prefix = "$indent$marker $box"
                val contPad = " ".repeat(indent.length + marker.length + 1)
                val parts = content.split("\n")
                lines = listOf(prefix + parts.first()) + parts.drop(1).map { if (it.isEmpty()) it else contPad + it }
            }
            BlockType.QUOTE -> {
                prefix = "> "
                lines = content.split("\n").map { if (it.isEmpty()) ">" else "> $it" }
            }
            BlockType.CODE -> {
                if (tpl?.raw == true) {
                    lines = content.split("\n")
                    return Segment(BlockType.CODE, content, lines = lines, gapBefore = 0, raw = true)
                }
                prefix = if (tpl?.type == BlockType.CODE && tpl.suffix != null) tpl.prefix else "```"
                suffix = if (tpl?.type == BlockType.CODE && tpl.suffix != null) tpl.suffix else "```"
                lines = listOf(prefix) + (if (content.isEmpty()) emptyList() else content.split("\n")) + suffix
            }
            BlockType.INK -> {
                prefix = "```$INK_FENCE"; suffix = "```"
                lines = listOf(prefix) + content.split("\n") + suffix
            }
            BlockType.DIVIDER -> {
                prefix = if (tpl?.type == BlockType.DIVIDER) tpl.prefix else "---"
                lines = listOf(prefix)
            }
            BlockType.IMAGE -> lines = listOf("![](${content.replace(" ", "%20")})")
        }
        return Segment(block.type, content, block.isChecked, lines, 0, prefix, suffix)
    }

    const val INK_FENCE = "bori-ink"
}
