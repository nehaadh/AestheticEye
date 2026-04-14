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

    // ─────────────────────────────────────────────────────────────────────────
    // MAIN PIPELINE — AI comparison removed, triggered on demand instead
    // ─────────────────────────────────────────────────────────────────────────

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

        onStepUpdate("Analyzing room style...")
        val roomDescription = describeImage(
            imageBase64  = roomB64,
            systemPrompt = """You are an expert interior designer.
                Describe this room in detail: the style, colors, lighting,
                furniture, and any windows or surfaces visible.
                Be concise (3-4 sentences)."""
        )

        onStepUpdate("Detecting room style...")
        val roomStyleCard = getRoomStyleCard(roomB64, roomDescription)

        onStepUpdate("Analyzing product...")
        val productDescription = describeImage(
            imageBase64  = productB64,
            systemPrompt = """You are a product photographer.
                Describe this product in detail: what it is, its shape,
                color, material and finish. Be concise (2-3 sentences)."""
        )

        onStepUpdate("Checking proportions...")
        val sizeWarning = getSizeWarning(roomB64, productB64, roomDescription, productDescription)

        onStepUpdate("Generating your scene (this may take ~30 seconds)...")
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

        onStepUpdate("Generating mood board suggestions...")
        val moodBoard = getMoodBoard(
            sceneBase64     = generatedBitmap?.let { bitmapToBase64(it) } ?: roomB64,
            styleCard       = roomStyleCard,
            roomDescription = roomDescription
        )

        AestheticResult(
            generatedBitmap  = generatedBitmap,
            feedback         = "$roomDescription\n\n$productDescription",
            directorFeedback = directorFeedback,
            roomStyleCard    = roomStyleCard,
            sizeWarning      = sizeWarning,
            moodBoard        = moodBoard,
            // Stash encoded images so on-demand comparison can reuse them
            // without re-encoding or re-uploading
            _roomB64    = roomB64,
            _productB64 = productB64,
            _detailB64  = detailB64,
            _roomDesc   = roomDescription,
            _productDesc = productDescription
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ON-DEMAND AI COMPARISON
    // Called only when user taps "Compare with AI" after seeing their result.
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun generateAiComparison(
        result: AestheticResult,
        onStepUpdate: (String) -> Unit
    ): AiComparisonResult = withContext(Dispatchers.IO) {

        val roomB64        = result._roomB64    ?: throw Exception("Room image data not available.")
        val productB64     = result._productB64 ?: throw Exception("Product image data not available.")
        val detailB64      = result._detailB64
        val roomDesc       = result._roomDesc   ?: ""
        val productDesc    = result._productDesc ?: ""
        val userScore      = result.directorFeedback?.score
        val userFlaws      = result.directorFeedback?.flaws ?: emptyList()
        val userSuggestion = result.directorFeedback?.suggestionForImprovement ?: ""

        onStepUpdate("AI is deciding the optimal placement...")
        val (aiBitmap, aiRationale) = generateAiOptimizedPlacement(
            roomBase64     = roomB64,
            productBase64  = productB64,
            detailBase64   = detailB64,
            roomDesc       = roomDesc,
            productDesc    = productDesc,
            userScore      = userScore,
            userFlaws      = userFlaws,
            userSuggestion = userSuggestion
        )

        onStepUpdate("Scoring AI placement...")
        val aiFeedback = getAiDirectorCritique(
            sceneBase64    = aiBitmap?.let { bitmapToBase64(it) } ?: roomB64,
            roomDesc       = roomDesc,
            productDesc    = productDesc,
            userScore      = userScore,
            userFlaws      = userFlaws
        )

        AiComparisonResult(
            aiBitmap    = aiBitmap,
            aiFeedback  = aiFeedback,
            aiRationale = aiRationale
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // QUICK GENERATION (iterative chaining — no extras)
    // ─────────────────────────────────────────────────────────────────────────

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

    // ─────────────────────────────────────────────────────────────────────────
    // AI OPTIMIZED PLACEMENT — returns scene + rationale text
    // ─────────────────────────────────────────────────────────────────────────

    private fun generateAiOptimizedPlacement(
        roomBase64: String,
        productBase64: String,
        detailBase64: String?,
        roomDesc: String,
        productDesc: String,
        userScore: Int? = null,
        userFlaws: List<String> = emptyList(),
        userSuggestion: String = ""
    ): Pair<Bitmap?, String> {
        val parts = mutableListOf(
            Part(inlineData = InlineData("image/jpeg", roomBase64)),
            Part(inlineData = InlineData("image/jpeg", productBase64))
        )
        if (detailBase64 != null) {
            parts.add(Part(inlineData = InlineData("image/jpeg", detailBase64)))
        }

        val userContext = if (userScore != null) {
            val flawText = if (userFlaws.isNotEmpty()) "Flaws in the user placement: ${userFlaws.joinToString("; ")}." else ""
            val suggText = if (userSuggestion.isNotBlank()) "Improvement suggestion: $userSuggestion." else ""
            """
            CONTEXT: The user already placed this product and scored $userScore/100.
            $flawText
            $suggText
            Your job is to do BETTER than the user. Choose a meaningfully different placement
            that directly addresses those flaws. If the user's placement is genuinely near-perfect
            (score 90+) and no better placement exists, say so honestly instead of generating a
            nearly identical scene.
            """.trimIndent()
        } else ""

        parts.add(Part(text = """
            You are a world-class interior designer with 20 years of experience.
            ROOM: $roomDesc
            PRODUCT: $productDesc
            $userContext
            
            Choose the placement that is CLEARLY superior in terms of:
            - Interior design principles (balance, proportion, scale)
            - Visual flow and focal points
            - Functional placement logic
            - Color harmony and style cohesion
            
            If a better placement exists: first write 2-3 sentences starting with
            "I placed the ..." explaining what you did differently and why it improves
            on the user's approach.
            
            If the user's placement is already optimal: write 1-2 sentences starting with
            "Your placement is already optimal because ..." explaining why nothing better
            can be done — then generate a refined version of the same placement.
            
            Then generate a photorealistic image. Make it look like a professional
            interior design photo.
        """.trimIndent()))

        val request = GeminiRequest(
            contents = listOf(Content(parts = parts)),
            generationConfig = GenerationConfig(
                temperature = 1f,
                responseModalities = listOf("TEXT", "IMAGE")
            )
        )
        val response  = callGemini("gemini-2.5-flash-image", request)
        val bitmap    = extractImage(response)
        val rationale = extractText(response)?.trim() ?: ""
        return Pair(bitmap, rationale)
    }

    // AI-specific critique — aware of the user's score, should score higher if placement improved
    private fun getAiDirectorCritique(
        sceneBase64: String,
        roomDesc: String,
        productDesc: String,
        userScore: Int?,
        userFlaws: List<String>
    ): DesignDirectorFeedback? {
        val context = if (userScore != null) {
            val flawText = if (userFlaws.isNotEmpty()) "The user's placement had these flaws: ${userFlaws.joinToString("; ")}." else ""
            "The user's placement scored $userScore/100. $flawText If this AI placement addresses those flaws, it should score HIGHER than $userScore."
        } else ""

        val request = GeminiRequest(
            contents = listOf(Content(parts = listOf(
                Part(inlineData = InlineData("image/jpeg", sceneBase64)),
                Part(text = """
                    You are a seasoned Design Director comparing an AI-optimized placement
                    against a user's placement.
                    Context: $roomDesc
                    Product: $productDesc
                    $context
                    
                    Score this placement honestly. If it genuinely improves on the user's
                    known flaws, the score MUST reflect that improvement and be higher than
                    the user's score. Do not give artificially low scores.
                    
                    Return ONLY a strict JSON object, no markdown:
                    {
                      "score": <integer 0-100>,
                      "status": "<APPROVED | NEEDS_REVISION | REJECTED>",
                      "critique_summary": "<2-3 sentence summary>",
                      "detected_style": "<interior design style>",
                      "flaws": ["<flaw 1>", "<flaw 2>"],
                      "suggestion_for_improvement": "<one concrete suggestion>"
                    }
                """.trimIndent())
            )))
        )
        val rawText = extractText(callGemini("gemini-2.5-flash", request)) ?: return null
        return try {
            val clean  = rawText.replace("```json", "").replace("```", "").trim()
            val parsed = json.decodeFromString<Map<String, JsonElement>>(clean)
            DesignDirectorFeedback(
                score                    = parsed["score"]?.toString()?.toIntOrNull() ?: 0,
                status                   = parsed["status"]?.toString()?.trim('"') ?: "UNKNOWN",
                critiqueSummary          = parsed["critique_summary"]?.toString()?.trim('"') ?: "",
                detectedStyle            = parsed["detected_style"]?.toString()?.trim('"') ?: "",
                flaws                    = try { json.decodeFromString(parsed["flaws"].toString()) } catch (e: Exception) { emptyList() },
                suggestionForImprovement = parsed["suggestion_for_improvement"]?.toString()?.trim('"') ?: ""
            )
        } catch (e: Exception) { null }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FEATURE 2: ROOM STYLE CARD
    // ─────────────────────────────────────────────────────────────────────────

    private fun getRoomStyleCard(roomBase64: String, roomDescription: String): RoomStyleCard? {
        val request = GeminiRequest(
            contents = listOf(
                Content(parts = listOf(
                    Part(inlineData = InlineData("image/jpeg", roomBase64)),
                    Part(text = """
                        You are an expert interior designer and color theorist.
                        Room context: $roomDescription
                        
                        Analyze this room and return ONLY a strict JSON object, no markdown:
                        {
                          "style_name": "<e.g. Scandinavian Minimalist, Mid-Century Modern, Industrial Chic, Japandi, Bohemian>",
                          "dominant_colors": ["<hex 1>", "<hex 2>", "<hex 3>"],
                          "key_traits": ["<trait 1>", "<trait 2>", "<trait 3>", "<trait 4>"],
                          "style_description": "<2 sentence description of this style>"
                        }
                        For dominant_colors return actual hex codes estimated from the image (e.g. "#C4A882").
                    """.trimIndent())
                ))
            )
        )
        val response = callGemini("gemini-2.5-flash", request)
        val rawText  = extractText(response) ?: return null
        return try {
            val clean  = rawText.replace("```json", "").replace("```", "").trim()
            val parsed = json.decodeFromString<Map<String, JsonElement>>(clean)
            RoomStyleCard(
                styleName        = parsed["style_name"]?.toString()?.trim('"') ?: "",
                dominantColors   = try { json.decodeFromString(parsed["dominant_colors"].toString()) } catch (e: Exception) { emptyList() },
                keyTraits        = try { json.decodeFromString(parsed["key_traits"].toString()) } catch (e: Exception) { emptyList() },
                styleDescription = parsed["style_description"]?.toString()?.trim('"') ?: ""
            )
        } catch (e: Exception) { null }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FEATURE 3: SIZE REALITY CHECK
    // ─────────────────────────────────────────────────────────────────────────

    private fun getSizeWarning(
        roomBase64: String,
        productBase64: String,
        roomDesc: String,
        productDesc: String
    ): SizeWarning? {
        val request = GeminiRequest(
            contents = listOf(
                Content(parts = listOf(
                    Part(inlineData = InlineData("image/jpeg", roomBase64)),
                    Part(inlineData = InlineData("image/jpeg", productBase64)),
                    Part(text = """
                        You are an interior design expert specializing in spatial proportion.
                        Room: $roomDesc
                        Product: $productDesc
                        
                        Using visible furniture as scale references (sofas ~85cm tall, doors ~200cm,
                        dining chairs ~80cm, countertops ~90cm), estimate the relative size of this
                        product vs. the room's scale.
                        
                        Return ONLY a strict JSON object, no markdown:
                        {
                          "has_issue": <true | false>,
                          "severity": "<NONE | MINOR | MAJOR>",
                          "description": "<1 sentence describing the size relationship>",
                          "recommendation": "<1 concrete suggestion if issue, else empty string>"
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
            SizeWarning(
                hasIssue       = parsed["has_issue"]?.toString()?.trim('"') == "true",
                severity       = parsed["severity"]?.toString()?.trim('"') ?: "NONE",
                description    = parsed["description"]?.toString()?.trim('"') ?: "",
                recommendation = parsed["recommendation"]?.toString()?.trim('"') ?: ""
            )
        } catch (e: Exception) { null }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FEATURE 4: MOOD BOARD
    // ─────────────────────────────────────────────────────────────────────────

    private fun getMoodBoard(
        sceneBase64: String,
        styleCard: RoomStyleCard?,
        roomDescription: String
    ): MoodBoard? {
        val styleContext = styleCard?.let {
            "Detected style: ${it.styleName}. Key traits: ${it.keyTraits.joinToString(", ")}."
        } ?: ""

        val request = GeminiRequest(
            contents = listOf(
                Content(parts = listOf(
                    Part(inlineData = InlineData("image/jpeg", sceneBase64)),
                    Part(text = """
                        You are a luxury interior designer creating a curated product mood board.
                        Room context: $roomDescription
                        $styleContext
                        
                        Suggest exactly 4 complementary products that would enhance this space.
                        Focus on variety: lighting, textiles, accent furniture, decor objects.
                        
                        Return ONLY a strict JSON array, no markdown:
                        [
                          {
                            "name": "<specific product name>",
                            "category": "<Lighting | Textile | Accent Furniture | Wall Decor | Plant | etc.>",
                            "description": "<1-2 sentence description>",
                            "rationale": "<why it complements this room>",
                            "price_range": "<e.g. $50-$150>",
                            "search_query": "<exact Google Shopping search term>"
                          }
                        ]
                    """.trimIndent())
                ))
            )
        )
        val response = callGemini("gemini-2.5-flash", request)
        val rawText  = extractText(response) ?: return null
        return try {
            val clean = rawText.replace("```json", "").replace("```", "").trim()
            val list  = json.decodeFromString<List<Map<String, JsonElement>>>(clean)
            MoodBoard(suggestions = list.map { item ->
                ProductSuggestion(
                    name        = item["name"]?.toString()?.trim('"') ?: "",
                    category    = item["category"]?.toString()?.trim('"') ?: "",
                    description = item["description"]?.toString()?.trim('"') ?: "",
                    rationale   = item["rationale"]?.toString()?.trim('"') ?: "",
                    priceRange  = item["price_range"]?.toString()?.trim('"') ?: "",
                    searchQuery = item["search_query"]?.toString()?.trim('"') ?: ""
                )
            })
        } catch (e: Exception) { null }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CORE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private fun describeImage(imageBase64: String, systemPrompt: String): String {
        val request = GeminiRequest(
            contents = listOf(Content(parts = listOf(
                Part(inlineData = InlineData("image/jpeg", imageBase64)),
                Part(text = systemPrompt)
            )))
        )
        return extractText(callGemini("gemini-2.5-flash", request)) ?: "Description unavailable."
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
        if (detailBase64 != null) parts.add(Part(inlineData = InlineData("image/jpeg", detailBase64)))
        parts.add(Part(text = """
            You are an expert interior design visualizer with great spatial awareness.
            ROOM: $roomDesc
            PRODUCT: $productDesc
            USER'S PLACEMENT IDEA: $userPrompt
            
            Interpret the user's placement instruction naturally:
            - "next to the couch" = on the side of the couch, on the floor
            - "on the wall" = cover the entire wall surface
            - "by the window" = near or beside the window
            - "in the corner" = in the most natural corner spot
            - "on the table" = on top of the table surface
            
            Generate a photorealistic interior design photo with the product placed naturally.
            Preserve the room's lighting and perspective.
        """.trimIndent()))

        val request = GeminiRequest(
            contents = listOf(Content(parts = parts)),
            generationConfig = GenerationConfig(temperature = 1f, responseModalities = listOf("TEXT", "IMAGE"))
        )
        return extractImage(callGemini("gemini-2.5-flash-image", request))
    }

    private fun getDirectorCritique(
        sceneBase64: String,
        roomDesc: String,
        productDesc: String
    ): DesignDirectorFeedback? {
        val request = GeminiRequest(
            contents = listOf(Content(parts = listOf(
                Part(inlineData = InlineData("image/jpeg", sceneBase64)),
                Part(text = """
                    You are a seasoned Design Director reviewing an interior design scene.
                    Context: $roomDesc
                    Product: $productDesc
                    
                    Return ONLY a strict JSON object, no markdown:
                    {
                      "score": <integer 0-100>,
                      "status": "<APPROVED | NEEDS_REVISION | REJECTED>",
                      "critique_summary": "<2-3 sentence summary>",
                      "detected_style": "<interior design style>",
                      "flaws": ["<flaw 1>", "<flaw 2>"],
                      "suggestion_for_improvement": "<one concrete suggestion>"
                    }
                """.trimIndent())
            )))
        )
        val rawText = extractText(callGemini("gemini-2.5-flash", request)) ?: return null
        return try {
            val clean  = rawText.replace("```json", "").replace("```", "").trim()
            val parsed = json.decodeFromString<Map<String, JsonElement>>(clean)
            DesignDirectorFeedback(
                score                    = parsed["score"]?.toString()?.toIntOrNull() ?: 0,
                status                   = parsed["status"]?.toString()?.trim('"') ?: "UNKNOWN",
                critiqueSummary          = parsed["critique_summary"]?.toString()?.trim('"') ?: "",
                detectedStyle            = parsed["detected_style"]?.toString()?.trim('"') ?: "",
                flaws                    = try { json.decodeFromString(parsed["flaws"].toString()) } catch (e: Exception) { emptyList() },
                suggestionForImprovement = parsed["suggestion_for_improvement"]?.toString()?.trim('"') ?: ""
            )
        } catch (e: Exception) { null }
    }

    private fun callGemini(modelName: String, request: GeminiRequest): GeminiResponse {
        val url  = "$baseUrl/$modelName:generateContent?key=$apiKey"
        val body = json.encodeToString(request).toRequestBody("application/json".toMediaType())
        val httpRequest  = Request.Builder().url(url).post(body).build()
        val httpResponse = client.newCall(httpRequest).execute()
        val responseBody = httpResponse.body?.string() ?: throw Exception("Empty response from Gemini API")
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

    fun bitmapToBase64(bitmap: Bitmap): String {
        val out    = ByteArrayOutputStream()
        val scaled = scaleBitmapIfNeeded(bitmap, 1024)
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    private fun scaleBitmapIfNeeded(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val w = bitmap.width; val h = bitmap.height
        if (w <= maxDimension && h <= maxDimension) return bitmap
        val scale = maxDimension.toFloat() / maxOf(w, h)
        return Bitmap.createScaledBitmap(bitmap, (w * scale).toInt(), (h * scale).toInt(), true)
    }
}