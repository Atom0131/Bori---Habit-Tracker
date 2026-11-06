package com.apagon.rhythm.ui.notes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.contentOrNull

/**
 * **Handwriting from the phone or tablet, shown on the desktop (1.1.8, mirrors Android 1.93).**
 *
 * An INK block's content is the Android app's page JSON (see `ui/notes/ink/InkModel.kt` there):
 * a 1000-unit-wide page of strokes, points delta-encoded in tenths of a unit. The desktop has no
 * Jetpack Ink, so strokes are drawn as round-capped paths at their brush width; pressure variation
 * is not reproduced. Read-only: writing and Convert to text need the phone or tablet.
 */
private const val PAGE_WIDTH = 1000f

class DesktopInkStroke(val color: Int, val size: Float, val highlighter: Boolean, val xs: FloatArray, val ys: FloatArray)

class DesktopInkPage(val strokes: List<DesktopInkStroke>, val text: String) {
    val top get() = strokes.minOfOrNull { s -> s.ys.minOrNull()?.minus(s.size) ?: 0f } ?: 0f
    val bottom get() = strokes.maxOfOrNull { s -> s.ys.maxOrNull()?.plus(s.size) ?: 0f } ?: 0f

    companion object {
        fun decode(content: String): DesktopInkPage = runCatching {
            val root = Json.parseToJsonElement(content).jsonObject
            val text = root["text"]?.jsonPrimitive?.contentOrNull ?: ""
            val strokes = (root["s"] as? JsonArray)?.mapNotNull { el ->
                val o = el as? JsonObject ?: return@mapNotNull null
                val p = o["p"]?.jsonArray ?: return@mapNotNull null
                val n = p.size / 4
                if (n == 0) return@mapNotNull null
                val xs = FloatArray(n); val ys = FloatArray(n)
                var x = 0; var y = 0
                for (i in 0 until n) {
                    val dx = p[i * 4].jsonPrimitive.int; val dy = p[i * 4 + 1].jsonPrimitive.int
                    if (i == 0) { x = dx; y = dy } else { x += dx; y += dy }
                    xs[i] = x / 10f; ys[i] = y / 10f
                }
                DesktopInkStroke(
                    color = o["c"]?.jsonPrimitive?.long?.toInt() ?: 0,
                    size = o["w"]?.jsonPrimitive?.doubleOrNull?.toFloat() ?: 7f,
                    highlighter = (o["k"]?.jsonPrimitive?.intOrNull ?: 0) == 1,
                    xs = xs, ys = ys
                )
            } ?: emptyList()
            DesktopInkPage(strokes, text)
        }.getOrDefault(DesktopInkPage(emptyList(), ""))

        /** The converted text alone, for previews and search. */
        fun textOf(content: String): String = runCatching {
            Json.parseToJsonElement(content).jsonObject["text"]?.jsonPrimitive?.contentOrNull ?: ""
        }.getOrDefault("")
    }
}

@Composable
fun InkBlockView(content: String, modifier: Modifier = Modifier) {
    val page = remember(content) { DesktopInkPage.decode(content) }
    val ink = MaterialTheme.colorScheme.onSurface
    Column(modifier) {
        if (page.strokes.isEmpty()) {
            Text(
                "Handwriting (empty). Write on your phone or tablet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(12.dp)
            )
            return@Column
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val density = LocalDensity.current
            val widthPx = with(density) { maxWidth.toPx() }
            val scale = widthPx / PAGE_WIDTH
            val top = (page.top - 24f).coerceAtLeast(0f)
            val bottom = page.bottom + 24f
            val h = with(density) { ((bottom - top) * scale).toDp() }.coerceIn(48.dp, 560.dp)
            Canvas(Modifier.fillMaxWidth().height(h)) {
                page.strokes.forEach { s ->
                    val base = if (s.color == 0) ink else Color(s.color)
                    val color = if (s.highlighter) base.copy(alpha = 0.4f) else base
                    val path = Path()
                    for (i in s.xs.indices) {
                        val o = Offset(s.xs[i] * scale, (s.ys[i] - top) * scale)
                        if (i == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
                    }
                    if (s.xs.size == 1) path.lineTo(s.xs[0] * scale + 0.1f, (s.ys[0] - top) * scale)
                    drawPath(path, color, style = Stroke(width = s.size * scale, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }
        }
        if (page.text.isNotBlank()) {
            Text(
                page.text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
