package com.example.menuapp.data.model

import androidx.annotation.Keep

@Keep
data class TaxSettings(
    val universalTax: Boolean = false,
    val taxRate: Double = 0.0
)