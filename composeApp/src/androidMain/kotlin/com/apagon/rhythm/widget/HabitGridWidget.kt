package com.apagon.rhythm.widget
import com.apagon.rhythm.core.time.*

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.BitmapImageProvider
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.background
import androidx.glance.state.PreferencesGlanceStateDefinition
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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.apagon.rhythm.R
import com.apagon.rhythm.data.model.Habit
import com.apagon.rhythm.ui.util.resolvedIcon
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate

class HabitGridWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(180.dp, 110.dp),
            DpSize(250.dp, 110.dp),
            DpSize(250.dp, 180.dp),
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
        val selectedIds = HabitGridWidgetPrefs.parseIds(prefs[HabitGridWidgetPrefs.SELECTED_HABITS_KEY])

        val ep = WidgetEntryPoint()
        val activeHabits = ep.habitRepository().getAllActiveHabits().first()
        val habits = if (selectedIds.isEmpty()) {
            activeHabits.take(6)
        } else {
            selectedIds.mapNotNull { id -> activeHabits.firstOrNull { it.id == id } }
        }
        val today = LocalDate.now().toString()
        val completions = ep.habitRepository().getCompletionsByDate(today).first()
        val doneIds = completions.map { it.habitId }.toSet()

        val accentArgb = ep.themePreferences().accentColorArgb.first()
        val accentIndex = ep.themePreferences().accentColorIndex.first()
        val appAccent = WidgetColors.resolveAccent(accentIndex, accentArgb)
        val style = prefs[HabitGridWidgetPrefs.STYLE_KEY] ?: WidgetColors.STYLE_DEFAULT
        val customArgb = prefs[HabitGridWidgetPrefs.CUSTOM_COLOR_KEY]
        val opacity = prefs[HabitGridWidgetPrefs.OPACITY_KEY] ?: HabitGridWidgetPrefs.DEFAULT_OPACITY
        val palette = WidgetColors.resolvePalette(context, style, customArgb, appAccent, opacity)

        val density = context.resources.displayMetrics.density
        val iconPx = (22 * density).toInt().coerceAtLeast(16)
        val iconBitmaps = habits.filter { it.id !in doneIds }.associate { habit ->
            habit.id to WidgetColors.iconToBitmap(habit.resolvedIcon(), iconPx, palette.primaryArgb)
        }

        provideContent { GridContent(habits, doneIds, palette, iconBitmaps) }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        val sampleHabits = listOf(
            Habit(id = 1, name = "Work"),
            Habit(id = 2, name = "Run"),
            Habit(id = 3, name = "Read"),
            Habit(id = 4, name = "Gym"),
            Habit(id = 5, name = "Sleep"),
            Habit(id = 6, name = "Write"),
        )
        val sampleDoneIds = setOf(1L, 2L, 3L, 4L)
        val density = context.resources.displayMetrics.density
        val iconPx = (22 * density).toInt().coerceAtLeast(16)
        val palette = WidgetColors.defaultPreviewPalette
        val iconBitmaps = sampleHabits.filter { it.id !in sampleDoneIds }.associate { habit ->
            habit.id to WidgetColors.iconToBitmap(habit.resolvedIcon(), iconPx, palette.primaryArgb)
        }
        provideContent { GridContent(sampleHabits, sampleDoneIds, palette, iconBitmaps) }
    }
}

@Composable
private fun GridContent(
    habits: List<Habit>,
    doneIds: Set<Long>,
    palette: WidgetColors.Palette,
    iconBitmaps: Map<Long, Bitmap>
) {
    val context = LocalContext.current
    val done = habits.count { it.id in doneIds }
    val rows = habits.chunked(3)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(palette.cardBackground))
            .cornerRadius(28.dp)
            .clickable(actionStartActivity(openTabIntent(context, "list")))
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today",
                    style = TextStyle(
                        color = ColorProvider(Color(palette.onSurfaceArgb)),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(GlanceModifier.defaultWeight())
                Text(
                    text = "$done/${habits.size}",
                    style = TextStyle(
                        color = ColorProvider(Color(palette.primaryArgb)),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            Spacer(GlanceModifier.height(10.dp))
            rows.forEachIndexed { rowIndex, rowHabits ->
                if (rowIndex > 0) Spacer(GlanceModifier.height(8.dp))
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    rowHabits.forEachIndexed { index, habit ->
                        if (index > 0) Spacer(GlanceModifier.defaultWeight())
                        HabitCircle(
                            habit,
                            isDone = habit.id in doneIds,
                            palette = palette,
                            iconBitmap = iconBitmaps[habit.id]
                        )
                    }
                    repeat((3 - rowHabits.size).coerceAtLeast(0)) {
                        if (rowHabits.isNotEmpty()) Spacer(GlanceModifier.defaultWeight())
                        Box(modifier = GlanceModifier.size(48.dp)) {}
                    }
                }
            }
        }
    }
}

@Composable
private fun HabitCircle(
    habit: Habit,
    isDone: Boolean,
    palette: WidgetColors.Palette,
    iconBitmap: Bitmap?
) {
    val context = LocalContext.current
    val clickAction = if (isDone) {
        actionStartActivity(openTabIntent(context, "list", habit.id))
    } else {
        actionSendBroadcast(
            Intent(context, HabitWidgetReceiver::class.java).apply {
                action = HabitWidgetReceiver.ACTION_COMPLETE
                putExtra(HabitWidgetReceiver.EXTRA_HABIT_ID, habit.id)
            }
        )
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = GlanceModifier
                .size(48.dp)
                .background(ColorProvider(Color(palette.primaryArgb)))
                .cornerRadius(24.dp)
                .clickable(clickAction),
            contentAlignment = Alignment.Center
        ) {
            if (isDone) {
                Image(
                    provider = ImageProvider(R.drawable.ic_widget_check),
                    contentDescription = null,
                    modifier = GlanceModifier.size(20.dp)
                )
            } else {
                // True-outline look: inner box matches the card fill, "erasing" the
                // middle of the solid circle above and leaving only a ring border.
                Box(
                    modifier = GlanceModifier
                        .size(44.dp)
                        .background(ColorProvider(palette.cardBackground))
                        .cornerRadius(22.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (iconBitmap != null) {
                        Image(
                            provider = BitmapImageProvider(iconBitmap),
                            contentDescription = habit.name,
                            modifier = GlanceModifier.size(22.dp)
                        )
                    } else {
                        Text(
                            text = habit.name.take(1).uppercase(),
                            style = TextStyle(
                                color = ColorProvider(Color(palette.primaryArgb)),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
        Spacer(GlanceModifier.height(3.dp))
        Text(
            text = habit.name.take(7),
            style = TextStyle(
                color = ColorProvider(Color(palette.onSurfaceArgb)),
                fontSize = 9.sp
            ),
            maxLines = 1
        )
    }
}

class HabitGridWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = HabitGridWidget()
}
