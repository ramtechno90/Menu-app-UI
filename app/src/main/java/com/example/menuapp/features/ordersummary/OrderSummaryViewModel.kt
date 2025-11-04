package com.example.menuapp.features.ordersummary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.menuapp.data.firebase.model.Order
import com.example.menuapp.data.repository.OrderRepository
import com.example.menuapp.data.service.FirebaseSettingsService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class OrderSummaryUiState(
    val order: Order? = null,
    val isLoading: Boolean = true,
    val showImages: Boolean = true,
    val contactNumber: String = ""
)

@HiltViewModel
class OrderSummaryViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val firebaseSettingsService: FirebaseSettingsService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val orderId: String = checkNotNull(savedStateHandle["orderId"])

    private val _showImages = firebaseSettingsService.getShowImagesSetting()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    private val _restaurantDetails = firebaseSettingsService.getRestaurantDetails()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val uiState: StateFlow<OrderSummaryUiState> = combine(
        orderRepository.getOrderById(orderId),
        _showImages,
        _restaurantDetails
    ) { order, showImages, details ->
        OrderSummaryUiState(
            order = order,
            isLoading = false,
            showImages = showImages,
            contactNumber = details?.contactNumber ?: ""
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = OrderSummaryUiState(isLoading = true)
    )
}
