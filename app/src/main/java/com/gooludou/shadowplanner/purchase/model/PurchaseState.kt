package com.gooludou.shadowplanner.purchase.model

data class InAppPurchaseState(
    val entitlement: EntitlementState = EntitlementState.Checking,
    val options: PurchaseOptionsState = PurchaseOptionsState.Loading,
    val purchaseStatus: PurchaseStatus = PurchaseStatus.Idle,
    val errorMessage: String? = null
)

sealed interface EntitlementState {
    data object Checking : EntitlementState

    data object Free : EntitlementState

    data object Premium : EntitlementState

    data class Unavailable(val message: String) : EntitlementState
}

sealed interface PurchaseOptionsState {
    data object Loading : PurchaseOptionsState

    data class Ready(val options: List<PurchaseOption>) : PurchaseOptionsState

    data class Unavailable(val message: String) : PurchaseOptionsState
}

enum class PurchaseStatus {
    Idle,
    Purchasing,
    Pending,
    Purchased
}

data class PurchaseOption(
    val id: String,
    val productId: String,
    val basePlanId: String?,
    val offerId: String?,
    val offerToken: String,
    val type: PurchaseOptionType,
    val productName: String,
    val description: String,
    val isFeatured: Boolean,
    val isBestValue: Boolean,
    val pricingPhases: List<PricePhase>
)

enum class PurchaseOptionType {
    OneTime,
    Subscription
}

data class PricePhase(
    val formattedPrice: String,
    val priceAmountMicros: Long,
    val priceCurrencyCode: String,
    val billingPeriod: String,
    val recurrenceMode: PriceRecurrence,
    val billingCycleCount: Int
)

enum class PriceRecurrence {
    Infinite,
    Finite,
    NonRecurring
}
