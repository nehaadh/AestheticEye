package com.example.aestheticeye.network

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.aestheticeye.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Extracts product information or images from any URL.
 * Uses Gemini to parse HTML and extract structured data.
 */
class ProductUrlExtractor(private val apiKey: String) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val geminiClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta/models"

    @Serializable
    data class ExtractedProduct(
        val name: String = "",
        val description: String = "",
        val price: String = "",
        val currency: String = "USD",
        val category: String = "",
        val material: String = "",
        val color: String = "",
        val dimensions: String = "",
        val imageUrl: String = "",
        val brand: String = ""
    )

    @Serializable
    data class ExtractedImage(
        val imageUrl: String = "",
        val title: String = ""
    )

    // ── Full product extraction (for product URLs) ──────────────────

    suspend fun extractProductFromUrl(
        url: String,
        onStep: (String) -> Unit = {}
    ): Result<Pair<ExtractedProduct, Bitmap?>> = withContext(Dispatchers.IO) {
        try {
            onStep("Fetching product page...")
            val html = fetchPageHtml(url)
                ?: return@withContext Result.failure(Exception("Could not load the page. Check the URL and try again."))

            val trimmedHtml = if (html.length > 15000) html.take(15000) else html

            onStep("Extracting product details with AI...")
            val extracted = extractProductWithGemini(trimmedHtml, url)
                ?: return@withContext Result.failure(Exception("Could not extract product details. Try a different URL."))

            onStep("Downloading product image...")
            val bitmap = if (extracted.imageUrl.isNotBlank()) {
                downloadImage(extracted.imageUrl)
            } else null

            Result.success(Pair(extracted, bitmap))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Failed to extract product"))
        }
    }

    // ── Simple image extraction (for room URLs, any image URL) ──────

    suspend fun extractImageFromUrl(
        url: String,
        onStep: (String) -> Unit = {}
    ): Result<Pair<String, Bitmap?>> = withContext(Dispatchers.IO) {
        try {
            // If the URL directly points to an image file, just download it
            if (isDirectImageUrl(url)) {
                onStep("Downloading image...")
                val bitmap = downloadImage(url)
                val title = url.substringAfterLast("/").substringBeforeLast(".")
                return@withContext if (bitmap != null) {
                    Result.success(Pair(title, bitmap))
                } else {
                    Result.failure(Exception("Could not download the image."))
                }
            }

            // Otherwise, fetch the page and extract the main image URL
            onStep("Fetching page...")
            val html = fetchPageHtml(url)
                ?: return@withContext Result.failure(Exception("Could not load the page."))

            val trimmedHtml = if (html.length > 10000) html.take(10000) else html

            onStep("Finding main image...")
            val extracted = extractImageWithGemini(trimmedHtml, url)
                ?: return@withContext Result.failure(Exception("Could not find an image on this page."))

            onStep("Downloading image...")
            val bitmap = if (extracted.imageUrl.isNotBlank()) {
                downloadImage(extracted.imageUrl)
            } else null

            if (bitmap != null) {
                Result.success(Pair(extracted.title, bitmap))
            } else {
                Result.failure(Exception("Could not download the image."))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Failed to extract image"))
        }
    }

    // ── Private helpers ─────────────────────────────────────────────

    private fun isDirectImageUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
                lower.endsWith(".png") || lower.endsWith(".webp") ||
                lower.contains(".jpg?") || lower.contains(".jpeg?") ||
                lower.contains(".png?") || lower.contains(".webp?")
    }

    private fun fetchPageHtml(url: String): String? {
        return try {
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .addHeader("Accept-Language", "en-US,en;q=0.9")
                .get()
                .build()
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) response.body?.string() else null
        } catch (e: Exception) { null }
    }

    private fun extractProductWithGemini(html: String, originalUrl: String): ExtractedProduct? {
        val prompt = """
            You are a product data extraction expert.
            I will give you HTML from a product page. URL: $originalUrl
            
            Extract product details and return ONLY a valid JSON object, no markdown:
            {
              "name": "<product name>",
              "description": "<2-3 sentence description>",
              "price": "<numeric price like 299.99>",
              "currency": "<USD, EUR, etc.>",
              "category": "<furniture, home-decoration, kitchen-accessories, or lighting>",
              "material": "<materials>",
              "color": "<color>",
              "dimensions": "<W x D x H>",
              "imageUrl": "<FULL absolute URL to main product image starting with https://>",
              "brand": "<brand>"
            }
            
            For imageUrl: look for og:image meta tag, main product image, or structured data.
            Must be a complete URL starting with https://
            Return ONLY the JSON, nothing else.
            
            HTML:
            $html
        """.trimIndent()

        val responseText = callGeminiText(prompt) ?: return null
        return try {
            val clean = responseText.replace("```json", "").replace("```", "").trim()
            json.decodeFromString<ExtractedProduct>(clean)
        } catch (e: Exception) { null }
    }

    private fun extractImageWithGemini(html: String, originalUrl: String): ExtractedImage? {
        val prompt = """
            Extract the main image URL and page title from this HTML.
            URL: $originalUrl
            
            Return ONLY a JSON object, no markdown:
            {"imageUrl": "<full https:// URL to the main/hero image>", "title": "<page title or image description>"}
            
            Look for: og:image meta tag, main hero image, largest product image.
            Must be absolute URL starting with https://
            
            HTML:
            $html
        """.trimIndent()

        val responseText = callGeminiText(prompt) ?: return null
        return try {
            val clean = responseText.replace("```json", "").replace("```", "").trim()
            json.decodeFromString<ExtractedImage>(clean)
        } catch (e: Exception) { null }
    }

    private fun callGeminiText(prompt: String): String? {
        val requestBody = json.encodeToString(
            GeminiRequest.serializer(),
            GeminiRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt))))
            )
        )
        val url = "$baseUrl/gemini-2.5-flash:generateContent?key=$apiKey"
        val httpRequest = Request.Builder()
            .url(url)
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()

        val response = geminiClient.newCall(httpRequest).execute()
        val body = response.body?.string() ?: return null
        if (!response.isSuccessful) return null

        val geminiResponse = json.decodeFromString<GeminiResponse>(body)
        return geminiResponse.candidates?.firstOrNull()
            ?.content?.parts?.firstOrNull { it.text != null }?.text
    }

    fun downloadImage(imageUrl: String): Bitmap? {
        return try {
            val request = Request.Builder()
                .url(imageUrl)
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                .addHeader("Referer", "https://www.ikea.com/")
                .addHeader("Accept", "image/webp,image/apng,image/*,*/*;q=0.8")
                .get()
                .build()
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return null
            response.body?.byteStream()?.use { BitmapFactory.decodeStream(it) }
        } catch (e: Exception) { null }
    }
}