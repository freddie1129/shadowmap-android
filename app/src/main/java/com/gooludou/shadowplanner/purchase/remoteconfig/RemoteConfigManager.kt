package com.gooludou.shadowplanner.purchase.remoteconfig

import com.gooludou.shadowplanner.purchase.model.PurchaseCatalogState
import kotlinx.coroutines.flow.StateFlow

interface RemoteConfigManager {
    val purchaseCatalog: StateFlow<PurchaseCatalogState>

    suspend fun initialize()

    suspend fun refresh()
}
