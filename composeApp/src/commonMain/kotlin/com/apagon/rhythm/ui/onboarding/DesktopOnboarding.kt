package com.apagon.rhythm.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.apagon.rhythm.data.preferences.ThemePreferences
import com.apagon.rhythm.ui.components.crystalButtonColors
import com.apagon.rhythm.ui.help.SyncSetupGuideSheet
import com.apagon.rhythm.ui.util.RhythmAlertDialog
import kotlinx.coroutines.launch

/**
 * The desktop's first-launch intro, mirroring Android's OnboardingScreen pages. Android's last page
 * asks for notification permission, which a desktop doesn't need; here it's syncing with the
 * phone, the one thing a new desktop user has to set up. Shown once (hasSeenOnboarding), and again
 * from Settings → Tutorials → Welcome intro.
 */
private data class IntroPage(val title: String, val description: String, val icon: ImageVector, val features: List<String> = emptyList())

private val pages = listOf(
    IntroPage(
        "Everything in one rhythm", "One app for the whole day:", Icons.Default.AddTask,
        listOf("Habits with streaks & checklists", "Notes with templates", "Journal & calendar", "Smart reminders")
    ),
    IntroPage(
        "Make it yours", "Pick the look that fits you:", Icons.Default.Palette,
        listOf(
            "Material 3 — the classic Bori look",
            "Material 3 Expressive — bolder, rounder, punchier",
            "Crystal — frosted glass over a soft colour field",
            "Switch anytime in Settings"
        )
    ),
    IntroPage(
        "Your data, your device",
        "Everything stays on this computer. No cloud, no account, no tracking.",
        Icons.Default.Shield
    ),
    IntroPage(
        "Sync with your phone",
        "Bori on your phone and your computers sync directly with each other over Tailscale, a free private network — nothing goes through a Bori server.",
        Icons.Default.Sync,
        listOf("Set it up now, or any time from Settings → Data Management", "Works at home or away")
    )
)

/** In-memory only: "Replay the intro" must not change the synced hasSeenOnboarding setting. */
object OnboardingReplay {
    var requested by mutableStateOf(false)
}

/** Shows the intro when it hasn't been seen (or a replay was asked for), and the guide after it. */
@Composable
fun DesktopOnboardingHost(themePreferences: ThemePreferences) {
    // null until DataStore answers, so a returning user never sees the intro flash.
    val seen by themePreferences.hasSeenOnboarding.collectAsState(initial = null)
    var showGuide by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val show = seen == false || OnboardingReplay.requested

    fun finish(openGuide: Boolean) {
        OnboardingReplay.requested = false
        if (seen == false) scope.launch { themePreferences.setHasSeenOnboarding(true) }
        showGuide = openGuide
    }

    if (show) DesktopOnboardingDialog(onFinish = ::finish)
    if (showGuide) SyncSetupGuideSheet(onDismiss = { showGuide = false })
}

@Composable
private fun DesktopOnboardingDialog(onFinish: (openGuide: Boolean) -> Unit) {
    val pagerState = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val last = pagerState.currentPage == pages.lastIndex
    RhythmAlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnClickOutside = false, dismissOnBackPress = false),
        maxWidth = 520.dp,
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth().height(330.dp)) { i ->
                    val page = pages[i]
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Box(
                            Modifier.size(72.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(page.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                        }
                        Text(page.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Text(page.description, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        page.features.forEach {
                            Text("•  $it", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth().padding(start = 24.dp))
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    repeat(pages.size) { i ->
                        Box(
                            Modifier.size(8.dp).background(
                                if (i == pagerState.currentPage) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                                CircleShape
                            )
                        )
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { onFinish(false) }) { Text(if (last) "Maybe later" else "Skip") }
        },
        confirmButton = {
            Button(
                colors = crystalButtonColors(),
                onClick = {
                    if (last) onFinish(true)
                    else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            ) { Text(if (last) "Set up sync" else "Next") }
        }
    )
}
