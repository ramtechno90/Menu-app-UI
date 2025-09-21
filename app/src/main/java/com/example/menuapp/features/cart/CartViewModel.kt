package com.example.menuapp.features.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.menuapp.data.local.model.CartItemEntity
import com.example.menuapp.data.repository.MenuRepository
import com.example.menuapp.data.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CartUiState(
    val cartItems: List<CartItemEntity> = emptyList(),
    val subtotal: Double = 0.0,
    val tax: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val grandTotal: Double = 0.0
)

@HiltViewModel
class CartViewModel @Inject constructor(
    private val menuRepository: MenuRepository,
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CartUiState())
    val uiState: StateFlow<CartUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            menuRepository.getCartItems().collect { items ->
                val subtotal = items.sumOf { it.price * it.quantity }
                val tax = subtotal * 0.08
                val deliveryFee = if (items.isNotEmpty()) 2.50 else 0.0
                val grandTotal = subtotal + tax + deliveryFee

                _uiState.value = CartUiState(
                    cartItems = items,
                    subtotal = subtotal,
                    tax = tax,
                    deliveryFee = deliveryFee,
                    grandTotal = grandTotal
                )
            }
        }
    }

    fun updateQuantity(itemId: Int, quantity: Int) {
        viewModelScope.launch {
            menuRepository.updateQuantity(itemId, quantity)
        }
    }

    fun removeItem(itemId: Int) {
        updateQuantity(itemId, 0)
    }

    fun placeOrder() {
        viewModelScope.launch {
            orderRepository.createOrder()
        }
    }
}
