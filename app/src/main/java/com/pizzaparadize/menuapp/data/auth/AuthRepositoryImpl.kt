package com.pizzaparadize.menuapp.data.auth

import android.app.Activity
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    private val _isAuthenticated = MutableStateFlow(firebaseAuth.currentUser != null)
    override val isAuthenticated = _isAuthenticated.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override val isAdmin: Flow<Boolean> = callbackFlow {
        val authListener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        firebaseAuth.addAuthStateListener(authListener)
        trySend(firebaseAuth.currentUser)
        awaitClose { firebaseAuth.removeAuthStateListener(authListener) }
    }.flatMapLatest { user ->
        if (user == null || user.email.isNullOrEmpty()) {
            flowOf(false)
        } else if (user.email == "admin@pizzaparadize.com" || user.email == "admin@gmail.com") {
            flowOf(true)
        } else {
            callbackFlow {
                val listener = firestore.collection("admins").document(user.uid)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            trySend(false)
                            return@addSnapshotListener
                        }
                        trySend(snapshot != null && snapshot.exists())
                    }
                awaitClose { listener.remove() }
            }
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override val isDeliveryStaff: Flow<Boolean> = callbackFlow {
        val authListener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        firebaseAuth.addAuthStateListener(authListener)
        trySend(firebaseAuth.currentUser)
        awaitClose { firebaseAuth.removeAuthStateListener(authListener) }
    }.flatMapLatest { user ->
        if (user == null || user.email.isNullOrEmpty()) {
            flowOf(false)
        } else {
            // Check if user exists in delivery_staff collection
            callbackFlow {
                val listener = firestore.collection("delivery_staff").document(user.uid)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            trySend(false)
                            return@addSnapshotListener
                        }
                        trySend(snapshot != null && snapshot.exists())
                    }
                awaitClose { listener.remove() }
            }
        }
    }

    init {
        firebaseAuth.addAuthStateListener { auth ->
            val user = auth.currentUser
            _isAuthenticated.value = user != null
        }
    }

    override suspend fun signInAnonymouslyAndSaveUsername(username: String): Result<Unit> {
        return try {
            val authResult = firebaseAuth.signInAnonymously().await()
            val firebaseUser = authResult.user
            if (firebaseUser != null) {
                val user = mapOf(
                    "uid" to firebaseUser.uid,
                    "username" to username
                )
                firestore.collection("users").document(firebaseUser.uid).set(user).await()
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("User not logged in"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signInWithEmailAndPassword(email: String, password: String): Result<Unit> {
        return try {
            firebaseAuth.signInWithEmailAndPassword(email, password).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun signOut() {
        firebaseAuth.signOut()
    }

    @kotlinx.coroutines.ExperimentalCoroutinesApi
    override fun getUserFlow(): Flow<com.pizzaparadize.menuapp.data.model.User?> {
        return callbackFlow {
            val authListener = FirebaseAuth.AuthStateListener { auth ->
                trySend(auth.currentUser)
            }
            firebaseAuth.addAuthStateListener(authListener)
            trySend(firebaseAuth.currentUser)
            awaitClose { firebaseAuth.removeAuthStateListener(authListener) }
        }.flatMapLatest { firebaseUser ->
            if (firebaseUser == null) {
                flowOf(null)
            } else {
                val documentRef = firestore.collection("users").document(firebaseUser.uid)
                callbackFlow {
                    val listener = documentRef.addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            // If permission denied or other error (e.g. during logout), just emit null
                            trySend(null)
                            return@addSnapshotListener
                        }

                        if (snapshot != null && snapshot.exists()) {
                            val user = snapshot.toObject(com.pizzaparadize.menuapp.data.model.User::class.java)
                            trySend(user)
                        } else {
                            trySend(null)
                        }
                    }
                    awaitClose { listener.remove() }
                }
            }
        }
    }

    override suspend fun getCurrentUser(): com.pizzaparadize.menuapp.data.model.User? {
        val firebaseUser = firebaseAuth.currentUser ?: return null
        return try {
            val documentRef = firestore.collection("users").document(firebaseUser.uid)
            val document = documentRef.get().await()

            var user = document.toObject(com.pizzaparadize.menuapp.data.model.User::class.java)

            if (user != null && user.phoneNumber.isNullOrBlank() && !firebaseUser.phoneNumber.isNullOrBlank()) {
                val authPhoneNumber = firebaseUser.phoneNumber!!
                documentRef.update("phoneNumber", authPhoneNumber).await()
                user = user.copy(phoneNumber = authPhoneNumber)
            } else if (user == null) {
                user = com.pizzaparadize.menuapp.data.model.User(
                    uid = firebaseUser.uid,
                    username = firebaseUser.displayName,
                    email = firebaseUser.email,
                    phoneNumber = firebaseUser.phoneNumber
                )
            }
            user
        } catch (e: Exception) {
            return com.pizzaparadize.menuapp.data.model.User(
                uid = firebaseUser.uid,
                username = firebaseUser.displayName,
                email = firebaseUser.email,
                phoneNumber = firebaseUser.phoneNumber
            )
        }
    }
}
