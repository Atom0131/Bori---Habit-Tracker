package com.apagon.rhythm.platform

/**
 * Starts a Pro purchase flow. Android actual: Play Billing against the
 * foreground activity. iOS (personal build): no-op — the build ships
 * pro-unlocked; StoreKit only becomes relevant for an App Store release.
 */
interface PurchaseLauncher {
    fun launchPurchase(productId: String = PRO_MONTHLY_ID)

    companion object {
        const val PRO_MONTHLY_ID = "pro_monthly_subscription"
        const val PRO_ANNUAL_ID = "pro_annual_subscription"
    }
}
