package com.example.menuapp.features.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.menuapp.data.auth.AuthRepository
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState = _authState.asStateFlow()

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.signIn(email, password)
            _authState.value = when {
                result.isSuccess -> AuthState.Success
                else -> AuthState.Error(result.exceptionOrNull()?.message ?: "An unexpected error occurred")
            }
        }
    }

    fun signUp(email: String, password: String, username: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.signUp(email, password, username)
            _authState.value = when {
                result.isSuccess -> AuthState.Success
                else -> AuthState.Error(result.exceptionOrNull()?.message ?: "An unexpected error occurred")
            }
        }
    }

    fun signInWithGoogle(account: GoogleSignInAccount) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.signInWithGoogle(account)
            _authState.value = when {
                result.isSuccess -> AuthState.Success
                else -> AuthState.Error(result.exceptionOrNull()?.message ?: "An unexpected error occurred")
            }
        }
    }

    fun sendOtp(phoneNumber: String, activity: Activity) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            authRepository.sendOtp(phoneNumber, activity).collect { result ->
                _authState.value = when {
                    result.isSuccess -> AuthState.OtpSent(result.getOrNull()!!)
                    else -> AuthState.Error(result.exceptionOrNull()?.message ?: "An unexpected error occurred")
                }
            }
        }
    }

    fun verifyOtp(verificationId: String, otp: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.verifyOtp(verificationId, otp)
            _authState.value = when {
                result.isSuccess -> AuthState.Success
                else -> AuthState.Error(result.exceptionOrNull()?.message ?: "An unexpected error occurred")
            }
        }
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class OtpSent(val verificationId: String) : AuthState()
    data class Error(val message: String) : AuthState()
}