package com.gooludou.shadowplanner.purchase.ui

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gooludou.shadowplanner.purchase.billing.InAppPurchaseManager
import com.gooludou.shadowplanner.purchase.developer.DeveloperSettings
import com.gooludou.shadowplanner.purchase.developer.DeveloperEntitlementOverride
import com.gooludou.shadowplanner.purchase.model.EntitlementState
import com.gooludou.shadowplanner.purchase.model.InAppPurchaseState
import com.gooludou.shadowplanner.purchase.model.PurchaseCatalogState
import com.gooludou.shadowplanner.purchase.remoteconfig.RemoteConfigManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@HiltViewModel
class PurchaseViewModel @Inject constructor(
    private val remoteConfigManager: RemoteConfigManager,
    private val inAppPurchaseManager: InAppPurchaseManager,
    private val developerSettings: DeveloperSettings
) : ViewModel() {
    val purchaseState: StateFlow<InAppPurchaseState> = inAppPurchaseManager.state
    val forcePremium: StateFlow<Boolean> = developerSettings.forcePremium

    private val _showPaywall = MutableStateFlow(false)
    val showPaywall: StateFlow<Boolean> = _showPaywall.asStateFlow()

    init {
        viewModelScope.launch {
            remoteConfigManager.purchaseCatalog.collectLatest { state ->
                if (state is PurchaseCatalogState.Ready) {
                    inAppPurchaseManager.updateCatalog(state.catalog)
                }
            }
        }
        viewModelScope.launch {
            remoteConfigManager.initialize()
        }
        viewModelScope.launch {
            inAppPurchaseManager.state.collectLatest { state ->
                if (state.entitlement == EntitlementState.Premium) {
                    _showPaywall.value = false
                }
            }
        }
    }

    fun requestPaywall() {
        if (effectiveEntitlement(purchaseState.value.entitlement, forcePremium.value) ==
            EntitlementState.Premium
        ) {
            return
        }
        if (
            purchaseState.value.entitlement == EntitlementState.Free ||
            purchaseState.value.entitlement is EntitlementState.Unavailable
        ) {
            _showPaywall.value = true
        }
    }

    fun setForcePremium(enabled: Boolean) {
        developerSettings.setForcePremium(enabled)
        if (enabled) _showPaywall.value = false
    }

    fun effectiveEntitlement(
        entitlement: EntitlementState,
        forcePremium: Boolean
    ): EntitlementState = DeveloperEntitlementOverride.resolve(
        entitlement = entitlement,
        forcePremium = forcePremium
    )

    fun dismissPaywall() {
        _showPaywall.value = false
        inAppPurchaseManager.clearError()
    }

    fun purchase(activity: Activity, optionId: String) {
        inAppPurchaseManager.launchPurchase(activity, optionId)
    }

    fun restorePurchases() {
        viewModelScope.launch { inAppPurchaseManager.refreshPurchases() }
    }

    fun onAppResumed() {
        viewModelScope.launch { inAppPurchaseManager.refreshPurchases() }
    }

    fun refresh() {
        viewModelScope.launch {
            remoteConfigManager.refresh()
            inAppPurchaseManager.refreshPurchases()
        }
    }
}
