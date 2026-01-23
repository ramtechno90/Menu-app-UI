package com.pizzaparadize.menuapp.data.repository

import com.pizzaparadize.menuapp.data.firebase.model.Order
import com.pizzaparadize.menuapp.data.firebase.model.DeliveryStaff
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import android.content.Context
import com.pizzaparadize.menuapp.data.auth.AuthRepository
import com.pizzaparadize.menuapp.data.service.FirebaseSettingsService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.combine

@Singleton
class OrderRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestore: FirebaseFirestore,
    private val menuRepository: MenuRepository,
    private val settingsService: FirebaseSettingsService,
    private val authRepository: AuthRepository
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getAllOrders(): Flow<List<Order>> {
        return authRepository.getUserFlow().flatMapLatest { user ->
            if (user == null) {
                return@flatMapLatest flowOf(emptyList<Order>())
            }
            firestore.collection("orders")
                .whereEqualTo("userId", user.uid)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.map { document ->
                    val order = document.toObject(Order::class.java)!!
                    order.id = document.id
                    order
                }.sortedByDescending { it.orderDate }
            }
        }
    }

    fun getAllOrdersForAdmin(): Flow<List<Order>> {
        return firestore.collection("orders")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.map { document ->
                    val order = document.toObject(Order::class.java)!!
                    order.id = document.id
                    order
                }.sortedByDescending { it.orderDate }
            }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getOngoingOrders(): Flow<List<Order>> {
        return authRepository.getUserFlow().flatMapLatest { user ->
            if (user == null) {
                return@flatMapLatest flowOf(emptyList<Order>())
            }

            callbackFlow {
                // Fetch all orders from the last 48 hours to include recent delivered/rejected ones
                val twentyFourHoursAgo = System.currentTimeMillis() - 24 * 60 * 60 * 1000

                val query = firestore.collection("orders")
                    .whereEqualTo("userId", user.uid)

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
                trySend(orders.sortedByDescending { it.orderDate })
            }
        }

                awaitClose { listener.remove() }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getDeliveredOrders(): Flow<List<Order>> {
        return authRepository.getUserFlow().flatMapLatest { user ->
            if (user == null) {
                return@flatMapLatest flowOf(emptyList<Order>())
            }
            callbackFlow {
                val query = firestore.collection("orders")
                    .whereEqualTo("userId", user.uid)

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
                }.filter { it.status == "DELIVERED" }
                trySend(orders.sortedByDescending { it.orderDate }) // Send the latest data to the flow
            }
        }

                awaitClose {
                    listener.remove() // Clean up the listener when the flow is cancelled
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getOrderById(orderId: String): Flow<Order?> {
        return authRepository.getUserFlow().flatMapLatest { user ->
            if (user == null) {
                return@flatMapLatest flowOf(null)
            }

            // Reference to the main order document
            val orderRef = firestore.collection("orders").document(orderId)
            // Reference to the private data (OTP)
            val privateRef = orderRef.collection("private").document("data")

            combine(
                orderRef.snapshots(),
                privateRef.snapshots()
            ) { orderSnap, privateSnap ->
                if (orderSnap.exists()) {
                    val order = orderSnap.toObject(Order::class.java)
                    if (order?.userId == user.uid) {
                        order.id = orderSnap.id

                        // Populate OTP from private doc if available
                        if (privateSnap.exists()) {
                            val otp = privateSnap.getString("otp")
                            if (otp != null) {
                                return@combine order.copy(otp = otp)
                            }
                        }
                        order
                    } else {
                        null // Order does not belong to the current user
                    }
                } else {
                    null
                }
            }
        }
    }

    suspend fun createOrder(
        address: String,
        customerName: String,
        customerPhoneNumber: String?,
        paymentMethod: String,
        tax: Double,
        deliveryFee: Double,
        grandTotal: Double,
        deliveryDistanceKm: Double?
    ) {
        val cartItems = menuRepository.getCartItems().first()
        if (cartItems.isEmpty()) {
            return // Can't create an empty order
        }

        val subtotal = cartItems.sumOf { it.price * it.quantity }

        // Create a new document with a unique ID
        val newOrderRef = firestore.collection("orders").document()

        val user = authRepository.getCurrentUser()
            ?: return // Ensure user is logged in

        val appVersion = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: Exception) {
            "Unknown"
        }

        val order = Order(
            id = newOrderRef.id,
            userId = user.uid,
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
            paymentMethod = paymentMethod,
            appVersion = appVersion,
            deliveryDistanceKm = deliveryDistanceKm
        )

        // Set the data for the new document
        newOrderRef.set(order).await()
        menuRepository.clearCart()
    }

    fun getDeliveryStaff(): Flow<List<DeliveryStaff>> {
        return firestore.collection("delivery_staff")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { document ->
                    val staff = document.toObject(DeliveryStaff::class.java)
                    staff?.uid = document.id
                    staff
                }
            }
    }

    suspend fun updateOrderStatus(orderId: String, status: String) {
        firestore.collection("orders").document(orderId)
            .update("status", status).await()
    }

    suspend fun assignOrderToStaff(orderId: String, staffName: String, staffUid: String) {
        val updates = mapOf(
            "assignedTo" to staffName,
            "assignedToUid" to staffUid
        )
        firestore.collection("orders").document(orderId)
            .update(updates).await()
    }

    fun getOrdersForDeliveryStaff(staffUid: String): Flow<List<Order>> {
        return firestore.collection("orders")
            .whereEqualTo("assignedToUid", staffUid)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.map { document ->
                    val order = document.toObject(Order::class.java)!!
                    order.id = document.id
                    order
                }.sortedByDescending { it.orderDate }
            }
    }

    suspend fun updateOrderOtp(orderId: String, otp: String) {
        val updates = mapOf(
            "otpEntered" to otp
        )
        firestore.collection("orders").document(orderId)
            .update(updates).await()
    }

    suspend fun markOrderAsPickedUp(orderId: String) {
        firestore.collection("orders").document(orderId)
            .update("status", "PICKED_UP").await()
    }

    suspend fun deleteOrder(orderId: String) {
        firestore.collection("orders").document(orderId).delete().await()
    }

    suspend fun deleteOrdersByStatus(status: String) {
        val snapshot = firestore.collection("orders")
            .whereEqualTo("status", status)
            .get()
            .await()

        val batch = firestore.batch()
        for (document in snapshot.documents) {
            batch.delete(document.reference)
        }
        batch.commit().await()
    }
}
