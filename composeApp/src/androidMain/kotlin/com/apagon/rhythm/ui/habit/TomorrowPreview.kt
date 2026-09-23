package com.apagon.rhythm.ui.habit

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.ui.stats.HabitProgressCard
import com.apagon.rhythm.ui.stats.HabitYearStats

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YourProgressSheet(
    habitStats: List<HabitYearStats>,
    onDismiss: () -> Unit,
    onStatClick: (HabitYearStats) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Your Progress",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(habitStats, key = { "progress_${it.habit.id}" }) { stat ->
                    HabitProgressCard(
                        habitStat = stat,
                        onClick = { onStatClick(stat) }
                    )
                }
                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
fun LazyListScope.statsSection(
    habitStats: List<HabitYearStats>,
    onStatClick: (HabitYearStats) -> Unit
) {
    if (habitStats.isEmpty()) return
    stickyHeader(key = "your_progress_header") {
        Text(
            text = "Your Progress",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
    items(habitStats, key = { "progress_${it.habit.id}" }) { stat ->
        HabitProgressCard(
            habitStat = stat,
            modifier = Modifier.padding(horizontal = 16.dp),
            onClick = { onStatClick(stat) }
        )
    }
    item(key = "progress_bottom_spacer") {
        Spacer(Modifier.height(80.dp))
    }
}
