package com.apagon.rhythm.ui.habit

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.apagon.rhythm.ui.calendar.DesktopTodayCalendarContent
import com.apagon.rhythm.ui.components.crystalScaffoldColor
import com.apagon.rhythm.ui.components.crystalScaffoldContentColor
import com.apagon.rhythm.ui.components.crystalTopAppBarColors
import com.apagon.rhythm.ui.settings.DesktopSettingsViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * Stage 17e: the desktop sidebar's "Today" entry — merges what were three separate sidebar
 * screens (Habits, To-dos, Calendar) into one, mirroring the real Android app exactly: its
 * bottom nav has no separate Habits/To-dos/Calendar tabs at all — MainActivity.kt's "list" ("Today")
 * destination directly composes TodoViewModel state alongside habits, with a calendarMode toggle
 * (the homeViewCalendar setting, already ported to DesktopSettingsViewModel — previously only the
 * Settings screen's own toggle consumed it; this is its first real consumer) swapping in
 * CalendarHabitView. This is the one Scaffold/TopAppBar for both modes; List mode is
 * DesktopTodayListContent (habit sections + a to-do section in one LazyColumn, Stage 15e's
 * habit-detail-as-pane still intact), Calendar mode is DesktopTodayCalendarContent (month grid +
 * day-detail, day-detail now including Reminders — moved off Clock in Stage 17d/17e to match
 * mobile's DayDetailView.kt).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopTodayScreen(settingsViewModel: DesktopSettingsViewModel = koinViewModel()) {
    val calendarMode by settingsViewModel.homeViewCalendar.collectAsState()

    Scaffold(
        containerColor = crystalScaffoldColor(),
        contentColor = crystalScaffoldContentColor(),
        topBar = {
            TopAppBar(
                title = { Text(if (calendarMode) "Calendar" else "Today", fontWeight = FontWeight.Bold) },
                actions = {
                    TextButton(onClick = { settingsViewModel.setHomeViewCalendar(false) }) {
                        Text("List", fontWeight = if (!calendarMode) FontWeight.Bold else FontWeight.Normal)
                    }
                    TextButton(onClick = { settingsViewModel.setHomeViewCalendar(true) }) {
                        Text("Calendar", fontWeight = if (calendarMode) FontWeight.Bold else FontWeight.Normal)
                    }
                },
                colors = crystalTopAppBarColors()
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (calendarMode) {
                DesktopTodayCalendarContent()
            } else {
                DesktopTodayListContent()
            }
        }
    }
}
