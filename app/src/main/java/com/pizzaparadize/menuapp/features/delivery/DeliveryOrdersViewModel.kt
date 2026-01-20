package com.pizzaparadize.menuapp.features.delivery

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pizzaparadize.menuapp.data.auth.AuthRepository
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

data class DeliveryOrdersUiState(
    val activeOrders: List<Order> = emptyList(),
    val historyOrders: List<Order> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class DeliveryOrdersViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeliveryOrdersUiState(isLoading = true))
    val uiState: StateFlow<DeliveryOrdersUiState> = _uiState.asStateFlow()

    private var knownOrderIds: Set<String> = emptySet()
    private var isFirstLoad = true

    init {
        createNotificationChannel()
        loadOrders()
    }

    private fun loadOrders() {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser()
            if (user == null) {
                _uiState.update { it.copy(isLoading = false, error = "User not found") }
                return@launch
            }

            orderRepository.getOrdersForDeliveryStaff(user.uid).collectLatest { allOrders ->
                // Filter logic
                val active = allOrders.filter {
                    it.status == "READY_FOR_DELIVERY" || it.status == "PICKED_UP"
                }
                val history = allOrders.filter {
                    it.status == "DELIVERED"
                }

                if (isFirstLoad) {
                    knownOrderIds = active.map { it.id }.toSet()
                    isFirstLoad = false
                } else {
                    // Check for NEWly assigned active orders
                    val newOrders = active.filter { it.id !in knownOrderIds }
                    newOrders.forEach { order ->
                         sendNotification(order)
                    }
                    knownOrderIds = active.map { it.id }.toSet()
                }

                _uiState.update {
                    it.copy(
                        activeOrders = active,
                        historyOrders = history,
                        isLoading = false,
                        error = null
                    )
                }
            }
        }
    }

    fun markPickedUp(orderId: String) {
        viewModelScope.launch {
            try {
                orderRepository.markOrderAsPickedUp(orderId)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun verifyOtp(orderId: String, otp: String) {
        if (otp.length != 4) return // Simple validation
        viewModelScope.launch {
            try {
                orderRepository.updateOrderOtp(orderId, otp)
                // Note: Cloud function will verify and update status to DELIVERED
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun logout() {
        authRepository.signOut()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "New Assigned Orders"
            val descriptionText = "Notifications for orders assigned to you"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("DELIVERY_ORDERS_CHANNEL", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendNotification(order: Order) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, "DELIVERY_ORDERS_CHANNEL")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("New Order Assigned!")
            .setContentText("Order #${order.id.take(5)} is ready for delivery.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(soundUri)
            .setAutoCancel(true)

        notificationManager.notify(order.id.hashCode(), builder.build())
    }
}
