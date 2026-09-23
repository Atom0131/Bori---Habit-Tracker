package com.apagon.rhythm.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.BitmapImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionSendBroadcast
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
import androidx.glance.layout.wrapContentWidth
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.apagon.rhythm.R
import com.apagon.rhythm.data.model.Timer
import kotlinx.coroutines.flow.first

class ActiveTimerWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(110.dp, 110.dp),
            DpSize(180.dp, 180.dp),
            DpSize(250.dp, 250.dp),
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
        val ep = WidgetEntryPoint()
        val now = System.currentTimeMillis()
        val activeTimer = ep.timerRepository().getAllTimers().first()
            .firstOrNull { it.deletedAt == null && it.endTimeMillis > now }

        val accentArgb = ep.themePreferences().accentColorArgb.first()
        val accentIndex = ep.themePreferences().accentColorIndex.first()
        val appAccent = WidgetColors.resolveAccent(accentIndex, accentArgb)
        val style = prefs[ActiveTimerWidgetPrefs.STYLE_KEY] ?: WidgetColors.STYLE_DEFAULT
        val customArgb = prefs[ActiveTimerWidgetPrefs.CUSTOM_COLOR_KEY]
        val opacity = prefs[ActiveTimerWidgetPrefs.OPACITY_KEY] ?: ActiveTimerWidgetPrefs.DEFAULT_OPACITY
        val palette = WidgetColors.resolvePalette(context, style, customArgb, appAccent, opacity)

        provideContent { TimerContent(activeTimer, palette) }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        val sampleTimer = Timer(
            label = "Focus Session",
            durationSeconds = 600,
            remainingSeconds = 330,
            endTimeMillis = System.currentTimeMillis() + 330_000L
        )
        provideContent { TimerContent(sampleTimer, WidgetColors.defaultPreviewPalette) }
    }
}

@Composable
private fun TimerContent(timer: Timer?, palette: WidgetColors.Palette) {
    val size = LocalSize.current
    val isLarge = size.width >= 240.dp
    val isMedium = size.width >= 160.dp

    if (timer == null) {
        val context = LocalContext.current
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(palette.cardBackground))
                .cornerRadius(28.dp)
                .clickable(actionStartActivity(openTabIntent(context, "clock"))),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "No active timer",
                    style = TextStyle(
                        color = ColorProvider(Color(palette.onSurfaceVariantArgb)),
                        fontSize = 12.sp
                    )
                )
                Spacer(GlanceModifier.height(4.dp))
                Text(
                    text = "Tap to open Rhythm",
                    style = TextStyle(
                        color = ColorProvider(Color(palette.primaryArgb)),
                        fontSize = 10.sp
                    )
                )
            }
        }
    } else {
        val context = LocalContext.current
        val density = context.resources.displayMetrics.density
        val now = System.currentTimeMillis()
        val remainingSecs = ((timer.endTimeMillis - now) / 1000L).coerceAtLeast(0L)
        val progress = if (timer.durationSeconds > 0) {
            1f - (remainingSecs.toFloat() / timer.durationSeconds.toFloat())
        } else 0f
        val mm = remainingSecs / 60
        val ss = remainingSecs % 60
        val timeText = "%d:%02d".format(mm, ss)

        val arcBoxSize = if (isLarge) 130.dp else if (isMedium) 100.dp else 80.dp
        val bitmapPx = (arcBoxSize.value * density).toInt().coerceAtLeast(80)
        val timeFontSizeSp = if (isLarge) 26f else if (isMedium) 22f else 18f
        val ringBitmap = WidgetColors.progressBitmap(progress, bitmapPx, palette.primaryArgb)
        val typeface = ResourcesCompat.getFont(context, R.font.plus_jakarta_sans_bold) ?: android.graphics.Typeface.DEFAULT_BOLD
        val heroWidthPx = (timeFontSizeSp * density * 2.6f).toInt()
        val heroHeightPx = (timeFontSizeSp * density * 1.3f).toInt()
        val timeBitmap = WidgetColors.textToBitmap(
            timeText, heroWidthPx, heroHeightPx, timeFontSizeSp * density, typeface, palette.primaryArgb
        )

        val pauseIntent = Intent(context, TimerWidgetReceiver::class.java).apply {
            action = TimerWidgetReceiver.ACTION_PAUSE
            putExtra(TimerWidgetReceiver.EXTRA_TIMER_ID, timer.id)
        }
        val cancelIntent = Intent(context, TimerWidgetReceiver::class.java).apply {
            action = TimerWidgetReceiver.ACTION_CANCEL
            putExtra(TimerWidgetReceiver.EXTRA_TIMER_ID, timer.id)
        }

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(palette.cardBackground))
                .cornerRadius(28.dp)
                .clickable(actionStartActivity(openTabIntent(context, "clock"))),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize().padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = GlanceModifier.size(arcBoxSize),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = BitmapImageProvider(ringBitmap),
                        contentDescription = null,
                        modifier = GlanceModifier.fillMaxSize()
                    )
                    Image(
                        provider = BitmapImageProvider(timeBitmap),
                        contentDescription = timeText,
                        modifier = GlanceModifier
                            .width((heroWidthPx / density).dp)
                            .height((heroHeightPx / density).dp)
                    )
                }
                Spacer(GlanceModifier.height(4.dp))
                Text(
                    text = timer.label.ifBlank { "Focus Session" },
                    style = TextStyle(
                        color = ColorProvider(Color(palette.onSurfaceVariantArgb)),
                        fontSize = 10.sp
                    ),
                    maxLines = 1
                )
                Spacer(GlanceModifier.height(6.dp))
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Pause: mint-tinted pill (primary-family fill, high emphasis)
                    Box(
                        modifier = GlanceModifier
                            .wrapContentWidth()
                            .height(32.dp)
                            .background(ColorProvider(Color(palette.primaryArgb).copy(alpha = 0.16f)))
                            .cornerRadius(16.dp)
                            .padding(horizontal = 14.dp)
                            .clickable(actionSendBroadcast(pauseIntent)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Pause",
                            style = TextStyle(
                                color = ColorProvider(Color(palette.primaryArgb)),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    Spacer(GlanceModifier.width(8.dp))
                    // Cancel: neutral gray pill (low emphasis, distinct from Pause)
                    Box(
                        modifier = GlanceModifier
                            .wrapContentWidth()
                            .height(32.dp)
                            .background(ColorProvider(Color(palette.onSurfaceArgb).copy(alpha = 0.08f)))
                            .cornerRadius(16.dp)
                            .padding(horizontal = 14.dp)
                            .clickable(actionSendBroadcast(cancelIntent)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Cancel",
                            style = TextStyle(
                                color = ColorProvider(Color(palette.onSurfaceArgb)),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}

class ActiveTimerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = ActiveTimerWidget()
}
