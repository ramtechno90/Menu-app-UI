package com.pizzaparadize.menuapp.data.model

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class DeliveryFeeSettings(
    val fee: Double = 0.0,
    val minDistanceKm: Double = 0.0,
    val minDistanceRate: Double = 0.0,
    val additionalRatePerKm: Double = 0.0
)
