package com.example.menuapp.data.service

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class FirebaseSettingsService @Inject constructor() {
    fun getShowImagesSetting(): Flow<Boolean> = callbackFlow {
        val db = Firebase.firestore
        val docRef = db.collection("app_settings").document("image_visibility")

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
}