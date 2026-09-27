package com.apagon.rhythm.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.apagon.rhythm.data.preferences.CrystalMesh

/**
 * The fixed palette behind Crystal's ten mesh fields — cool, neutral surfaces, ported from the
 * Android app's `ui/theme/CrystalColor.kt`. Neutrals and mesh blob colours only; accent roles are
 * derived from the user's accent colour separately (see `Theme.kt`).
 *
 * Every field beyond Aurora reuses Aurora's per-slot tones exactly and only varies hue (and, for
 * Mist/Dune/Graphite, chroma) — this is a safety invariant enforced upstream by
 * `scripts/verify_mesh_palettes.py`, not a stylistic choice. See the Stage 14 plan doc
 * (`ref_notes/plan_2026-09-26_stage14_crystal_theme_port.md`) invariant #8 before editing any value
 * here.
 */

// ---- Light neutrals ----------------------------------------------------------------------------
val CoolWhite = Color(0xFFF6F7F9)
val CoolSurfaceLight = Color(0xFFFFFFFF)
val CoolSurfaceVariantLight = Color(0xFFE9EBEF)
val InkLight = Color(0xFF1C1C1E)
val InkMutedLight = Color(0xFF45484F)
val OutlineLight = Color(0xFFC8CCD2)
val OutlineVariantLight = Color(0xFFDDE0E5)
val SlateSecondaryLight = Color(0xFF5A6472)

// ---- Dark neutrals ------------------------------------------------------------------------------
val InkBackgroundDark = Color(0xFF0B0B0D)
val CoolSurfaceDark = Color(0xFF1C1C1E)
val CoolSurfaceVariantDark = Color(0xFF2C2C2E)
val InkDark = Color(0xFFF2F3F7)
val InkMutedDark = Color(0xFFD0D6E0)
val OutlineDark = Color(0xFF56575A)
val OutlineVariantDark = Color(0xFF48494C)
val SlateSecondaryDark = Color(0xFFAAB2C0)

// ---- Ambient mesh (Aurora, the default field) ---------------------------------------------------
val AmbientBaseLight = Color(0xFFEAF0F6)
val AmbientBaseDark = Color(0xFF242732)

val CrystalSolidLight = Color(0xFFDFE6F0)

val AmbientBlobsLight = listOf(
    Color(0xFF74D0C4), Color(0xFF7FB4F5), Color(0xFF8E9BF0), Color(0xFFC49BEA), Color(0xFFF6B79E)
)
val AmbientBlobsDark = listOf(
    Color(0xFF4F96A3), Color(0xFF5477B5), Color(0xFF5A51A3), Color(0xFF785195), Color(0xFF9F5F64)
)

// ---- The other nine fields ----------------------------------------------------------------------
val AmbientBlobsEmberLight = listOf(
    Color(0xFFE7B78D), Color(0xFFF29A8C), Color(0xFFE58498), Color(0xFFE094CA), Color(0xFFE3C090)
)
val AmbientBlobsEmberDark = listOf(
    Color(0xFFA58569), Color(0xFFAB635C), Color(0xFF8F4451), Color(0xFF82517D), Color(0xFF886D46)
)

val AmbientBlobsVerdantLight = listOf(
    Color(0xFF97CDA2), Color(0xFF86BE85), Color(0xFF6DB068), Color(0xFF47C095), Color(0xFFA9D0A0)
)
val AmbientBlobsVerdantDark = listOf(
    Color(0xFF6E9476), Color(0xFF4D824E), Color(0xFF316730), Color(0xFF336C56), Color(0xFF577950)
)

val AmbientBlobsRoseLight = listOf(
    Color(0xFFF2ADCB), Color(0xFFE09BCE), Color(0xFFC38CD8), Color(0xFFF18FA8), Color(0xFFF1B3D7)
)
val AmbientBlobsRoseDark = listOf(
    Color(0xFFAE7D91), Color(0xFFA26190), Color(0xFF7F4491), Color(0xFFA0415B), Color(0xFF96617E)
)

val AmbientBlobsMistLight = listOf(
    Color(0xFF82CEC4), Color(0xFF8AB3EB), Color(0xFF929CE4), Color(0xFFC09EE1), Color(0xFFF0B9A4)
)
val AmbientBlobsMistDark = listOf(
    Color(0xFF5B949F), Color(0xFF5B77AC), Color(0xFF5D558C), Color(0xFF75558A), Color(0xFF986266)
)

val AmbientBlobsTideLight = listOf(
    Color(0xFF73D0C5), Color(0xFF4BC1C6), Color(0xFF45AEC2), Color(0xFF4CB8DE), Color(0xFF7BD4CF)
)
val AmbientBlobsTideDark = listOf(
    Color(0xFF56978F), Color(0xFF158487), Color(0xFF156572), Color(0xFF136A83), Color(0xFF1C7E7A)
)

