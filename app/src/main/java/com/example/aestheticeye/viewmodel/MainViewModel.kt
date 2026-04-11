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
import com.example.aestheticeye.ui.components.ImportSlotType
import com.example.aestheticeye.ui.components.UrlImportState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val geminiService = GeminiService(apiKey = BuildConfig.GEMINI_API_KEY)
    private val urlExtractor = ProductUrlExtractor(apiKey = BuildConfig.GEMINI_API_KEY)

    // ── Image slots ─────────────────────────────────────────────────
    private val _roomBitmap    = MutableStateFlow<Bitmap?>(null)
    private val _productBitmap = MutableStateFlow<Bitmap?>(null)
    private val _detailBitmap  = MutableStateFlow<Bitmap?>(null)

    val roomBitmap:    StateFlow<Bitmap?> = _roomBitmap.asStateFlow()
    val productBitmap: StateFlow<Bitmap?> = _productBitmap.asStateFlow()
    val detailBitmap:  StateFlow<Bitmap?> = _detailBitmap.asStateFlow()

    // ── Labels ──────────────────────────────────────────────────────
    private val _roomLabel    = MutableStateFlow("1. Room Photo")
    private val _productLabel = MutableStateFlow("2. Product")
    private val _detailLabel  = MutableStateFlow("3. Detail")

    val roomLabel:    StateFlow<String> = _roomLabel.asStateFlow()
    val productLabel: StateFlow<String> = _productLabel.asStateFlow()
    val detailLabel:  StateFlow<String> = _detailLabel.asStateFlow()

    // ── Prompt and UI state ─────────────────────────────────────────
    private val _prompt = MutableStateFlow("")
    val prompt: StateFlow<String> = _prompt.asStateFlow()

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // ── Iterative mode: tracks whether we're adding more products ───
    // When true, the generated scene becomes the new room base and
    // the user can pick another product to add.
    private val _isIterating = MutableStateFlow(false)
    val isIterating: StateFlow<Boolean> = _isIterating.asStateFlow()

    // Keeps track of how many products have been placed
    private val _productsPlaced = MutableStateFlow(0)
    val productsPlaced: StateFlow<Int> = _productsPlaced.asStateFlow()

    // Stores the original room so user can "Start Over"
    private var originalRoomBitmap: Bitmap? = null

    // ── URL Import Sheet state ──────────────────────────────────────
    private val _showUrlSheet = MutableStateFlow(false)
    val showUrlSheet: StateFlow<Boolean> = _showUrlSheet.asStateFlow()

    private val _urlImportState = MutableStateFlow<UrlImportState>(UrlImportState.Idle)
    val urlImportState: StateFlow<UrlImportState> = _urlImportState.asStateFlow()

    private val _activeSlot = MutableStateFlow<ImageSlot>(ImageSlot.ROOM)
    val activeSlot: StateFlow<ImageSlot> = _activeSlot.asStateFlow()

    enum class ImageSlot { ROOM, PRODUCT, DETAIL }

    // ── Camera/Gallery setters ──────────────────────────────────────

    fun setRoomImage(uri: Uri, context: Context) {
        val bmp = uriToBitmap(uri, context)
        _roomBitmap.value = bmp
        originalRoomBitmap = bmp
        _roomLabel.value = "1. Room Photo"
        _isIterating.value = false
        _productsPlaced.value = 0
    }

    fun setProductImage(uri: Uri, context: Context) {
        _productBitmap.value = uriToBitmap(uri, context)
        _productLabel.value = "2. Product"
    }

    fun setDetailImage(uri: Uri, context: Context) {
        _detailBitmap.value = uriToBitmap(uri, context)
        _detailLabel.value = "3. Detail"
    }

    // ── Clear functions ─────────────────────────────────────────────

    fun clearRoom() {
        _roomBitmap.value = null
        originalRoomBitmap = null
        _roomLabel.value = "1. Room Photo"
        _isIterating.value = false
        _productsPlaced.value = 0
    }

    fun clearProduct() {
        _productBitmap.value = null
        _productLabel.value = "2. Product"
    }

    fun clearDetail() {
        _detailBitmap.value = null
        _detailLabel.value = "3. Detail"
    }

    fun setPrompt(newPrompt: String) {
        _prompt.value = newPrompt
    }

    fun resetResult() {
        _uiState.value = UiState.Idle
        _isIterating.value = false
        _productsPlaced.value = 0
        _prompt.value = ""
        // Clear everything
        _roomBitmap.value = null
        originalRoomBitmap = null
        _roomLabel.value = "1. Room Photo"
        _productBitmap.value = null
        _productLabel.value = "2. Product"
        _detailBitmap.value = null
        _detailLabel.value = "3. Detail"
    }

    // ── "Add Another Product" — uses generated scene as new room ────

    fun addAnotherProduct(generatedScene: Bitmap) {
        // The generated scene becomes the new "room"
        _roomBitmap.value = generatedScene
        _roomLabel.value = "1. Current Scene"

        // Clear product slot and prompt for the next one
        _productBitmap.value = null
        _productLabel.value = "2. Next Product"
        _prompt.value = ""

        // Keep detail if user wants
        _isIterating.value = true
        _uiState.value = UiState.Idle
    }

    fun startOver() {
        _roomBitmap.value = originalRoomBitmap
        _roomLabel.value = "1. Room Photo"
        _productBitmap.value = null
        _productLabel.value = "2. Product"
        _detailBitmap.value = null
        _detailLabel.value = "3. Detail"
        _prompt.value = ""
        _isIterating.value = false
        _productsPlaced.value = 0
        _uiState.value = UiState.Idle
    }

    // ── URL Import ──────────────────────────────────────────────────

    fun openUrlSheet(slot: ImageSlot) {
        _activeSlot.value = slot
        _urlImportState.value = UrlImportState.Idle
        _showUrlSheet.value = true
    }

    fun closeUrlSheet() {
        _showUrlSheet.value = false
        _urlImportState.value = UrlImportState.Idle
    }

    fun getSlotType(): ImportSlotType {
        return when (_activeSlot.value) {
            ImageSlot.ROOM -> ImportSlotType.ROOM
            ImageSlot.PRODUCT -> ImportSlotType.PRODUCT
            ImageSlot.DETAIL -> ImportSlotType.DETAIL
        }
    }

    fun extractFromUrl(url: String) {
        val slot = _activeSlot.value
        val isProductSlot = slot == ImageSlot.PRODUCT

        viewModelScope.launch {
            _urlImportState.value = UrlImportState.Loading("Starting...")

            if (isProductSlot) {
                val result = urlExtractor.extractProductFromUrl(url) { step ->
                    _urlImportState.value = UrlImportState.Loading(step)
                }
                result.fold(
                    onSuccess = { (product, bitmap) ->
                        _urlImportState.value = UrlImportState.ProductResult(product, bitmap)
                    },
                    onFailure = { error ->
                        _urlImportState.value = UrlImportState.Error(
                            error.message ?: "Failed to extract product"
                        )
                    }
                )
            } else {
                val result = urlExtractor.extractImageFromUrl(url) { step ->
                    _urlImportState.value = UrlImportState.Loading(step)
                }
                result.fold(
                    onSuccess = { (title, bitmap) ->
                        _urlImportState.value = UrlImportState.ImageResult(title, bitmap)
                    },
                    onFailure = { error ->
                        _urlImportState.value = UrlImportState.Error(
                            error.message ?: "Failed to extract image"
                        )
                    }
                )
            }
        }
    }

    fun useImportedImage(bitmap: Bitmap, name: String) {
        when (_activeSlot.value) {
            ImageSlot.ROOM -> {
                _roomBitmap.value = bitmap
                originalRoomBitmap = bitmap
                _roomLabel.value = if (name.isNotBlank()) "1. $name" else "1. Room Photo"
                _isIterating.value = false
                _productsPlaced.value = 0
            }
            ImageSlot.PRODUCT -> {
                _productBitmap.value = bitmap
                _productLabel.value = if (name.isNotBlank()) "2. $name" else "2. Product"
            }
            ImageSlot.DETAIL -> {
                _detailBitmap.value = bitmap
                _detailLabel.value = if (name.isNotBlank()) "3. $name" else "3. Detail"
            }
        }
        closeUrlSheet()
    }

    // ── Scene Generation ────────────────────────────────────────────

    fun generateScene() {
        val room    = _roomBitmap.value
        val product = _productBitmap.value

        if (room == null) {
            _uiState.value = UiState.Error("Please select a room image.")
            return
        }
        if (product == null) {
            _uiState.value = UiState.Error("Please select a product image.")
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
                _productsPlaced.value += 1
                _uiState.value = UiState.Success(result)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "An unknown error occurred.")
            }
        }
    }
//
    private fun uriToBitmap(uri: Uri, context: Context): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (e: Exception) { null }
    }
}