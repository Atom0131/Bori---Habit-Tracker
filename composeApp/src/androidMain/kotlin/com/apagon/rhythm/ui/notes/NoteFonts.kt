package com.apagon.rhythm.ui.notes

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.apagon.rhythm.R

private val noteFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val NoteInter = FontFamily(
    Font(googleFont = GoogleFont("Inter"), fontProvider = noteFontProvider, weight = FontWeight.Normal),
    Font(googleFont = GoogleFont("Inter"), fontProvider = noteFontProvider, weight = FontWeight.Medium),
    Font(googleFont = GoogleFont("Inter"), fontProvider = noteFontProvider, weight = FontWeight.Bold),
)

private val Lora = FontFamily(
    Font(googleFont = GoogleFont("Lora"), fontProvider = noteFontProvider, weight = FontWeight.Normal),
    Font(googleFont = GoogleFont("Lora"), fontProvider = noteFontProvider, weight = FontWeight.Medium),
    Font(googleFont = GoogleFont("Lora"), fontProvider = noteFontProvider, weight = FontWeight.Bold),
)

private val DmSans = FontFamily(
    Font(googleFont = GoogleFont("DM Sans"), fontProvider = noteFontProvider, weight = FontWeight.Normal),
    Font(googleFont = GoogleFont("DM Sans"), fontProvider = noteFontProvider, weight = FontWeight.Medium),
    Font(googleFont = GoogleFont("DM Sans"), fontProvider = noteFontProvider, weight = FontWeight.Bold),
)

private val RobotoMono = FontFamily(
    Font(googleFont = GoogleFont("Roboto Mono"), fontProvider = noteFontProvider, weight = FontWeight.Normal),
    Font(googleFont = GoogleFont("Roboto Mono"), fontProvider = noteFontProvider, weight = FontWeight.Medium),
)

fun resolveNoteFont(key: String): FontFamily = when (key) {
    "classic" -> Lora
    "round"   -> DmSans
    "mono"    -> RobotoMono
    else      -> NoteInter
}

fun resolveNoteBodySize(key: String): TextUnit = when (key) {
    "small" -> 13.sp
    "large" -> 19.sp
    else    -> 16.sp
}

fun resolveNoteHeaderSize(key: String): TextUnit = when (key) {
    "small" -> 20.sp
    "large" -> 28.sp
    else    -> 24.sp
}
