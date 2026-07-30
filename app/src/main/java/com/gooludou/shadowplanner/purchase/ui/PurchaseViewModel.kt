package com.gooludou.shadowplanner.purchase.ui

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gooludou.shadowplanner.purchase.billing.InAppPurchaseManager
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
    private val inAppPurchaseManager: InAppPurchaseManager
) : ViewModel() {
    val purchaseState: StateFlow<InAppPurchaseState> = inAppPurchaseManager.state

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
        if (
            purchaseState.value.entitlement == EntitlementState.Free ||
            purchaseState.value.entitlement is EntitlementState.Unavailable
        ) {
            _showPaywall.value = true
        }
    }

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
