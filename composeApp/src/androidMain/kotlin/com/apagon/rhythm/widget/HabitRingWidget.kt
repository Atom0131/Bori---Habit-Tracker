package com.apagon.rhythm.widget
import com.apagon.rhythm.core.time.*

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.BitmapImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.background
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.apagon.rhythm.R
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate

class HabitRingWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(110.dp, 110.dp),
            DpSize(180.dp, 180.dp),
            DpSize(250.dp, 250.dp),
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
        val configuredHabitId = prefs[HabitRingWidgetPrefs.HABIT_ID_KEY] ?: HabitRingWidgetPrefs.ALL_HABITS_ID

        val ep = WidgetEntryPoint()
        val habits = ep.habitRepository().getAllActiveHabits().first()
        val today = LocalDate.now().toString()
        val completions = ep.habitRepository().getCompletionsByDate(today).first()

        val accentArgb = ep.themePreferences().accentColorArgb.first()
        val accentIndex = ep.themePreferences().accentColorIndex.first()
        val appAccent = WidgetColors.resolveAccent(accentIndex, accentArgb)
        val style = prefs[HabitRingWidgetPrefs.STYLE_KEY] ?: WidgetColors.STYLE_DEFAULT
        val customArgb = prefs[HabitRingWidgetPrefs.CUSTOM_COLOR_KEY]
        val opacity = prefs[HabitRingWidgetPrefs.OPACITY_KEY] ?: HabitRingWidgetPrefs.DEFAULT_OPACITY
        val palette = WidgetColors.resolvePalette(context, style, customArgb, appAccent, opacity)

        val targetHabit = if (configuredHabitId != HabitRingWidgetPrefs.ALL_HABITS_ID) {
            habits.firstOrNull { it.id == configuredHabitId }
        } else null

        if (targetHabit != null) {
            val isDone = completions.any { it.habitId == targetHabit.id }
            provideContent { RingContent(if (isDone) 1 else 0, 1, palette, targetHabit.name, targetHabit.id) }
        } else {
            val total = habits.size
            val activeIds = habits.map { it.id }.toSet()
            val done = completions.count { it.habitId in activeIds }
            provideContent { RingContent(done, total, palette, null, null) }
        }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        provideContent { RingContent(4, 6, WidgetColors.defaultPreviewPalette, null, null) }
    }
}

@Composable
private fun RingContent(done: Int, total: Int, palette: WidgetColors.Palette, habitName: String?, habitId: Long?) {
    val size = LocalSize.current
    val isLarge = size.width >= 240.dp
    val isMedium = size.width >= 160.dp

    val context = LocalContext.current
    val density = context.resources.displayMetrics.density
    val minSideDp = minOf(size.width, size.height)
    val ringDp = (minSideDp.value * 0.8f).dp
    val bitmapPx = (ringDp.value * density).toInt().coerceAtLeast(80)

    val countFontSizeSp = if (isLarge) 52f else if (isMedium) 44f else 36f
    val labelFontSize = if (isLarge) 14.sp else if (isMedium) 12.sp else 11.sp

    val progress = if (total > 0) done.toFloat() / total else 0f
    val ringBitmap = WidgetColors.progressBitmap(progress, bitmapPx, palette.primaryArgb)
    val typeface = ResourcesCompat.getFont(context, R.font.plus_jakarta_sans_bold) ?: android.graphics.Typeface.DEFAULT_BOLD
    val heroText = if (habitName != null) (if (done > 0) "✓" else "—") else "$done"
    val heroBitmap = WidgetColors.textToBitmap(
        heroText, bitmapPx, (countFontSizeSp * density * 1.3f).toInt(), countFontSizeSp * density, typeface, palette.onSurfaceArgb
    )

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(palette.cardBackground))
            .cornerRadius(28.dp)
            .padding(10.dp)
            .clickable(actionStartActivity(openTabIntent(context, "list", habitId))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = GlanceModifier.size(ringDp), contentAlignment = Alignment.Center) {
                Image(
                    provider = BitmapImageProvider(ringBitmap),
                    contentDescription = null,
                    modifier = GlanceModifier.fillMaxSize()
                )
                Image(
                    provider = BitmapImageProvider(heroBitmap),
                    contentDescription = null,
                    modifier = GlanceModifier
                        .width(ringDp)
                        .height((countFontSizeSp * 1.3f).dp)
                )
            }
            Spacer(GlanceModifier.height(2.dp))
            Text(
                text = if (habitName != null) (if (done > 0) "done today" else "not yet today") else "of $total today",
                style = TextStyle(
                    color = ColorProvider(Color(palette.onSurfaceVariantArgb)),
                    fontSize = labelFontSize
                )
            )
            Spacer(GlanceModifier.height(6.dp))
            Text(
                text = habitName?.take(14) ?: "Rhythm",
                style = TextStyle(
                    color = ColorProvider(Color(palette.primaryArgb)),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
        }
    }
}

class HabitRingWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = HabitRingWidget()
}
