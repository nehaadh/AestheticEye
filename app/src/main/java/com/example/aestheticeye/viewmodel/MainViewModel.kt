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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val geminiService = GeminiService(apiKey = BuildConfig.GEMINI_API_KEY)

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

    fun setRoomImage(uri: Uri, context: Context) {
        _roomBitmap.value = uriToBitmap(uri, context)
    }

    fun setProductImage(uri: Uri, context: Context) {
        _productBitmap.value = uriToBitmap(uri, context)
    }

    fun setDetailImage(uri: Uri, context: Context) {
        _detailBitmap.value = uriToBitmap(uri, context)
    }

    fun clearDetailImage() {
        _detailBitmap.value = null
    }

    fun setPrompt(newPrompt: String) {
        _prompt.value = newPrompt
    }

    fun resetResult() {
        _uiState.value = UiState.Idle
    }

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