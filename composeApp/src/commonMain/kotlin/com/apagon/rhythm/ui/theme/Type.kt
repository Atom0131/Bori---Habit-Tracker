package com.apagon.rhythm.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import rhythm.composeapp.generated.resources.Res
import rhythm.composeapp.generated.resources.inter_bold
import rhythm.composeapp.generated.resources.inter_medium
import rhythm.composeapp.generated.resources.inter_regular
import rhythm.composeapp.generated.resources.noto_color_emoji
import rhythm.composeapp.generated.resources.plus_jakarta_sans_bold
import rhythm.composeapp.generated.resources.plus_jakarta_sans_semibold

// ── Typefaces ─────────────────────────────────────────────────────────────────
// Bundled via Compose Multiplatform Resources (commonMain/composeResources/font/)
// instead of Android's Google-Fonts-provider — works on every target, not just
// Android, and doesn't depend on GMS being present at runtime.
//
// This Linux environment has no color-emoji system font installed at all
// (`fc-list | grep -i emoji` returns nothing), so every emoji glyph used
// throughout the app (streak flame, notebook, lock/unlock, pin, journal
// feelings, note templates) renders as an invisible tofu box. Rather than
// replace every emoji usage with a vector icon (blocked anyway — see the
// abandoned material-icons-extended attempt: this project's Compose 1.11.x
// stack has no compatible fetchable version under this environment's
// TLS/clock constraints), each font family appends Android Studio's bundled
// NotoColorEmoji.ttf as a fallback: Compose resolves a glyph missing from the
// primary typeface by walking to the next Font in the family, so this fixes
// every emoji everywhere in one place instead of piecemeal.
@Composable
fun emojiFallback(weight: FontWeight): Font = Font(Res.font.noto_color_emoji, weight = weight)

@Composable
fun plusJakartaSansFamily(): FontFamily = FontFamily(
    Font(Res.font.plus_jakarta_sans_semibold, weight = FontWeight.SemiBold),
    Font(Res.font.plus_jakarta_sans_bold, weight = FontWeight.Bold),
    emojiFallback(FontWeight.SemiBold),
    emojiFallback(FontWeight.Bold),
)

@Composable
fun interFamily(): FontFamily = FontFamily(
    Font(Res.font.inter_regular, weight = FontWeight.Normal),
    Font(Res.font.inter_medium, weight = FontWeight.Medium),
    Font(Res.font.inter_bold, weight = FontWeight.Bold),
    emojiFallback(FontWeight.Normal),
    emojiFallback(FontWeight.Medium),
    emojiFallback(FontWeight.Bold),
)

// ── Typography Scale ──────────────────────────────────────────────────────────
// Display / Headline → Plus Jakarta Sans (expressive, editorial)
// Title / Body / Label → Inter (utility, legible)
val Typography: Typography
    @Composable
    get() {
        val plusJakartaSans = plusJakartaSansFamily()
        val inter = interFamily()
        return remember(plusJakartaSans, inter) {
            Typography(
                // Display — Plus Jakarta Sans
                displayLarge  = TextStyle(fontFamily = plusJakartaSans, fontWeight = FontWeight.Bold,     fontSize = 56.sp, lineHeight = 64.sp, letterSpacing = (-0.02).sp),
                displayMedium = TextStyle(fontFamily = plusJakartaSans, fontWeight = FontWeight.Bold,     fontSize = 45.sp, lineHeight = 52.sp, letterSpacing = (-0.01).sp),
                displaySmall  = TextStyle(fontFamily = plusJakartaSans, fontWeight = FontWeight.Bold,     fontSize = 36.sp, lineHeight = 44.sp, letterSpacing = 0.sp),

                // Headline — Plus Jakarta Sans
                headlineLarge  = TextStyle(fontFamily = plusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = 0.sp),
                headlineMedium = TextStyle(fontFamily = plusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = 0.sp),
                headlineSmall  = TextStyle(fontFamily = plusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = 0.sp),

                // Title — Inter
                titleLarge  = TextStyle(fontFamily = inter, fontWeight = FontWeight.Medium, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = 0.sp),
                titleMedium = TextStyle(fontFamily = inter, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.01.sp),
                titleSmall  = TextStyle(fontFamily = inter, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.01.sp),

                // Body — Inter
                bodyLarge  = TextStyle(fontFamily = inter, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.01.sp),
                bodyMedium = TextStyle(fontFamily = inter, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.01.sp),
                bodySmall  = TextStyle(fontFamily = inter, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.01.sp),

                // Label — Inter Bold
                labelLarge  = TextStyle(fontFamily = inter, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.01.sp),
                labelMedium = TextStyle(fontFamily = inter, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.01.sp),
                labelSmall  = TextStyle(fontFamily = inter, fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.01.sp),
            )
        }
    }

/**
 * Material 3 Expressive scale, ported from the Android original. Expressive's character comes from
 * *weight contrast* — big, confident headlines over an unchanged body — not a different typeface.
 *
 * **Deviation from the Android original**: that version escalates the two Display roles and
 * `headlineLarge`/`headlineMedium` to `FontWeight.ExtraBold`, backed by a Google-Fonts-provider
 * `Font` request for that exact weight. This port's fonts are bundled via Compose Multiplatform
 * Resources (see `plusJakartaSansFamily()` above) with only Bold/SemiBold shipped for Plus Jakarta
 * Sans — there is no ExtraBold `.ttf` in `commonMain/composeResources/font/`. Registering
 * `FontWeight.ExtraBold` against a `FontFamily` that has no matching `Font` entry gets *synthesized*
 * (smeared) rather than failing, which is exactly the rendering-fault-that-looks-like-a-cause trap
 * the Android original's own Type.kt warns about for a different weight. Using `FontWeight.Bold`
 * (the heaviest weight actually bundled) avoids that at the cost of slightly less contrast between
 * Expressive and the standard scale than the Android app has. Revisit if an ExtraBold font asset is
 * ever added to this port's resources.
 */
val ExpressiveTypography: Typography
    @Composable
    get() {
        val plusJakartaSans = plusJakartaSansFamily()
        val inter = interFamily()
        return remember(plusJakartaSans, inter) {
            Typography(
                displayLarge  = TextStyle(fontFamily = plusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = (-0.03).sp),
                displayMedium = TextStyle(fontFamily = plusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 45.sp, lineHeight = 52.sp, letterSpacing = (-0.02).sp),
                displaySmall  = TextStyle(fontFamily = plusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 44.sp, letterSpacing = (-0.01).sp),

                headlineLarge  = TextStyle(fontFamily = plusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = 0.sp),
                headlineMedium = TextStyle(fontFamily = plusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = 0.sp),
                headlineSmall  = TextStyle(fontFamily = plusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = 0.sp),

                // Title — Inter, Medium → Bold. Habit names live here, so this is the most visible change.
                titleLarge  = TextStyle(fontFamily = inter, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = 0.sp),
                titleMedium = TextStyle(fontFamily = inter, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.01.sp),
                titleSmall  = TextStyle(fontFamily = inter, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.01.sp),

                // Body/Label — unchanged from the standard scale
                bodyLarge  = TextStyle(fontFamily = inter, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.01.sp),
                bodyMedium = TextStyle(fontFamily = inter, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.01.sp),
                bodySmall  = TextStyle(fontFamily = inter, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.01.sp),

                labelLarge  = TextStyle(fontFamily = inter, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.01.sp),
                labelMedium = TextStyle(fontFamily = inter, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.01.sp),
                labelSmall  = TextStyle(fontFamily = inter, fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.01.sp),
            )
        }
    }
