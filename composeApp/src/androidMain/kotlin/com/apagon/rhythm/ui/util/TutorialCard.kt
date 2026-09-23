package com.apagon.rhythm.ui.util

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Small, dismissible "want to learn how to do this?" prompt shown the first time a user reaches
 * a feature. Starts collapsed — just a title + one-line teaser — so it reads as an actual question
 * rather than dumping the full explanation on the user unasked. Tapping "Show me" reveals [bullets]
 * inline (tween, not spring — matches the fix already applied to WeekStripComposable.kt); "Not now"
 * dismisses immediately without ever expanding. Either path calls [onDismiss] once, since after
 * being shown once (in either direction) it shouldn't self-trigger again — the caller persists that
 * via the matching `ThemePreferences.hasSeen*Tutorial` flag.
 *
 * Plain [HabitCard]-based, not a [androidx.compose.material3.ModalBottomSheet], so it can be placed
 * inline on a bare screen or inside an already-open sheet without nesting modals.
 */
@Composable
fun TutorialCard(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    bullets: List<String> = emptyList(),
    onDismiss: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    HabitCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(
                modifier = Modifier.animateContentSize(animationSpec = tween(300, easing = FastOutSlowInEasing)),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (expanded) {
                    bullets.forEach { bullet ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = bullet,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (expanded || bullets.isEmpty()) {
                    MomentumButton(
                        text = "Got it",
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(56.dp)
                    ) {
                        Text("Not now")
                    }
                    MomentumButton(
                        text = "Show me",
                        onClick = { expanded = true },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
