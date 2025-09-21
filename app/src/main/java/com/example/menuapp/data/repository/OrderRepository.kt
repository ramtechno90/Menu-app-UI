package com.example.menuapp.data.repository

import com.example.menuapp.data.local.dao.CartDao
import com.example.menuapp.data.local.dao.OrderDao
import com.example.menuapp.data.local.model.OrderEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderRepository @Inject constructor(
    private val orderDao: OrderDao,
    private val cartDao: CartDao
) {
    fun getAllOrders(): Flow<List<OrderEntity>> = orderDao.getAllOrders()

    suspend fun createOrder() {
        val cartItems = cartDao.getCartItems().first()
        if (cartItems.isEmpty()) {
            return // Can't create an empty order
        }

        val subtotal = cartItems.sumOf { it.price * it.quantity }
        val tax = subtotal * 0.08 // Assuming 8% tax
        val deliveryFee = 2.50 // Assuming a flat delivery fee
        val grandTotal = subtotal + tax + deliveryFee

        val order = OrderEntity(
            id = UUID.randomUUID().toString(),
            items = cartItems,
            subtotal = subtotal,
            tax = tax,
            deliveryFee = deliveryFee,
            grandTotal = grandTotal,
            orderDate = System.currentTimeMillis(),
            status = "COMPLETED" // Or "CURRENT" depending on desired logic
        )

        orderDao.insertOrder(order)
        cartDao.clearCart()
    }
}
