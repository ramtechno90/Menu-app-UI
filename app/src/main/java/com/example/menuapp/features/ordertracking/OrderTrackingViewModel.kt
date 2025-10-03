package com.example.menuapp.features.ordertracking

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.menuapp.data.firebase.model.Order
import com.example.menuapp.data.repository.OrderRepository
import com.example.menuapp.data.service.FirebaseSettingsService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class OrderTrackingUiState(
    val order: Order? = null,
    val isLoading: Boolean = true,
    val showImages: Boolean = true
)

@HiltViewModel
class OrderTrackingViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val firebaseSettingsService: FirebaseSettingsService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val orderId: String = checkNotNull(savedStateHandle["orderId"])

    private val _showImages = firebaseSettingsService.getShowImagesSetting()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val uiState: StateFlow<OrderTrackingUiState> = orderRepository.getOrderById(orderId)
        .combine(_showImages) { order, showImages ->
            OrderTrackingUiState(order = order, isLoading = false, showImages = showImages)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = OrderTrackingUiState(isLoading = true)
        )
}
