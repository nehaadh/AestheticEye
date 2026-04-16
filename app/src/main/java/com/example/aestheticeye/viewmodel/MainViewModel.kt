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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class MainViewModel(private val favoritesDao: FavoritesDao) : ViewModel() {

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

    // ── Prompt & UI state ─────────────────────────────────────────────────────
    private val _prompt  = MutableStateFlow("")
    val prompt: StateFlow<String> = _prompt.asStateFlow()

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // ── AI Comparison state ───────────────────────────────────────────────────
    private val _improvementState = MutableStateFlow<ImprovementState>(ImprovementState.Idle)
    val improvementState: StateFlow<ImprovementState> = _improvementState.asStateFlow()

    // ── Iterative mode ────────────────────────────────────────────────────────
    private val _isIterating    = MutableStateFlow(false)
    val isIterating: StateFlow<Boolean> = _isIterating.asStateFlow()

    private val _productsPlaced = MutableStateFlow(0)
    val productsPlaced: StateFlow<Int> = _productsPlaced.asStateFlow()

    // Track product labels used (for favorites summary)
    private val _productLabels = mutableListOf<String>()
    private var originalRoomBitmap: Bitmap? = null

    // Track per-stage scenes for the favorites story
    private val _stages = mutableListOf<SceneStage>()
    private var _pendingUserSceneBitmap: Bitmap? = null
    private var _pendingUserScenePath: String = ""
    private var _currentStageNumber: Int = 1
    private val _stageBitmaps = mutableMapOf<Int, Pair<Bitmap?, Bitmap?>>() // stageNum -> (user, ai)

    // ── Favorites ─────────────────────────────────────────────────────────────
    val favorites: StateFlow<List<FavoriteScene>> = favoritesDao
        .getAllFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _showFavorites     = MutableStateFlow(false)
    val showFavorites: StateFlow<Boolean> = _showFavorites.asStateFlow()

    private val _selectedFavorite  = MutableStateFlow<FavoriteScene?>(null)
    val selectedFavorite: StateFlow<FavoriteScene?> = _selectedFavorite.asStateFlow()

    private val _saveDialogVisible = MutableStateFlow(false)
    val saveDialogVisible: StateFlow<Boolean> = _saveDialogVisible.asStateFlow()

    fun openFavorites()              { _showFavorites.value = true }
    fun closeFavorites()             { _showFavorites.value = false; _selectedFavorite.value = null }
    fun openFavoriteDetail(s: FavoriteScene) { _selectedFavorite.value = s }
    fun closeFavoriteDetail()        { _selectedFavorite.value = null }
    fun openSaveDialog()             { _saveDialogVisible.value = true }
    fun closeSaveDialog()            { _saveDialogVisible.value = false }

    fun saveFavorite(context: Context, projectName: String, bitmap: Bitmap, result: AestheticResult) {
        viewModelScope.launch {
            val imagePath = saveBitmapToInternal(context, bitmap)
            val styleCard = result.roomStyleCard
            val feedback  = result.directorFeedback

            // Finalise current stage before saving
            val improvement = (_improvementState.value as? ImprovementState.Success)?.improvement
            val finalStages = buildFinalStages(
                context          = context,
                result           = result,
                improvement      = improvement,
                choseMine        = true  // saving = user is happy with current scene
            )

            val json = Json { ignoreUnknownKeys = true }
            val scene = FavoriteScene(
                projectName      = projectName.ifBlank { "My Scene" },
                score            = feedback?.score ?: 0,
                status           = feedback?.status ?: "APPROVED",
                detectedStyle    = feedback?.detectedStyle ?: styleCard?.styleName ?: "Unknown",
                critiqueSummary  = feedback?.critiqueSummary ?: "",
                flaws            = feedback?.flaws?.joinToString(" | ") ?: "",
                suggestion       = feedback?.suggestionForImprovement ?: "",
                imagePath        = imagePath,
                productsPlaced   = _productsPlaced.value,
                productLabels    = _productLabels.joinToString(", "),
                roomDescription  = result._roomDesc ?: "",
                styleDescription = styleCard?.styleDescription ?: "",
                dominantColors   = styleCard?.dominantColors?.joinToString(",") ?: "",
                keyTraits        = styleCard?.keyTraits?.joinToString(", ") ?: "",
                sizeNote         = result.sizeWarning?.let { "${it.label}: ${it.description}" } ?: "",
                stagesJson       = try { json.encodeToString(finalStages) } catch (_: Exception) { "" }
            )
            favoritesDao.insert(scene)
            _saveDialogVisible.value = false
        }
    }

    private fun buildFinalStages(
        context: Context,
        result: AestheticResult,
        improvement: AiImprovementResult?,
        choseMine: Boolean
    ): List<SceneStage> {
        // Resolve paths for all previous stages that have bitmaps stored
        val resolved = _stages.map { stage ->
            val (userBmp, aiBmp) = _stageBitmaps[stage.stageNumber] ?: Pair(null, null)
            stage.copy(
                userScenePath = if (userBmp != null) saveBitmapToInternal(context, userBmp) else stage.userScenePath,
                aiScenePath   = if (aiBmp != null) saveBitmapToInternal(context, aiBmp) else stage.aiScenePath
            )
        }.toMutableList()

        // Add the current (final) stage — save both user and AI bitmaps
        val userBitmap    = _pendingUserSceneBitmap ?: result.generatedBitmap
        val userScenePath = userBitmap?.let { saveBitmapToInternal(context, it) } ?: ""
        val aiScenePath   = improvement?.improvedBitmap?.let { saveBitmapToInternal(context, it) } ?: ""
        // choseMine = user saved without using AI's version, so userChose = choseMine

        val rawFinalLabel = _productLabel.value.removePrefix("2. ").removePrefix("Next ")
        val label = when {
            rawFinalLabel.isNotBlank() && rawFinalLabel != "Product" && rawFinalLabel != "Next Product" -> rawFinalLabel
            result._productName?.isNotBlank() == true -> result._productName!!
            else -> "Product $_currentStageNumber"
        }

        resolved.add(SceneStage(
            stageNumber    = _currentStageNumber,
            productLabel   = label,
            userScenePath  = userScenePath,
            aiScenePath    = aiScenePath,
            userChose      = choseMine,
            aiImprovements = improvement?.improvements ?: emptyList(),
            aiOverallNote  = improvement?.overallNote ?: ""
        ))
        return resolved
    }

    fun deleteFavorite(scene: FavoriteScene) {
        viewModelScope.launch {
            if (scene.imagePath.isNotBlank()) File(scene.imagePath).delete()
            favoritesDao.delete(scene)
            if (_selectedFavorite.value?.id == scene.id) _selectedFavorite.value = null
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
    } catch (_: Exception) { null }

    // ── URL Import ────────────────────────────────────────────────────────────
    private val _showUrlSheet   = MutableStateFlow(false)
    val showUrlSheet: StateFlow<Boolean> = _showUrlSheet.asStateFlow()

    private val _urlImportState = MutableStateFlow<UrlImportState>(UrlImportState.Idle)
    val urlImportState: StateFlow<UrlImportState> = _urlImportState.asStateFlow()

    private val _activeSlot = MutableStateFlow<ImageSlot>(ImageSlot.ROOM)

    enum class ImageSlot { ROOM, PRODUCT, DETAIL }

    // ── Camera/Gallery setters ────────────────────────────────────────────────

    fun setRoomImage(uri: Uri, context: Context) {
        val bmp = uriToBitmap(uri, context)
        _roomBitmap.value  = bmp
        originalRoomBitmap = bmp
        _roomLabel.value   = "1. Room Photo"
        _isIterating.value = false
        _productsPlaced.value = 0
        _productLabels.clear()
    }

    fun setProductImage(uri: Uri, context: Context) {
        _productBitmap.value = uriToBitmap(uri, context)
        _productLabel.value  = "2. Product"
    }

    fun setDetailImage(uri: Uri, context: Context) {
        _detailBitmap.value = uriToBitmap(uri, context)
        _detailLabel.value  = "3. Detail"
    }

    fun clearRoom()    { _roomBitmap.value = null; originalRoomBitmap = null; _roomLabel.value = "1. Room Photo"; _isIterating.value = false; _productsPlaced.value = 0; _stages.clear(); _currentStageNumber = 1; _pendingUserScenePath = "" }
    fun clearProduct() { _productBitmap.value = null; _productLabel.value = "2. Product" }
    fun clearDetail()  { _detailBitmap.value = null; _detailLabel.value = "3. Detail" }
    fun setPrompt(v: String) { _prompt.value = v }

    fun resetResult() {
        _uiState.value = UiState.Idle
        _improvementState.value = ImprovementState.Idle
        _isIterating.value = false
        _productsPlaced.value = 0
        _productLabels.clear()
        _stages.clear()
        _currentStageNumber = 1
        _pendingUserScenePath = ""
        _pendingUserSceneBitmap = null
        _stageBitmaps.clear()
        _prompt.value = ""
        _roomBitmap.value = null; originalRoomBitmap = null; _roomLabel.value = "1. Room Photo"
        _productBitmap.value = null; _productLabel.value = "2. Product"
        _detailBitmap.value = null; _detailLabel.value = "3. Detail"
    }

    // ── Iterative: Add Another Product ────────────────────────────────────────

    fun addAnotherProduct(chosenBitmap: Bitmap) {
        val currentResult = when (val s = _uiState.value) {
            is UiState.Success    -> s.result
            is UiState.SceneReady -> s.result
            else -> null
        }
        val improvement = (_improvementState.value as? ImprovementState.Success)?.improvement
        val choseAi = improvement?.improvedBitmap != null && chosenBitmap == improvement.improvedBitmap

        // Derive a meaningful product label: URL name > Gemini product name > fallback
        val rawLabel = _productLabel.value.removePrefix("2. ").removePrefix("Next ")
        val currentStageResult = when (val s = _uiState.value) {
            is UiState.Success    -> s.result
            is UiState.SceneReady -> s.result
            else -> null
        }
        val label = when {
            rawLabel.isNotBlank() && rawLabel != "Product" && rawLabel != "Next Product" -> rawLabel
            currentStageResult?._productName?.isNotBlank() == true -> currentStageResult._productName!!
            else -> "Product $_currentStageNumber"
        }
        if (label.isNotBlank()) _productLabels.add(label)

        // Always store BOTH bitmaps regardless of which was chosen
        // so both can be shown in favorites history
        val userSceneBitmap = _pendingUserSceneBitmap
        val aiSceneBitmap   = improvement?.improvedBitmap  // null if AI was never run

        _stages.add(SceneStage(
            stageNumber    = _currentStageNumber,
            productLabel   = label,
            userScenePath  = "",   // resolved at save time
            aiScenePath    = "",   // resolved at save time
            userChose      = !choseAi,
            aiImprovements = improvement?.improvements ?: emptyList(),
            aiOverallNote  = improvement?.overallNote ?: ""
        ))
        _stageBitmaps[_currentStageNumber] = Pair(userSceneBitmap, aiSceneBitmap)

        _currentStageNumber++
        _pendingUserScenePath = ""

        // The chosen scene becomes the new room base for the next stage.
        _roomBitmap.value    = chosenBitmap
        _roomLabel.value     = "1. Current Scene"
        _productBitmap.value = null; _productLabel.value = "2. Next Product"
        _detailBitmap.value  = null; _detailLabel.value  = "3. Detail"
        _prompt.value        = ""
        _isIterating.value   = true
        _uiState.value       = UiState.Idle
        _improvementState.value = ImprovementState.Idle
    }

    fun startOver() {
        _roomBitmap.value    = originalRoomBitmap; _roomLabel.value = "1. Room Photo"
        _productBitmap.value = null; _productLabel.value = "2. Product"
        _detailBitmap.value  = null; _detailLabel.value = "3. Detail"
        _prompt.value = ""
        _isIterating.value = false; _productsPlaced.value = 0
        _productLabels.clear()
        _uiState.value = UiState.Idle
        _improvementState.value = ImprovementState.Idle
    }

    // ── AI Comparison ─────────────────────────────────────────────────────────

    fun requestAiImprovement() {
        val currentResult = when (val s = _uiState.value) {
            is UiState.Success    -> s.result
            is UiState.SceneReady -> s.result
            else -> return
        }
        viewModelScope.launch {
            _improvementState.value = ImprovementState.Loading("AI is analysing your scene...")
            try {
                val improvement = geminiService.generateAiImprovement(
                    result       = currentResult,
                    onStepUpdate = { step -> _improvementState.value = ImprovementState.Loading(step) }
                )
                _improvementState.value = ImprovementState.Success(improvement)
            } catch (e: Exception) {
                _improvementState.value = ImprovementState.Error(e.message ?: "Improvement failed.")
            }
        }
    }

    fun dismissImprovement() { _improvementState.value = ImprovementState.Idle }

    // ── URL Import ────────────────────────────────────────────────────────────

    fun openUrlSheet(slot: ImageSlot) {
        _activeSlot.value     = slot
        _urlImportState.value = UrlImportState.Idle
        _showUrlSheet.value   = true
    }

    fun closeUrlSheet() { _showUrlSheet.value = false; _urlImportState.value = UrlImportState.Idle }

    fun getSlotType(): ImportSlotType = when (_activeSlot.value) {
        ImageSlot.ROOM    -> ImportSlotType.ROOM
        ImageSlot.PRODUCT -> ImportSlotType.PRODUCT
        ImageSlot.DETAIL  -> ImportSlotType.DETAIL
    }

    fun extractFromUrl(url: String) {
        val isProduct = _activeSlot.value == ImageSlot.PRODUCT
        viewModelScope.launch {
            _urlImportState.value = UrlImportState.Loading("Starting...")
            if (isProduct) {
                urlExtractor.extractProductFromUrl(url) { step ->
                    _urlImportState.value = UrlImportState.Loading(step)
                }.fold(
                    onSuccess = { (product, bitmap) -> _urlImportState.value = UrlImportState.ProductResult(product, bitmap) },
                    onFailure = { e -> _urlImportState.value = UrlImportState.Error(e.message ?: "Failed") }
                )
            } else {
                urlExtractor.extractImageFromUrl(url) { step ->
                    _urlImportState.value = UrlImportState.Loading(step)
                }.fold(
                    onSuccess = { (title, bitmap) -> _urlImportState.value = UrlImportState.ImageResult(title, bitmap) },
                    onFailure = { e -> _urlImportState.value = UrlImportState.Error(e.message ?: "Failed") }
                )
            }
        }
    }

    fun useImportedImage(bitmap: Bitmap, name: String) {
        when (_activeSlot.value) {
            ImageSlot.ROOM -> {
                _roomBitmap.value = bitmap; originalRoomBitmap = bitmap
                _roomLabel.value  = if (name.isNotBlank()) "1. $name" else "1. Room Photo"
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
        if (BuildConfig.GEMINI_API_KEY.isBlank()) { _uiState.value = UiState.Error("No API key found."); return }

        viewModelScope.launch {
            _uiState.value = UiState.Loading("Starting...")
            _improvementState.value = ImprovementState.Idle
            try {
                val result = geminiService.generateScene(
                    roomBitmap    = room,
                    productBitmap = product,
                    detailBitmap  = _detailBitmap.value,
                    prompt        = _prompt.value,
                    onStepUpdate  = { step ->
                        val current = _uiState.value
                        when (current) {
                            is UiState.SceneReady ->
                                _uiState.value = UiState.SceneReady(current.result, step)
                            else ->
                                _uiState.value = UiState.Loading(step)
                        }
                    },
                    onSceneReady  = { partial ->
                        val current = _uiState.value
                        if (current !is UiState.SceneReady && current !is UiState.Success) {
                            _productsPlaced.value += 1
                            // Stash bitmap — path will be saved when needed (requires context)
                            _pendingUserSceneBitmap = partial.generatedBitmap
                        }
                        _uiState.value = UiState.SceneReady(partial, "")
                    }
                )
                _uiState.value = UiState.Success(result)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "An unknown error occurred.")
            }
        }
    }

    private fun uriToBitmap(uri: Uri, context: Context): Bitmap? = try {
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
    } catch (_: Exception) { null }

    class Factory(private val dao: FavoritesDao) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel(dao) as T
    }
}