package com.example.aestheticeye.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── DummyJSON API Response ──────────────────────────────────────────────────

@Serializable
data class DummyJsonResponse(
    val products: List<FurnitureProduct> = emptyList(),
    val total: Int = 0,
    val skip: Int = 0,
    val limit: Int = 0
)

@Serializable
data class FurnitureProduct(
    val id: Int,
    val title: String,
    val description: String,
    val category: String = "",
    val price: Double = 0.0,
    @SerialName("discountPercentage")
    val discountPercentage: Double = 0.0,
    val rating: Double = 0.0,
    val stock: Int = 0,
    val brand: String = "",
    val thumbnail: String = "",
    val images: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val dimensions: ProductDimensions? = null
)

@Serializable
data class ProductDimensions(
    val width: Double = 0.0,
    val height: Double = 0.0,
    val depth: Double = 0.0
)

// ── Filter state for the catalog screen ─────────────────────────────────────

data class CatalogFilters(
    val category: String? = null,
    val sortBy: String = "default",
    val searchQuery: String = ""
)

// ── Available filter options ────────────────────────────────────────────────

object CatalogOptions {

    // DummyJSON categories relevant to interior design / home products
    val categories = listOf(
        "All",
        "furniture",
        "home-decoration",
        "kitchen-accessories"
    )

    // Display-friendly labels
    val categoryLabels = mapOf(
        "All" to "All",
        "furniture" to "Furniture",
        "home-decoration" to "Decor",
        "kitchen-accessories" to "Kitchen"
    )

    val sortOptions = listOf(
        "default" to "Default",
        "price_asc" to "Price: Low → High",
        "price_desc" to "Price: High → Low",
        "rating" to "Top Rated"
    )
}