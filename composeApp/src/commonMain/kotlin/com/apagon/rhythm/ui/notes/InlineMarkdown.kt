package com.apagon.rhythm.ui.notes

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration

// Stage 10: mechanical move from androidMain to commonMain — zero Android
// dependencies to begin with (pure androidx.compose.ui.text logic).
private data class MarkdownSpan(
    val fullStart: Int,
    val fullEnd: Int,
    val textStart: Int,
    val textEnd: Int,
    val style: SpanStyle
)

fun parseInlineMarkdown(
    text: String,
    codeBackground: Color = Color.Unspecified,
    highlightColor: Color = Color.Unspecified
): AnnotatedString {
    val patterns = listOf(
        Regex("""\*\*(.+?)\*\*""") to SpanStyle(fontWeight = FontWeight.Bold),
        Regex("""~~(.+?)~~""") to SpanStyle(textDecoration = TextDecoration.LineThrough),
        Regex("""<u>(.+?)</u>""") to SpanStyle(textDecoration = TextDecoration.Underline),
        Regex("""==(.+?)==""") to SpanStyle(background = highlightColor),
        Regex("""`(.+?)`""") to SpanStyle(fontFamily = FontFamily.Monospace, background = codeBackground),
        Regex("""_(.+?)_""") to SpanStyle(fontStyle = FontStyle.Italic),
    )

    val spans = mutableListOf<MarkdownSpan>()
    val usedRanges = mutableListOf<IntRange>()

    for ((regex, style) in patterns) {
        for (match in regex.findAll(text)) {
            val range = match.range
            if (usedRanges.any { it.first < range.last && it.last > range.first }) continue
            val group = match.groups[1] ?: continue
            spans.add(MarkdownSpan(range.first, range.last + 1, group.range.first, group.range.last + 1, style))
            usedRanges.add(range)
        }
    }

    spans.sortBy { it.fullStart }

    return buildAnnotatedString {
        var cursor = 0
        for (span in spans) {
            if (span.fullStart > cursor) append(text.substring(cursor, span.fullStart))
            pushStyle(span.style)
            append(text.substring(span.textStart, span.textEnd))
            pop()
            cursor = span.fullEnd
        }
        if (cursor < text.length) append(text.substring(cursor))
    }
}
