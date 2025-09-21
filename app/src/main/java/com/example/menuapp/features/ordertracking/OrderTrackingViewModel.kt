package com.example.menuapp.features.ordertracking

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.menuapp.data.local.model.OrderEntity
import com.example.menuapp.data.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class OrderTrackingUiState(
    val order: OrderEntity? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class OrderTrackingViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val orderId: String = checkNotNull(savedStateHandle["orderId"])

    val uiState: StateFlow<OrderTrackingUiState> = orderRepository.getOrderById(orderId)
        .map { order -> OrderTrackingUiState(order = order, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = OrderTrackingUiState(isLoading = true)
        )
}
