package com.gooludou.shadowplanner.purchase.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.gooludou.shadowplanner.R
import com.gooludou.shadowplanner.purchase.model.PricePhase
import com.gooludou.shadowplanner.purchase.model.PriceRecurrence
import com.gooludou.shadowplanner.purchase.model.PurchaseOption
import com.gooludou.shadowplanner.purchase.model.PurchaseOptionType

@Composable
internal fun PurchaseOption.planTitle(): String {
    if (type == PurchaseOptionType.OneTime) return productName
    return when (recurringPhase()?.billingPeriod) {
        "P1D" -> stringResource(R.string.daily)
        "P1W" -> stringResource(R.string.weekly)
        "P1M" -> stringResource(R.string.monthly)
        "P1Y" -> stringResource(R.string.yearly)
        else -> productName
    }
}

@Composable
internal fun PurchaseOption.offerDescription(): String? {
    val finitePhases = pricingPhases.filter { it.recurrenceMode == PriceRecurrence.Finite }
    if (finitePhases.isEmpty()) return null
    val descriptions = mutableListOf<String>()
    for (phase in finitePhases) {
        descriptions += if (phase.priceAmountMicros == 0L) {
            stringResource(R.string.free_for, phase.periodDescription())
        } else {
            stringResource(
                R.string.intro_price_for,
                phase.formattedPrice,
                phase.periodDescription()
            )
        }
    }
    return descriptions.joinToString(separator = ", ")
}

@Composable
internal fun PurchaseOption.priceDescription(): String {
    val phase = recurringPhase() ?: pricingPhases.lastOrNull()
        ?: return stringResource(R.string.price_unavailable)
    return if (type == PurchaseOptionType.OneTime) {
        stringResource(R.string.price_once, phase.formattedPrice)
    } else {
        val recurringPrice = stringResource(
            R.string.price_per_period,
            phase.formattedPrice,
            phase.periodDescription()
        )
        if (offerDescription() != null) {
            stringResource(R.string.then_price, recurringPrice)
        } else {
            recurringPrice
        }
    }
}

@Composable
internal fun PurchaseOption.purchaseButtonLabel(): String {
    val price = recurringPhase()?.formattedPrice ?: pricingPhases.lastOrNull()?.formattedPrice
        ?: return stringResource(R.string.continue_label)
    return when {
        pricingPhases.firstOrNull()?.priceAmountMicros == 0L -> {
            stringResource(R.string.start_free_trial)
        }

        type == PurchaseOptionType.OneTime -> stringResource(R.string.buy_for, price)

        else -> stringResource(R.string.subscribe_for, price)
    }
}

private fun PurchaseOption.recurringPhase(): PricePhase? =
    pricingPhases.lastOrNull { it.recurrenceMode == PriceRecurrence.Infinite }

@Composable
private fun PricePhase.periodDescription(): String {
    val match = BILLING_PERIOD_PATTERN.matchEntire(billingPeriod)
        ?: return billingPeriod
    val amount = match.groupValues[1].toIntOrNull()?.coerceAtLeast(1) ?: 1
    val total = amount * billingCycleCount.coerceAtLeast(1)
    val plural = when (match.groupValues[2]) {
        "D" -> R.plurals.billing_days
        "W" -> R.plurals.billing_weeks
        "M" -> R.plurals.billing_months
        "Y" -> R.plurals.billing_years
        else -> null
    }
    return if (plural == null) billingPeriod else pluralStringResource(plural, total, total)
}

private val BILLING_PERIOD_PATTERN = Regex("P(\\d+)([DWMY])")
