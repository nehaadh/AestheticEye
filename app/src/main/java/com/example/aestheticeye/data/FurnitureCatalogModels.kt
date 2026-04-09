package com.example.aestheticeye.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── DummyJSON API Response ──────────────────────────────────────────────────

@Serializable
data class DummyJsonResponse(
    val products: List<DummyJsonProduct> = emptyList(),
    val total: Int = 0,
    val skip: Int = 0,
    val limit: Int = 0
)

@Serializable
data class DummyJsonProduct(
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

// ── Unified Product (used across the entire app) ────────────────────────────

/**
 * Single product type that the UI and Gemini pipeline work with.
 * Created from either DummyJSON responses or the local JSON catalog.
 */
data class FurnitureProduct(
    val id: String,
    val title: String,
    val description: String,
    val category: String = "",
    val subcategory: String = "",
    val price: Double = 0.0,
    val discountPercentage: Double = 0.0,
    val rating: Double = 0.0,
    val brand: String = "",
    val material: String = "",
    val color: String = "",
    val thumbnail: String = "",
    val images: List<String> = emptyList(),
    val dimensions: ProductDimensions? = null,
    val tags: List<String> = emptyList(),
    val source: ProductSource = ProductSource.LOCAL
)

enum class ProductSource { DUMMYJSON, LOCAL }

@Serializable
data class ProductDimensions(
    val width: Double = 0.0,
    val height: Double = 0.0,
    val depth: Double = 0.0
)

// ── Local JSON catalog model (matches furniture_catalog.json) ───────────────

@Serializable
data class LocalProduct(
    val id: String,
    val title: String,
    val description: String,
    val category: String = "",
    val subcategory: String = "",
    val price: Double = 0.0,
    val discountPercentage: Double = 0.0,
    val rating: Double = 0.0,
    val brand: String = "",
    val material: String = "",
    val color: String = "",
    val thumbnail: String = "",
    val images: List<String> = emptyList(),
    val dimensions: ProductDimensions? = null,
    val tags: List<String> = emptyList()
)

// ── Room catalog model (matches room_catalog.json) ──────────────────────────

@Serializable
data class RoomImage(
    val id: String,
    val name: String,
    val style: String = "",
    val roomType: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val thumbnailUrl: String = "",
    val tags: List<String> = emptyList()
)

// ── Conversion helpers ──────────────────────────────────────────────────────

fun DummyJsonProduct.toFurnitureProduct() = FurnitureProduct(
    id = "dj_$id",
    title = title,
    description = description,
    category = category,
    price = price,
    discountPercentage = discountPercentage,
    rating = rating,
    brand = brand,
    thumbnail = thumbnail,
    images = images,
    dimensions = dimensions,
    tags = tags,
    source = ProductSource.DUMMYJSON
)

fun LocalProduct.toFurnitureProduct() = FurnitureProduct(
    id = id,
    title = title,
    description = description,
    category = category,
    subcategory = subcategory,
    price = price,
    discountPercentage = discountPercentage,
    rating = rating,
    brand = brand,
    material = material,
    color = color,
    thumbnail = thumbnail,
    images = images,
    dimensions = dimensions,
    tags = tags,
    source = ProductSource.LOCAL
)

// ── Filter state for the catalog screen ─────────────────────────────────────

data class CatalogFilters(
    val category: String? = null,
    val sortBy: String = "default",
    val searchQuery: String = ""
)

// ── Available filter options ────────────────────────────────────────────────

object CatalogOptions {

    val categories = listOf(
        "All",
        "furniture",
        "home-decoration",
        "kitchen-accessories"
    )

    val categoryLabels = mapOf(
        "All"                  to "All",
        "furniture"            to "Furniture",
        "home-decoration"      to "Decor",
        "kitchen-accessories"  to "Kitchen"
    )

    val sortOptions = listOf(
        "default"    to "Default",
        "price_asc"  to "Price: Low → High",
        "price_desc" to "Price: High → Low",
        "rating"     to "Top Rated"
    )

    // Room filter options
    val roomTypes = listOf(
        "All",
        "Living Room",
        "Bedroom",
        "Kitchen",
        "Dining Room",
        "Home Office",
        "Bathroom",
        "Patio",
        "Other"
    )
}