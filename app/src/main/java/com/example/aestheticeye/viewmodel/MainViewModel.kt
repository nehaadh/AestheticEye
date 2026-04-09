package com.example.aestheticeye.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aestheticeye.BuildConfig
import com.example.aestheticeye.data.UiState
import com.example.aestheticeye.network.GeminiService
import com.example.aestheticeye.network.ProductUrlExtractor
import com.example.aestheticeye.ui.components.UrlProductState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val geminiService = GeminiService(apiKey = BuildConfig.GEMINI_API_KEY)
    private val urlExtractor = ProductUrlExtractor(apiKey = BuildConfig.GEMINI_API_KEY)

    private val _roomBitmap    = MutableStateFlow<Bitmap?>(null)
    private val _productBitmap = MutableStateFlow<Bitmap?>(null)
    private val _detailBitmap  = MutableStateFlow<Bitmap?>(null)

    val roomBitmap:    StateFlow<Bitmap?> = _roomBitmap.asStateFlow()
    val productBitmap: StateFlow<Bitmap?> = _productBitmap.asStateFlow()
    val detailBitmap:  StateFlow<Bitmap?> = _detailBitmap.asStateFlow()

    private val _prompt = MutableStateFlow("Place the product on the window, making it look natural.")
    val prompt: StateFlow<String> = _prompt.asStateFlow()

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // ── Track whether product came from catalog ─────────────────────
    private val _productSource = MutableStateFlow<ProductSource>(ProductSource.None)
    val productSource: StateFlow<ProductSource> = _productSource.asStateFlow()

    // ── Navigation state for showing catalog ────────────────────────
    private val _showCatalog = MutableStateFlow(false)
    val showCatalog: StateFlow<Boolean> = _showCatalog.asStateFlow()

    // ── URL product extraction state ────────────────────────────────
    private val _showUrlSheet = MutableStateFlow(false)
    val showUrlSheet: StateFlow<Boolean> = _showUrlSheet.asStateFlow()

    private val _urlProductState = MutableStateFlow<UrlProductState>(UrlProductState.Idle)
    val urlProductState: StateFlow<UrlProductState> = _urlProductState.asStateFlow()

    fun openCatalog() { _showCatalog.value = true }
    fun closeCatalog() { _showCatalog.value = false }

    fun openUrlSheet() {
        _urlProductState.value = UrlProductState.Idle
        _showUrlSheet.value = true
    }
    fun closeUrlSheet() {
        _showUrlSheet.value = false
        _urlProductState.value = UrlProductState.Idle
    }

    fun setRoomImage(uri: Uri, context: Context) {
        _roomBitmap.value = uriToBitmap(uri, context)
    }

    fun setProductImage(uri: Uri, context: Context) {
        _productBitmap.value = uriToBitmap(uri, context)
        _productSource.value = ProductSource.Camera
    }

    fun setDetailImage(uri: Uri, context: Context) {
        _detailBitmap.value = uriToBitmap(uri, context)
    }

    fun clearDetailImage() {
        _detailBitmap.value = null
    }

    /**
     * Set product from catalog — accepts a Bitmap directly
     * instead of a Uri, since the image was downloaded from the API.
     */
    fun setProductFromCatalog(bitmap: Bitmap, productName: String) {
        _productBitmap.value = bitmap
        _productSource.value = ProductSource.Catalog(productName)
        _showCatalog.value = false  // Return to main screen
    }

    /**
     * Set product from URL extraction — accepts a Bitmap and name.
     */
    fun setProductFromUrl(bitmap: Bitmap, productName: String) {
        _productBitmap.value = bitmap
        _productSource.value = ProductSource.Url(productName)
        _showUrlSheet.value = false
        _urlProductState.value = UrlProductState.Idle
    }

    fun clearProduct() {
        _productBitmap.value = null
        _productSource.value = ProductSource.None
    }

    fun setPrompt(newPrompt: String) {
        _prompt.value = newPrompt
    }

    fun resetResult() {
        _uiState.value = UiState.Idle
    }

    // ── URL Product Extraction ──────────────────────────────────────

    fun extractProductFromUrl(url: String) {
        viewModelScope.launch {
            _urlProductState.value = UrlProductState.Loading("Starting extraction...")

            val result = urlExtractor.extractFromUrl(url) { step ->
                _urlProductState.value = UrlProductState.Loading(step)
            }

            result.fold(
                onSuccess = { (product, bitmap) ->
                    _urlProductState.value = UrlProductState.Success(product, bitmap)
                },
                onFailure = { error ->
                    _urlProductState.value = UrlProductState.Error(
                        error.message ?: "Failed to extract product"
                    )
                }
            )
        }
    }

    // ── Scene Generation ────────────────────────────────────────────

    fun generateScene() {
        val room    = _roomBitmap.value
        val product = _productBitmap.value

        if (room == null || product == null) {
            _uiState.value = UiState.Error("Please select both a room and a product image.")
            return
        }

        if (BuildConfig.GEMINI_API_KEY.isBlank()) {
            _uiState.value = UiState.Error("No API key found. Add GEMINI_API_KEY to local.properties and rebuild.")
            return
        }

        viewModelScope.launch {
            _uiState.value = UiState.Loading("Starting...")
            try {
                val result = geminiService.generateScene(
                    roomBitmap    = room,
                    productBitmap = product,
                    detailBitmap  = _detailBitmap.value,
                    prompt        = _prompt.value,
                    onStepUpdate  = { step -> _uiState.value = UiState.Loading(step) }
                )
                _uiState.value = UiState.Success(result)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "An unknown error occurred.")
            }
        }
    }

    private fun uriToBitmap(uri: Uri, context: Context): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (e: Exception) { null }
    }
}

/**
 * Tracks where the product image came from so the UI
 * can show the appropriate label and clear behavior.
 */
sealed class ProductSource {
    object None : ProductSource()
    object Camera : ProductSource()
    data class Catalog(val productName: String) : ProductSource()
    data class Url(val productName: String) : ProductSource()
}