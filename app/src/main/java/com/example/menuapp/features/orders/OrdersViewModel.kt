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
    val ongoingOrders: List<Order> = emptyList(),
    val deliveredOrders: List<Order> = emptyList(),
    val showImages: Boolean = true,
    val selectedTabIndex: Int = 0
)

@HiltViewModel
class OrdersViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val firebaseSettingsService: FirebaseSettingsService
) : ViewModel() {

    private val _showImages = firebaseSettingsService.getShowImagesSetting()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    private val _selectedTabIndex = MutableStateFlow(0)

    val uiState: StateFlow<OrdersUiState> = combine(
        orderRepository.getOngoingOrders(),
        orderRepository.getDeliveredOrders(),
        _showImages,
        _selectedTabIndex
    ) { ongoing, delivered, showImages, tabIndex ->
        OrdersUiState(
            ongoingOrders = ongoing,
            deliveredOrders = delivered,
            showImages = showImages,
            selectedTabIndex = tabIndex
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = OrdersUiState()
    )

    fun onTabSelected(index: Int) {
        _selectedTabIndex.value = index
    }
}