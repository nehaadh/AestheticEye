package com.example.aestheticeye.data

import android.graphics.Bitmap
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

sealed class UiState {
    object Idle : UiState()
    data class Loading(val step: String = "Initializing...") : UiState()
    data class Success(val result: AestheticResult) : UiState()
    data class Error(val message: String) : UiState()
}

data class AestheticResult(
    val generatedBitmap: Bitmap?,
    val feedback: String,
    val directorFeedback: DesignDirectorFeedback? = null
)

data class DesignDirectorFeedback(
    val score: Int,
    val status: String,
    val critiqueSummary: String,
    val detectedStyle: String,
    val flaws: List<String>,
    val suggestionForImprovement: String
)

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