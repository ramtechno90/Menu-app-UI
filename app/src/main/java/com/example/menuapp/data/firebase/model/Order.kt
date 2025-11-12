package com.example.menuapp.data.firebase.model

data class Order(
    var id: String = "",
    val userId: String = "",
    val customerName: String = "",
    val customerPhoneNumber: String? = null,
    val items: List<CartItem> = emptyList(),
    val subtotal: Double = 0.0,
    val tax: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val grandTotal: Double = 0.0,
    val orderDate: Long = 0L,
    val deliveryDate: Long? = null,
    val status: String = "",
    val deliveryAddress: String = "",
    val assignedTo: String? = null,
    val paymentMethod: String? = null,

    // OTP fields
    val otp: String? = null,
    val otpEntered: String? = null,
    val otpVerified: Boolean = false,
    val otpExpiry: com.google.firebase.Timestamp? = null,

    val otpInvalid: Boolean = false,
    val appVersion: String? = null
)
