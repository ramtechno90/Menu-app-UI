package com.example.menuapp.features.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.menuapp.data.local.model.OrderEntity
import com.example.menuapp.data.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class OrdersUiState(
    val orders: List<OrderEntity> = emptyList()
)

@HiltViewModel
class OrdersViewModel @Inject constructor(
    private val orderRepository: OrderRepository
) : ViewModel() {

    val uiState: StateFlow<OrdersUiState> = orderRepository.getAllOrders()
        .map { orders -> OrdersUiState(orders = orders) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = OrdersUiState()
        )
}
