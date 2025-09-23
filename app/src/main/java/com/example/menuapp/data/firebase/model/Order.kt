package com.example.menuapp.data.firebase.model

data class Order(
    var id: String = "",
    val items: List<CartItem> = emptyList(),
    val subtotal: Double = 0.0,
    val tax: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val grandTotal: Double = 0.0,
    val orderDate: Long = 0L,
    val status: String = "",
    val deliveryAddress: String = ""
)
