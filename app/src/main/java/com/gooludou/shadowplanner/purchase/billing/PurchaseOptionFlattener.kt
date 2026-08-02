package com.gooludou.shadowplanner.purchase.billing

import com.gooludou.shadowplanner.purchase.model.PurchaseCatalog
import com.gooludou.shadowplanner.purchase.model.PurchaseOption
import com.gooludou.shadowplanner.purchase.model.PurchaseOptionType
import com.gooludou.shadowplanner.purchase.model.PurchaseProductConfig
import com.gooludou.shadowplanner.purchase.model.PurchaseProductType

object PurchaseOptionFlattener {
    fun flatten(
        catalog: PurchaseCatalog,
        productDetails: List<StoreProductDetails>
    ): List<PurchaseOption> {
        val detailsByKey = productDetails.associateBy { it.productId to it.type }
        return catalog.activeProducts.mapNotNull { config ->
            val details = detailsByKey[config.productId to config.type]
                ?: return@mapNotNull null
            when (config.type) {
                PurchaseProductType.OTP -> flattenOneTime(config, details)
                PurchaseProductType.SUB -> flattenSubscription(config, details)
            }
        }
    }

    private fun flattenOneTime(
        config: PurchaseProductConfig,
        details: StoreProductDetails
    ): PurchaseOption? {
        val offer = details.oneTimeOffers.firstOrNull() ?: return null
        return PurchaseOption(
            id = config.productId,
            productId = config.productId,
            basePlanId = null,
            offerId = null,
            offerToken = offer.offerToken,
            type = PurchaseOptionType.OneTime,
            productName = details.name,
            description = details.description,
            isFeatured = config.isFeatured,
            isBestValue = config.isBestValue,
            pricingPhases = listOf(offer.pricePhase)
        )
    }

    @Suppress("ReturnCount")
    private fun flattenSubscription(
        config: PurchaseProductConfig,
        details: StoreProductDetails
    ): PurchaseOption? {
        val basePlanId = config.basePlanId ?: return null
        val eligibleOffers = details.subscriptionOffers.filter {
            it.basePlanId == basePlanId
        }
        val selectedOffer = eligibleOffers.firstOrNull { it.offerId != null }
            ?: eligibleOffers.firstOrNull { it.offerId == null }
            ?: return null
        return PurchaseOption(
            id = "${config.productId}:$basePlanId",
            productId = config.productId,
            basePlanId = basePlanId,
            offerId = selectedOffer.offerId,
            offerToken = selectedOffer.offerToken,
            type = PurchaseOptionType.Subscription,
            productName = details.name,
            description = details.description,
            isFeatured = config.isFeatured,
            isBestValue = config.isBestValue,
            pricingPhases = selectedOffer.pricingPhases
        )
    }
}
