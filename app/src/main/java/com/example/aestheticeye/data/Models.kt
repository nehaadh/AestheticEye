package com.example.aestheticeye.data

import android.graphics.Bitmap
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── UI State ──────────────────────────────────────────────────────────────────

sealed class UiState {
    object Idle : UiState()
    data class Loading(val step: String = "Initializing...") : UiState()
    data class Success(val result: AestheticResult) : UiState()
    data class Error(val message: String) : UiState()
}

// ── Comparison state (separate from main UiState) ─────────────────────────────

sealed class ComparisonState {
    object Idle : ComparisonState()
    data class Loading(val step: String = "Generating AI placement...") : ComparisonState()
    data class Success(val comparison: AiComparisonResult) : ComparisonState()
    data class Error(val message: String) : ComparisonState()
}

// ── Core Result ───────────────────────────────────────────────────────────────

data class AestheticResult(
    val generatedBitmap: Bitmap?,
    val feedback: String,
    val directorFeedback: DesignDirectorFeedback? = null,
    val roomStyleCard: RoomStyleCard? = null,
    val sizeWarning: SizeWarning? = null,
    val moodBoard: MoodBoard? = null,

    // Stashed for on-demand AI comparison — not displayed directly
    val _roomB64: String? = null,
    val _productB64: String? = null,
    val _detailB64: String? = null,
    val _roomDesc: String? = null,
    val _productDesc: String? = null
)

// ── Feature 1: AI Comparison ──────────────────────────────────────────────────

data class AiComparisonResult(
    val aiBitmap: Bitmap?,
    val aiFeedback: DesignDirectorFeedback?,
    val aiRationale: String = ""
)

// ── Feature 2: Room Style Card ────────────────────────────────────────────────

data class RoomStyleCard(
    val styleName: String,
    val dominantColors: List<String>,
    val keyTraits: List<String>,
    val styleDescription: String
)

// ── Feature 3: Size Reality Check ────────────────────────────────────────────

data class SizeWarning(
    val hasIssue: Boolean,
    val severity: String,           // "NONE" | "MINOR" | "MAJOR"
    val description: String,
    val recommendation: String
)

// ── Feature 4: Mood Board ─────────────────────────────────────────────────────

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

// ── Favorites (Room DB) ───────────────────────────────────────────────────────

@androidx.room.Entity(tableName = "favorites")
data class FavoriteScene(
    @androidx.room.PrimaryKey(autoGenerate = true) val id: Int = 0,
    val projectName: String,
    val createdAt: Long = System.currentTimeMillis(),
    val score: Int,
    val detectedStyle: String,
    val critiqueSummary: String,
    val imagePath: String,
    val productsPlaced: Int = 1
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
    @SerialName("mimeType")
    val mimeType: String,
    val data: String
)

@Serializable
data class GenerationConfig(
    val temperature: Float = 1f,
    @SerialName("responseModalities")
    val responseModalities: List<String>? = null
)

@Serializable
data class GeminiResponse(
    val candidates: List<Candidate>? = null,
    val error: GeminiError? = null
)

@Serializable
data class Candidate(
    val content: Content? = null
)

@Serializable
data class GeminiError(
    val code: Int,
    val message: String,
    val status: String
)