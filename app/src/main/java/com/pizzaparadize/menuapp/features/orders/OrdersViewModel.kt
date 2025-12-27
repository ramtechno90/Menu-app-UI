package com.pizzaparadize.menuapp.features.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pizzaparadize.menuapp.data.firebase.model.Order
import com.pizzaparadize.menuapp.data.repository.OrderRepository
import com.pizzaparadize.menuapp.data.service.FirebaseSettingsService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class OrdersUiState(
    val ongoingOrders: List<Order> = emptyList(),
    val showImages: Boolean = true,
    val contactNumber: String = ""
)

@HiltViewModel
class OrdersViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val firebaseSettingsService: FirebaseSettingsService
) : ViewModel() {

    private val _showImages = firebaseSettingsService.getShowImagesSetting()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val uiState: StateFlow<OrdersUiState> = combine(
        orderRepository.getOngoingOrders(),
        _showImages,
        firebaseSettingsService.getRestaurantDetails()
    ) { ongoing, showImages, restaurantDetails ->
        OrdersUiState(
            ongoingOrders = ongoing,
            showImages = showImages,
            contactNumber = restaurantDetails.contactNumber
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = OrdersUiState()
    )
}