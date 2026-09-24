package com.apagon.rhythm.platform

// Desktop actual: there's no desktop payment rail and no plan to build one
// (Stage 6/9 decision — desktop is unconditionally Pro via ThemePreferences,
// see main.kt), so this exists only to satisfy JournalViewModel's
// constructor dependency. Never expected to actually fire.
class DesktopPurchaseLauncher : PurchaseLauncher {
    override fun launchPurchase(productId: String) {}
}
