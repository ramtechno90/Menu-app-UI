package com.pizzaparadize.menuapp.data.model

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class User(
    val uid: String = "",
    val username: String? = null,
    val email: String? = null,
    val phoneNumber: String? = null
)