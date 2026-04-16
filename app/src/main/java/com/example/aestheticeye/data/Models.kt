package com.example.aestheticeye.data

import android.graphics.Bitmap
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── UI State ──────────────────────────────────────────────────────────────────

sealed class UiState {
    object Idle : UiState()
    data class Loading(val step: String = "Initializing...") : UiState()
    // Scene image ready — analysis still loading in background
    data class SceneReady(val result: AestheticResult, val analysisStep: String = "") : UiState()
    data class Success(val result: AestheticResult) : UiState()
    data class Error(val message: String) : UiState()
}

sealed class ImprovementState {
    object Idle : ImprovementState()
    data class Loading(val step: String = "AI is redesigning your scene...") : ImprovementState()
    data class Success(val improvement: AiImprovementResult) : ImprovementState()
    data class Error(val message: String) : ImprovementState()
}

// ── Core Result ───────────────────────────────────────────────────────────────

data class AestheticResult(
    val generatedBitmap: Bitmap?,
    val feedback: String,
    val directorFeedback: DesignDirectorFeedback? = null,
    val roomStyleCard: RoomStyleCard? = null,
    val sizeWarning: SizeWarning? = null,
    val moodBoard: MoodBoard? = null,
    val _roomB64: String? = null,
    val _productB64: String? = null,
    val _detailB64: String? = null,
    val _roomDesc: String? = null,
    val _productDesc: String? = null,
    val _productName: String? = null,         // short 2-3 word product name from Gemini
    val _generatedSceneB64: String? = null   // user's generated scene, for comparison display
)

// ── AI Comparison ─────────────────────────────────────────────────────────────

data class AiImprovementResult(
    val improvedBitmap: Bitmap?,
    val improvements: List<String>,           // bullet list of what was changed
    val overallNote: String,                  // 1-2 sentence summary
    val changedProducts: List<ChangedProduct> = emptyList()
)

enum class ChangeType { ADDED, REPLACED, REMOVED }

data class ChangedProduct(
    val type: ChangeType,
    val name: String,                         // new product name (or removed product name)
    val replacedName: String = "",            // original name if REPLACED
    val reason: String,
    val searchQuery: String = ""              // empty for REMOVED
)

// ── Room Style ────────────────────────────────────────────────────────────────

data class RoomStyleCard(
    val styleName: String,
    val dominantColors: List<String>,
    val keyTraits: List<String>,
    val styleDescription: String
)

// ── Size Check ────────────────────────────────────────────────────────────────

data class SizeWarning(
    val label: String,          // "PERFECT FIT" | "TOO LARGE" | "TOO SMALL"
    val hasIssue: Boolean,
    val description: String
)

// ── Mood Board ────────────────────────────────────────────────────────────────

data class MoodBoard(
    val suggestions: List<ProductSuggestion>
)

data class ProductSuggestion(
    val name: String,
    val category: String,
    val description: String,
    val rationale: String,
    val priceRange: String,
    val searchQuery: String
)

// ── Scene Stages (stored as JSON in FavoriteScene.stagesJson) ────────────────

@Serializable
data class SceneStage(
    val stageNumber: Int,
    val productLabel: String,
    val userScenePath: String = "",
    val aiScenePath: String = "",
    val userChose: Boolean = true,
    val aiImprovements: List<String> = emptyList(),
    val aiOverallNote: String = ""
)

// ── Favorites ─────────────────────────────────────────────────────────────────

@androidx.room.Entity(tableName = "favorites")
data class FavoriteScene(
    @androidx.room.PrimaryKey(autoGenerate = true) val id: Int = 0,
    val projectName: String,
    val createdAt: Long = System.currentTimeMillis(),
    val score: Int,
    val status: String = "APPROVED",
    val detectedStyle: String,
    val critiqueSummary: String,
    val flaws: String = "",                 // comma-separated
    val suggestion: String = "",
    val imagePath: String,
    val productsPlaced: Int = 1,
    val productLabels: String = "",         // comma-separated product names/labels
    val roomDescription: String = "",
    val styleDescription: String = "",
    val dominantColors: String = "",        // "hex1,hex2,hex3"
    val keyTraits: String = "",             // comma-separated
    val sizeNote: String = "",
    val stagesJson: String = ""        // JSON array of SceneStage objects
)

// ── Design Director Feedback ──────────────────────────────────────────────────

data class DesignDirectorFeedback(
    val score: Int,
    val status: String,
    val critiqueSummary: String,
    val detectedStyle: String,
    val flaws: List<String>,
    val suggestionForImprovement: String
)

// ── Gemini API wire types ─────────────────────────────────────────────────────

@Serializable
data class GeminiRequest(
    val contents: List<Content>,
    @SerialName("generationConfig")
    val generationConfig: GenerationConfig? = null
)

@Serializable
data class Content(
    val parts: List<Part>,
    val role: String = "user"
)

@Serializable
data class Part(
    val text: String? = null,
    @SerialName("inlineData")
    val inlineData: InlineData? = null
)

@Serializable
data class InlineData(
    @SerialName("mimeType") val mimeType: String,
    val data: String
)

@Serializable
data class GenerationConfig(
    val temperature: Float = 1f,
    @SerialName("responseModalities") val responseModalities: List<String>? = null
)

@Serializable
data class GeminiResponse(
    val candidates: List<Candidate>? = null,
    val error: GeminiError? = null
)

@Serializable
data class Candidate(val content: Content? = null)

@Serializable
data class GeminiError(val code: Int, val message: String, val status: String)