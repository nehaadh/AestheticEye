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
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class GeminiService(private val apiKey: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta/models"

    // ─────────────────────────────────────────────────────────────────────────
    // MAIN PIPELINE
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun generateScene(
        roomBitmap: Bitmap,
        productBitmap: Bitmap,
        detailBitmap: Bitmap?,
        prompt: String,
        onStepUpdate: (String) -> Unit,
        onSceneReady: ((AestheticResult) -> Unit)? = null  // called as soon as scene image is ready
    ): AestheticResult = withContext(Dispatchers.IO) {

        val roomB64    = bitmapToBase64(roomBitmap)
        val productB64 = bitmapToBase64(productBitmap)
        val detailB64  = detailBitmap?.let { bitmapToBase64(it) }

        // ── Phase 1: Run analyses needed BEFORE generation ────────────────────
        onStepUpdate("Analyzing room...")
        val roomDescription = describeImage(roomB64, """You are an expert interior designer.
            Describe this room: style, colors, lighting, furniture, surfaces. Be concise (3-4 sentences).""")

        onStepUpdate("Analyzing product...")
        val productAnalysis = analyseProduct(productB64)
        val productDescription = productAnalysis.first
        val productName        = productAnalysis.second

        onStepUpdate("Checking size proportions...")
        val sizeWarning = getSizeWarning(roomB64, productB64, roomDescription, productDescription)

        // ── Phase 2: Generate scene image ─────────────────────────────────────
        onStepUpdate("Generating your scene (~30 seconds)...")
        val generatedBitmap = generateCompositeImage(
            roomBase64  = roomB64, productBase64 = productB64, detailBase64 = detailB64,
            roomDesc    = roomDescription, productDesc = productDescription, userPrompt = prompt
        )
        val generatedB64 = generatedBitmap?.let { bitmapToBase64(it) }

        // ── Phase 3: Emit progressive results — each step reveals more ──────
        // Helper to build result with whatever is ready so far
        fun buildPartial(
            styleCard: RoomStyleCard? = null,
            critique: DesignDirectorFeedback? = null,
            mood: MoodBoard? = null
        ) = AestheticResult(
            generatedBitmap    = generatedBitmap,
            feedback           = roomDescription + "\n\n" + productDescription,
            directorFeedback   = critique,
            roomStyleCard      = styleCard,
            sizeWarning        = sizeWarning,
            moodBoard          = mood,
            _roomB64           = roomB64,
            _productB64        = productB64,
            _detailB64         = detailB64,
            _roomDesc          = roomDescription,
            _productDesc       = productDescription,
            _productName       = productName,
            _generatedSceneB64 = generatedB64
        )

        // Step 1: Show scene + size immediately
        onSceneReady?.invoke(buildPartial())

        // Step 2: Room style card
        onStepUpdate("Detecting room style...")
        val roomStyleCard = getRoomStyleCard(roomB64, roomDescription)
        onSceneReady?.invoke(buildPartial(styleCard = roomStyleCard))

        // Step 3: Design critique
        onStepUpdate("Getting design critique...")
        val directorFeedback = getDirectorCritique(
            sceneBase64 = generatedB64 ?: roomB64,
            roomDesc    = roomDescription,
            productDesc = productDescription,
            hasDetail   = detailB64 != null
        )
        onSceneReady?.invoke(buildPartial(styleCard = roomStyleCard, critique = directorFeedback))

        // Step 4: Mood board
        onStepUpdate("Building mood board...")
        val moodBoard = getMoodBoard(
            sceneBase64     = generatedB64 ?: roomB64,
            styleCard       = roomStyleCard,
            roomDescription = roomDescription
        )

        AestheticResult(
            generatedBitmap    = generatedBitmap,
            feedback           = roomDescription + "\n\n" + productDescription,
            directorFeedback   = directorFeedback,
            roomStyleCard      = roomStyleCard,
            sizeWarning        = sizeWarning,
            moodBoard          = moodBoard,
            _roomB64           = roomB64,
            _productB64        = productB64,
            _detailB64         = detailB64,
            _roomDesc          = roomDescription,
            _productDesc       = productDescription,
            _productName       = productName,
            _generatedSceneB64 = generatedBitmap?.let { bitmapToBase64(it) }
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AI DESIGN IMPROVEMENT
    // Takes the user's generated scene and improves it — better placement,
    // complementary additions, composition fixes. Shows what was changed and why.
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun generateAiImprovement(
        result: AestheticResult,
        onStepUpdate: (String) -> Unit
    ): AiImprovementResult = withContext(Dispatchers.IO) {

        val generatedSceneB64 = result._generatedSceneB64
            ?: throw Exception("Scene not available — please regenerate.")
        val roomDesc    = result._roomDesc    ?: ""
        val productDesc = result._productDesc ?: ""
        val hasDetail   = result._detailB64   != null
        val userScore   = result.directorFeedback?.score
        val userFlaws   = result.directorFeedback?.flaws ?: emptyList()

        onStepUpdate("AI is analysing your scene...")
        val improvements = analyseAndPlanImprovements(
            sceneB64    = generatedSceneB64,
            roomDesc    = roomDesc,
            productDesc = productDesc,
            hasDetail   = hasDetail,
            userScore   = userScore,
            userFlaws   = userFlaws
        )

        onStepUpdate("AI is redesigning your scene...")
        val improvedBitmap = generateImprovedScene(
            sceneB64     = generatedSceneB64,
            roomDesc     = roomDesc,
            productDesc  = productDesc,
            improvements = improvements
        )

        AiImprovementResult(
            improvedBitmap  = improvedBitmap,
            improvements    = improvements.bulletPoints,
            overallNote     = improvements.overallNote,
            changedProducts = improvements.changedProducts
        )
    }

    private data class ImprovementPlan(
        val bulletPoints: List<String>,
        val overallNote: String,
        val instructionForGeneration: String,
        val changedProducts: List<ChangedProduct> = emptyList()
    )

    private fun analyseAndPlanImprovements(
        sceneB64: String,
        roomDesc: String,
        productDesc: String,
        hasDetail: Boolean,
        userScore: Int?,
        userFlaws: List<String>
    ): ImprovementPlan {
        val flawContext = if (userFlaws.isNotEmpty())
            "Known issues with this scene: ${userFlaws.joinToString("; ")}."
        else ""
        val scoreContext = if (userScore != null)
            "Current scene score: $userScore/100."
        else ""

        val request = GeminiRequest(
            contents = listOf(Content(parts = listOf(
                Part(inlineData = InlineData("image/jpeg", sceneB64)),
                Part(text = """
                    You are a world-class interior designer reviewing a user's room design scene.
                    Room context: $roomDesc
                    Product placed: $productDesc
                    $scoreContext $flawContext

                    Analyse this scene and decide what specific improvements would make it better.
                    Focus on: product placement, spatial balance, lighting, colour harmony, and overall composition.
                    Only suggest changes that are genuinely needed — if the scene is already excellent, say so.

                    Return ONLY strict JSON, no markdown:
                    {
                      "improvements": [
                        "<specific improvement 1, e.g. 'Moved the lamp to the left corner to balance the sofa visual weight'>",
                        "<specific improvement 2>",
                        "<specific improvement 3>"
                      ],
                      "overall_note": "<1-2 sentence summary of the overall improvement direction>",
                      "generation_instruction": "<A single precise instruction for image generation describing exactly what to change, e.g. Reposition the floor lamp to the far left corner, add a light wool area rug under the seating zone, shift the plant closer to the window>",
                      "changed_products": [
                        {
                          "type": "<ADDED | REPLACED | REMOVED>",
                          "name": "<name of the new or removed product, e.g. 'Jute Area Rug'>",
                          "replaced_name": "<original product name if type is REPLACED, else empty string>",
                          "reason": "<why this change was made>",
                          "search_query": "<precise Google Shopping search query — empty string if REMOVED>"
                        }
                      ]
                    }
                    Track EVERY product change in changed_products:
                    - ADDED: a completely new product was introduced to the scene
                    - REPLACED: an existing product was swapped for a different one (e.g. replaced the floor lamp style)
                    - REMOVED: an existing product was taken out of the scene
                    Use an empty array [] if no products were added, replaced, or removed.
                    If the scene is already optimal, return 1 improvement with a minor refinement suggestion.
                """.trimIndent())
            )))
        )
        val rawText = extractText(callGemini("gemini-2.5-flash", request)) ?: return ImprovementPlan(
            bulletPoints = listOf("Scene composition looks strong — minor refinements only"),
            overallNote = "Your scene is well-balanced.",
            instructionForGeneration = "Keep the existing arrangement but slightly refine the product position for optimal balance."
        )
        return try {
            val clean  = rawText.replace("```json", "").replace("```", "").trim()
            val parsed = json.decodeFromString<Map<String, JsonElement>>(clean)
            val bullets = try {
                json.decodeFromString<List<String>>(parsed["improvements"].toString())
            } catch (e: Exception) { listOf("Refine overall composition") }
            val changedProducts = try {
                val arr = json.decodeFromString<List<Map<String, JsonElement>>>(parsed["changed_products"].toString())
                arr.mapNotNull { p ->
                    val typeStr = p["type"]?.toString()?.trim('"')?.uppercase() ?: return@mapNotNull null
                    val type = when (typeStr) {
                        "ADDED"    -> ChangeType.ADDED
                        "REPLACED" -> ChangeType.REPLACED
                        "REMOVED"  -> ChangeType.REMOVED
                        else       -> return@mapNotNull null
                    }
                    ChangedProduct(
                        type         = type,
                        name         = p["name"]?.toString()?.trim('"') ?: "",
                        replacedName = p["replaced_name"]?.toString()?.trim('"') ?: "",
                        reason       = p["reason"]?.toString()?.trim('"') ?: "",
                        searchQuery  = p["search_query"]?.toString()?.trim('"') ?: ""
                    )
                }
            } catch (e: Exception) { emptyList() }
            ImprovementPlan(
                bulletPoints             = bullets,
                overallNote              = parsed["overall_note"]?.toString()?.trim('"') ?: "",
                instructionForGeneration = parsed["generation_instruction"]?.toString()?.trim('"') ?: "",
                changedProducts          = changedProducts
            )
        } catch (e: Exception) {
            ImprovementPlan(
                bulletPoints             = listOf("Refine product placement and composition"),
                overallNote              = "AI suggests compositional improvements.",
                instructionForGeneration = "Improve the placement and overall composition of the scene.",
                changedProducts          = emptyList()
            )
        }
    }

    private fun generateImprovedScene(
        sceneB64: String,
        roomDesc: String,
        productDesc: String,
        improvements: ImprovementPlan
    ): Bitmap? {
        val request = GeminiRequest(
            contents = listOf(Content(parts = listOf(
                Part(inlineData = InlineData("image/jpeg", sceneB64)),
                Part(text = """
                    You are a world-class interior design visualizer.
                    This is an existing interior design scene. Apply the following improvements to it:

                    IMPROVEMENT INSTRUCTION: ${improvements.instructionForGeneration}

                    Room context: $roomDesc
                    Product: $productDesc

                    RULES:
                    1. Make the improvements clearly visible — subtle changes are not enough
                    2. Keep all existing furniture and elements unless instructed to reposition them
                    3. You MAY add small complementary items (cushions, a rug, a small decorative object) if they directly address a composition flaw — if you do, note them
                    4. Preserve the room's lighting, style, and atmosphere
                    5. The result must look noticeably different and better than the input

                    Generate a photorealistic interior design photo showing the improved scene.
                """.trimIndent())
            ))),
            generationConfig = GenerationConfig(temperature = 1f, responseModalities = listOf("TEXT", "IMAGE"))
        )
        var response = callGemini("gemini-2.5-flash-image", request)
        var bitmap   = extractImage(response)
        if (bitmap == null) {
            response = callGemini("gemini-2.5-flash-image", request)
            bitmap   = extractImage(response)
        }
        return bitmap
    }

    // ─────────────────────────────────────────────────────────────────────────
    // QUICK GENERATION (iterative chaining)
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
        val roomDesc = describeImage(roomB64, "Describe this room: style, colors, lighting. 3-4 sentences.")
        onStepUpdate("Analyzing product...")
        val productDesc = describeImage(productB64, "Describe only what you can clearly see in this product image: what it is, shape, visible color, and only materials you can identify with certainty. Do not guess or infer. 2-3 sentences.")
        onStepUpdate("Generating scene...")
        generateCompositeImage(roomB64, productB64, detailB64, roomDesc, productDesc, prompt)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AI OPTIMIZED PLACEMENT
    // ─────────────────────────────────────────────────────────────────────────

    // ─────────────────────────────────────────────────────────────────────────
    // AI DIRECTOR CRITIQUE
    // ─────────────────────────────────────────────────────────────────────────

    private fun getAiDirectorCritique(
        sceneBase64: String,
        roomDesc: String,
        productDesc: String,
        hasDetail: Boolean = false,
        userScore: Int?,
        userFlaws: List<String>
    ): DesignDirectorFeedback? {
        val context = if (userScore != null) {
            val flawText = if (userFlaws.isNotEmpty())
                "The user's scene had these flaws: ${userFlaws.joinToString("; ")}."
            else ""
            """
            The user's scene scored $userScore/100. $flawText
            SCORING RULE: Your score MUST be higher than $userScore if you found a genuinely better placement.
            If your score would be equal to or less than $userScore, that means you did NOT find a better placement — in that case you MUST write Option B explaining why the user's placement is already optimal. Do not give the same or lower score and claim a better placement was found.
            """.trimIndent()
        } else ""

        val elementsDesc = if (hasDetail)
            "Elements in this scene: (1) a product — $productDesc, and (2) a texture or detail material also applied."
        else
            "Element in this scene: $productDesc"

        val request = GeminiRequest(
            contents = listOf(Content(parts = listOf(
                Part(inlineData = InlineData("image/jpeg", sceneBase64)),
                Part(text = """
                    You are a Design Director giving a holistic review of an AI-optimized interior design scene.
                    Room context: $roomDesc
                    $elementsDesc
                    $context

                    CRITICAL RULES:
                    1. Judge ONLY what you can directly observe in this image. Never mention, reference, or repeat any material details, finishes, or features from the product description that you cannot clearly see in the scene.
                    2. Do not use the product description as a source for critique points — only use your eyes on this image.
                    3. If you cannot clearly see a detail, do not mention it.

                    Evaluate the COMPLETE scene as a whole, judging all elements equally:
                    - Placement of each element — spatial and functional logic
                    - How the product and any texture or detail work together in the room
                    - Scale, proportion, visual balance, and overall composition
                    - Lighting coherence and color and material harmony across all elements
                    - Style cohesion — do all additions belong in this room?

                    Do NOT critique the inherent design of any element — only how each works in this space.
                    Score honestly — if this scene improves on the user's known flaws, score it higher.

                    Return ONLY strict JSON, no markdown:
                    {
                      "score": <0-100>,
                      "status": "<APPROVED|NEEDS_REVISION|REJECTED>",
                      "critique_summary": "<2-3 sentences reviewing the overall scene and how all elements work together>",
                      "detected_style": "<room's interior design style>",
                      "flaws": ["<observed flaw based on visible evidence>"],
                      "suggestion_for_improvement": "<one concrete improvement for the overall scene>"
                    }
                """.trimIndent())
            )))
        )
        val rawText = extractText(callGemini("gemini-2.5-flash", request)) ?: return null
        return parseDirectorFeedback(rawText)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ROOM STYLE CARD
    // ─────────────────────────────────────────────────────────────────────────

    private fun getRoomStyleCard(roomBase64: String, roomDescription: String): RoomStyleCard? {
        val request = GeminiRequest(
            contents = listOf(Content(parts = listOf(
                Part(inlineData = InlineData("image/jpeg", roomBase64)),
                Part(text = """
                    Expert interior designer and color theorist. Room: $roomDescription
                    Return ONLY strict JSON, no markdown:
                    {
                      "style_name": "<e.g. Scandinavian Minimalist, Mid-Century Modern, Japandi>",
                      "dominant_colors": ["<hex1>", "<hex2>", "<hex3>"],
                      "key_traits": ["<trait1>", "<trait2>", "<trait3>", "<trait4>"],
                      "style_description": "<2 sentences describing this style>"
                    }
                    For colors, estimate actual hex codes from the image (e.g. "#C4A882").
                """.trimIndent())
            )))
        )
        val rawText = extractText(callGemini("gemini-2.5-flash", request)) ?: return null
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
    // SIZE CHECK
    // ─────────────────────────────────────────────────────────────────────────

    private fun getSizeWarning(
        roomBase64: String,
        productBase64: String,
        roomDesc: String,
        productDesc: String
    ): SizeWarning? {
        val request = GeminiRequest(
            contents = listOf(Content(parts = listOf(
                Part(inlineData = InlineData("image/jpeg", roomBase64)),
                Part(inlineData = InlineData("image/jpeg", productBase64)),
                Part(text = """
                    Interior design spatial proportion expert.
                    Room: $roomDesc  Product: $productDesc
                    Using visible furniture as reference (sofas ~85cm, doors ~200cm, chairs ~80cm),
                    assess if the product is appropriately sized for this room.
                    Return ONLY strict JSON, no markdown:
                    {
                      "label": "<PERFECT FIT | TOO LARGE | TOO SMALL>",
                      "has_issue": <true|false>,
                      "description": "<one short sentence, e.g. 'The lamp is well-proportioned for this living room.'>"
                    }
                """.trimIndent())
            )))
        )
        val rawText = extractText(callGemini("gemini-2.5-flash", request)) ?: return null
        return try {
            val clean  = rawText.replace("```json", "").replace("```", "").trim()
            val parsed = json.decodeFromString<Map<String, JsonElement>>(clean)
            SizeWarning(
                label       = parsed["label"]?.toString()?.trim('"') ?: "PERFECT FIT",
                hasIssue    = parsed["has_issue"]?.toString()?.trim('"') == "true",
                description = parsed["description"]?.toString()?.trim('"') ?: ""
            )
        } catch (e: Exception) { null }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MOOD BOARD
    // ─────────────────────────────────────────────────────────────────────────

    private fun getMoodBoard(
        sceneBase64: String,
        styleCard: RoomStyleCard?,
        roomDescription: String
    ): MoodBoard? {
        val styleContext = styleCard?.let {
            "Detected style: ${it.styleName}. Traits: ${it.keyTraits.joinToString(", ")}."
        } ?: ""

        val request = GeminiRequest(
            contents = listOf(Content(parts = listOf(
                Part(inlineData = InlineData("image/jpeg", sceneBase64)),
                Part(text = """
                    Luxury interior designer building a curated mood board.
                    Room: $roomDescription  $styleContext
                    Suggest exactly 4 real, buyable complementary products.
                    Variety: lighting, textile, accent furniture, decor. No fictional brands.
                    Return ONLY a strict JSON array, no markdown:
                    [
                      {
                        "name": "<specific product name, e.g. 'Rattan Pendant Light'>",
                        "category": "<Lighting|Textile|Accent Furniture|Wall Decor|Plant|Mirror|Decor>",
                        "description": "<1-2 sentences on appearance>",
                        "rationale": "<why it fits this specific room>",
                        "price_range": "<e.g. $50-$150>",
                        "search_query": "<precise Google Shopping search query>"
                      }
                    ]
                """.trimIndent())
            )))
        )
        val rawText = extractText(callGemini("gemini-2.5-flash", request)) ?: return null
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
    // COMPOSITE IMAGE GENERATION
    // ─────────────────────────────────────────────────────────────────────────

    // Returns Pair(description, shortName)
    private fun analyseProduct(productBase64: String): Pair<String, String> {
        val request = GeminiRequest(
            contents = listOf(Content(parts = listOf(
                Part(inlineData = InlineData("image/jpeg", productBase64)),
                Part(text = """
                    You are a product analyst. Look at this product image and return ONLY strict JSON, no markdown:
                    {
                      "name": "<2-3 word product name using only what is clearly visible, e.g. 'Wooden Floor Lamp', 'White Ceramic Vase', 'Grey Linen Sofa'>",
                      "description": "<2-3 sentence description: what it is, shape, visible color, clearly identifiable materials only. No guessing.>"
                    }
                    Be specific with the name — use the product type and its most obvious visible characteristic.
                    Never use generic names like 'Product' or 'Item'.
                """.trimIndent())
            )))
        )
        val rawText = extractText(callGemini("gemini-2.5-flash", request)) ?: return Pair("", "")
        return try {
            val clean  = rawText.replace("```json", "").replace("```", "").trim()
            val parsed = json.decodeFromString<Map<String, JsonElement>>(clean)
            val name   = parsed["name"]?.toString()?.trim('"') ?: ""
            val desc   = parsed["description"]?.toString()?.trim('"') ?: ""
            Pair(desc, name)
        } catch (_: Exception) {
            Pair(rawText.trim(), "")
        }
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

        val imageManifest = buildCompositeImageManifest(
            hasDetail   = detailBase64 != null,
            productDesc = productDesc
        )

        val taskDesc = if (detailBase64 != null) {
            """
            TASK — complete ALL of the following in the final image:
            - PRIMARY: The PRODUCT OBJECT from Image 2 ($productDesc) MUST be physically present and clearly visible standing in the room at the instructed location. This is the most important element.
            - SECONDARY: Apply the MATERIAL/TEXTURE from Image 3 across the ENTIRE wall surface, floor to ceiling.
            Both must be visible. Missing the product is an automatic failure.
            """.trimIndent()
        } else {
            """
            TASK:
            - The PRODUCT OBJECT from Image 2 ($productDesc) MUST be physically present and clearly visible standing in the room at the instructed location.
            This is the most important requirement. The product must be easy to see in the final image.
            """.trimIndent()
        }

        val strictReqs = if (detailBase64 != null) {
            """
            STRICT REQUIREMENTS:
            1. MOST IMPORTANT: The product object (Image 2) MUST be clearly visible and physically present in the room. If the product is not visible, the image is wrong.
            2. The texture or material (Image 3) MUST cover the full wall surface from floor to ceiling.
            3. Preserve room perspective and lighting exactly.
            4. Do NOT add or modify any existing furniture.
            """.trimIndent()
        } else {
            """
            STRICT REQUIREMENTS:
            1. MOST IMPORTANT: The product object (Image 2) MUST be clearly visible and physically present in the room. If the product is not visible, the image is wrong.
            2. Preserve room perspective and lighting exactly.
            3. Do NOT add or modify any existing furniture.
            """.trimIndent()
        }

        // Is this an iterative stage? If the room image already contains placed products,
        // mention that context so the AI preserves them and only adds the new one.
        val iterativeNote = if (roomDesc.contains("lamp", ignoreCase = true) ||
            roomDesc.contains("plant", ignoreCase = true) ||
            roomDesc.contains("chair", ignoreCase = true) ||
            roomDesc.contains("rug", ignoreCase = true) ||
            roomDesc.contains("sofa", ignoreCase = true) ||
            roomDesc.contains("table", ignoreCase = true)) {
            "NOTE: Image 1 may already contain previously placed products. Preserve ALL existing elements exactly — only add the new product/detail as instructed."
        } else ""

        parts.add(Part(text = """
            You are a world-class interior design visualizer and spatial reasoning expert.
            ROOM CONTEXT: $roomDesc

            $imageManifest

            $iterativeNote

            USER PLACEMENT INSTRUCTION: "$userPrompt"

            Interpret the instruction intelligently like a professional designer:
            - "by the window" / "near the window" = place it right next to or flanking the window frame
            - "add plant by the window" = place the plant directly beside the window, on the floor
            - "next to the sofa / couch" = on the floor directly beside the sofa
            - "in the corner" = in the most natural visible corner of the room
            - "against the wall" = pushed up against the specified wall
            - "add texture to the wall" = apply Image 3 across the ENTIRE wall surface, floor to ceiling, corner to corner
            - "on the wall" = covering the full intended wall surface
            - For any short or vague instruction, use your spatial reasoning and design expertise to choose the most logical, aesthetically pleasing position that matches the instruction's intent

            $taskDesc

            $strictReqs

            Generate a single photorealistic, professional interior design photo showing the completed scene.
        """.trimIndent()))

        val request = GeminiRequest(
            contents = listOf(Content(parts = parts)),
            generationConfig = GenerationConfig(temperature = 1f, responseModalities = listOf("TEXT", "IMAGE"))
        )
        return extractImage(callGemini("gemini-2.5-flash-image", request))
    }

    private fun buildCompositeImageManifest(hasDetail: Boolean, productDesc: String): String {
        return if (hasDetail) {
            """
            IMAGE MANIFEST — you have been given 3 images:
            - IMAGE 1: The room photo (base scene — do not modify existing furniture)
            - IMAGE 2: A PRODUCT OBJECT that must be physically placed standing in the room ($productDesc)
            - IMAGE 3: A MATERIAL or TEXTURE SAMPLE that must be applied to cover a wall surface
            """.trimIndent()
        } else {
            """
            IMAGE MANIFEST — you have been given 2 images:
            - IMAGE 1: The room photo (base scene — do not modify existing furniture)
            - IMAGE 2: A PRODUCT OBJECT that must be physically placed standing in the room ($productDesc)
            """.trimIndent()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DIRECTOR CRITIQUE
    // ─────────────────────────────────────────────────────────────────────────

    private fun getDirectorCritique(
        sceneBase64: String,
        roomDesc: String,
        productDesc: String,
        hasDetail: Boolean = false
    ): DesignDirectorFeedback? {
        val elementsDesc = if (hasDetail)
            "Elements added to the room: (1) a product — $productDesc, and (2) a texture or detail material also applied to the space."
        else
            "Element added to the room: $productDesc"

        val request = GeminiRequest(
            contents = listOf(Content(parts = listOf(
                Part(inlineData = InlineData("image/jpeg", sceneBase64)),
                Part(text = """
                    You are a seasoned Design Director giving a holistic review of an interior design scene.
                    Room context: $roomDesc
                    $elementsDesc

                    CRITICAL RULES:
                    1. Judge ONLY what you can directly observe in this image. Never mention, reference, or repeat any material details, finishes, or features from the product description that you cannot clearly see in the scene.
                    2. Do not use the product description as a source for critique points — only use your eyes on this image.
                    3. If you cannot clearly see a detail, do not mention it.

                    Evaluate the COMPLETE scene as a whole, judging all added elements equally:
                    - Placement of each element — does each position make spatial and functional sense?
                    - How the product and any texture or detail work together in the room
                    - Scale and proportion of all elements relative to the space and furniture
                    - Visual balance and overall composition of the room
                    - Lighting coherence — do the additions suit the room's light quality?
                    - Color and material harmony across all elements and the existing room palette
                    - Style cohesion — do all additions belong in this room aesthetically?

                    Do NOT critique the inherent design of any element — only how each works within this specific space and with each other.

                    Return ONLY strict JSON, no markdown:
                    {
                      "score": <0-100>,
                      "status": "<APPROVED|NEEDS_REVISION|REJECTED>",
                      "critique_summary": "<2-3 sentences reviewing the overall scene and how all elements work together>",
                      "detected_style": "<detected interior design style of the room>",
                      "flaws": ["<observed flaw 1>", "<observed flaw 2>"],
                      "suggestion_for_improvement": "<one concrete actionable improvement for the overall scene>"
                    }
                """.trimIndent())
            )))
        )
        val rawText = extractText(callGemini("gemini-2.5-flash", request)) ?: return null
        return parseDirectorFeedback(rawText)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SHARED HELPERS
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

    private fun parseDirectorFeedback(rawText: String): DesignDirectorFeedback? = try {
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

fun buildRetailerUrls(searchQuery: String): Triple<String, String, String> {
    val encoded = URLEncoder.encode(searchQuery, "UTF-8")
    return Triple(
        "https://www.ikea.com/us/en/search/?q=$encoded",
        "https://www.amazon.com/s?k=$encoded",
        "https://www.wayfair.com/keyword.php?keyword=$encoded"
    )
}