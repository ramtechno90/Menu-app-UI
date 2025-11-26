package com.pizzaparadize.menuapp.data.model

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class TaxSettings(
    val universalTax: Boolean = false,
    val taxRate: Double = 0.0
)
