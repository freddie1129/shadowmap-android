package com.gooludou.shadowplanner.purchase.billing

import com.gooludou.shadowplanner.purchase.model.PricePhase
import com.gooludou.shadowplanner.purchase.model.PurchaseProductType

data class StoreProductDetails(
    val productId: String,
    val type: PurchaseProductType,
    val name: String,
    val description: String,
    val oneTimeOffers: List<StoreOneTimeOffer> = emptyList(),
    val subscriptionOffers: List<StoreSubscriptionOffer> = emptyList()
)

data class StoreOneTimeOffer(val offerToken: String, val pricePhase: PricePhase)

data class StoreSubscriptionOffer(
    val basePlanId: String,
    val offerId: String?,
    val offerToken: String,
    val pricingPhases: List<PricePhase>
)
