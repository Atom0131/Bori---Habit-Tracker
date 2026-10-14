package com.apagon.rhythm.ui.habit

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.data.routine.formatStepDuration
import com.apagon.rhythm.data.routine.isRoutine
import com.apagon.rhythm.data.routine.routineTotalSeconds
import com.apagon.rhythm.ui.util.MomentumButton
import com.apagon.rhythm.core.time.now
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.apagon.rhythm.data.repository.HabitRepository
import com.apagon.rhythm.ui.components.crystalScaffoldColor
import com.apagon.rhythm.ui.components.crystalScaffoldContentColor
import com.apagon.rhythm.ui.components.crystalTopAppBarColors
import com.apagon.rhythm.ui.stats.DesktopHabitStatsContent
import org.koin.compose.koinInject
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons

/**
 * Stage 9's new minimal desktop habit-detail surface — built specifically to
 * host the real Stats content (DesktopHabitStatsContent, ported from
 * androidMain's StatsHabitSheet), which had no desktop consumer before this.
 * A full navigation destination rather than a ModalBottomSheet (matching
 * DesktopHabitScreen's tap-to-navigate rather than the Android app's
 * sheet-over-list), with no edit affordance — desktop has no habit-edit flow
 * at all yet, so onEdit is simply omitted.
 *
 * The ViewModel is built directly (remember(habitId) { ... }) rather than
 * through koinViewModel() — this project has no existing precedent for a
 * parameterized koinViewModel() call, and DesktopHabitDetailViewModel is
 * narrow enough (one repository, no lifecycle-sensitive state) that plain
 * `remember` scoping is sufficient on desktop, which has no configuration
 * changes to survive.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopHabitDetailScreen(habitId: Long, onBack: () -> Unit, onStartRoutine: () -> Unit = {}) {
    val repository = koinInject<HabitRepository>()
    val viewModel = remember(habitId) { DesktopHabitDetailViewModel(habitId, repository) }
    val stat by viewModel.habitStat.collectAsState()
    val items by remember(habitId) { repository.getItemsForHabit(habitId) }.collectAsState(initial = emptyList())

    Scaffold(
        containerColor = crystalScaffoldColor(),
        contentColor = crystalScaffoldContentColor(),
        topBar = {
            TopAppBar(
                title = { Text(stat?.habit?.name ?: "") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
                colors = crystalTopAppBarColors()
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            // As on the phone's habit sheet: a routine's way in, and to its history once it's done.
            if (items.isRoutine()) {
                // Shown when done too, as the way to the routine's history (and to run it again).
                val doneToday = stat?.completedDates?.contains(kotlinx.datetime.LocalDate.now().toString()) == true
                MomentumButton(
                    text = if (doneToday) "Routine & history" else "Start routine · ${formatStepDuration(items.routineTotalSeconds())}",
                    onClick = onStartRoutine,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            stat?.let { DesktopHabitStatsContent(it) }
        }
    }
}
