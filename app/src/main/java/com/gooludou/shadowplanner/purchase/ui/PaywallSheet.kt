package com.gooludou.shadowplanner.purchase.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gooludou.shadowplanner.R
import com.gooludou.shadowplanner.core.ui.theme.ShadowMapDesign
import com.gooludou.shadowplanner.core.ui.theme.ShadowMapTheme
import com.gooludou.shadowplanner.purchase.model.InAppPurchaseState
import com.gooludou.shadowplanner.purchase.model.PricePhase
import com.gooludou.shadowplanner.purchase.model.PriceRecurrence
import com.gooludou.shadowplanner.purchase.model.PurchaseOption
import com.gooludou.shadowplanner.purchase.model.PurchaseOptionType
import com.gooludou.shadowplanner.purchase.model.PurchaseOptionsState
import com.gooludou.shadowplanner.purchase.model.PurchaseStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallSheet(
    state: InAppPurchaseState,
    onDismiss: () -> Unit,
    onPurchase: (String) -> Unit,
    onRestore: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Box(modifier = modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = PAYWALL_MAX_WIDTH)
                    .fillMaxWidth()
                    .padding(
                        start = ShadowMapDesign.dimensions.screenPadding,
                        end = ShadowMapDesign.dimensions.screenPadding,
                        bottom = ShadowMapDesign.dimensions.spacingExtraLarge
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(
                    ShadowMapDesign.dimensions.spacingMedium
                )
            ) {
                PaywallHeader(onDismiss)
                PaywallBenefits()
                PaywallOptions(
                    state = state,
                    onPurchase = onPurchase,
                    onRestore = onRestore,
                    onRetry = onRetry
                )
            }
        }
    }
}

@Composable
private fun PaywallHeader(onDismiss: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth()) {
        IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd)) {
            Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.close))
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(
                top = ShadowMapDesign.dimensions.spacingSmall
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ShadowMapDesign.dimensions.spacingSmall)
        ) {
            Icon(
                imageVector = Icons.Outlined.WbSunny,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(PAYWALL_ICON_SIZE)
            )
            Text(
                text = stringResource(R.string.paywall_title),
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = stringResource(R.string.paywall_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PaywallBenefits() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ShadowMapDesign.dimensions.spacingSmall)
    ) {
        BenefitRow(stringResource(R.string.paywall_benefit_dates))
        BenefitRow(stringResource(R.string.paywall_benefit_compare))
        BenefitRow(stringResource(R.string.paywall_benefit_devices))
    }
}

