package com.example.menuapp.data.model

import androidx.annotation.Keep

@Keep
data class RestaurantDetails(
    val name: String = "",
    val logoUrl: String = "",
    val contactNumber: String = ""
)