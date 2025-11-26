package com.pizzaparadize.menuapp.data.firebase.model

data class MenuItem(
    var id: String = "",
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val imageUrl: String = "",
    val category: String = ""
)
