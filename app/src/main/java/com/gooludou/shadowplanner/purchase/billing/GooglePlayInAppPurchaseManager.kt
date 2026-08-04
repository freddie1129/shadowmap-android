package com.gooludou.shadowplanner.purchase.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.gooludou.shadowplanner.R
import com.gooludou.shadowplanner.purchase.model.EntitlementState
import com.gooludou.shadowplanner.purchase.model.InAppPurchaseState
import com.gooludou.shadowplanner.purchase.model.PricePhase
import com.gooludou.shadowplanner.purchase.model.PriceRecurrence
import com.gooludou.shadowplanner.purchase.model.PurchaseCatalog
import com.gooludou.shadowplanner.purchase.model.PurchaseOptionType
import com.gooludou.shadowplanner.purchase.model.PurchaseOptionsState
import com.gooludou.shadowplanner.purchase.model.PurchaseProductType
import com.gooludou.shadowplanner.purchase.model.PurchaseStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
@Suppress("TooManyFunctions")
class GooglePlayInAppPurchaseManager @Inject constructor(
    @ApplicationContext private val context: Context
) : InAppPurchaseManager,
    PurchasesUpdatedListener {
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val connectionMutex = Mutex()
    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .enableAutoServiceReconnection()
        .build()
    private val _state = MutableStateFlow(InAppPurchaseState())
    private var catalog = PurchaseCatalog.EMPTY
    private var productDetailsById = emptyMap<String, ProductDetails>()

    override val state: StateFlow<InAppPurchaseState> = _state.asStateFlow()

    override suspend fun updateCatalog(catalog: PurchaseCatalog) {
        this.catalog = catalog
        _state.update {
            it.copy(
                entitlement = EntitlementState.Checking,
                options = PurchaseOptionsState.Loading,
                errorMessage = null
            )
        }
        if (catalog.products.isEmpty()) {
            productDetailsById = emptyMap()
            _state.value = InAppPurchaseState(
                entitlement = EntitlementState.Unavailable(
                    context.getString(R.string.purchase_products_not_configured)
                ),
                options = PurchaseOptionsState.Unavailable(
                    context.getString(R.string.purchase_products_not_configured)
                )
            )
            return
        }
        val connection = ensureConnected()
        if (connection.responseCode != BillingClient.BillingResponseCode.OK) {
            publishUnavailable()
            return
        }
        refreshProductDetails()
        refreshPurchases()
    }

    @Suppress("ReturnCount")
    override suspend fun refreshPurchases() {
        if (catalog.products.isEmpty()) return
        val connection = ensureConnected()
        if (connection.responseCode != BillingClient.BillingResponseCode.OK) {
            _state.update {
                it.copy(
                    entitlement = EntitlementState.Unavailable(
                        context.getString(R.string.billing_unavailable)
                    ),
                    errorMessage = context.getString(R.string.billing_unavailable)
                )
            }
            return
        }

        val configuredTypes = catalog.products.mapTo(mutableSetOf()) { it.type }
        val results = configuredTypes.map { type -> queryPurchases(type) }
        val failedResult = results.firstOrNull {
            it.first.responseCode !=
                BillingClient.BillingResponseCode.OK
        }
        if (failedResult != null) {
            val message = context.getString(R.string.purchase_check_failed)
            _state.update {
                it.copy(
                    entitlement = EntitlementState.Unavailable(message),
                    errorMessage = message
                )
            }
            return
        }

        val matchingPurchases = results.flatMap { it.second }.filter { purchase ->
            purchase.products.any(catalog.entitlementProductIds::contains)
        }
        val resolution = PurchaseEntitlementResolver.resolve(
            catalog = catalog,
            purchases = matchingPurchases.map { it.toOwnedPurchaseRecord() }
        )
        _state.update {
            it.copy(
                entitlement = resolution.entitlement,
                purchaseStatus = resolution.purchaseStatus,
                errorMessage = null
            )
        }
        matchingPurchases
            .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isAcknowledged }
            .forEach(::acknowledge)
    }

    override fun launchPurchase(activity: Activity, optionId: String) {
        val option = (_state.value.options as? PurchaseOptionsState.Ready)
            ?.options
            ?.firstOrNull { it.id == optionId }
            ?: return
        val productDetails = productDetailsById[option.productId] ?: return
        val detailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .apply {
                if (option.offerToken.isNotEmpty()) setOfferToken(option.offerToken)
            }
            .build()
        _state.update {
            it.copy(purchaseStatus = PurchaseStatus.Purchasing, errorMessage = null)
        }
        val result = billingClient.launchBillingFlow(
            activity,
            BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(detailsParams))
                .build()
        )
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _state.update {
                it.copy(
                    purchaseStatus = PurchaseStatus.Idle,
                    errorMessage = context.getString(R.string.purchase_start_failed)
                )
            }
        }
    }

    override fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> managerScope.launch {
                processPurchaseUpdate(purchases.orEmpty())
            }

            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _state.update { it.copy(purchaseStatus = PurchaseStatus.Idle) }
            }

            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> managerScope.launch {
                refreshPurchases()
            }

            else -> {
                _state.update {
                    it.copy(
                        purchaseStatus = PurchaseStatus.Idle,
                        errorMessage = context.getString(R.string.purchase_failed)
                    )
                }
            }
        }
    }

    private suspend fun processPurchaseUpdate(purchases: List<Purchase>) {
        val configuredIds = catalog.entitlementProductIds
        val matchingPurchases = purchases.filter { purchase ->
            purchase.products.any(configuredIds::contains)
        }
        matchingPurchases
            .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isAcknowledged }
            .forEach(::acknowledge)
        if (matchingPurchases.any { it.purchaseState == Purchase.PurchaseState.PENDING }) {
            _state.update {
                it.copy(purchaseStatus = PurchaseStatus.Pending, errorMessage = null)
            }
        }
        refreshPurchases()
    }

    @Suppress("ReturnCount")
    private suspend fun refreshProductDetails() {
        val activeProducts = catalog.activeBillingProducts
        if (activeProducts.isEmpty()) {
            productDetailsById = emptyMap()
            _state.update { it.copy(options = PurchaseOptionsState.Ready(emptyList())) }
            return
        }
        val results = try {
            activeProducts
                .groupBy { it.type }
                .map { (_, products) ->
                    val params = QueryProductDetailsParams.newBuilder()
                        .setProductList(
                            products.map { product ->
                                QueryProductDetailsParams.Product.newBuilder()
                                    .setProductId(product.productId)
                                    .setProductType(product.type.toBillingProductType())
                                    .build()
                            }
                        )
                        .build()
                    queryProductDetails(params)
                }
        } catch (error: IllegalArgumentException) {
            Log.w(TAG, "Unable to query product details", error)
            val message = context.getString(R.string.purchase_options_load_failed)
            _state.update {
                it.copy(
                    options = PurchaseOptionsState.Unavailable(message),
                    errorMessage = message
                )
            }
            return
        }
        val failedResult = results.firstOrNull {
            it.first.responseCode != BillingClient.BillingResponseCode.OK
        }
        if (failedResult != null) {
            val message = context.getString(R.string.purchase_options_load_failed)
            _state.update {
                it.copy(
                    options = PurchaseOptionsState.Unavailable(message),
                    errorMessage = message
                )
            }
            return
        }
        val productDetails = results.flatMap { it.second }
        productDetailsById = productDetails.associateBy(ProductDetails::getProductId)
        val snapshots = productDetails.mapNotNull(::toStoreProductDetails)
        val options = PurchaseOptionFlattener.flatten(catalog, snapshots)
        _state.update { it.copy(options = PurchaseOptionsState.Ready(options)) }
    }

    private companion object {
        const val TAG = "GooglePlayBilling"
    }

    private suspend fun ensureConnected(): BillingResult = connectionMutex.withLock {
        if (billingClient.isReady) return@withLock okBillingResult()
        suspendCancellableCoroutine { continuation ->
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (continuation.isActive) continuation.resume(billingResult)
                }

                override fun onBillingServiceDisconnected() = Unit
            })
        }
    }

    private suspend fun queryProductDetails(
        params: QueryProductDetailsParams
    ): Pair<BillingResult, List<ProductDetails>> = suspendCancellableCoroutine { continuation ->
        billingClient.queryProductDetailsAsync(params) { billingResult, queryResult ->
            if (continuation.isActive) {
                continuation.resume(billingResult to queryResult.productDetailsList)
            }
        }
    }

    private suspend fun queryPurchases(
        type: PurchaseProductType
    ): Pair<BillingResult, List<Purchase>> = suspendCancellableCoroutine { continuation ->
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(type.toBillingProductType())
            .apply {
                if (type == PurchaseProductType.SUB) includeSuspendedSubscriptions(true)
            }
            .build()
        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (continuation.isActive) continuation.resume(billingResult to purchases)
        }
    }

    private fun acknowledge(purchase: Purchase) {
        billingClient.acknowledgePurchase(
            AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
        ) { billingResult ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                _state.update {
                    it.copy(
                        errorMessage = context.getString(
                            R.string.purchase_acknowledgement_failed
                        )
                    )
                }
            }
        }
    }

    private fun toStoreProductDetails(details: ProductDetails): StoreProductDetails? {
        val config = catalog.products.firstOrNull { it.productId == details.productId }
            ?: return null
        return StoreProductDetails(
            productId = details.productId,
            type = config.type,
            name = details.name,
            description = details.description,
            oneTimeOffers = details.oneTimePurchaseOfferDetailsList.orEmpty().map { offer ->
                StoreOneTimeOffer(
                    offerToken = offer.offerToken.orEmpty(),
                    pricePhase = PricePhase(
                        formattedPrice = offer.formattedPrice,
                        priceAmountMicros = offer.priceAmountMicros,
                        priceCurrencyCode = offer.priceCurrencyCode,
                        billingPeriod = "",
                        recurrenceMode = PriceRecurrence.NonRecurring,
                        billingCycleCount = 0
                    )
                )
            },
            subscriptionOffers = details.subscriptionOfferDetails.orEmpty().map { offer ->
                StoreSubscriptionOffer(
                    basePlanId = offer.basePlanId,
                    offerId = offer.offerId,
                    offerToken = offer.offerToken,
                    pricingPhases = offer.pricingPhases.pricingPhaseList.map { phase ->
                        PricePhase(
                            formattedPrice = phase.formattedPrice,
                            priceAmountMicros = phase.priceAmountMicros,
                            priceCurrencyCode = phase.priceCurrencyCode,
                            billingPeriod = phase.billingPeriod,
                            recurrenceMode = phase.recurrenceMode.toPriceRecurrence(),
                            billingCycleCount = phase.billingCycleCount
                        )
                    }
                )
            }
        )
    }

    private fun Int.toPriceRecurrence(): PriceRecurrence = when (this) {
        ProductDetails.RecurrenceMode.INFINITE_RECURRING -> PriceRecurrence.Infinite
        ProductDetails.RecurrenceMode.FINITE_RECURRING -> PriceRecurrence.Finite
        else -> PriceRecurrence.NonRecurring
    }

    private fun Purchase.toOwnedPurchaseRecord(): OwnedPurchaseRecord = OwnedPurchaseRecord(
        productIds = products,
        state = when (purchaseState) {
            Purchase.PurchaseState.PURCHASED -> OwnedPurchaseState.Purchased
            Purchase.PurchaseState.PENDING -> OwnedPurchaseState.Pending
            else -> OwnedPurchaseState.Other
        },
        isSuspended = isSuspended,
        isAcknowledged = isAcknowledged
    )

    private fun PurchaseProductType.toBillingProductType(): String = when (this) {
        PurchaseProductType.OTP -> BillingClient.ProductType.INAPP
        PurchaseProductType.SUB -> BillingClient.ProductType.SUBS
    }

    private fun publishUnavailable() {
        val message = context.getString(R.string.billing_unavailable)
        _state.value = InAppPurchaseState(
            entitlement = EntitlementState.Unavailable(message),
            options = PurchaseOptionsState.Unavailable(message),
            errorMessage = message
        )
    }

    private fun okBillingResult(): BillingResult = BillingResult.newBuilder()
        .setResponseCode(BillingClient.BillingResponseCode.OK)
        .build()
}
