package com.gooludou.shadowplanner.purchase.billing

import com.gooludou.shadowplanner.purchase.model.EntitlementState
import com.gooludou.shadowplanner.purchase.model.PurchaseCatalog
import com.gooludou.shadowplanner.purchase.model.PurchaseProductConfig
import com.gooludou.shadowplanner.purchase.model.PurchaseProductType
import com.gooludou.shadowplanner.purchase.model.PurchaseStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class PurchaseEntitlementResolverTest {
    @Test
    fun resolve_grantsPremiumForConfiguredPurchasedProduct() {
        val result = PurchaseEntitlementResolver.resolve(
            CATALOG,
            listOf(purchase("one_time", OwnedPurchaseState.Purchased))
        )

        assertEquals(EntitlementState.Premium, result.entitlement)
        assertEquals(PurchaseStatus.Purchased, result.purchaseStatus)
    }

    @Test
    fun resolve_doesNotGrantPremiumForPendingPurchase() {
        val result = PurchaseEntitlementResolver.resolve(
            CATALOG,
            listOf(purchase("subscription", OwnedPurchaseState.Pending))
        )

        assertEquals(EntitlementState.Free, result.entitlement)
        assertEquals(PurchaseStatus.Pending, result.purchaseStatus)
    }

    @Test
    fun resolve_doesNotGrantPremiumForSuspendedSubscription() {
        val result = PurchaseEntitlementResolver.resolve(
            CATALOG,
            listOf(
                purchase(
                    productId = "subscription",
                    state = OwnedPurchaseState.Purchased,
                    isSuspended = true
                )
            )
        )

        assertEquals(EntitlementState.Free, result.entitlement)
        assertEquals(PurchaseStatus.Idle, result.purchaseStatus)
    }

    @Test
    fun resolve_ignoresPurchasesOutsideConfiguredCatalog() {
        val result = PurchaseEntitlementResolver.resolve(
            CATALOG,
            listOf(purchase("other", OwnedPurchaseState.Purchased))
        )

        assertEquals(EntitlementState.Free, result.entitlement)
    }

    private fun purchase(
        productId: String,
        state: OwnedPurchaseState,
        isSuspended: Boolean = false
    ) = OwnedPurchaseRecord(
        productIds = listOf(productId),
        state = state,
        isSuspended = isSuspended,
        isAcknowledged = true
    )

    private companion object {
        val CATALOG = PurchaseCatalog(
            listOf(
                PurchaseProductConfig(
                    productId = "one_time",
                    type = PurchaseProductType.OTP,
                    isActive = false,
                    isFeatured = false,
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
                )
            )
        )
    }
}