@Composable
private fun BenefitRow(label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ShadowMapDesign.dimensions.spacingSmall)
    ) {
        Icon(
            imageVector = Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PaywallOptions(
    state: InAppPurchaseState,
    onPurchase: (String) -> Unit,
    onRestore: () -> Unit,
    onRetry: () -> Unit
) {
    when (val optionsState = state.options) {
        PurchaseOptionsState.Loading -> CircularProgressIndicator()

        is PurchaseOptionsState.Unavailable -> PaywallUnavailable(
            message = optionsState.message,
            onRetry = onRetry
        )

        is PurchaseOptionsState.Ready -> PaywallReady(
            options = optionsState.options,
            purchaseStatus = state.purchaseStatus,
            errorMessage = state.errorMessage,
            onPurchase = onPurchase,
            onRestore = onRestore,
            onRetry = onRetry
        )
    }
}

@Composable
private fun PaywallReady(
    options: List<PurchaseOption>,
    purchaseStatus: PurchaseStatus,
    errorMessage: String?,
    onPurchase: (String) -> Unit,
    onRestore: () -> Unit,
    onRetry: () -> Unit
) {
    if (options.isEmpty()) {
        PaywallUnavailable(stringResource(R.string.purchase_options_empty), onRetry)
        return
    }
    var selectedOptionId by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(options) {
        if (options.none { it.id == selectedOptionId }) {
            selectedOptionId = options.firstOrNull { it.isBestValue }?.id
                ?: options.firstOrNull { it.isFeatured }?.id
                ?: options.first().id
        }
    }
    val selectedOption = options.firstOrNull { it.id == selectedOptionId }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ShadowMapDesign.dimensions.spacingSmall)
    ) {
        options.forEach { option ->
            PurchaseOptionCard(
                option = option,
                savingsPercent = option.annualSavingsPercent(options),
                selected = option.id == selectedOptionId,
                enabled = purchaseStatus != PurchaseStatus.Purchasing,
                onClick = { selectedOptionId = option.id }
            )
        }
        if (purchaseStatus == PurchaseStatus.Pending) {
            Text(
                text = stringResource(R.string.purchase_pending),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        errorMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Button(
            onClick = { selectedOption?.let { onPurchase(it.id) } },
            enabled = selectedOption != null && purchaseStatus != PurchaseStatus.Purchasing,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (purchaseStatus == PurchaseStatus.Purchasing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(ShadowMapDesign.dimensions.iconSize),
                    strokeWidth = PURCHASE_PROGRESS_STROKE
                )
            } else {
                Text(selectedOption?.purchaseButtonLabel().orEmpty())
            }
        }
        TextButton(onClick = onRestore) {
            Text(stringResource(R.string.restore_purchases))
        }
        Text(
            text = stringResource(R.string.subscription_disclosure),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PurchaseOptionCard(
    option: PurchaseOption,
    savingsPercent: Int?,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        border = BorderStroke(
            PURCHASE_OPTION_BORDER,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ShadowMapDesign.dimensions.spacingLarge),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ShadowMapDesign.dimensions.spacingMedium)
        ) {
            RadioButton(selected = selected, onClick = onClick, enabled = enabled)
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(
                        ShadowMapDesign.dimensions.spacingSmall
                    )
                ) {
                    Text(option.planTitle(), style = MaterialTheme.typography.titleMedium)
                    when {
                        savingsPercent != null -> PurchaseBadge(
                            stringResource(R.string.save_percent, savingsPercent)
                        )

                        option.isBestValue -> PurchaseBadge(stringResource(R.string.best_value))

                        option.isFeatured -> PurchaseBadge(stringResource(R.string.featured))
                    }
                }
                option.offerDescription()?.let { offer ->
                    Text(
                        text = offer,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = option.priceDescription(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PurchaseBadge(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(
                horizontal = ShadowMapDesign.dimensions.spacingSmall,
                vertical = ShadowMapDesign.dimensions.spacingXxs
            ),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun PaywallUnavailable(message: String, onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ShadowMapDesign.dimensions.spacingSmall)
    ) {
        Text(text = message, color = MaterialTheme.colorScheme.error)
        TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}

@Preview(name = "Paywall - light", showBackground = true)
@Composable
private fun PaywallLightPreview() {
    PaywallPreviewContent(darkTheme = false)
}

@Preview(name = "Paywall - dark", showBackground = true)
@Composable
private fun PaywallDarkPreview() {
    PaywallPreviewContent(darkTheme = true)
}

@Composable
private fun PaywallPreviewContent(darkTheme: Boolean) {
    ShadowMapTheme(darkTheme = darkTheme, dynamicColor = false) {
        PaywallSheet(
            state = InAppPurchaseState(
                options = PurchaseOptionsState.Ready(PREVIEW_OPTIONS)
            ),
            onDismiss = {},
            onPurchase = {},
            onRestore = {},
            onRetry = {}
        )
    }
}

private val PAYWALL_MAX_WIDTH = 560.dp
private val PAYWALL_ICON_SIZE = 48.dp
private val PURCHASE_OPTION_BORDER = 1.dp
private val PURCHASE_PROGRESS_STROKE = 2.dp
private val PREVIEW_OPTIONS = listOf(
    PurchaseOption(
        id = "one_time",
        productId = "one_time",
        basePlanId = null,
        offerId = null,
        offerToken = "token",
        type = PurchaseOptionType.OneTime,
        productName = "Lifetime",
        description = "Lifetime premium access",
        isFeatured = true,
        isBestValue = false,
        pricingPhases = listOf(
            PricePhase("$49.99", 49_990_000, "AUD", "", PriceRecurrence.NonRecurring, 0)
        )
    ),
    PurchaseOption(
        id = "subscription:monthly",
        productId = "subscription",
        basePlanId = "monthly",
        offerId = "trial",
        offerToken = "token",
        type = PurchaseOptionType.Subscription,
        productName = "Premium",
        description = "Premium subscription",
        isFeatured = false,
        isBestValue = true,
        pricingPhases = listOf(
            PricePhase("$0.00", 0, "AUD", "P1W", PriceRecurrence.Finite, 1),
            PricePhase("$23.00", 23_000_000, "AUD", "P1M", PriceRecurrence.Infinite, 0)
        )
    )
)
