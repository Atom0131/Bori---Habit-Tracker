package com.apagon.rhythm.widget
import com.apagon.rhythm.core.time.*

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
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
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.apagon.rhythm.R
import com.apagon.rhythm.data.model.Reminder
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import com.apagon.rhythm.core.time.DateTimeFormatter
import com.apagon.rhythm.core.time.DateTimeParseException

class TodayGlanceWidget : GlanceAppWidget() {

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
        val showHabits = prefs[TodayGlanceWidgetPrefs.SHOW_HABITS_KEY] ?: true
        val showTodos = prefs[TodayGlanceWidgetPrefs.SHOW_TODOS_KEY] ?: true
        val showReminders = prefs[TodayGlanceWidgetPrefs.SHOW_REMINDERS_KEY] ?: true

        val ep = WidgetEntryPoint()
        val habits = ep.habitRepository().getAllActiveHabits().first()
        val today = LocalDate.now().toString()
        val completions = ep.habitRepository().getCompletionsByDate(today).first()
        val activeIds = habits.map { it.id }.toSet()
        val done = completions.count { it.habitId in activeIds }
        val total = habits.size
        val pendingTodos = ep.todoRepository().getPendingTodos().first().size
        val nextReminder = ep.reminderRepository().getAllActiveReminders().first()
            .firstOrNull { isUpcoming(it) }

        val accentArgb = ep.themePreferences().accentColorArgb.first()
        val accentIndex = ep.themePreferences().accentColorIndex.first()
        val appAccent = WidgetColors.resolveAccent(accentIndex, accentArgb)
        val style = prefs[TodayGlanceWidgetPrefs.STYLE_KEY] ?: WidgetColors.STYLE_DEFAULT
        val customArgb = prefs[TodayGlanceWidgetPrefs.CUSTOM_COLOR_KEY]
        val opacity = prefs[TodayGlanceWidgetPrefs.OPACITY_KEY] ?: TodayGlanceWidgetPrefs.DEFAULT_OPACITY
        val palette = WidgetColors.resolvePalette(context, style, customArgb, appAccent, opacity)

        provideContent {
            GlanceContent(
                done, total, pendingTodos, nextReminder, palette,
                showHabits, showTodos, showReminders
            )
        }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        val sampleTime = LocalDateTime.now().plusHours(2)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        val sampleReminder = Reminder(title = "Morning Run", dateTime = sampleTime)
        provideContent {
            GlanceContent(
                done = 4, total = 6, pendingTodos = 3, nextReminder = sampleReminder,
                palette = WidgetColors.defaultPreviewPalette,
                showHabits = true, showTodos = true, showReminders = true
            )
        }
    }

    private fun isUpcoming(reminder: Reminder): Boolean {
        if (reminder.isCompleted) return false
        return try {
            val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            LocalDateTime.parse(reminder.dateTime, fmt).isAfter(LocalDateTime.now())
        } catch (e: DateTimeParseException) {
            false
        }
    }
}

private class Section(val tab: String, val content: @Composable () -> Unit)

@Composable
private fun GlanceContent(
    done: Int,
    total: Int,
    pendingTodos: Int,
    nextReminder: Reminder?,
    palette: WidgetColors.Palette,
    showHabits: Boolean,
    showTodos: Boolean,
    showReminders: Boolean
) {
    val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    val timeFmt = DateTimeFormatter.ofPattern("h:mm a")
    val reminderTime = nextReminder?.let {
        try { LocalDateTime.parse(it.dateTime, fmt).format(timeFmt) } catch (e: Exception) { "" }
    } ?: ""

    val context = LocalContext.current
    val sections = buildList {
        if (showHabits) add(Section("list") { HabitsSection(done, total, palette) })
        if (showTodos) add(Section("list") { TodosSection(pendingTodos, palette) })
        if (showReminders) add(Section("clock") { RemindersSection(nextReminder, reminderTime, palette) })
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(palette.cardBackground))
            .cornerRadius(28.dp)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (sections.isEmpty()) {
            Box(
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxWidth()
                    .clickable(actionStartActivity(openTabIntent(context, "list")))
            ) {
                Text(
                    text = "Nothing to show — edit widget",
                    style = TextStyle(color = ColorProvider(Color(palette.onSurfaceVariantArgb)), fontSize = 11.sp)
                )
            }
        }
        sections.forEachIndexed { index, section ->
            if (index > 0) {
                Spacer(GlanceModifier.height(8.dp))
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(ColorProvider(WidgetColors.border(palette.primaryArgb)))
                ) {}
                Spacer(GlanceModifier.height(8.dp))
            }
            Row(
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxWidth()
                    .clickable(actionStartActivity(openTabIntent(context, section.tab))),
                verticalAlignment = Alignment.CenterVertically
            ) {
                section.content()
            }
        }
    }
}

@Composable
private fun SectionRow(iconRes: Int, palette: WidgetColors.Palette, text: String) {
    Image(
        provider = ImageProvider(iconRes),
        contentDescription = null,
        colorFilter = ColorFilter.tint(ColorProvider(Color(palette.primaryArgb))),
        modifier = GlanceModifier.size(18.dp)
    )
    Spacer(GlanceModifier.width(10.dp))
    Text(
        text = text,
        style = TextStyle(
            color = ColorProvider(Color(palette.onSurfaceArgb)),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        ),
        maxLines = 1
    )
}

@Composable
private fun HabitsSection(done: Int, total: Int, palette: WidgetColors.Palette) {
    SectionRow(R.drawable.ic_widget_check_circle, palette, "$done of $total habits done")
}

@Composable
private fun TodosSection(pendingTodos: Int, palette: WidgetColors.Palette) {
    SectionRow(R.drawable.ic_widget_checklist, palette, "$pendingTodos to-dos pending")
}

@Composable
private fun RemindersSection(nextReminder: Reminder?, reminderTime: String, palette: WidgetColors.Palette) {
    if (nextReminder != null) {
        SectionRow(
            R.drawable.ic_widget_notifications, palette,
            "${nextReminder.title.take(14)} — $reminderTime"
        )
    } else {
        SectionRow(R.drawable.ic_widget_notifications, palette, "No reminders")
    }
}

class TodayGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = TodayGlanceWidget()
}
