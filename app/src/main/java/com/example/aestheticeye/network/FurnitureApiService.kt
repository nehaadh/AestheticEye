package com.example.aestheticeye.network

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.aestheticeye.data.DummyJsonResponse
import com.example.aestheticeye.data.FurnitureProduct
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class FurnitureApiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val baseUrl = "https://dummyjson.com/products"

    /**
     * Fetch products — either all or by category.
     * DummyJSON supports: /products (all), /products/category/{name}
     * Sorting and searching are done client-side for simplicity.
     */
    suspend fun getProducts(
        category: String? = null,
        sortBy: String = "default",
        searchQuery: String = "",
        limit: Int = 30
    ): Result<List<FurnitureProduct>> = withContext(Dispatchers.IO) {
        try {
            val url = when {
                searchQuery.isNotBlank() ->
                    "$baseUrl/search?q=$searchQuery&limit=$limit"
                !category.isNullOrBlank() && category != "All" ->
                    "$baseUrl/category/$category?limit=$limit"
                else ->
                    "$baseUrl?limit=$limit"
            }

            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string()
                ?: return@withContext Result.failure(Exception("Empty response"))

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("API error: HTTP ${response.code}")
                )
            }

            val apiResponse = json.decodeFromString<DummyJsonResponse>(body)

            // Sort client-side
            val sorted = when (sortBy) {
                "price_asc" -> apiResponse.products.sortedBy { it.price }
                "price_desc" -> apiResponse.products.sortedByDescending { it.price }
                "rating" -> apiResponse.products.sortedByDescending { it.rating }
                else -> apiResponse.products
            }

            Result.success(sorted)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch all home-related categories at once (furniture + decor + kitchen).
     * Useful for the "All" filter to show only relevant products.
     */
    suspend fun getHomeProducts(
        sortBy: String = "default",
        limit: Int = 20
    ): Result<List<FurnitureProduct>> = withContext(Dispatchers.IO) {
        try {
            val categories = listOf("furniture", "home-decoration", "kitchen-accessories")
            val allProducts = mutableListOf<FurnitureProduct>()

            for (cat in categories) {
                val url = "$baseUrl/category/$cat?limit=$limit"
                val request = Request.Builder().url(url).get().build()
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: continue

                if (response.isSuccessful) {
                    val apiResponse = json.decodeFromString<DummyJsonResponse>(body)
                    allProducts.addAll(apiResponse.products)
                }
            }

            val sorted = when (sortBy) {
                "price_asc" -> allProducts.sortedBy { it.price }
                "price_desc" -> allProducts.sortedByDescending { it.price }
                "rating" -> allProducts.sortedByDescending { it.rating }
                else -> allProducts.shuffled() // Mix categories for variety
            }

            Result.success(sorted)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Download a product image as a Bitmap for use in the Gemini pipeline.
     * Tries the first full-size image, falls back to thumbnail.
     */
    suspend fun downloadProductImage(product: FurnitureProduct): Bitmap? =
        withContext(Dispatchers.IO) {
            val imageUrl = product.images.firstOrNull() ?: product.thumbnail
            if (imageUrl.isBlank()) return@withContext null

            try {
                val request = Request.Builder().url(imageUrl).get().build()
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