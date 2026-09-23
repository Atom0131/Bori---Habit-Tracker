import java.util.Properties
import java.io.FileInputStream
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.ksp)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    iosArm64()
    iosSimulatorArm64()

    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    jvm("desktop")

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(libs.kotlinx.coroutines.core)
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
        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.core.splashscreen)
            implementation(libs.androidx.lifecycle.runtime.ktx)
            implementation(libs.androidx.activity.compose)
            implementation(project.dependencies.platform(libs.androidx.compose.bom))
            implementation(libs.androidx.compose.ui)
            implementation(libs.androidx.compose.ui.graphics)
            implementation(libs.androidx.compose.ui.tooling.preview)
            implementation(libs.androidx.compose.material3)
            implementation(libs.androidx.compose.material3.windowsizeclass)
            implementation(libs.androidx.compose.material.icons.core)
            implementation(libs.androidx.compose.material.icons.extended)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.biometric)
            implementation(libs.androidx.fragment.ktx)

            // Room (Android extras)
            implementation(libs.room.ktx)

            // Koin Android integration
            implementation(libs.koin.android)

            // Coil 3 — image loading (Android-only for now; journal photos use
            // content:// URIs, so iOS needs its own photo storage in Phase 4/5)
            implementation(libs.coil.compose)

            // Coroutines
            implementation(libs.kotlinx.coroutines.android)

            // DataStore
            implementation(libs.androidx.datastore.preferences)

            // MaterialKolor — seed-color-based M3 color scheme generation
            implementation(libs.materialkolor)

            // Google Play Billing
            implementation(libs.google.billing)
            implementation(libs.google.billing.ktx)

            // Google Fonts for Compose (Plus Jakarta Sans, Inter)
            implementation(libs.androidx.compose.ui.text.google.fonts)

            // Glance — home screen widgets
            implementation(libs.glance.appwidget)
            implementation(libs.glance.material3)

            // WorkManager — periodic widget refresh
            implementation(libs.work.runtime.ktx)
        }
        val desktopMain by getting
        desktopMain.dependencies {
            // Everything else (Room KMP, Koin, coroutines-core, DataStore) is
            // already visible here via commonMain's `implementation`/`api`
            // deps — desktopMain sits under commonMain in the source-set
            // hierarchy. This is the one genuinely desktop-only addition.
            implementation(compose.desktop.currentOs)
        }
    }
}

android {
    namespace = "com.apagon.rhythm"
    compileSdk = 36

    val keystorePropertiesFile = rootProject.file("local.properties")
    val keystoreProperties = Properties()
    if (keystorePropertiesFile.exists()) {
        keystoreProperties.load(FileInputStream(keystorePropertiesFile))
    }

    signingConfigs {
        create("release") {
            storeFile = keystoreProperties["release.storeFile"]?.let { file(it.toString()) }
            storePassword = keystoreProperties["release.storePassword"] as String?
            keyAlias = keystoreProperties["release.keyAlias"] as String?
            keyPassword = keystoreProperties["release.keyPassword"] as String?
        }
    }

    defaultConfig {
        // Deliberately distinct from the live Android app's applicationId
        // (com.apagon.rhythm) so the dormant androidTarget here can never
        // collide with a real install of production Rhythm on the same phone.
        applicationId = "com.apagon.rhythm.desktop"
        minSdk = 24
        targetSdk = 35
        versionCode = 74
        versionName = "1.74.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    add("kspAndroid", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
    add("kspDesktop", libs.room.compiler)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

compose.desktop {
    application {
        mainClass = "com.apagon.rhythm.MainKt"
    }
}
