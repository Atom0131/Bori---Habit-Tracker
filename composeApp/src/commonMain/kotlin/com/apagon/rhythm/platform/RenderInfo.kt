package com.apagon.rhythm.platform

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * What the window is drawn with, and the frame meter's switch. Filled in by `main()` once the main
 * window has drawn (Skiko's `GraphicsApi` name: DIRECT3D, OPENGL, METAL, ANGLE, SOFTWARE_FAST…), and
 * shown in Settings → About. Added for a choppy-tab-switch report on a Windows gaming laptop
 * (2026-10-14): the .exe has no console, so this is the only way to see it there.
 */
object RenderInfo {
    val api = MutableStateFlow<String?>(null)

    /** Settings → About → Show frame rate. Not saved: a diagnostic, off at every launch. */
    val showFrameMeter = MutableStateFlow(false)

    /** "Direct3D", "OpenGL"…, or the raw name for anything else. */
    fun label(api: String): String = when (api.uppercase()) {
        "DIRECT3D" -> "Direct3D (graphics card)"
        "OPENGL" -> "OpenGL (graphics card)"
        "METAL" -> "Metal (graphics card)"
        "ANGLE" -> "ANGLE (graphics card)"
        "SOFTWARE_FAST", "SOFTWARE_COMPAT", "SOFTWARE", "DIRECT_SOFTWARE" -> "Software (no graphics card)"
        else -> api
    }

    fun isSoftware(api: String?): Boolean = api?.uppercase()?.contains("SOFTWARE") == true
}
