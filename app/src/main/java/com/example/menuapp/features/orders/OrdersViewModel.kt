package com.example.menuapp.features.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.menuapp.data.firebase.model.Order
import com.example.menuapp.data.repository.OrderRepository
import com.example.menuapp.data.service.FirebaseSettingsService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class OrdersUiState(
    val orders: List<Order> = emptyList(),
    val showImages: Boolean = true
)

@HiltViewModel
class OrdersViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val firebaseSettingsService: FirebaseSettingsService
) : ViewModel() {

    private val _showImages = firebaseSettingsService.getShowImagesSetting()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val uiState: StateFlow<OrdersUiState> = orderRepository.getAllOrders()
        .combine(_showImages) { orders, showImages ->
            OrdersUiState(orders = orders, showImages = showImages)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = OrdersUiState()
        )
}
