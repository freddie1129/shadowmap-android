package com.gooludou.shadowplanner.purchase.remoteconfig

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.gooludou.shadowplanner.purchase.model.PurchaseCatalog
import com.gooludou.shadowplanner.purchase.model.PurchaseProductConfig
import com.gooludou.shadowplanner.purchase.model.PurchaseProductType

object PurchaseCatalogParser {
    fun parse(json: String): Result<PurchaseCatalog> = runCatching {
        val root = JsonParser.parseString(json).asJsonObject
        val productsJson = root.getAsJsonArray(ALL_PRODUCTS_KEY)
            ?: error("Missing $ALL_PRODUCTS_KEY array")
        val seenOptions = mutableSetOf<PurchaseOptionKey>()
        val productTypes = mutableMapOf<String, PurchaseProductType>()
        val products = productsJson.mapNotNull { element ->
            runCatching { parseProduct(element.asJsonObject) }.getOrNull()
        }.filter { product ->
            val existingType = productTypes.putIfAbsent(product.productId, product.type)
            (existingType == null || existingType == product.type) && seenOptions.add(
                PurchaseOptionKey(product.productId, product.type, product.basePlanId)
            )
        }
        PurchaseCatalog(products)
    }

    private fun parseProduct(json: JsonObject): PurchaseProductConfig {
        val productId = json.requiredString(PRODUCT_ID_KEY)
        val type = PurchaseProductType.valueOf(json.requiredString(TYPE_KEY).uppercase())
        val basePlanId = if (type == PurchaseProductType.SUB) {
            json.requiredString(BASE_PLAN_ID_KEY)
        } else {
            null
        }
        return PurchaseProductConfig(
            productId = productId,
            type = type,
            isActive = json.booleanOrDefault(IS_ACTIVE_KEY),
            isFeatured = json.booleanOrDefault(IS_FEATURED_KEY),
            isBestValue = json.booleanOrDefault(IS_BEST_VALUE_KEY),
            rank = json.intOrDefault(RANK_KEY, Int.MAX_VALUE),
            basePlanId = basePlanId
        )
    }

    private fun JsonObject.requiredString(key: String): String {
        val value = get(key)?.takeUnless { it.isJsonNull }?.asString?.trim().orEmpty()
        require(value.isNotEmpty()) { "Missing $key" }
        return value
    }

    private fun JsonObject.booleanOrDefault(key: String): Boolean =
        runCatching { get(key)?.asBoolean }.getOrNull() ?: false

    private fun JsonObject.intOrDefault(key: String, default: Int): Int =
        runCatching { get(key)?.asInt }.getOrNull() ?: default

    private const val ALL_PRODUCTS_KEY = "all_products"
    private const val PRODUCT_ID_KEY = "product_id"
    private const val TYPE_KEY = "type"
    private const val IS_ACTIVE_KEY = "is_active"
    private const val IS_FEATURED_KEY = "is_featured"
    private const val IS_BEST_VALUE_KEY = "is_best_value"
    private const val RANK_KEY = "rank"
    private const val BASE_PLAN_ID_KEY = "base_plan_id"

    private data class PurchaseOptionKey(
        val productId: String,
        val type: PurchaseProductType,
        val basePlanId: String?
    )
}
