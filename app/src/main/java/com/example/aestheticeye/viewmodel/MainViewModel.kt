package com.example.aestheticeye.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.aestheticeye.BuildConfig
import com.example.aestheticeye.data.*
import com.example.aestheticeye.network.GeminiService
import com.example.aestheticeye.network.ProductUrlExtractor
import com.example.aestheticeye.ui.components.ImportSlotType
import com.example.aestheticeye.ui.components.UrlImportState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class MainViewModel(
    private val favoritesDao: FavoritesDao
) : ViewModel() {

    private val geminiService = GeminiService(apiKey = BuildConfig.GEMINI_API_KEY)
    private val urlExtractor  = ProductUrlExtractor(apiKey = BuildConfig.GEMINI_API_KEY)

    // ── Image slots ───────────────────────────────────────────────────────────
    private val _roomBitmap    = MutableStateFlow<Bitmap?>(null)
    private val _productBitmap = MutableStateFlow<Bitmap?>(null)
    private val _detailBitmap  = MutableStateFlow<Bitmap?>(null)

    val roomBitmap:    StateFlow<Bitmap?> = _roomBitmap.asStateFlow()
    val productBitmap: StateFlow<Bitmap?> = _productBitmap.asStateFlow()
    val detailBitmap:  StateFlow<Bitmap?> = _detailBitmap.asStateFlow()

    // ── Labels ────────────────────────────────────────────────────────────────
    private val _roomLabel    = MutableStateFlow("1. Room Photo")
    private val _productLabel = MutableStateFlow("2. Product")
    private val _detailLabel  = MutableStateFlow("3. Detail")

    val roomLabel:    StateFlow<String> = _roomLabel.asStateFlow()
    val productLabel: StateFlow<String> = _productLabel.asStateFlow()
    val detailLabel:  StateFlow<String> = _detailLabel.asStateFlow()

    // ── Prompt and main UI state ──────────────────────────────────────────────
    private val _prompt   = MutableStateFlow("")
    val prompt: StateFlow<String> = _prompt.asStateFlow()

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // ── On-demand AI comparison state (separate from main pipeline) ───────────
    private val _comparisonState = MutableStateFlow<ComparisonState>(ComparisonState.Idle)
    val comparisonState: StateFlow<ComparisonState> = _comparisonState.asStateFlow()

    // ── Iterative mode ────────────────────────────────────────────────────────
    private val _isIterating    = MutableStateFlow(false)
    val isIterating: StateFlow<Boolean> = _isIterating.asStateFlow()

    private val _productsPlaced = MutableStateFlow(0)
    val productsPlaced: StateFlow<Int> = _productsPlaced.asStateFlow()

    private var originalRoomBitmap: Bitmap? = null

    // ── Favorites ─────────────────────────────────────────────────────────────
    val favorites: StateFlow<List<FavoriteScene>> = favoritesDao
        .getAllFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _showFavorites    = MutableStateFlow(false)
    val showFavorites: StateFlow<Boolean> = _showFavorites.asStateFlow()

    private val _saveDialogVisible = MutableStateFlow(false)
    val saveDialogVisible: StateFlow<Boolean> = _saveDialogVisible.asStateFlow()

    fun openFavorites()  { _showFavorites.value = true }
    fun closeFavorites() { _showFavorites.value = false }
    fun openSaveDialog() { _saveDialogVisible.value = true }
    fun closeSaveDialog(){ _saveDialogVisible.value = false }

    fun saveFavorite(context: Context, projectName: String, bitmap: Bitmap, result: AestheticResult) {
        viewModelScope.launch {
            val imagePath = saveBitmapToInternal(context, bitmap)
            val scene = FavoriteScene(
                projectName     = projectName.ifBlank { "My Scene" },
                score           = result.directorFeedback?.score ?: 0,
                detectedStyle   = result.directorFeedback?.detectedStyle
                    ?: result.roomStyleCard?.styleName ?: "Unknown",
                critiqueSummary = result.directorFeedback?.critiqueSummary ?: "",
                imagePath       = imagePath,
                productsPlaced  = _productsPlaced.value
            )
            favoritesDao.insert(scene)
            _saveDialogVisible.value = false
        }
    }

    fun deleteFavorite(scene: FavoriteScene) {
        viewModelScope.launch {
            if (scene.imagePath.isNotBlank()) File(scene.imagePath).delete()
            favoritesDao.delete(scene)
        }
    }

    private fun saveBitmapToInternal(context: Context, bitmap: Bitmap): String {
        val dir  = File(context.filesDir, "favorites").apply { mkdirs() }
        val file = File(dir, "scene_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
        return file.absolutePath
    }

    fun loadBitmapFromPath(path: String): Bitmap? = try {
        BitmapFactory.decodeFile(path)
    } catch (e: Exception) { null }

    // ── URL Import ────────────────────────────────────────────────────────────
    private val _showUrlSheet    = MutableStateFlow(false)
    val showUrlSheet: StateFlow<Boolean> = _showUrlSheet.asStateFlow()

    private val _urlImportState  = MutableStateFlow<UrlImportState>(UrlImportState.Idle)
    val urlImportState: StateFlow<UrlImportState> = _urlImportState.asStateFlow()

    private val _activeSlot = MutableStateFlow<ImageSlot>(ImageSlot.ROOM)
    val activeSlot: StateFlow<ImageSlot> = _activeSlot.asStateFlow()

    enum class ImageSlot { ROOM, PRODUCT, DETAIL }

    // ── Camera/Gallery setters ────────────────────────────────────────────────

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

    fun clearRoom() {
        _roomBitmap.value = null; originalRoomBitmap = null
        _roomLabel.value = "1. Room Photo"
        _isIterating.value = false; _productsPlaced.value = 0
    }

    fun clearProduct() { _productBitmap.value = null; _productLabel.value = "2. Product" }
    fun clearDetail()  { _detailBitmap.value = null;  _detailLabel.value = "3. Detail" }
    fun setPrompt(newPrompt: String) { _prompt.value = newPrompt }

    fun resetResult() {
        _uiState.value = UiState.Idle
        _comparisonState.value = ComparisonState.Idle
        _isIterating.value = false
        _productsPlaced.value = 0
        _prompt.value = ""
        _roomBitmap.value = null; originalRoomBitmap = null
        _roomLabel.value = "1. Room Photo"
        _productBitmap.value = null; _productLabel.value = "2. Product"
        _detailBitmap.value = null; _detailLabel.value = "3. Detail"
    }

    // ── Iterative product placement ───────────────────────────────────────────

    fun addAnotherProduct(generatedScene: Bitmap) {
        _roomBitmap.value    = generatedScene
        _roomLabel.value     = "1. Current Scene"
        _productBitmap.value = null; _productLabel.value = "2. Next Product"
        _prompt.value = ""
        _isIterating.value = true
        _uiState.value = UiState.Idle
        _comparisonState.value = ComparisonState.Idle
    }

    fun startOver() {
        _roomBitmap.value    = originalRoomBitmap; _roomLabel.value = "1. Room Photo"
        _productBitmap.value = null; _productLabel.value = "2. Product"
        _detailBitmap.value  = null; _detailLabel.value = "3. Detail"
        _prompt.value = ""
        _isIterating.value = false; _productsPlaced.value = 0
        _uiState.value = UiState.Idle
        _comparisonState.value = ComparisonState.Idle
    }

    // ── On-demand AI Comparison ───────────────────────────────────────────────

    fun requestAiComparison() {
        val currentResult = (_uiState.value as? UiState.Success)?.result ?: return

        viewModelScope.launch {
            _comparisonState.value = ComparisonState.Loading("Generating AI placement...")
            try {
                val comparison = geminiService.generateAiComparison(
                    result       = currentResult,
                    onStepUpdate = { step -> _comparisonState.value = ComparisonState.Loading(step) }
                )
                _comparisonState.value = ComparisonState.Success(comparison)
            } catch (e: Exception) {
                _comparisonState.value = ComparisonState.Error(e.message ?: "Comparison failed.")
            }
        }
    }

    fun dismissComparison() {
        _comparisonState.value = ComparisonState.Idle
    }

    // ── URL Import ────────────────────────────────────────────────────────────

    fun openUrlSheet(slot: ImageSlot) {
        _activeSlot.value     = slot
        _urlImportState.value = UrlImportState.Idle
        _showUrlSheet.value   = true
    }

    fun closeUrlSheet() {
        _showUrlSheet.value   = false
        _urlImportState.value = UrlImportState.Idle
    }

    fun getSlotType(): ImportSlotType = when (_activeSlot.value) {
        ImageSlot.ROOM    -> ImportSlotType.ROOM
        ImageSlot.PRODUCT -> ImportSlotType.PRODUCT
        ImageSlot.DETAIL  -> ImportSlotType.DETAIL
    }

    fun extractFromUrl(url: String) {
        val isProductSlot = _activeSlot.value == ImageSlot.PRODUCT
        viewModelScope.launch {
            _urlImportState.value = UrlImportState.Loading("Starting...")
            if (isProductSlot) {
                urlExtractor.extractProductFromUrl(url) { step ->
                    _urlImportState.value = UrlImportState.Loading(step)
                }.fold(
                    onSuccess = { (product, bitmap) -> _urlImportState.value = UrlImportState.ProductResult(product, bitmap) },
                    onFailure = { error -> _urlImportState.value = UrlImportState.Error(error.message ?: "Failed") }
                )
            } else {
                urlExtractor.extractImageFromUrl(url) { step ->
                    _urlImportState.value = UrlImportState.Loading(step)
                }.fold(
                    onSuccess = { (title, bitmap) -> _urlImportState.value = UrlImportState.ImageResult(title, bitmap) },
                    onFailure = { error -> _urlImportState.value = UrlImportState.Error(error.message ?: "Failed") }
                )
            }
        }
    }

    fun useImportedImage(bitmap: Bitmap, name: String) {
        when (_activeSlot.value) {
            ImageSlot.ROOM -> {
                _roomBitmap.value = bitmap; originalRoomBitmap = bitmap
                _roomLabel.value = if (name.isNotBlank()) "1. $name" else "1. Room Photo"
                _isIterating.value = false; _productsPlaced.value = 0
            }
            ImageSlot.PRODUCT -> {
                _productBitmap.value = bitmap
                _productLabel.value  = if (name.isNotBlank()) "2. $name" else "2. Product"
            }
            ImageSlot.DETAIL -> {
                _detailBitmap.value = bitmap
                _detailLabel.value  = if (name.isNotBlank()) "3. $name" else "3. Detail"
            }
        }
        closeUrlSheet()
    }

    // ── Scene Generation ──────────────────────────────────────────────────────

    fun generateScene() {
        val room    = _roomBitmap.value
        val product = _productBitmap.value

        if (room == null)    { _uiState.value = UiState.Error("Please select a room image."); return }
        if (product == null) { _uiState.value = UiState.Error("Please select a product image."); return }
        if (BuildConfig.GEMINI_API_KEY.isBlank()) {
            _uiState.value = UiState.Error("No API key found. Add GEMINI_API_KEY to local.properties.")
            return
        }

        viewModelScope.launch {
            _uiState.value = UiState.Loading("Starting...")
            _comparisonState.value = ComparisonState.Idle
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

    private fun uriToBitmap(uri: Uri, context: Context): Bitmap? = try {
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
    } catch (e: Exception) { null }

    // ── Factory ───────────────────────────────────────────────────────────────

    class Factory(private val dao: FavoritesDao) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel(dao) as T
    }
}