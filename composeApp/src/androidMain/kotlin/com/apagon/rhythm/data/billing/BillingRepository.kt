package com.apagon.rhythm.data.billing

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.android.billingclient.api.*
import com.apagon.rhythm.data.preferences.ThemePreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
class BillingRepository constructor(
    private val context: Context,
    private val themePreferences: ThemePreferences
) : PurchasesUpdatedListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    private val _isBillingReady = MutableStateFlow(false)
    val isBillingReady: StateFlow<Boolean> = _isBillingReady.asStateFlow()

    /** Keyed by product ID (PRO_MONTHLY_ID / PRO_ANNUAL_ID) — populated once on connect so the
     * paywall can show real, live-priced options instead of a hardcoded string. */
    private val _productDetails = MutableStateFlow<Map<String, ProductDetails>>(emptyMap())
    val productDetails: StateFlow<Map<String, ProductDetails>> = _productDetails.asStateFlow()

    // Subscriptions and product IDs
    companion object {
        const val PRO_MONTHLY_ID = "pro_monthly_subscription"
        const val PRO_ANNUAL_ID = "pro_annual_subscription"
        const val VIP_PROMO_OFFER_ID = "3yearoffer"
        const val VIP_PROMO_PASSCODE = "RHY-7K4P-VIP"
    }

    init {
        startConnection()
    }

    private var isConnecting = false

    private fun startConnection() {
        if (isConnecting || billingClient.isReady) return
        isConnecting = true
        try {
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    try {
                        isConnecting = false
                        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            _isBillingReady.value = true
                            queryPurchases()
                            queryProductDetails()
                        }
                    } catch (e: Exception) {
                        _isBillingReady.value = false
                    }
                }

                override fun onBillingServiceDisconnected() {
                    try {
                        isConnecting = false
                        _isBillingReady.value = false
                        startConnection()
                    } catch (e: Exception) { }
                }
            })
        } catch (e: Exception) {
            isConnecting = false
            _isBillingReady.value = false
        }
    }

    fun queryPurchases() {
        if (!billingClient.isReady) return
        try {
            val params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()

            billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    val hasPro = purchases.any { purchase ->
                        (purchase.products.contains(PRO_MONTHLY_ID) || purchase.products.contains(PRO_ANNUAL_ID)) &&
                            purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                    }
                    scope.launch {
                        themePreferences.setIsPro(hasPro)
                    }
                }
            }
        } catch (e: Exception) {
            // billing unavailable — Pro status stays as-is
        }
    }

    /** Queries both subscription products once and caches the result in [productDetails] so the
     * paywall can render real prices immediately, without waiting on a purchase-flow tap. */
    fun queryProductDetails() {
        if (!billingClient.isReady) return
        val queryParams = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(PRO_MONTHLY_ID)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build(),
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(PRO_ANNUAL_ID)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            ))
            .build()

        billingClient.queryProductDetailsAsync(queryParams) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                _productDetails.value = productDetailsList.associateBy { it.productId }
            }
        }
    }

    fun launchBillingFlow(activity: Activity, productId: String = PRO_MONTHLY_ID) {
        if (!billingClient.isReady) {
            startConnection()
            return
        }
        val cached = _productDetails.value[productId]
        if (cached != null) {
            launchFlowWith(activity, cached)
            return
        }
        // Cache not populated yet (e.g. tapped upgrade before the connect-time query returned) — query fresh.
        val queryParams = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(productId)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            ))
            .build()
        billingClient.queryProductDetailsAsync(queryParams) { billingResult, productDetailsList ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) return@queryProductDetailsAsync
            val productDetails = productDetailsList.find { it.productId == productId }
                ?: return@queryProductDetailsAsync
            launchFlowWith(activity, productDetails)
        }
    }

    private fun launchFlowWith(activity: Activity, productDetails: ProductDetails) {
        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: return
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .setOfferToken(offerToken)
                    .build()
            ))
            .build()
        billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    /** Deep link into Play Store's own subscription management page for [productId], so a Pro
     * user can view/cancel their subscription without leaving a trail of app-side cancel logic. */
    fun manageSubscriptionsIntent(productId: String = PRO_ANNUAL_ID): Intent =
        Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/account/subscriptions?sku=$productId&package=${context.packageName}")
        )


    fun launchVipPromoFlow(activity: Activity) {
        if (!billingClient.isReady) {
            startConnection()
            return
        }
        val queryParams = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(PRO_ANNUAL_ID)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            ))
            .build()

        billingClient.queryProductDetailsAsync(queryParams) { billingResult, productDetailsList ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) return@queryProductDetailsAsync
            val productDetails = productDetailsList.firstOrNull() ?: return@queryProductDetailsAsync
            val targetOffer = productDetails.subscriptionOfferDetails
                ?.find { it.offerId == VIP_PROMO_OFFER_ID } ?: return@queryProductDetailsAsync

            val billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .setOfferToken(targetOffer.offerToken)
                        .build()
                ))
                .build()
            billingClient.launchBillingFlow(activity, billingFlowParams)
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        queryPurchases()
                    }
                }
            } else {
                queryPurchases()
            }
        }
    }
}
