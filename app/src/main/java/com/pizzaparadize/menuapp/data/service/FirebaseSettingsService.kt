package com.pizzaparadize.menuapp.data.service

import com.pizzaparadize.menuapp.data.model.DeliveryFeeSettings
import com.pizzaparadize.menuapp.data.model.RestaurantDetails
import com.pizzaparadize.menuapp.data.model.TaxSettings
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class FirebaseSettingsService @Inject constructor(
    private val db: FirebaseFirestore
) {
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

            val details = snapshot?.toObject(RestaurantDetails::class.java) ?: RestaurantDetails()
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
            val settings = snapshot?.toObject(TaxSettings::class.java) ?: TaxSettings()
            trySend(settings)
        }
        awaitClose { listener.remove() }
    }

    fun getDeliveryFeeSettings(): Flow<DeliveryFeeSettings> = callbackFlow {
        val docRef = settingsCollection.document("delivery_fee")

        val listener = docRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                close(e)
                return@addSnapshotListener
            }
            val settings = snapshot?.toObject(DeliveryFeeSettings::class.java) ?: DeliveryFeeSettings()
            trySend(settings)
        }
        awaitClose { listener.remove() }
    }
}
