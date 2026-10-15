package com.apagon.rhythm.platform

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.ptr.IntByReference
import java.awt.Window

/**
 * Colours the Windows title bar to match Bori instead of the system's white bar (Windows report,
 * 2026-10-15). Windows 11 (build 22000+) takes a caption and text colour through
 * `DwmSetWindowAttribute`; Windows 10 only takes the dark-mode switch, which at least turns the
 * bar dark in dark mode. Anything that fails is ignored: the bar just stays as Windows drew it.
 * A no-op off Windows.
 */
object WindowsTitleBar {
    private val isWindows = System.getProperty("os.name").orEmpty().lowercase().contains("windows")

    private interface Dwmapi : Library {
        fun DwmSetWindowAttribute(hwnd: Pointer, attribute: Int, value: IntByReference, size: Int): Int
    }

    private val dwm: Dwmapi? by lazy {
        if (!isWindows) null else runCatching { Native.load("dwmapi", Dwmapi::class.java) }.getOrNull()
    }

    private const val DWMWA_USE_IMMERSIVE_DARK_MODE = 20
    private const val DWMWA_BORDER_COLOR = 34
    private const val DWMWA_CAPTION_COLOR = 35
    private const val DWMWA_TEXT_COLOR = 36

    fun apply(window: Window, caption: Color, text: Color, dark: Boolean) {
        val api = dwm ?: return
        runCatching {
            val hwnd = Native.getWindowPointer(window) ?: return
            fun set(attribute: Int, value: Int) = api.DwmSetWindowAttribute(hwnd, attribute, IntByReference(value), 4)
            set(DWMWA_USE_IMMERSIVE_DARK_MODE, if (dark) 1 else 0)
            set(DWMWA_CAPTION_COLOR, colorRef(caption))
            set(DWMWA_BORDER_COLOR, colorRef(caption))
            set(DWMWA_TEXT_COLOR, colorRef(text))
        }
    }

    /** Win32 COLORREF is 0x00BBGGRR. */
    private fun colorRef(c: Color): Int {
        val argb = c.copy(alpha = 1f).toArgb()
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        return (b shl 16) or (g shl 8) or r
    }
}
