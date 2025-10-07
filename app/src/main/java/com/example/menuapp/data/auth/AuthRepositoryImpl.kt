package com.example.menuapp.data.auth

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
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    private val _isAuthenticated = MutableStateFlow(firebaseAuth.currentUser != null)
    override val isAuthenticated = _isAuthenticated.asStateFlow()

    init {
        firebaseAuth.addAuthStateListener { auth ->
            _isAuthenticated.value = auth.currentUser != null
        }
    }

    override suspend fun sendOtp(phoneNumber: String, activity: Activity): Flow<Result<String>> = callbackFlow {
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // Auto-retrieval of verification code completed.
            }

            override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
                trySend(Result.failure(e))
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                trySend(Result.success(verificationId))
            }
        }
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
            phoneNumber,
            60,
            TimeUnit.SECONDS,
            activity,
            callbacks
        )
        awaitClose { }
    }

    override suspend fun verifyOtp(verificationId: String, otp: String): Result<Unit> {
        return try {
            val credential = PhoneAuthProvider.getCredential(verificationId, otp)
            firebaseAuth.signInWithCredential(credential).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createUserProfile(username: String): Result<Unit> {
        return try {
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser != null) {
                val user = mapOf(
                    "uid" to firebaseUser.uid,
                    "username" to username,
                    "phoneNumber" to firebaseUser.phoneNumber
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

    override fun signOut() {
        firebaseAuth.signOut()
    }

    override suspend fun getCurrentUser(): com.example.menuapp.data.model.User? {
        val firebaseUser = firebaseAuth.currentUser ?: return null
        return try {
            val document = firestore.collection("users").document(firebaseUser.uid).get().await()
            document.toObject(com.example.menuapp.data.model.User::class.java)
        } catch (e: Exception) {
            null
        }
    }
}