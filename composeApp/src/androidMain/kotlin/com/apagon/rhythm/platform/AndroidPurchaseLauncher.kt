package com.apagon.rhythm.platform

import android.app.Activity
import com.apagon.rhythm.data.billing.BillingRepository
import java.lang.ref.WeakReference

/**
 * Play Billing purchase flows need the foreground Activity; MainActivity
 * attaches itself in onCreate and detaches in onDestroy.
 */
class AndroidPurchaseLauncher(private val billing: BillingRepository) : PurchaseLauncher {

    private var activityRef: WeakReference<Activity>? = null

    fun attach(activity: Activity) {
        activityRef = WeakReference(activity)
    }

    fun detach(activity: Activity) {
        if (activityRef?.get() === activity) activityRef = null
    }

    override fun launchPurchase(productId: String) {
        val activity = activityRef?.get() ?: return
        billing.launchBillingFlow(activity, productId)
    }
}
