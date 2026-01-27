package com.pizzaparadize.menuapp.features.admin

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pizzaparadize.menuapp.data.firebase.model.DeliveryStaff
import com.pizzaparadize.menuapp.data.firebase.model.Order
import com.pizzaparadize.menuapp.data.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminOrdersUiState(
    val orders: List<Order> = emptyList(),
    val deliveryStaffList: List<DeliveryStaff> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class AdminOrdersViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val authRepository: com.pizzaparadize.menuapp.data.auth.AuthRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminOrdersUiState(isLoading = true))
    val uiState: StateFlow<AdminOrdersUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            orderRepository.getAllOrdersForAdmin().collectLatest { orders ->
                _uiState.update { it.copy(orders = orders, isLoading = false) }
            }
        }

        viewModelScope.launch {
            orderRepository.getDeliveryStaff().collectLatest { staffList ->
                _uiState.update { it.copy(deliveryStaffList = staffList) }
            }
        }
    }

    fun updateOrderStatus(orderId: String, status: String) {
        viewModelScope.launch {
            orderRepository.updateOrderStatus(orderId, status)
        }
    }

    fun assignOrder(orderId: String, staffName: String, staffUid: String) {
        viewModelScope.launch {
            orderRepository.assignOrderToStaff(orderId, staffName, staffUid)
        }
    }

    fun deleteOrder(orderId: String) {
        viewModelScope.launch {
            orderRepository.deleteOrder(orderId)
        }
    }

    fun deleteOrdersByStatus(status: String) {
        viewModelScope.launch {
            orderRepository.deleteOrdersByStatus(status)
        }
    }

    fun logout() {
        authRepository.signOut()
    }
}
