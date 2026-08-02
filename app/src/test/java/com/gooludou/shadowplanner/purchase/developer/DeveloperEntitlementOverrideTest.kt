package com.gooludou.shadowplanner.purchase.developer

import com.gooludou.shadowplanner.purchase.model.EntitlementState
import org.junit.Assert.assertEquals
import org.junit.Test

class DeveloperEntitlementOverrideTest {
    @Test
    fun resolve_grantsPremiumWhenEnabledInDebugBuild() {
        val result = DeveloperEntitlementOverride.resolve(
            entitlement = EntitlementState.Free,
            forcePremium = true,
            isDebugBuild = true
        )

        assertEquals(EntitlementState.Premium, result)
    }

    @Test
    fun resolve_doesNotBypassPurchasesInReleaseBuild() {
        val result = DeveloperEntitlementOverride.resolve(
            entitlement = EntitlementState.Free,
            forcePremium = true,
            isDebugBuild = false
        )

        assertEquals(EntitlementState.Free, result)
    }

    @Test
    fun resolve_preservesRealEntitlementWhenDisabled() {
        val unavailable = EntitlementState.Unavailable("Billing unavailable")

        val result = DeveloperEntitlementOverride.resolve(
            entitlement = unavailable,
            forcePremium = false,
            isDebugBuild = true
        )

        assertEquals(unavailable, result)
    }
}
