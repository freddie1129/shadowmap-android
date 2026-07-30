package com.gooludou.shadowplanner.purchase.billing

import android.app.Activity
import com.gooludou.shadowplanner.purchase.model.InAppPurchaseState
import com.gooludou.shadowplanner.purchase.model.PurchaseCatalog
import kotlinx.coroutines.flow.StateFlow

interface InAppPurchaseManager {
    val state: StateFlow<InAppPurchaseState>

    suspend fun updateCatalog(catalog: PurchaseCatalog)

    suspend fun refreshPurchases()

    fun launchPurchase(activity: Activity, optionId: String)

    fun clearError()
}
