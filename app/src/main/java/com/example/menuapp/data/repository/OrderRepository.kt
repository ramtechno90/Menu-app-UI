package com.example.menuapp.data.repository

import com.example.menuapp.data.firebase.model.Order
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ktx.snapshots
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import com.example.menuapp.data.service.FirebaseSettingsService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val menuRepository: MenuRepository,
    private val settingsService: FirebaseSettingsService
) {
    fun getAllOrders(): Flow<List<Order>> {
        return firestore.collection("orders")
            .orderBy("orderDate", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.map { document ->
                    val order = document.toObject(Order::class.java)!!
                    order.id = document.id
                    order
                }
            }
    }

    fun getOngoingOrders(): Flow<List<Order>> = callbackFlow {
        // Fetch all orders from the last 48 hours to include recent delivered/rejected ones
        val fortyEightHoursAgo = System.currentTimeMillis() - 48 * 60 * 60 * 1000
        val twentyFourHoursAgo = System.currentTimeMillis() - 24 * 60 * 60 * 1000

        val query = firestore.collection("orders")
            .whereGreaterThanOrEqualTo("orderDate", fortyEightHoursAgo)
            .orderBy("orderDate", Query.Direction.DESCENDING)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val orders = snapshot.documents.mapNotNull { document ->
                    document.toObject(Order::class.java)?.apply { id = document.id }
                }.filter { order ->
                    // Standard ongoing statuses
                    val isOngoing = order.status in listOf(
                        "PENDING", "ACCEPTED", "PREPARING",
                        "READY_FOR_DELIVERY", "OUT_FOR_DELIVERY", "PICKED_UP", "COMPLETED"
                    )
                    // Delivered or Rejected within the last 24 hours
                    val isRecentAndFinished =
                        (order.status == "DELIVERED" || order.status == "REJECTED") &&
                                order.orderDate >= twentyFourHoursAgo

                    isOngoing || isRecentAndFinished
                }
                trySend(orders)
            }
        }

        awaitClose { listener.remove() }
    }

    fun getDeliveredOrders(): Flow<List<Order>> = callbackFlow {
        val query = firestore.collection("orders")
            .whereEqualTo("status", "DELIVERED")
            .orderBy("orderDate", Query.Direction.DESCENDING)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error) // Close the flow on error
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val orders = snapshot.documents.mapNotNull { document ->
                    document.toObject(Order::class.java)?.apply {
                        id = document.id
                    }
                }
                trySend(orders) // Send the latest data to the flow
            }
        }

        awaitClose {
            listener.remove() // Clean up the listener when the flow is cancelled
        }
    }

    fun getOrderById(orderId: String): Flow<Order?> {
        return firestore.collection("orders").document(orderId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    val order = snapshot.toObject(Order::class.java)
                    order?.id = snapshot.id
                    order
                } else {
                    null
                }
            }
    }

    suspend fun createOrder(
        address: String,
        customerName: String,
        customerPhoneNumber: String?,
        paymentMethod: String
    ) {
        val cartItems = menuRepository.getCartItems().first()
        if (cartItems.isEmpty()) {
            return // Can't create an empty order
        }

        // Fetch dynamic settings
        val taxSettings = settingsService.getTaxSettings().first()
        val deliveryFeeSettings = settingsService.getDeliveryFeeSettings().first()

        val subtotal = cartItems.sumOf { it.price * it.quantity }

        // Calculate tax based on settings
        val taxRate = if (taxSettings.universalTax) taxSettings.taxRate / 100.0 else 0.08
        val tax = subtotal * taxRate

        // Get delivery fee from settings
        val deliveryFee = if (cartItems.isNotEmpty()) deliveryFeeSettings.fee else 0.0

        val grandTotal = subtotal + tax + deliveryFee

        // Create a new document with a unique ID
        val newOrderRef = firestore.collection("orders").document()

        val order = Order(
            id = newOrderRef.id, // Use the unique ID from the document reference
            customerName = customerName,
            customerPhoneNumber = customerPhoneNumber,
            items = cartItems,
            subtotal = subtotal,
            tax = tax,
            deliveryFee = deliveryFee,
            grandTotal = grandTotal,
            orderDate = System.currentTimeMillis(),
            status = "PENDING",
            deliveryAddress = address,
            paymentMethod = paymentMethod
        )

        // Set the data for the new document
        newOrderRef.set(order).await()
        menuRepository.clearCart()
    }
}
