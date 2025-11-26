package com.pizzaparadize.menuapp.data.model

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class RestaurantDetails(
    val name: String = "",
    val logoUrl: String = "",
    val contactNumber: String = ""
)
