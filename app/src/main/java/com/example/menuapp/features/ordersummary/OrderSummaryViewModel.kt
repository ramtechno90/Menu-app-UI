package com.example.menuapp.features.ordersummary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.menuapp.data.local.model.OrderEntity
import com.example.menuapp.data.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class OrderSummaryUiState(
    val order: OrderEntity? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class OrderSummaryViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val orderId: String = checkNotNull(savedStateHandle["orderId"])

    val uiState: StateFlow<OrderSummaryUiState> = orderRepository.getOrderById(orderId)
        .map { order -> OrderSummaryUiState(order = order, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = OrderSummaryUiState(isLoading = true)
        )
}
