package com.apagon.rhythm.widget
import com.apagon.rhythm.core.time.*

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.BitmapImageProvider
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.apagon.rhythm.R
import com.apagon.rhythm.data.model.Habit
import com.apagon.rhythm.data.model.HabitCompletion
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate

class StreakSpotlightWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(110.dp, 110.dp),
            DpSize(180.dp, 180.dp),
            DpSize(250.dp, 250.dp),
            DpSize(300.dp, 300.dp),
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
        val drumIcon = prefs[StreakWidgetPrefs.DRUM_ICON_KEY] ?: "fire"
        val configuredHabitId = prefs[StreakWidgetPrefs.HABIT_ID_KEY] ?: -1L

        val ep = WidgetEntryPoint()

        val accentArgb = ep.themePreferences().accentColorArgb.first()
        val accentIndex = ep.themePreferences().accentColorIndex.first()
        val appAccent = WidgetColors.resolveAccent(accentIndex, accentArgb)
        val style = prefs[StreakWidgetPrefs.STYLE_KEY] ?: WidgetColors.STYLE_DEFAULT
        val customArgb = prefs[StreakWidgetPrefs.CUSTOM_COLOR_KEY]
        val opacity = prefs[StreakWidgetPrefs.OPACITY_KEY] ?: StreakWidgetPrefs.DEFAULT_OPACITY
        val palette = WidgetColors.resolvePalette(context, style, customArgb, appAccent, opacity)

        val habits = ep.habitRepository().getAllActiveHabits().first()

        if (habits.isEmpty()) {
            provideContent { EmptyStreakContent(palette) }
            return
        }

        val today = LocalDate.now()
        val yearAgo = today.minusDays(365).toString()
        val yearCompletions = ep.habitRepository()
            .getCompletionsBetweenDates(yearAgo, today.toString()).first()

        val targetHabit = if (configuredHabitId != -1L) {
            habits.firstOrNull { it.id == configuredHabitId } ?: habits.first()
        } else {
            habits.maxByOrNull { habit -> computeStreak(habit.id, yearCompletions, today) }
                ?: habits.first()
        }

        val targetCompletions = yearCompletions.filter { it.habitId == targetHabit.id }
        val streak = computeStreak(targetHabit.id, yearCompletions, today)
        val monthCalendar = computeCurrentMonthCalendar(targetHabit, targetCompletions, today)

        provideContent {
            StreakContent(drumIcon, streak, targetHabit.name, targetHabit.id, today, monthCalendar, palette)
        }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        // 3 leading blanks (month starting on a Wednesday) + 30 days: mostly done, a few missed, last 3 in the future.
        val sampleCalendar: List<DayState?> = List(3) { null } +
            List(19) { DayState.DONE } +
            listOf(DayState.MISSED, DayState.DONE, DayState.MISSED, DayState.DONE, DayState.DONE) +
            List(3) { DayState.NOT_SCHEDULED } +
            List(3) { DayState.NO_DATA }
        provideContent {
            StreakContent("fire", 21, "Morning Run", null, LocalDate.now(), sampleCalendar, WidgetColors.defaultPreviewPalette)
        }
    }

    private fun computeStreak(habitId: Long, completions: List<HabitCompletion>, today: LocalDate): Int {
        val doneSet = completions
            .filter { it.habitId == habitId }
            .map { LocalDate.parse(it.dateCompleted) }
            .toHashSet()
        var streak = 0
        var day = today
        while (doneSet.contains(day)) {
            streak++
            day = day.minusDays(1)
        }
        return streak
    }
}

@Composable
private fun EmptyStreakContent(palette: WidgetColors.Palette) {
    val context = LocalContext.current
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(palette.cardBackground))
            .cornerRadius(28.dp)
            .clickable(actionStartActivity(openTabIntent(context, "list"))),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No habits yet",
            style = TextStyle(color = ColorProvider(Color(palette.onSurfaceVariantArgb)), fontSize = 12.sp)
        )
    }
}

