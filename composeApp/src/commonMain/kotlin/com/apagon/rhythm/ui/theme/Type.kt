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
import rhythm.composeapp.generated.resources.plus_jakarta_sans_bold
import rhythm.composeapp.generated.resources.plus_jakarta_sans_semibold

// ── Typefaces ─────────────────────────────────────────────────────────────────
// Bundled via Compose Multiplatform Resources (commonMain/composeResources/font/)
// instead of Android's Google-Fonts-provider — works on every target, not just
// Android, and doesn't depend on GMS being present at runtime.
@Composable
fun plusJakartaSansFamily(): FontFamily = FontFamily(
    Font(Res.font.plus_jakarta_sans_semibold, weight = FontWeight.SemiBold),
    Font(Res.font.plus_jakarta_sans_bold, weight = FontWeight.Bold),
)

@Composable
fun interFamily(): FontFamily = FontFamily(
    Font(Res.font.inter_regular, weight = FontWeight.Normal),
    Font(Res.font.inter_medium, weight = FontWeight.Medium),
    Font(Res.font.inter_bold, weight = FontWeight.Bold),
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
