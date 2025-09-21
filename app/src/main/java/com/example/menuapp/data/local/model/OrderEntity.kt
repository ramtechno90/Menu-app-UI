package com.example.menuapp.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.menuapp.data.local.Converters

@Entity(tableName = "orders")
@TypeConverters(Converters::class)
data class OrderEntity(
    @PrimaryKey
    val id: String, // A unique ID for the order, e.g., timestamp-based
    val items: List<CartItemEntity>,
    val subtotal: Double,
    val tax: Double,
    val deliveryFee: Double,
    val grandTotal: Double,
    val orderDate: Long,
    val status: String // e.g., "COMPLETED", "CANCELLED"
)
