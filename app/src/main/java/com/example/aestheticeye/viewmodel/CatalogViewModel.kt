package com.example.aestheticeye.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aestheticeye.data.CatalogFilters
import com.example.aestheticeye.data.FurnitureProduct
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

class CatalogViewModel : ViewModel() {

    private val furnitureApi = FurnitureApiService()

    private val _catalogState = MutableStateFlow<CatalogUiState>(CatalogUiState.Loading)
    val catalogState: StateFlow<CatalogUiState> = _catalogState.asStateFlow()

    private val _filters = MutableStateFlow(CatalogFilters())
    val filters: StateFlow<CatalogFilters> = _filters.asStateFlow()

    private val _selectedProduct = MutableStateFlow<FurnitureProduct?>(null)
    val selectedProduct: StateFlow<FurnitureProduct?> = _selectedProduct.asStateFlow()

    // Holds the downloaded bitmap of the selected product, ready for the pipeline
    private val _selectedProductBitmap = MutableStateFlow<Bitmap?>(null)
    val selectedProductBitmap: StateFlow<Bitmap?> = _selectedProductBitmap.asStateFlow()

    private val _isDownloadingImage = MutableStateFlow(false)
    val isDownloadingImage: StateFlow<Boolean> = _isDownloadingImage.asStateFlow()

    init {
        loadProducts()
    }

    fun loadProducts() {
        viewModelScope.launch {
            _catalogState.value = CatalogUiState.Loading
            val f = _filters.value

            val result = if (f.category.isNullOrBlank() || f.category == "All") {
                // "All" fetches from all home-related categories
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

    /**
     * Select a product and download its image as a Bitmap.
     */
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
}