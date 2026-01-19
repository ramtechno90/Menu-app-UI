package com.pizzaparadize.menuapp.data.auth

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val isAuthenticated: Flow<Boolean>
    val isAdmin: Flow<Boolean>
    fun getUserFlow(): Flow<com.pizzaparadize.menuapp.data.model.User?>
    suspend fun signInAnonymouslyAndSaveUsername(username: String): Result<Unit>
    suspend fun signInWithEmailAndPassword(email: String, password: String): Result<Unit>
    fun signOut()
    suspend fun getCurrentUser(): com.pizzaparadize.menuapp.data.model.User?
}