val AmbientBlobsIndigoLight = listOf(
    Color(0xFF84C8F6), Color(0xFF74B6F5), Color(0xFF70A1F4), Color(0xFFA0A5F7), Color(0xFF94CBFA)
)
val AmbientBlobsIndigoDark = listOf(
    Color(0xFF6491B0), Color(0xFF317CB5), Color(0xFF155DA5), Color(0xFF515CA3), Color(0xFF3F769E)
)

val AmbientBlobsOrchidLight = listOf(
    Color(0xFFB1BEF6), Color(0xFFB4A8EC), Color(0xFFBA8FE0), Color(0xFFDA94DA), Color(0xFFC2C0F7)
)
val AmbientBlobsOrchidDark = listOf(
    Color(0xFF828AB2), Color(0xFF796FAC), Color(0xFF734999), Color(0xFF884B88), Color(0xFF6C6D9B)
)

val AmbientBlobsDuneLight = listOf(
    Color(0xFFDEB8AE), Color(0xFFCFA995), Color(0xFFBE9A78), Color(0xFFC2A881), Color(0xFFE1BEB0)
)
val AmbientBlobsDuneDark = listOf(
    Color(0xFFA1867F), Color(0xFF91705E), Color(0xFF755636), Color(0xFF745E3C), Color(0xFF896A5E)
)

val AmbientBlobsGraphiteLight = listOf(
    Color(0xFFB5C3C3), Color(0xFFA3B3BC), Color(0xFF9BA0B2), Color(0xFFADAAB8), Color(0xFFD1C2C0)
)
val AmbientBlobsGraphiteDark = listOf(
    Color(0xFF838E8E), Color(0xFF6C7980), Color(0xFF575C6B), Color(0xFF63606D), Color(0xFF7C6E6C)
)

/**
 * The accent that ships with each field, as an index into `habitColorPalette`. A starting point
 * only — the accent picker overrides it and nothing re-applies this afterwards. See the Android
 * original for the per-field hue-matching rationale.
 */
fun crystalMeshAccent(mesh: CrystalMesh): Int = when (mesh) {
    CrystalMesh.AURORA -> 0
    CrystalMesh.EMBER -> 4
    CrystalMesh.VERDANT -> 1
    CrystalMesh.MIST -> 2
    CrystalMesh.ROSE -> 0
    CrystalMesh.TIDE -> 1
    CrystalMesh.INDIGO -> 2
    CrystalMesh.ORCHID -> 0
    CrystalMesh.DUNE -> 4
    CrystalMesh.GRAPHITE -> 0
    CrystalMesh.CUSTOM -> 0
}

/**
 * The literal blob list for [mesh] — the ten shipped fields only. `CrystalMesh.CUSTOM` is solved at
 * runtime by `crystalCustomBlobs` (`CrystalCustomMesh.kt`); the composable wrapper that picks
 * between the two by reading the custom seed from composition (`crystalFieldBlobs` in the Android
 * original) is deferred to the phase that ports `ThemedRoot`'s CompositionLocals.
 */
fun ambientBlobs(mesh: CrystalMesh, dark: Boolean): List<Color> = when (mesh) {
    CrystalMesh.AURORA -> if (dark) AmbientBlobsDark else AmbientBlobsLight
    CrystalMesh.EMBER -> if (dark) AmbientBlobsEmberDark else AmbientBlobsEmberLight
    CrystalMesh.VERDANT -> if (dark) AmbientBlobsVerdantDark else AmbientBlobsVerdantLight
    CrystalMesh.MIST -> if (dark) AmbientBlobsMistDark else AmbientBlobsMistLight
    CrystalMesh.ROSE -> if (dark) AmbientBlobsRoseDark else AmbientBlobsRoseLight
    CrystalMesh.TIDE -> if (dark) AmbientBlobsTideDark else AmbientBlobsTideLight
    CrystalMesh.INDIGO -> if (dark) AmbientBlobsIndigoDark else AmbientBlobsIndigoLight
    CrystalMesh.ORCHID -> if (dark) AmbientBlobsOrchidDark else AmbientBlobsOrchidLight
    CrystalMesh.DUNE -> if (dark) AmbientBlobsDuneDark else AmbientBlobsDuneLight
    CrystalMesh.GRAPHITE -> if (dark) AmbientBlobsGraphiteDark else AmbientBlobsGraphiteLight
    CrystalMesh.CUSTOM -> if (dark) AmbientBlobsDark else AmbientBlobsLight
}

/**
 * The blob list actually in effect: [mesh]'s literal palette, or (for [CrystalMesh.CUSTOM]) the
 * runtime-solved one from `crystalCustomBlobs`, reading the custom seed/chroma from the
 * CompositionLocals `Theme.kt` provides. Ported now that those locals exist (Stage 14 Phase 4).
 */
@Composable
@ReadOnlyComposable
fun crystalFieldBlobs(mesh: CrystalMesh, dark: Boolean): List<Color> =
    if (mesh == CrystalMesh.CUSTOM) {
        crystalCustomBlobs(LocalCrystalCustomSeed.current, LocalCrystalCustomChroma.current, dark)
    } else {
        ambientBlobs(mesh, dark)
    }
