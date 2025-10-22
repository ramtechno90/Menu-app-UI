package com.example.menuapp.data.auth

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val isAuthenticated: Flow<Boolean>
    fun getUserFlow(): Flow<com.example.menuapp.data.model.User?>
    suspend fun signInAnonymouslyAndSaveUsername(username: String): Result<Unit>
    fun signOut()
    suspend fun getCurrentUser(): com.example.menuapp.data.model.User?
}
