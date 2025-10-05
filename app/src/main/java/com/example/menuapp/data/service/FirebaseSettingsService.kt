package com.example.menuapp.data.service

import com.example.menuapp.data.model.RestaurantDetails
import com.example.menuapp.data.model.TaxSettings
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class FirebaseSettingsService @Inject constructor() {
    private val db = Firebase.firestore
    private val settingsCollection = db.collection("app_settings")

    fun getShowImagesSetting(): Flow<Boolean> = callbackFlow {
        val docRef = settingsCollection.document("image_visibility")

        val listener = docRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                close(e)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val showImages = snapshot.getBoolean("show_images") ?: true
                trySend(showImages)
            } else {
                trySend(true) // Default to true if the document doesn't exist
            }
        }

        awaitClose { listener.remove() }
    }

    fun getRestaurantDetails(): Flow<RestaurantDetails> = callbackFlow {
        val docRef = settingsCollection.document("restaurant_details")

        val listener = docRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                close(e)
                return@addSnapshotListener
            }

            val details = snapshot?.toObject<RestaurantDetails>() ?: RestaurantDetails()
            trySend(details)
        }

        awaitClose { listener.remove() }
    }

    fun getTaxSettings(): Flow<TaxSettings> = callbackFlow {
        val docRef = settingsCollection.document("tax_settings")

        val listener = docRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                close(e)
                return@addSnapshotListener
            }
            val settings = snapshot?.toObject<TaxSettings>() ?: TaxSettings()
            trySend(settings)
        }
        awaitClose { listener.remove() }
    }
}