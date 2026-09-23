package com.apagon.rhythm.widget

import org.koin.android.ext.android.inject

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.lifecycle.lifecycleScope
import com.apagon.rhythm.data.preferences.DarkReadability
import com.apagon.rhythm.data.preferences.ThemeMode
import com.apagon.rhythm.data.preferences.ThemePreferences
import com.apagon.rhythm.ui.theme.RhythmTheme
import kotlinx.coroutines.launch
class TodayGlanceConfigActivity : ComponentActivity() {

    val themePreferences: ThemePreferences by inject()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appWidgetId = intent.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setResult(RESULT_CANCELED)

        setContent {
            val themeMode by themePreferences.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val amoledMode by themePreferences.amoledMode.collectAsState(initial = false)
            val accentColorIndex by themePreferences.accentColorIndex.collectAsState(initial = 0)
            val accentColorArgb by themePreferences.accentColorArgb.collectAsState(initial = null)
            val darkReadability by themePreferences.darkReadability.collectAsState(initial = DarkReadability.STANDARD)

            val isDark = when (themeMode) {
                ThemeMode.LIGHT  -> false
                ThemeMode.DARK   -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            val primaryArgb = WidgetColors.resolveAccent(accentColorIndex, accentColorArgb)

            RhythmTheme(
                darkTheme = isDark,
                isAmoled = amoledMode,
                seedColor = Color(primaryArgb),
                darkContrastLevel = when (darkReadability) {
                    DarkReadability.STANDARD    -> 0.0
                    DarkReadability.COMFORTABLE -> 0.3
                    DarkReadability.HIGH        -> 0.65
                }
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.systemBars),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TodayGlanceConfigScreen(
                        appWidgetId = appWidgetId,
                        fallbackColorArgb = primaryArgb,
                        onConfirm = { showHabits, showTodos, showReminders, style, customColorArgb, opacity ->
                            lifecycleScope.launch {
                                try {
                                    val glanceId = GlanceAppWidgetManager(this@TodayGlanceConfigActivity)
                                        .getGlanceIdBy(appWidgetId)
                                    updateAppWidgetState(
                                        this@TodayGlanceConfigActivity,
                                        glanceId
                                    ) { prefs ->
                                        prefs[TodayGlanceWidgetPrefs.SHOW_HABITS_KEY] = showHabits
                                        prefs[TodayGlanceWidgetPrefs.SHOW_TODOS_KEY] = showTodos
                                        prefs[TodayGlanceWidgetPrefs.SHOW_REMINDERS_KEY] = showReminders
                                        prefs[TodayGlanceWidgetPrefs.STYLE_KEY] = style
                                        if (customColorArgb != null) {
                                            prefs[TodayGlanceWidgetPrefs.CUSTOM_COLOR_KEY] = customColorArgb
                                        } else {
                                            prefs.remove(TodayGlanceWidgetPrefs.CUSTOM_COLOR_KEY)
                                        }
                                        prefs[TodayGlanceWidgetPrefs.OPACITY_KEY] = opacity
                                    }
                                    TodayGlanceWidget().update(this@TodayGlanceConfigActivity, glanceId)
                                } catch (_: Exception) { }
                                setResult(
                                    RESULT_OK,
                                    Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                                )
                                finish()
                            }
                        },
                        onCancel = { finish() }
                    )
                }
            }
        }
    }
}

@Composable
private fun TodayGlanceConfigScreen(
    appWidgetId: Int,
    fallbackColorArgb: Int,
    onConfirm: (showHabits: Boolean, showTodos: Boolean, showReminders: Boolean, style: String, customColorArgb: Int?, opacity: Float) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var showHabits by remember { mutableStateOf(true) }
    var showTodos by remember { mutableStateOf(true) }
    var showReminders by remember { mutableStateOf(true) }
    var style by remember { mutableStateOf(WidgetColors.STYLE_DEFAULT) }
    var customColorArgb by remember { mutableStateOf<Int?>(null) }
    var opacity by remember { mutableFloatStateOf(TodayGlanceWidgetPrefs.DEFAULT_OPACITY) }

    LaunchedEffect(Unit) {
        try {
            val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)
            val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)
            showHabits = prefs[TodayGlanceWidgetPrefs.SHOW_HABITS_KEY] ?: true
            showTodos = prefs[TodayGlanceWidgetPrefs.SHOW_TODOS_KEY] ?: true
            showReminders = prefs[TodayGlanceWidgetPrefs.SHOW_REMINDERS_KEY] ?: true
            style = prefs[TodayGlanceWidgetPrefs.STYLE_KEY] ?: WidgetColors.STYLE_DEFAULT
            customColorArgb = prefs[TodayGlanceWidgetPrefs.CUSTOM_COLOR_KEY]
            opacity = prefs[TodayGlanceWidgetPrefs.OPACITY_KEY] ?: TodayGlanceWidgetPrefs.DEFAULT_OPACITY
        } catch (_: Exception) { }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Today Glance Widget",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Choose what shows in the widget",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        ListItem(
            headlineContent = { Text("Habits") },
            trailingContent = {
                Switch(checked = showHabits, onCheckedChange = { showHabits = it })
            }
        )
        ListItem(
            headlineContent = { Text("To-dos") },
            trailingContent = {
                Switch(checked = showTodos, onCheckedChange = { showTodos = it })
            }
        )
        ListItem(
            headlineContent = { Text("Next reminder") },
            trailingContent = {
                Switch(checked = showReminders, onCheckedChange = { showReminders = it })
            }
        )

        WidgetColorOpacitySection(
            style = style,
            customColorArgb = customColorArgb,
            opacity = opacity,
            fallbackColorArgb = fallbackColorArgb,
            onStyleChange = { style = it },
            onCustomColorChange = { customColorArgb = it },
            onOpacityChange = { opacity = it }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Cancel")
            }
            Button(
                onClick = { onConfirm(showHabits, showTodos, showReminders, style, customColorArgb, opacity) },
                modifier = Modifier
                    .weight(2f)
                    .height(52.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Add Widget")
            }
        }
    }
}
