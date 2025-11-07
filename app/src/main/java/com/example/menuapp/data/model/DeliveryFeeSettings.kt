package com.example.menuapp.data.model

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class DeliveryFeeSettings(
    val fee: Double = 0.0
)
