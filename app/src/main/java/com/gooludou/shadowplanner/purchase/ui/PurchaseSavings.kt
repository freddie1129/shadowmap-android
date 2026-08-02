package com.gooludou.shadowplanner.purchase.ui

import com.gooludou.shadowplanner.purchase.model.PricePhase
import com.gooludou.shadowplanner.purchase.model.PriceRecurrence
import com.gooludou.shadowplanner.purchase.model.PurchaseOption
import com.gooludou.shadowplanner.purchase.model.PurchaseOptionType

internal fun PurchaseOption.annualSavingsPercent(options: List<PurchaseOption>): Int? =
    recurringPhaseForSavings()
        ?.takeIf {
            type == PurchaseOptionType.Subscription &&
                it.billingPeriod == ANNUAL_PERIOD &&
                it.priceAmountMicros > 0L
        }
        ?.let { annualPhase ->
            options
                .asSequence()
                .filter {
                    it.type == PurchaseOptionType.Subscription && it.productId == productId
                }
                .mapNotNull { it.recurringPhaseForSavings() }
                .firstOrNull {
                    it.billingPeriod == MONTHLY_PERIOD &&
                        it.priceCurrencyCode == annualPhase.priceCurrencyCode &&
                        it.priceAmountMicros > 0L
                }
                ?.let { monthlyPhase -> calculateSavingsPercent(monthlyPhase, annualPhase) }
        }

private fun calculateSavingsPercent(monthlyPhase: PricePhase, annualPhase: PricePhase): Int? {
    val monthlyAnnualCost = monthlyPhase.priceAmountMicros.toDouble() * MONTHS_PER_YEAR
    val savingRatio = (monthlyAnnualCost - annualPhase.priceAmountMicros) / monthlyAnnualCost
    val roundedDownPercent = (savingRatio * PERCENT / ROUNDING_STEP).toInt() * ROUNDING_STEP
    return roundedDownPercent.takeIf { it >= ROUNDING_STEP }
}

private fun PurchaseOption.recurringPhaseForSavings(): PricePhase? =
    pricingPhases.lastOrNull { it.recurrenceMode == PriceRecurrence.Infinite }

private const val MONTHLY_PERIOD = "P1M"
private const val ANNUAL_PERIOD = "P1Y"
private const val MONTHS_PER_YEAR = 12
private const val PERCENT = 100
private const val ROUNDING_STEP = 10
