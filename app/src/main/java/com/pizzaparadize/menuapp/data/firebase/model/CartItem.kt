package com.pizzaparadize.menuapp.data.firebase.model

data class CartItem(
    var id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val imageUrl: String = "",
    var quantity: Int = 0,
    var notes: String = "",
    val userId: String = ""
)
