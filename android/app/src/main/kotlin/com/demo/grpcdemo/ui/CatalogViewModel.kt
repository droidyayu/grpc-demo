// android/app/src/main/kotlin/com/demo/grpcdemo/ui/CatalogViewModel.kt
package com.demo.grpcdemo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demo.grpcdemo.domain.exception.CatalogException
import com.demo.grpcdemo.domain.model.Product
import com.demo.grpcdemo.domain.usecase.GetProductUseCase
import com.demo.grpcdemo.domain.usecase.ListProductsUseCase
import com.demo.grpcdemo.domain.usecase.WatchPriceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── UI state ──────────────────────────────────────────────────
// Uses domain models — no proto types ever appear here.
sealed interface CatalogUiState {
    data object Idle                              : CatalogUiState
    data object Loading                           : CatalogUiState
    data class  Products(val list: List<Product>) : CatalogUiState
    data class  Error(val message: String)        : CatalogUiState
}

data class PriceTickerState(
    val productId:  String  = "",
    val pricePaise: Long    = 0L,
    val currency:   String  = "INR",
    val isLive:     Boolean = false,
)

// ── ViewModel ─────────────────────────────────────────────────
// @HiltViewModel + @Inject constructor — Hilt generates the factory.
// Never constructs its own dependencies.
@HiltViewModel
class CatalogViewModel @Inject constructor(
    private val listProductsUseCase: ListProductsUseCase,
    private val getProductUseCase: GetProductUseCase,
    private val watchPriceUseCase: WatchPriceUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<CatalogUiState>(CatalogUiState.Idle)
    val uiState: StateFlow<CatalogUiState> = _uiState.asStateFlow()

    private val _priceTicker = MutableStateFlow(PriceTickerState())
    val priceTicker: StateFlow<PriceTickerState> = _priceTicker.asStateFlow()

    private var priceWatchJob: Job? = null

    fun loadProducts(category: String = "") {
        _uiState.value = CatalogUiState.Loading
        viewModelScope.launch {
            val products = mutableListOf<Product>()
            try {
                listProductsUseCase(category).collect { product ->
                    products.add(product)
                    _uiState.value = CatalogUiState.Products(products.toList())
                }
            } catch (e: CatalogException) {
                _uiState.value = CatalogUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun getProduct(productId: String) {
        _uiState.value = CatalogUiState.Loading
        viewModelScope.launch {
            getProductUseCase(productId)
                .onSuccess { product ->
                    _uiState.value = CatalogUiState.Products(listOf(product))
                }
                .onFailure { e ->
                    _uiState.value = CatalogUiState.Error(e.message ?: "Unknown error")
                }
        }
    }

    fun startWatchingPrice(productId: String) {
        priceWatchJob?.cancel()
        priceWatchJob = viewModelScope.launch {
            _priceTicker.update { it.copy(productId = productId, isLive = true) }
            try {
                watchPriceUseCase(productId).collect { update ->
                    _priceTicker.update {
                        it.copy(
                            pricePaise = update.pricePaise,
                            currency   = update.currency,
                        )
                    }
                }
            } catch (e: CatalogException) {
                _priceTicker.update { it.copy(isLive = false) }
            }
        }
    }

    fun stopWatchingPrice() {
        priceWatchJob?.cancel()
        _priceTicker.update { it.copy(isLive = false) }
    }

    override fun onCleared() {
        super.onCleared()
        priceWatchJob?.cancel()
    }
}
