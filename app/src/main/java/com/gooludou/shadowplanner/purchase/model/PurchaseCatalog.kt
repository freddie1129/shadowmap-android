package com.gooludou.shadowplanner.purchase.model

data class PurchaseCatalog(val products: List<PurchaseProductConfig>) {
    val entitlementProductIds: Set<String>
        get() = products.mapTo(mutableSetOf()) { it.productId }

    val activeProducts: List<PurchaseProductConfig>
        get() = products.filter(PurchaseProductConfig::isActive).sortedBy { it.rank }

    val activeBillingProducts: List<PurchaseProductConfig>
        get() = activeProducts.distinctBy { it.productId to it.type }

    companion object {
        val EMPTY = PurchaseCatalog(emptyList())
    }
}

data class PurchaseProductConfig(
    val productId: String,
    val type: PurchaseProductType,
    val isActive: Boolean,
    val isFeatured: Boolean,
    val isBestValue: Boolean,
    val rank: Int,
    val basePlanId: String?
)

enum class PurchaseProductType {
    OTP,
    SUB
}

sealed interface PurchaseCatalogState {
    data object Loading : PurchaseCatalogState

    data class Ready(val catalog: PurchaseCatalog) : PurchaseCatalogState

    data class Error(val message: String) : PurchaseCatalogState
}
