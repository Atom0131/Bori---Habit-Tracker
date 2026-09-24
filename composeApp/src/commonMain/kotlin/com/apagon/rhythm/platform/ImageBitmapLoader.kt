package com.apagon.rhythm.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale

/**
 * Renders an image from an absolute file path. Android's real Journal/Notes
 * screens use coil3's AsyncImage directly (content:// URIs) and never touch
 * this — it exists only for the desktop-native Journal screens (Stage 9),
 * which need a decode path that doesn't depend on coil3 (androidMain-only,
 * see composeApp/build.gradle.kts) or a content:// URI scheme (JVM has
 * neither). A DI interface rather than expect/actual: this project's
 * platform shims are already Koin-bound single implementations
 * (PhotoStorage, HapticAlerter, …), and an interface with a @Composable
 * member needs no actual in every KMP target the way expect/actual would —
 * important since iosMain is currently empty and would fail to resolve one.
 */
interface ImageBitmapLoader {
    @Composable
    fun LoadedImage(path: String, modifier: Modifier, contentScale: ContentScale)
}
