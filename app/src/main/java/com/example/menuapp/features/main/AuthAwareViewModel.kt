package com.example.menuapp.features.main

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.menuapp.data.auth.AuthRepository
import com.example.menuapp.data.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val WELCOME_DIALOG_SHOWN_KEY = "welcomeDialogShown"

@HiltViewModel
class AuthAwareViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user = _user.asStateFlow()

    val showWelcomeDialog: StateFlow<Boolean> = savedStateHandle.getStateFlow(WELCOME_DIALOG_SHOWN_KEY, true)

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
        savedStateHandle[WELCOME_DIALOG_SHOWN_KEY] = false
    }
}