@Composable
private fun StreakContent(
    drumIcon: String,
    streak: Int,
    habitName: String,
    habitId: Long?,
    today: LocalDate,
    monthCalendar: List<DayState?>,
    palette: WidgetColors.Palette
) {
    val size = LocalSize.current
    val isXLarge = size.width >= 280.dp
    val isLarge = size.width >= 240.dp
    val isMedium = size.width >= 160.dp

    val iconSize = if (isLarge) 56.dp else if (isMedium) 40.dp else 28.dp
    val streakFontSizeSp = if (isLarge) 72f else if (isMedium) 54f else 40f
    val labelFontSize = if (isLarge) 18.sp else if (isMedium) 14.sp else 11.sp
    val nameFontSize = if (isLarge) 13.sp else if (isMedium) 11.sp else 10.sp
    val monthLabelFontSize = if (isLarge) 12.sp else if (isMedium) 10.sp else 9.sp
    // Up to 6 rows x 7 columns for a full month — sized to still fit the smallest 110dp widget.
    val calendarDotSize = if (isXLarge) 16.dp else if (isLarge) 12.dp else if (isMedium) 8.dp else 5.dp
    val calendarGap = if (isXLarge) 5.dp else if (isLarge) 4.dp else if (isMedium) 2.5.dp else 1.5.dp
    val context = LocalContext.current
    val density = context.resources.displayMetrics.density
    val typeface = ResourcesCompat.getFont(context, R.font.plus_jakarta_sans_bold) ?: android.graphics.Typeface.DEFAULT_BOLD
    val heroWidthPx = (streakFontSizeSp * density * 2.2f).toInt()
    val heroHeightPx = (streakFontSizeSp * density * 1.3f).toInt()
    val streakBitmap = WidgetColors.textToBitmap(
        "$streak", heroWidthPx, heroHeightPx, streakFontSizeSp * density, typeface, palette.onSurfaceArgb
    )

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(palette.cardBackground))
            .cornerRadius(28.dp)
            .padding(12.dp)
            .clickable(actionStartActivity(openTabIntent(context, "list", habitId))),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            provider = ImageProvider(drumDrawableFor(drumIcon)),
            contentDescription = null,
            colorFilter = ColorFilter.tint(ColorProvider(Color(palette.primaryArgb))),
            modifier = GlanceModifier.size(iconSize)
        )
        Spacer(GlanceModifier.height(4.dp))
        Image(
            provider = BitmapImageProvider(streakBitmap),
            contentDescription = "$streak",
            modifier = GlanceModifier
                .width((heroWidthPx / density).dp)
                .height((heroHeightPx / density).dp)
        )
        Text(
            text = "DAY STREAK",
            style = TextStyle(
                color = ColorProvider(Color(palette.onSurfaceVariantArgb)),
                fontSize = labelFontSize,
                fontWeight = FontWeight.Medium
            )
        )
        Spacer(GlanceModifier.height(6.dp))
        Text(
            text = today.month.name.lowercase().replaceFirstChar { it.uppercase() },
            style = TextStyle(
                color = ColorProvider(Color(palette.onSurfaceVariantArgb)),
                fontSize = monthLabelFontSize,
                fontWeight = FontWeight.Medium
            )
        )
        Spacer(GlanceModifier.height(3.dp))
        CalendarGrid(monthCalendar, palette, dotSize = calendarDotSize, gap = calendarGap)
        Spacer(GlanceModifier.height(4.dp))
        Text(
            text = habitName,
            style = TextStyle(
                color = ColorProvider(Color(palette.onSurfaceVariantArgb)),
                fontSize = nameFontSize
            ),
            maxLines = 1
        )
    }
}

/** Current-month calendar grid, Sunday-first, 7 columns — the only streak visualization now.
 * Leading `null` cells are blank padding so day 1 lands on its real weekday, same as a normal calendar. */
@Composable
private fun CalendarGrid(
    days: List<DayState?>,
    palette: WidgetColors.Palette,
    dotSize: androidx.compose.ui.unit.Dp,
    gap: androidx.compose.ui.unit.Dp
) {
    val rows = days.chunked(7)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        rows.forEachIndexed { rowIndex, rowDays ->
            if (rowIndex > 0) Spacer(GlanceModifier.height(gap))
            Row(horizontalAlignment = Alignment.CenterHorizontally) {
                rowDays.forEachIndexed { index, state ->
                    if (index > 0) Spacer(GlanceModifier.width(gap))
                    Box(
                        modifier = GlanceModifier
                            .size(dotSize)
                            .background(ColorProvider(WidgetColors.calendarDotColor(state, palette.primaryArgb, palette.onSurfaceVariantArgb)))
                            .cornerRadius(dotSize / 2)
                    ) {}
                }
            }
        }
    }
}

private fun drumDrawableFor(icon: String): Int = when (icon) {
    "bomba" -> R.drawable.widget_drum_bomba
    "taiko" -> R.drawable.widget_drum_taiko
    "djembe" -> R.drawable.widget_drum_djembe
    "snare" -> R.drawable.widget_drum_snare
    "tabla" -> R.drawable.widget_drum_tabla
    "steelpan" -> R.drawable.widget_drum_steelpan
    "conga" -> R.drawable.widget_drum_conga
    "bongo" -> R.drawable.widget_drum_bongo
    "cajon" -> R.drawable.widget_drum_cajon
    "timbales" -> R.drawable.widget_drum_timbales
    "talkingdrum" -> R.drawable.widget_drum_talkingdrum
    "bassdrum" -> R.drawable.widget_drum_bassdrum
    "framedrum" -> R.drawable.widget_drum_framedrum
    "darbuka" -> R.drawable.widget_drum_darbuka
    else -> R.drawable.widget_drum_fire
}

class StreakSpotlightWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = StreakSpotlightWidget()
}
