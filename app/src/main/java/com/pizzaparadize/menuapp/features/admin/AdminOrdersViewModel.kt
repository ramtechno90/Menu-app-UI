package com.pizzaparadize.menuapp.features.admin

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pizzaparadize.menuapp.R
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

    // Keep track of known orders to detect new ones
    private var knownOrderIds: Set<String> = emptySet()
    private var isFirstLoad = true

    init {
        createNotificationChannel()
        viewModelScope.launch {
            orderRepository.getAllOrdersForAdmin().collectLatest { orders ->
                if (isFirstLoad) {
                    knownOrderIds = orders.map { it.id }.toSet()
                    isFirstLoad = false
                } else {
                    // Check for new PENDING orders
                    val newOrders = orders.filter { it.id !in knownOrderIds }
                    newOrders.forEach { order ->
                        if (order.status == "PENDING") {
                            sendNotification(order)
                        }
                    }
                    knownOrderIds = orders.map { it.id }.toSet()
                }
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

    fun logout() {
        authRepository.signOut()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "New Orders"
            val descriptionText = "Notifications for new incoming orders"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("NEW_ORDERS_CHANNEL", name, importance).apply {
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

        // Use a generic icon if ic_launcher is not available in R, but it should be.
        // If build fails, I might need to check resources.
        // Assuming R.mipmap.ic_launcher is standard.

        val builder = NotificationCompat.Builder(context, "NEW_ORDERS_CHANNEL")
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Fallback system icon to be safe
            .setContentTitle("New Order Received!")
            .setContentText("Order #${order.id.take(8)} from ${order.customerName}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(soundUri)
            .setAutoCancel(true)

        // Use Order ID hash code as notification ID to allow multiple notifications
        notificationManager.notify(order.id.hashCode(), builder.build())
    }
}
