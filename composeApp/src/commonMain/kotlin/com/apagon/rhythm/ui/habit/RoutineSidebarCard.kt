package com.apagon.rhythm.ui.habit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.data.routine.RoutineRunner
import com.apagon.rhythm.data.routine.formatCountdown
import com.apagon.rhythm.ui.components.crystalTileSurface
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

/**
 * The desktop's stand-in for the phone's ongoing routine notification: while a routine runs, a card
 * at the bottom of the sidebar shows its current step and time left on every page. Clicking it opens
 * the player again. Nothing shows otherwise.
 */
@Composable
fun RoutineSidebarCard(onOpen: (habitId: Long) -> Unit, modifier: Modifier = Modifier) {
    val runner = koinInject<RoutineRunner>()
    val run by runner.run.collectAsState()
    val r = run?.takeIf { !it.isFinished } ?: return
    val step = r.currentStep ?: return

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(r.stepEndMillis, r.isPaused) {
        while (true) {
            now = System.currentTimeMillis()
            delay(250)
        }
    }
    val left = r.remainingMs(now)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .crystalTileSurface(fill = MaterialTheme.colorScheme.secondaryContainer)
            .clickable { onOpen(r.habitId) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                r.habitName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                step.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            when {
                r.isPaused -> "Paused"
                left != null -> formatCountdown((left + 999) / 1000)
                else -> "Step ${r.currentIndex + 1}/${r.steps.size}"
            },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
