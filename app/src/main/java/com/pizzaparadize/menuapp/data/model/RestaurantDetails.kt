package com.pizzaparadize.menuapp.data.model

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class RestaurantDetails(
    val name: String = "",
    val logoUrl: String = "",
    val contactNumber: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)
