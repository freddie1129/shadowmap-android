package com.gooludou.shadowplanner.purchase.billing

import com.gooludou.shadowplanner.purchase.model.PricePhase
import com.gooludou.shadowplanner.purchase.model.PriceRecurrence
import com.gooludou.shadowplanner.purchase.model.PurchaseCatalog
import com.gooludou.shadowplanner.purchase.model.PurchaseProductConfig
import com.gooludou.shadowplanner.purchase.model.PurchaseProductType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PurchaseOptionFlattenerTest {
    @Test
    fun flatten_createsOneTimeAndOneOptionPerBasePlan() {
        val options = PurchaseOptionFlattener.flatten(CATALOG, PRODUCT_DETAILS)

        assertEquals(
            listOf("one_time", "subscription:monthly", "subscription:yearly"),
            options.map {
                it.id
            }
        )
        assertEquals("monthly-trial", options[1].offerId)
        assertEquals(2, options[1].pricingPhases.size)
        assertEquals(null, options[2].offerId)
    }

    @Test
    fun flatten_excludesInactiveProductsAndPlans() {
        val catalog = PurchaseCatalog(
            CATALOG.products.map { product ->
                when (product.productId) {
                    "one_time" -> product.copy(isActive = false)
                    else -> product.copy(isActive = product.basePlanId == "yearly")
                }
            }
        )

        val options = PurchaseOptionFlattener.flatten(catalog, PRODUCT_DETAILS)

        assertEquals(listOf("subscription:yearly"), options.map { it.id })
    }

    @Test
    fun flatten_ignoresUnknownStoreProductsAndMissingPlans() {
        val options = PurchaseOptionFlattener.flatten(
            CATALOG,
            PRODUCT_DETAILS.filterNot { it.productId == "subscription" } +
                StoreProductDetails(
                    productId = "other",
                    type = PurchaseProductType.OTP,
                    name = "Other",
                    description = "Other product"
                )
        )

        assertEquals(1, options.size)
        assertTrue(options.single().id == "one_time")
    }

    private companion object {
        val ONE_TIME_PHASE = PricePhase(
            formattedPrice = "$49.99",
            priceAmountMicros = 49_990_000,
            priceCurrencyCode = "AUD",
            billingPeriod = "",
            recurrenceMode = PriceRecurrence.NonRecurring,
            billingCycleCount = 0
        )
        val TRIAL_PHASE = PricePhase(
            formattedPrice = "$0.00",
            priceAmountMicros = 0,
            priceCurrencyCode = "AUD",
            billingPeriod = "P1W",
            recurrenceMode = PriceRecurrence.Finite,
            billingCycleCount = 1
        )
        val MONTHLY_PHASE = PricePhase(
            formattedPrice = "$23.00",
            priceAmountMicros = 23_000_000,
            priceCurrencyCode = "AUD",
            billingPeriod = "P1M",
            recurrenceMode = PriceRecurrence.Infinite,
            billingCycleCount = 0
        )
        val YEARLY_PHASE = PricePhase(
            formattedPrice = "$180.00",
            priceAmountMicros = 180_000_000,
            priceCurrencyCode = "AUD",
            billingPeriod = "P1Y",
            recurrenceMode = PriceRecurrence.Infinite,
            billingCycleCount = 0
        )
        val CATALOG = PurchaseCatalog(
            listOf(
                PurchaseProductConfig(
                    productId = "one_time",
                    type = PurchaseProductType.OTP,
                    isActive = true,
                    isFeatured = true,
                    isBestValue = false,
                    rank = 0,
                    basePlanId = null
                ),
                PurchaseProductConfig(
                    productId = "subscription",
                    type = PurchaseProductType.SUB,
                    isActive = true,
                    isFeatured = false,
                    isBestValue = false,
                    rank = 1,
                    basePlanId = "monthly"
                ),
                PurchaseProductConfig(
                    productId = "subscription",
                    type = PurchaseProductType.SUB,
                    isActive = true,
                    isFeatured = false,
                    isBestValue = true,
                    rank = 2,
                    basePlanId = "yearly"
                )
            )
        )
        val PRODUCT_DETAILS = listOf(
            StoreProductDetails(
                productId = "one_time",
                type = PurchaseProductType.OTP,
                name = "Lifetime",
                description = "Lifetime premium access",
                oneTimeOffers = listOf(StoreOneTimeOffer("one-time-token", ONE_TIME_PHASE))
            ),
            StoreProductDetails(
                productId = "subscription",
                type = PurchaseProductType.SUB,
                name = "Premium",
                description = "Premium subscription",
                subscriptionOffers = listOf(
                    StoreSubscriptionOffer(
                        basePlanId = "monthly",
                        offerId = null,
                        offerToken = "monthly-base-token",
                        pricingPhases = listOf(MONTHLY_PHASE)
                    ),
                    StoreSubscriptionOffer(
                        basePlanId = "monthly",
                        offerId = "monthly-trial",
                        offerToken = "monthly-trial-token",
                        pricingPhases = listOf(TRIAL_PHASE, MONTHLY_PHASE)
                    ),
                    StoreSubscriptionOffer(
                        basePlanId = "yearly",
                        offerId = null,
                        offerToken = "yearly-base-token",
                        pricingPhases = listOf(YEARLY_PHASE)
                    )
                )
            )
        )
    }
}
