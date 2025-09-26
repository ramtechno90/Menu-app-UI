package com.example.menuapp.features.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.menuapp.data.auth.AuthRepository
import com.example.menuapp.data.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthAwareViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user = _user.asStateFlow()

    private val _showWelcomeDialog = MutableStateFlow(true)
    val showWelcomeDialog = _showWelcomeDialog.asStateFlow()

    init {
        getCurrentUser()
    }

    private fun getCurrentUser() {
        viewModelScope.launch {
            _user.value = authRepository.getCurrentUser()
        }
    }

    fun signOut() {
        authRepository.signOut()
    }

    fun onWelcomeDialogDismissed() {
        _showWelcomeDialog.value = false
    }
}