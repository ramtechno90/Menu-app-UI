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

    private var tempUsername: String? = null

    fun signUpWithPhone(username: String, phoneNumber: String, activity: Activity) {
        tempUsername = username
        sendOtp(phoneNumber, activity)
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
            if (result.isSuccess) {
                tempUsername?.let { username ->
                    val profileResult = authRepository.createUserProfile(username)
                    if (profileResult.isSuccess) {
                        _authState.value = AuthState.Success
                    } else {
                        _authState.value = AuthState.Error(
                            profileResult.exceptionOrNull()?.message ?: "Failed to create profile"
                        )
                    }
                    tempUsername = null
                } ?: run {
                    _authState.value = AuthState.Success
                }
            } else {
                _authState.value = AuthState.Error(
                    result.exceptionOrNull()?.message ?: "An unexpected error occurred"
                )
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