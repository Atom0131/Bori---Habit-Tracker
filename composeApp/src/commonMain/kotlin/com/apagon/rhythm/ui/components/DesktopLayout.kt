package com.apagon.rhythm.ui.components

import androidx.compose.ui.unit.dp

/**
 * Stage 16: shared spacing/sizing constants for the desktop app's primary screens. Before this,
 * every `Desktop*Screen.kt` hand-wrote its own `16.dp`/`12.dp`/`8.dp`/`4.dp` literals for the same
 * visual roles (screen outer margin, card content padding, section/item spacing), which is how
 * they drifted apart across separate porting/layout sessions — Journal's search row ended up at
 * 4dp internal padding while every other "input row" card used 12dp, Calendar's outer margin
 * ended up at 4-8dp while every other screen used 16dp, and only two of seven screens got a
 * centered content-width cap. Values here are the values that were *already* the majority
 * convention across the app — this is a normalization pass, not a redesign.
 */
object DesktopLayout {
    /** Outer margin every screen's content sits inside. */
    val screenPadding = 16.dp

    /** Content padding for a primary `crystalCardSurface()` card (stat cards, notebook cards,
     * section cards). */
    val cardPadding = 16.dp

    /** Content padding for a compact "input row + button" card (New habit, New to-do, a search
     * field row) — visually a lighter-weight card than [cardPadding]'s full sections. */
    val compactCardPadding = 12.dp

    /** Centered content-width cap applied to every primary list/form screen so a wide window
     * doesn't stretch cards edge-to-edge. Notes' adaptive notebook grid is the deliberate
     * exception — a grid, not a list, so capping it would just reduce it to 1-2 columns. */
    val contentMaxWidth = 720.dp

    /** Vertical gap between stacked sections within a screen. */
    val sectionSpacing = 16.dp

    /** Vertical gap between stacked rows within a list or section. */
    val itemSpacing = 8.dp
}
