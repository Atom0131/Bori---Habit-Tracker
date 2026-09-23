package com.apagon.rhythm.widget

import org.koin.android.ext.android.inject

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.lifecycle.lifecycleScope
import com.apagon.rhythm.R
import com.apagon.rhythm.data.model.Habit
import com.apagon.rhythm.data.preferences.DarkReadability
import com.apagon.rhythm.data.preferences.ThemeMode
import com.apagon.rhythm.data.preferences.ThemePreferences
import com.apagon.rhythm.data.repository.HabitRepository
import com.apagon.rhythm.ui.theme.RhythmTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
class StreakConfigActivity : ComponentActivity() {

    val habitRepository: HabitRepository by inject()
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
                    StreakConfigScreen(
                        habitRepository = habitRepository,
                        appWidgetId = appWidgetId,
                        fallbackColorArgb = primaryArgb,
                        onConfirm = { drumIcon, habitId, style, customColorArgb, opacity ->
                            lifecycleScope.launch {
                                try {
                                    val glanceId = GlanceAppWidgetManager(this@StreakConfigActivity)
                                        .getGlanceIdBy(appWidgetId)
                                    updateAppWidgetState(
                                        this@StreakConfigActivity,
                                        glanceId
                                    ) { prefs ->
                                        prefs[StreakWidgetPrefs.DRUM_ICON_KEY] = drumIcon
                                        prefs[StreakWidgetPrefs.HABIT_ID_KEY] = habitId
                                        prefs[StreakWidgetPrefs.STYLE_KEY] = style
                                        if (customColorArgb != null) {
                                            prefs[StreakWidgetPrefs.CUSTOM_COLOR_KEY] = customColorArgb
                                        } else {
                                            prefs.remove(StreakWidgetPrefs.CUSTOM_COLOR_KEY)
                                        }
                                        prefs[StreakWidgetPrefs.OPACITY_KEY] = opacity
                                    }
                                    StreakSpotlightWidget().update(this@StreakConfigActivity, glanceId)
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
private fun StreakConfigScreen(
    habitRepository: HabitRepository,
    appWidgetId: Int,
    fallbackColorArgb: Int,
    onConfirm: (drumIcon: String, habitId: Long, style: String, customColorArgb: Int?, opacity: Float) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val habits = remember { mutableStateListOf<Habit>() }
    var selectedDrum by remember { mutableStateOf("fire") }
    var selectedHabitId by remember { mutableLongStateOf(-1L) }
    var style by remember { mutableStateOf(WidgetColors.STYLE_DEFAULT) }
    var customColorArgb by remember { mutableStateOf<Int?>(null) }
    var opacity by remember { mutableFloatStateOf(StreakWidgetPrefs.DEFAULT_OPACITY) }

    LaunchedEffect(Unit) {
        habits.addAll(habitRepository.getAllActiveHabits().first())
        try {
            val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)
            val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)
            selectedDrum = prefs[StreakWidgetPrefs.DRUM_ICON_KEY] ?: "fire"
            selectedHabitId = prefs[StreakWidgetPrefs.HABIT_ID_KEY] ?: -1L
            style = prefs[StreakWidgetPrefs.STYLE_KEY] ?: WidgetColors.STYLE_DEFAULT
            customColorArgb = prefs[StreakWidgetPrefs.CUSTOM_COLOR_KEY]
            opacity = prefs[StreakWidgetPrefs.OPACITY_KEY] ?: StreakWidgetPrefs.DEFAULT_OPACITY
        } catch (_: Exception) { }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Streak Widget",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Your drum",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 80.dp),
            contentPadding = PaddingValues(4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.heightIn(max = 300.dp)
        ) {
            gridItems(StreakWidgetPrefs.DRUM_OPTIONS) { drumKey ->
                val isSelected = drumKey == selectedDrum
                ElevatedCard(
                    onClick = { selectedDrum = drumKey },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary
                                         else MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    elevation = CardDefaults.elevatedCardElevation(
                        defaultElevation = if (isSelected) 4.dp else 1.dp
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Image(
                                painter = painterResource(id = drumDrawableResFor(drumKey)),
                                contentDescription = StreakWidgetPrefs.DRUM_LABELS[drumKey],
                                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                                    if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else Color(WidgetColors.resolveForeground(style, customColorArgb, fallbackColorArgb))
                                ),
                                modifier = Modifier.size(44.dp)
                            )
                            Text(
                                text = StreakWidgetPrefs.DRUM_LABELS[drumKey] ?: drumKey,
                                fontSize = 11.sp,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                            if (isSelected) {
                                Text(
                                    text = "✓",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        Text(
            text = "Track which habit?",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            item {
                HabitPickerRow(
                    name = "Best streak (auto)",
                    isSelected = selectedHabitId == -1L,
                    onClick = { selectedHabitId = -1L }
                )
            }
            items(habits) { habit ->
                HabitPickerRow(
                    name = habit.name,
                    isSelected = habit.id == selectedHabitId,
                    onClick = { selectedHabitId = habit.id }
                )
            }
        }

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
                onClick = { onConfirm(selectedDrum, selectedHabitId, style, customColorArgb, opacity) },
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

@Composable
private fun HabitPickerRow(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = name,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
        },
        leadingContent = {
            RadioButton(selected = isSelected, onClick = onClick)
        },
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
        )
    )
}

private fun drumDrawableResFor(icon: String): Int = when (icon) {
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
