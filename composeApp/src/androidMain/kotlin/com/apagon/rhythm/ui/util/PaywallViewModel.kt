package com.apagon.rhythm.ui.util

import androidx.lifecycle.ViewModel
import com.android.billingclient.api.ProductDetails
import com.apagon.rhythm.data.billing.BillingRepository
import kotlinx.coroutines.flow.StateFlow

/** Thin wrapper so [ProPaywallSheet] can read live subscription pricing without every one of its
 * 6 call sites needing to thread [BillingRepository.productDetails] through their own ViewModel. */
class PaywallViewModel constructor(
    billingRepository: BillingRepository
) : ViewModel() {
    val productDetails: StateFlow<Map<String, ProductDetails>> = billingRepository.productDetails
}
