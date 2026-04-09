package com.example.aestheticeye.network

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.example.aestheticeye.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

/**
 * Extracts product information from any product URL (IKEA, Amazon, Wayfair, etc.)
 * by fetching the page HTML and sending it to Gemini for structured extraction.
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

    /**
     * Data class for the extracted product info.
     */
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

    /**
     * Main extraction method:
     * 1. Fetch the page HTML
     * 2. Send to Gemini for structured extraction
     * 3. Download the product image
     * Returns a pair of (ExtractedProduct, Bitmap?)
     */
    suspend fun extractFromUrl(
        url: String,
        onStep: (String) -> Unit = {}
    ): Result<Pair<ExtractedProduct, Bitmap?>> = withContext(Dispatchers.IO) {
        try {
            onStep("Fetching product page...")

            // 1. Fetch the HTML
            val html = fetchPageHtml(url)
                ?: return@withContext Result.failure(Exception("Could not load the product page. Check the URL and try again."))

            // Trim HTML to avoid token limits (keep first 15k chars — enough for product info)
            val trimmedHtml = if (html.length > 15000) html.take(15000) else html

            onStep("Extracting product details with AI...")

            // 2. Send to Gemini for extraction
            val extracted = extractWithGemini(trimmedHtml, url)
                ?: return@withContext Result.failure(Exception("Could not extract product details. Try a different product URL."))

            onStep("Downloading product image...")

            // 3. Download the product image
            val bitmap = if (extracted.imageUrl.isNotBlank()) {
                downloadImage(extracted.imageUrl)
            } else null

            Result.success(Pair(extracted, bitmap))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Failed to extract product"))
        }
    }

    // ── Fetch page HTML ─────────────────────────────────────────────────

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
            if (response.isSuccessful) {
                response.body?.string()
            } else null
        } catch (e: Exception) {
            null
        }
    }

    // ── Gemini extraction ───────────────────────────────────────────────

    private fun extractWithGemini(html: String, originalUrl: String): ExtractedProduct? {
        val prompt = """
            You are a product data extraction expert.
            
            I will give you the HTML source of a product page from an online furniture/home store.
            The original URL is: $originalUrl
            
            Extract the following product details and return ONLY a valid JSON object with no markdown formatting:
            {
              "name": "<product name>",
              "description": "<product description, 2-3 sentences>",
              "price": "<numeric price like 299.99>",
              "currency": "<USD, EUR, etc.>",
              "category": "<furniture, home-decoration, kitchen-accessories, or lighting>",
              "material": "<materials used>",
              "color": "<color name>",
              "dimensions": "<width x depth x height in cm, or inches if cm not available>",
              "imageUrl": "<FULL URL to the main product image - must start with https://>",
              "brand": "<brand name>"
            }
            
            IMPORTANT for imageUrl:
            - Find the main product image URL in the HTML (look for og:image meta tags, main product image src, or structured data)
            - The URL must be a complete, absolute URL starting with https://
            - Prefer the highest resolution image available
            - For IKEA products, look for URLs containing ikea.com/us/en/images/products/
            
            If you cannot find a field, use an empty string "".
            Return ONLY the JSON object, no explanation, no markdown backticks.
            
            HTML SOURCE:
            $html
        """.trimIndent()

        val requestBody = json.encodeToString(
            GeminiRequest.serializer(),
            GeminiRequest(
                contents = listOf(
                    Content(parts = listOf(Part(text = prompt)))
                )
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
        val rawText = geminiResponse.candidates?.firstOrNull()
            ?.content?.parts?.firstOrNull { it.text != null }?.text
            ?: return null

        return try {
            val cleanJson = rawText
                .replace("```json", "")
                .replace("```", "")
                .trim()
            json.decodeFromString<ExtractedProduct>(cleanJson)
        } catch (e: Exception) {
            null
        }
    }

    // ── Image downloading ───────────────────────────────────────────────

    private fun downloadImage(imageUrl: String): Bitmap? {
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

            response.body?.byteStream()?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (e: Exception) {
            null
        }
    }
}