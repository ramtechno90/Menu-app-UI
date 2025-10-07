package com.example.menuapp.data.auth

import android.app.Activity
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val isAuthenticated: Flow<Boolean>
    suspend fun sendOtp(phoneNumber: String, activity: Activity): Flow<Result<String>>
    suspend fun verifyOtp(verificationId: String, otp: String): Result<Unit>
    suspend fun createUserProfile(username: String): Result<Unit>
    fun signOut()
    suspend fun getCurrentUser(): com.example.menuapp.data.model.User?
}