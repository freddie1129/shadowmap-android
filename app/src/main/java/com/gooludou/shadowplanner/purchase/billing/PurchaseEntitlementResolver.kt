package com.gooludou.shadowplanner.purchase.billing

import com.gooludou.shadowplanner.purchase.model.EntitlementState
import com.gooludou.shadowplanner.purchase.model.PurchaseCatalog
import com.gooludou.shadowplanner.purchase.model.PurchaseStatus

data class OwnedPurchaseRecord(
    val productIds: List<String>,
    val state: OwnedPurchaseState,
    val isSuspended: Boolean,
    val isAcknowledged: Boolean
)

enum class OwnedPurchaseState {
    Purchased,
    Pending,
    Other
}

data class EntitlementResolution(
    val entitlement: EntitlementState,
    val purchaseStatus: PurchaseStatus
)

object PurchaseEntitlementResolver {
    fun resolve(
        catalog: PurchaseCatalog,
        purchases: List<OwnedPurchaseRecord>
    ): EntitlementResolution {
        val configuredIds = catalog.entitlementProductIds
        val matching = purchases.filter { purchase ->
            purchase.productIds.any(configuredIds::contains)
        }
        val hasPremium = matching.any { purchase ->
            purchase.state == OwnedPurchaseState.Purchased && !purchase.isSuspended
        }
        val hasPending = matching.any { purchase ->
            purchase.state == OwnedPurchaseState.Pending
        }
        return EntitlementResolution(
            entitlement = if (hasPremium) EntitlementState.Premium else EntitlementState.Free,
            purchaseStatus = when {
                hasPremium -> PurchaseStatus.Purchased
                hasPending -> PurchaseStatus.Pending
                else -> PurchaseStatus.Idle
            }
        )
    }
}
