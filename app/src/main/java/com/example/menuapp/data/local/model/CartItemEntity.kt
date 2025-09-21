package com.example.menuapp.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cart_items")
data class CartItemEntity(
    @PrimaryKey
    val id: Int, // This will be the same as the MenuItemEntity id
    val name: String,
    val price: Double,
    val imageUrl: String,
    var quantity: Int
)
