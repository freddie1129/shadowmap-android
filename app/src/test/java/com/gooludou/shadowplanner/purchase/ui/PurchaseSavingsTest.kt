package com.gooludou.shadowplanner.purchase.ui

import com.gooludou.shadowplanner.purchase.model.PricePhase
import com.gooludou.shadowplanner.purchase.model.PriceRecurrence
import com.gooludou.shadowplanner.purchase.model.PurchaseOption
import com.gooludou.shadowplanner.purchase.model.PurchaseOptionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PurchaseSavingsTest {
    @Test
    fun annualSavingsPercent_roundsDownToNearestTenPercent() {
        val monthly = subscription("monthly", "P1M", 10_000_000)
        val annual = subscription("annual", "P1Y", 30_000_000)

        assertEquals(70, annual.annualSavingsPercent(listOf(monthly, annual)))
    }

    @Test
    fun annualSavingsPercent_returnsNullForMonthlyOption() {
        val monthly = subscription("monthly", "P1M", 10_000_000)

        assertNull(monthly.annualSavingsPercent(listOf(monthly)))
    }

    @Test
    fun annualSavingsPercent_returnsNullWhenCurrenciesDiffer() {
        val monthly = subscription("monthly", "P1M", 10_000_000, "USD")
        val annual = subscription("annual", "P1Y", 60_000_000, "AUD")

        assertNull(annual.annualSavingsPercent(listOf(monthly, annual)))
    }

    @Test
    fun annualSavingsPercent_hidesSavingBelowTenPercent() {
        val monthly = subscription("monthly", "P1M", 10_000_000)
        val annual = subscription("annual", "P1Y", 110_000_000)

        assertNull(annual.annualSavingsPercent(listOf(monthly, annual)))
    }

    private fun subscription(
        id: String,
        billingPeriod: String,
        priceAmountMicros: Long,
        currency: String = "USD"
    ) = PurchaseOption(
        id = id,
        productId = "subscription",
        basePlanId = id,
        offerId = null,
        offerToken = "$id-token",
        type = PurchaseOptionType.Subscription,
        productName = "Premium",
        description = "",
        isFeatured = false,
        isBestValue = false,
        pricingPhases = listOf(
            PricePhase(
                formattedPrice = "$10.00",
                priceAmountMicros = priceAmountMicros,
                priceCurrencyCode = currency,
                billingPeriod = billingPeriod,
                recurrenceMode = PriceRecurrence.Infinite,
                billingCycleCount = 0
            )
        )
    )
}
