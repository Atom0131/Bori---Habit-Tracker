package com.apagon.rhythm.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.apagon.rhythm.R

// ── Google Fonts provider (GMS) ───────────────────────────────────────────────
private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage   = "com.google.android.gms",
    certificates      = R.array.com_google_android_gms_fonts_certs
)

// ── Typefaces ─────────────────────────────────────────────────────────────────
val PlusJakartaSans = FontFamily(
    Font(googleFont = GoogleFont("Plus Jakarta Sans"), fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = GoogleFont("Plus Jakarta Sans"), fontProvider = provider, weight = FontWeight.Bold),
)

val Inter = FontFamily(
    Font(googleFont = GoogleFont("Inter"), fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = GoogleFont("Inter"), fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = GoogleFont("Inter"), fontProvider = provider, weight = FontWeight.Bold),
)

// ── Typography Scale ──────────────────────────────────────────────────────────
// Display / Headline → Plus Jakarta Sans (expressive, editorial)
// Title / Body / Label → Inter (utility, legible)

val Typography = Typography(
    // Display — Plus Jakarta Sans
    displayLarge  = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold,     fontSize = 56.sp, lineHeight = 64.sp, letterSpacing = (-0.02).sp),
    displayMedium = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold,     fontSize = 45.sp, lineHeight = 52.sp, letterSpacing = (-0.01).sp),
    displaySmall  = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold,     fontSize = 36.sp, lineHeight = 44.sp, letterSpacing = 0.sp),

    // Headline — Plus Jakarta Sans
    headlineLarge  = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = 0.sp),
    headlineMedium = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = 0.sp),
    headlineSmall  = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = 0.sp),

    // Title — Inter
    titleLarge  = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = 0.sp),
    titleMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.01.sp),
    titleSmall  = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.01.sp),

    // Body — Inter
    bodyLarge  = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.01.sp),
    bodyMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.01.sp),
    bodySmall  = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.01.sp),

    // Label — Inter Bold
    labelLarge  = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.01.sp),
    labelMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.01.sp),
    labelSmall  = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.01.sp),
)
