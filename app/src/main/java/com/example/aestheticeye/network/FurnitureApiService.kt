package com.example.aestheticeye.network

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.aestheticeye.data.DummyJsonResponse
import com.example.aestheticeye.data.FurnitureProduct
import com.example.aestheticeye.data.LocalProduct
import com.example.aestheticeye.data.RoomImage
import com.example.aestheticeye.data.toFurnitureProduct
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class FurnitureApiService(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val baseUrl = "https://dummyjson.com/products"

    // ── Cached local data ───────────────────────────────────────────────

    private var localProductsCache: List<FurnitureProduct>? = null
    private var roomCatalogCache: List<RoomImage>? = null

    // ── Product fetching (merged) ───────────────────────────────────────

    /**
     * Fetch products from both DummyJSON AND the local catalog, merged together.
     * Local products are always available; DummyJSON adds live API variety.
     * If DummyJSON fails, local products still show up (graceful fallback).
     */
    suspend fun getProducts(
        category: String? = null,
        sortBy: String = "default",
        searchQuery: String = "",
        limit: Int = 30
    ): Result<List<FurnitureProduct>> = withContext(Dispatchers.IO) {
        try {
            val allProducts = mutableListOf<FurnitureProduct>()

            // 1. Always load local products first (instant, offline-safe)
            val localProducts = getLocalProducts()
            allProducts.addAll(localProducts)

            // 2. Try fetching from DummyJSON (live API)
            try {
                val dummyProducts = fetchDummyJsonProducts(category, searchQuery, limit)
                allProducts.addAll(dummyProducts)
            } catch (e: Exception) {
                // DummyJSON failed — that's fine, we still have local products
                android.util.Log.w("FurnitureApi", "DummyJSON unavailable: ${e.message}")
            }

            // 3. Apply category filter
            val filtered = if (!category.isNullOrBlank() && category != "All") {
                allProducts.filter { it.category.equals(category, ignoreCase = true) }
            } else {
                allProducts
            }

            // 4. Apply search filter
            val searched = if (searchQuery.isNotBlank()) {
                filtered.filter {
                    it.title.contains(searchQuery, ignoreCase = true) ||
                            it.description.contains(searchQuery, ignoreCase = true) ||
                            it.tags.any { tag -> tag.contains(searchQuery, ignoreCase = true) }
                }
            } else {
                filtered
            }

            // 5. Sort
            val sorted = when (sortBy) {
                "price_asc"  -> searched.sortedBy { it.price }
                "price_desc" -> searched.sortedByDescending { it.price }
                "rating"     -> searched.sortedByDescending { it.rating }
                else         -> searched.shuffled() // Mix local + API for variety
            }

            Result.success(sorted)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Convenience method — used by the "All" tab.
     */
    suspend fun getHomeProducts(
        sortBy: String = "default",
        limit: Int = 20
    ): Result<List<FurnitureProduct>> {
        return getProducts(sortBy = sortBy, limit = limit)
    }

    // ── DummyJSON fetching ──────────────────────────────────────────────

    private fun fetchDummyJsonProducts(
        category: String?,
        searchQuery: String,
        limit: Int
    ): List<FurnitureProduct> {
        val categories = if (!category.isNullOrBlank() && category != "All") {
            listOf(category)
        } else {
            listOf("furniture", "home-decoration", "kitchen-accessories")
        }

        val allProducts = mutableListOf<FurnitureProduct>()

        for (cat in categories) {
            val url = if (searchQuery.isNotBlank()) {
                "$baseUrl/search?q=$searchQuery&limit=$limit"
            } else {
                "$baseUrl/category/$cat?limit=$limit"
            }

            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: continue

            if (response.isSuccessful) {
                val apiResponse = json.decodeFromString<DummyJsonResponse>(body)
                allProducts.addAll(apiResponse.products.map { it.toFurnitureProduct() })
            }

            // Only search once (not per category)
            if (searchQuery.isNotBlank()) break
        }

        return allProducts
    }

    // ── Local catalog loading ───────────────────────────────────────────

    private fun getLocalProducts(): List<FurnitureProduct> {
        localProductsCache?.let { return it }

        return try {
            val jsonString = context.assets.open("furniture_catalog.json")
                .bufferedReader()
                .use { it.readText() }

            val localProducts = json.decodeFromString<List<LocalProduct>>(jsonString)
            val products = localProducts.map { it.toFurnitureProduct() }
            localProductsCache = products
            products
        } catch (e: Exception) {
            android.util.Log.e("FurnitureApi", "Failed to load local catalog: ${e.message}")
            emptyList()
        }
    }

    // ── Room catalog ────────────────────────────────────────────────────

    /**
     * Load room images from the bundled room_catalog.json.
     * Optionally filter by room type.
     */
    fun getRoomImages(roomType: String? = null): List<RoomImage> {
        val rooms = getRoomCatalog()
        return if (!roomType.isNullOrBlank() && roomType != "All") {
            rooms.filter { it.roomType.equals(roomType, ignoreCase = true) }
        } else {
            rooms
        }
    }

    private fun getRoomCatalog(): List<RoomImage> {
        roomCatalogCache?.let { return it }

        return try {
            val jsonString = context.assets.open("room_catalog.json")
                .bufferedReader()
                .use { it.readText() }

            val rooms = json.decodeFromString<List<RoomImage>>(jsonString)
            roomCatalogCache = rooms
            rooms
        } catch (e: Exception) {
            android.util.Log.e("FurnitureApi", "Failed to load room catalog: ${e.message}")
            emptyList()
        }
    }

    // ── Image downloading ───────────────────────────────────────────────

    /**
     * Download a product image as a Bitmap for use in the Gemini pipeline.
     * Tries the first full-size image, falls back to thumbnail.
     */
    suspend fun downloadProductImage(product: FurnitureProduct): Bitmap? =
        withContext(Dispatchers.IO) {
            val imageUrl = product.images.firstOrNull() ?: product.thumbnail
            if (imageUrl.isBlank()) return@withContext null
            downloadBitmap(imageUrl)
        }

    /**
     * Download a room image as a Bitmap for use in the Gemini pipeline.
     */
    suspend fun downloadRoomImage(room: RoomImage): Bitmap? =
        withContext(Dispatchers.IO) {
            if (room.imageUrl.isBlank()) return@withContext null
            downloadBitmap(room.imageUrl)
        }

    /**
     * Download any image URL as a Bitmap.
     */
    suspend fun downloadBitmap(url: String): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(url).get().build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) return@withContext null

                response.body?.byteStream()?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            } catch (e: Exception) {
                null
            }
        }
}