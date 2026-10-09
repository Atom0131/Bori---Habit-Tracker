import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvm("desktop")

    sourceSets {
        // Intermediate source set below desktopMain — kept as its own layer
        // (rather than folded into commonMain) so a future JVM-based target
        // could share it without dragging Ktor into commonMain. See the
        // sync-engine plan (Stage 4).
        val jvmMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                implementation(libs.ktor.server.core)
                implementation(libs.ktor.server.cio)
                implementation(libs.ktor.server.websockets)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.cio)
                implementation(libs.ktor.client.websockets)
                implementation(libs.ktor.serialization.kotlinx.json)
            }
        }

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(libs.kotlinx.coroutines.core)

            // MaterialKolor — seed-color-based M3 color scheme generation.
            // Publishes real per-target variants (jvm/android/iOS/js), so —
            // unlike Ktor in Stage 4a — it doesn't need the jvmMain-only
            // workaround and can be declared here directly.
            implementation(libs.materialkolor)
            api(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)

            // Room (KMP) — entities/DAOs shared across platforms
            api(libs.room.runtime)
            api(libs.sqlite.bundled)

            // DataStore (KMP)
            api(libs.androidx.datastore.preferences.core)

            // Koin — multiplatform DI
            implementation(libs.koin.core)
            implementation(libs.koin.compose.viewmodel)

            // Navigation (JetBrains multiplatform port; same androidx.navigation API)
            implementation(libs.jetbrains.navigation.compose)
        }
        val desktopMain by getting {
            dependsOn(jvmMain)
        }
        desktopMain.dependencies {
            // Everything else (Room KMP, Koin, coroutines-core, DataStore,
            // Ktor) is already visible here via commonMain's/jvmMain's
            // `implementation`/`api` deps — desktopMain sits under jvmMain
            // under commonMain in the source-set hierarchy.
            implementation(compose.desktop.currentOs)

            // Provides Dispatchers.Main backed by the Swing/AWT event thread
            // — Compose Desktop's windowing runs on AWT, same role
            // kotlinx-coroutines-android plays for the Android main looper.
            // Without this, viewModelScope.launch { } (which defaults to
            // Dispatchers.Main.immediate) throws at runtime on desktop.
            implementation(libs.kotlinx.coroutines.swing)

            // Native tray icon on KDE/GNOME-with-AppIndicator (see LinuxStatusNotifierTray).
            implementation(libs.dbus.java.core)
            implementation(libs.dbus.java.transport)

            // QR pairing (Stage 13 follow-up) — pure-Java QR bit-matrix
            // generation, no Android dependency needed for encoding.
            implementation(libs.zxing.core)

            // Crystal's real backdrop blur. The multiplatform `haze` coordinate (not
            // `haze-android`) resolves to the `haze-jvm` artifact for this source set,
            // which ships a real Skia-backed RenderEffect blur for non-Android targets —
            // verified via javap against the actual downloaded jar before wiring this up;
            // DesktopGlassBlur.kt previously assumed this didn't exist and stubbed out to
            // a flat-tint-only fallback permanently.
            implementation(libs.haze)
        }
    }
}

dependencies {
    add("kspDesktop", libs.room.compiler)
}

compose.desktop {
    application {
        mainClass = "com.apagon.rhythm.MainKt"
        // Lets main() set the X11 window class to "bori" (taskbar icon matching via bori.desktop).
        jvmArgs += listOf("--add-opens=java.desktop/sun.awt.X11=ALL-UNNAMED")

        // First public release's installer packaging — no auto-versioning scheme exists yet,
        // bump packageVersion by hand for future releases. Bundles its own JRE via jpackage, so
        // an end user installing the .deb/.rpm needs no separate Java install.
        nativeDistributions {
            targetFormats(TargetFormat.Deb, TargetFormat.Rpm)

            // jlink's default trimmed runtime only includes modules it can see referenced via
            // static jdeps analysis — it missed jdk.unsupported (needed for sun.misc.Unsafe,
            // which DataStore's protobuf implementation reaches via reflection), confirmed live:
            // `rhythm` crashed at launch with NoClassDefFoundError: sun/misc/Unsafe the moment
            // ThemePreferences' DataStore tried to read its file. Rather than hand-picking modules
            // and risking the same class of gap elsewhere (Ktor networking, locale formatting),
            // bundle the full JDK instead of a jlink-trimmed one.
            includeAllModules = true

            packageName = "bori"
            packageVersion = "1.0.0"
            description = "Bori — habit tracker, journal, and planner"
            copyright = "© 2026 Apagon. Licensed under Apache 2.0."
            vendor = "Apagon"

            linux {
                iconFile.set(project.file("../new_icon_assets/bori_app_icon_512.png"))
                menuGroup = "Utility"
                appCategory = "Utility"
            }
        }
    }
}
