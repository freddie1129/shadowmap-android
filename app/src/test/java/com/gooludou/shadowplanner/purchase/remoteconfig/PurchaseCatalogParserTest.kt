package com.gooludou.shadowplanner.purchase.remoteconfig

import com.gooludou.shadowplanner.purchase.model.PurchaseProductType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PurchaseCatalogParserTest {
    @Test
    fun parse_readsFlattenedProductOptions() {
        val catalog = PurchaseCatalogParser.parse(VALID_CONFIG).getOrThrow()

        assertEquals(
            listOf("one_time", "subscription", "subscription"),
            catalog.products.map { it.productId }
        )
        assertEquals(PurchaseProductType.OTP, catalog.products[0].type)
        assertTrue(catalog.products[0].isFeatured)
        assertEquals(PurchaseProductType.SUB, catalog.products[1].type)
        assertEquals(listOf("monthly", "yearly"), catalog.products.drop(1).map { it.basePlanId })
        assertEquals(2, catalog.activeBillingProducts.size)
    }

    @Test
    fun parse_ignoresMalformedDuplicateAndConflictingOptions() {
        val catalog = PurchaseCatalogParser.parse(
            """{"all_products":[
                {"product_id":"one_time","type":"OTP","is_active":true},
                {"product_id":"one_time","type":"OTP","is_active":false},
                {"product_id":"one_time","type":"SUB","base_plan_id":"monthly"},
                {"product_id":"subscription","type":"SUB","base_plan_id":"monthly"},
                {"product_id":"subscription","type":"SUB","base_plan_id":"monthly"},
                {"product_id":"subscription","type":"SUB"},
                {"product_id":"broken","type":"UNKNOWN"},
                {"type":"SUB"}
            ]}"""
        ).getOrThrow()

        assertEquals(2, catalog.products.size)
        assertTrue(catalog.products.first().isActive)
        assertEquals("monthly", catalog.products.last().basePlanId)
    }

    @Test
    fun parse_defaultsOptionalFields() {
        val product = PurchaseCatalogParser.parse(
            """{"all_products":[{"product_id":"one_time","type":"OTP"}]}"""
        ).getOrThrow().products.single()

        assertFalse(product.isActive)
        assertFalse(product.isFeatured)
        assertFalse(product.isBestValue)
        assertEquals(Int.MAX_VALUE, product.rank)
        assertEquals(null, product.basePlanId)
    }

    @Test
    fun parse_rejectsInvalidRoot() {
        assertTrue(PurchaseCatalogParser.parse("{}").isFailure)
        assertTrue(PurchaseCatalogParser.parse("not-json").isFailure)
    }

    private companion object {
        val VALID_CONFIG = """
            {
              "all_products": [
                {
                  "product_id": "one_time",
                  "type": "OTP",
                  "is_active": true,
                  "is_featured": true,
                  "rank": 0
                },
                {
                  "product_id": "subscription",
                  "base_plan_id": "monthly",
                  "type": "SUB",
                  "is_active": true,
                  "is_featured": true,
                  "rank": 1
                },
                {
                  "product_id": "subscription",
                  "base_plan_id": "yearly",
                  "type": "SUB",
                  "is_active": true,
                  "is_featured": true,
                  "rank": 2
                }
              ]
            }
        """.trimIndent()
    }
}
