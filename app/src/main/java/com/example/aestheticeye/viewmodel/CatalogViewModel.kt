package com.example.aestheticeye.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.aestheticeye.data.CatalogFilters
import com.example.aestheticeye.data.FurnitureProduct
import com.example.aestheticeye.data.RoomImage
import com.example.aestheticeye.network.FurnitureApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class CatalogUiState {
    object Loading : CatalogUiState()
    data class Success(val products: List<FurnitureProduct>) : CatalogUiState()
    data class Error(val message: String) : CatalogUiState()
}

class CatalogViewModel(application: Application) : AndroidViewModel(application) {

    private val furnitureApi = FurnitureApiService(application.applicationContext)

    // ── Product catalog state ───────────────────────────────────────────

    private val _catalogState = MutableStateFlow<CatalogUiState>(CatalogUiState.Loading)
    val catalogState: StateFlow<CatalogUiState> = _catalogState.asStateFlow()

    private val _filters = MutableStateFlow(CatalogFilters())
    val filters: StateFlow<CatalogFilters> = _filters.asStateFlow()

    private val _selectedProduct = MutableStateFlow<FurnitureProduct?>(null)
    val selectedProduct: StateFlow<FurnitureProduct?> = _selectedProduct.asStateFlow()

    private val _selectedProductBitmap = MutableStateFlow<Bitmap?>(null)
    val selectedProductBitmap: StateFlow<Bitmap?> = _selectedProductBitmap.asStateFlow()

    private val _isDownloadingImage = MutableStateFlow(false)
    val isDownloadingImage: StateFlow<Boolean> = _isDownloadingImage.asStateFlow()

    // ── Room catalog state ──────────────────────────────────────────────

    private val _roomImages = MutableStateFlow<List<RoomImage>>(emptyList())
    val roomImages: StateFlow<List<RoomImage>> = _roomImages.asStateFlow()

    private val _selectedRoomType = MutableStateFlow<String?>(null)
    val selectedRoomType: StateFlow<String?> = _selectedRoomType.asStateFlow()

    private val _selectedRoom = MutableStateFlow<RoomImage?>(null)
    val selectedRoom: StateFlow<RoomImage?> = _selectedRoom.asStateFlow()

    private val _selectedRoomBitmap = MutableStateFlow<Bitmap?>(null)
    val selectedRoomBitmap: StateFlow<Bitmap?> = _selectedRoomBitmap.asStateFlow()

    private val _isDownloadingRoom = MutableStateFlow(false)
    val isDownloadingRoom: StateFlow<Boolean> = _isDownloadingRoom.asStateFlow()

    init {
        loadProducts()
        loadRooms()
    }

    // ── Product actions ─────────────────────────────────────────────────

    fun loadProducts() {
        viewModelScope.launch {
            _catalogState.value = CatalogUiState.Loading
            val f = _filters.value

            val result = if (f.category.isNullOrBlank() || f.category == "All") {
                furnitureApi.getHomeProducts(sortBy = f.sortBy)
            } else {
                furnitureApi.getProducts(
                    category = f.category,
                    sortBy = f.sortBy,
                    searchQuery = f.searchQuery
                )
            }

            result.fold(
                onSuccess = { products ->
                    _catalogState.value = CatalogUiState.Success(products)
                },
                onFailure = { error ->
                    _catalogState.value = CatalogUiState.Error(
                        error.message ?: "Failed to load products"
                    )
                }
            )
        }
    }

    fun setCategory(category: String) {
        val cat = if (category == "All") null else category
        _filters.value = _filters.value.copy(category = cat)
        loadProducts()
    }

    fun setSortBy(sort: String) {
        _filters.value = _filters.value.copy(sortBy = sort)
        loadProducts()
    }

    fun selectProduct(product: FurnitureProduct) {
        _selectedProduct.value = product
        _selectedProductBitmap.value = null

        viewModelScope.launch {
            _isDownloadingImage.value = true
            val bitmap = furnitureApi.downloadProductImage(product)
            _selectedProductBitmap.value = bitmap
            _isDownloadingImage.value = false
        }
    }

    fun clearSelection() {
        _selectedProduct.value = null
        _selectedProductBitmap.value = null
    }

    // ── Room actions ────────────────────────────────────────────────────

    fun loadRooms(roomType: String? = null) {
        _selectedRoomType.value = roomType
        _roomImages.value = furnitureApi.getRoomImages(roomType)
    }

    fun setRoomTypeFilter(roomType: String) {
        val type = if (roomType == "All") null else roomType
        loadRooms(type)
    }

    fun selectRoom(room: RoomImage) {
        _selectedRoom.value = room
        _selectedRoomBitmap.value = null

        viewModelScope.launch {
            _isDownloadingRoom.value = true
            val bitmap = furnitureApi.downloadRoomImage(room)
            _selectedRoomBitmap.value = bitmap
            _isDownloadingRoom.value = false
        }
    }

    fun clearRoomSelection() {
        _selectedRoom.value = null
        _selectedRoomBitmap.value = null
    }
}