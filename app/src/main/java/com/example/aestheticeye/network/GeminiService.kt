package com.example.aestheticeye.network

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.example.aestheticeye.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class GeminiService(private val apiKey: String) {

    private val client = OkHttpClient.Builder()
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
     * Full pipeline for a single product placement.
     * For multi-product, call this multiple times — each time using
     * the previous generated scene as the new "room".
     */
    suspend fun generateScene(
        roomBitmap: Bitmap,
        productBitmap: Bitmap,
        detailBitmap: Bitmap?,
        prompt: String,
        onStepUpdate: (String) -> Unit
    ): AestheticResult = withContext(Dispatchers.IO) {

        val roomB64    = bitmapToBase64(roomBitmap)
        val productB64 = bitmapToBase64(productBitmap)
        val detailB64  = detailBitmap?.let { bitmapToBase64(it) }

        onStepUpdate("Analyzing room...")
        val roomDescription = describeImage(
            imageBase64  = roomB64,
            systemPrompt = """You are an expert interior designer.
                Describe this room in detail: the style, colors, lighting,
                furniture, and any windows or surfaces visible.
                Be concise (3-4 sentences)."""
        )

        onStepUpdate("Analyzing product...")
        val productDescription = describeImage(
            imageBase64  = productB64,
            systemPrompt = """You are a product photographer.
                Describe this product in detail: what it is, its shape,
                color, material and finish. Be concise (2-3 sentences)."""
        )

        onStepUpdate("Generating scene (this may take ~30 seconds)...")
        val generatedBitmap = generateCompositeImage(
            roomBase64    = roomB64,
            productBase64 = productB64,
            detailBase64  = detailB64,
            roomDesc      = roomDescription,
            productDesc   = productDescription,
            userPrompt    = prompt
        )

        onStepUpdate("Getting expert critique...")
        val directorFeedback = getDirectorCritique(
            sceneBase64 = generatedBitmap?.let { bitmapToBase64(it) } ?: roomB64,
            roomDesc    = roomDescription,
            productDesc = productDescription
        )

        AestheticResult(
            generatedBitmap  = generatedBitmap,
            feedback         = "$roomDescription\n\n$productDescription",
            directorFeedback = directorFeedback
        )
    }

    /**
     * Quick generation pass — skips the critique step.
     * Used for intermediate steps when chaining multiple products.
     */
    suspend fun generateSceneQuick(
        roomBitmap: Bitmap,
        productBitmap: Bitmap,
        detailBitmap: Bitmap?,
        prompt: String,
        onStepUpdate: (String) -> Unit
    ): Bitmap? = withContext(Dispatchers.IO) {

        val roomB64    = bitmapToBase64(roomBitmap)
        val productB64 = bitmapToBase64(productBitmap)
        val detailB64  = detailBitmap?.let { bitmapToBase64(it) }

        onStepUpdate("Analyzing room...")
        val roomDescription = describeImage(
            imageBase64  = roomB64,
            systemPrompt = """You are an expert interior designer.
                Describe this room in detail: the style, colors, lighting,
                furniture, and any windows or surfaces visible.
                Be concise (3-4 sentences)."""
        )

        onStepUpdate("Analyzing product...")
        val productDescription = describeImage(
            imageBase64  = productB64,
            systemPrompt = """You are a product photographer.
                Describe this product in detail: what it is, its shape,
                color, material and finish. Be concise (2-3 sentences)."""
        )

        onStepUpdate("Generating scene...")
        generateCompositeImage(
            roomBase64    = roomB64,
            productBase64 = productB64,
            detailBase64  = detailB64,
            roomDesc      = roomDescription,
            productDesc   = productDescription,
            userPrompt    = prompt
        )
    }

    private fun describeImage(imageBase64: String, systemPrompt: String): String {
        val request = GeminiRequest(
            contents = listOf(
                Content(parts = listOf(
                    Part(inlineData = InlineData("image/jpeg", imageBase64)),
                    Part(text = systemPrompt)
                ))
            )
        )
        val response = callGemini("gemini-2.5-flash", request)
        return extractText(response) ?: "Description unavailable."
    }

    private fun generateCompositeImage(
        roomBase64: String,
        productBase64: String,
        detailBase64: String?,
        roomDesc: String,
        productDesc: String,
        userPrompt: String
    ): Bitmap? {
        val parts = mutableListOf(
            Part(inlineData = InlineData("image/jpeg", roomBase64)),
            Part(inlineData = InlineData("image/jpeg", productBase64))
        )
        if (detailBase64 != null) {
            parts.add(Part(inlineData = InlineData("image/jpeg", detailBase64)))
        }
        parts.add(Part(text = """
            You are an expert interior design visualizer with great spatial awareness.
            ROOM: $roomDesc
            PRODUCT: $productDesc
            USER'S PLACEMENT IDEA: $userPrompt
            
            Interpret the user's placement instruction naturally and use your 
            interior design expertise to decide the best exact position:
            - "next to the couch" = on the side of the couch, on the floor
            - "on the wall" or "add texture to the wall" = cover the entire wall surface
            - "by the window" = near or beside the window
            - "in the corner" = in the most natural corner spot
            - "on the table" = on top of the table surface
            - Use common sense for any vague instructions
            
            Generate a photorealistic image of the product placed naturally
            in the room based on your interpretation.
            Make it look like a professional interior design photo.
            Preserve the room's lighting and perspective.
            The product should look like it truly belongs there.
        """.trimIndent()))

        val request = GeminiRequest(
            contents = listOf(Content(parts = parts)),
            generationConfig = GenerationConfig(
                temperature = 1f,
                responseModalities = listOf("TEXT", "IMAGE")
            )
        )
        val response = callGemini("gemini-2.5-flash-image", request)
        return extractImage(response)
    }

    private fun getDirectorCritique(
        sceneBase64: String,
        roomDesc: String,
        productDesc: String
    ): DesignDirectorFeedback? {
        val request = GeminiRequest(
            contents = listOf(
                Content(parts = listOf(
                    Part(inlineData = InlineData("image/jpeg", sceneBase64)),
                    Part(text = """
                        You are a seasoned Design Director reviewing an interior design scene.
                        
                        Context about the space: $roomDesc
                        Product being evaluated: $productDesc
                        
                        Focus your critique PRIMARILY on the product that was just placed, 
                        but also consider how it fits within the overall room composition.
                        Evaluate the product's placement, scale, color harmony with the 
                        existing space, and whether it complements the other furniture 
                        and decor already in the room.
                        
                        Provide your critique as a strict JSON object only, no markdown:
                        {
                          "score": <integer 0-100>,
                          "status": "<APPROVED | NEEDS_REVISION | REJECTED>",
                          "critique_summary": "<2-3 sentence summary focusing on the placed product and how it works with the rest of the room>",
                          "detected_style": "<detected interior design style>",
                          "flaws": ["<flaw 1>", "<flaw 2>"],
                          "suggestion_for_improvement": "<one concrete suggestion>"
                        }
                    """.trimIndent())
                ))
            )
        )
        val response = callGemini("gemini-2.5-flash", request)
        val rawText  = extractText(response) ?: return null

        return try {
            val clean  = rawText.replace("```json", "").replace("```", "").trim()
            val parsed = json.decodeFromString<Map<String, JsonElement>>(clean)
            DesignDirectorFeedback(
                score    = parsed["score"]?.toString()?.toIntOrNull() ?: 0,
                status   = parsed["status"]?.toString()?.trim('"') ?: "UNKNOWN",
                critiqueSummary          = parsed["critique_summary"]?.toString()?.trim('"') ?: "",
                detectedStyle            = parsed["detected_style"]?.toString()?.trim('"') ?: "",
                flaws                    = try {
                    json.decodeFromString<List<String>>(parsed["flaws"].toString())
                } catch (e: Exception) { emptyList() },
                suggestionForImprovement = parsed["suggestion_for_improvement"]?.toString()?.trim('"') ?: ""
            )
        } catch (e: Exception) { null }
    }

    private fun callGemini(modelName: String, request: GeminiRequest): GeminiResponse {
        val url  = "$baseUrl/$modelName:generateContent?key=$apiKey"
        val body = json.encodeToString(request)
            .toRequestBody("application/json".toMediaType())
        val httpRequest = Request.Builder().url(url).post(body).build()
        val httpResponse = client.newCall(httpRequest).execute()
        val responseBody = httpResponse.body?.string()
            ?: throw Exception("Empty response from Gemini API")
        if (!httpResponse.isSuccessful) {
            val err = try { json.decodeFromString<GeminiResponse>(responseBody) } catch (e: Exception) { null }
            throw Exception("Gemini API error: ${err?.error?.message ?: "HTTP ${httpResponse.code}"}")
        }
        return json.decodeFromString<GeminiResponse>(responseBody)
    }

    private fun extractText(response: GeminiResponse): String? =
        response.candidates?.firstOrNull()?.content?.parts?.firstOrNull { it.text != null }?.text

    private fun extractImage(response: GeminiResponse): Bitmap? {
        val base64Data = response.candidates?.firstOrNull()?.content?.parts
            ?.firstOrNull { it.inlineData != null }?.inlineData?.data ?: return null
        return try {
            val bytes = Base64.decode(base64Data, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) { null }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val out    = ByteArrayOutputStream()
        val scaled = scaleBitmapIfNeeded(bitmap, 1024)
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    private fun scaleBitmapIfNeeded(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxDimension && h <= maxDimension) return bitmap
        val scale = maxDimension.toFloat() / maxOf(w, h)
        return Bitmap.createScaledBitmap(bitmap, (w * scale).toInt(), (h * scale).toInt(), true)
    }
}