package com.pizzaparadize.menuapp.features.main

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pizzaparadize.menuapp.data.auth.AuthRepository
import com.pizzaparadize.menuapp.data.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
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

    private val _showWelcomeDialogEvent = MutableStateFlow(false)
    val showWelcomeDialogEvent = _showWelcomeDialogEvent.asStateFlow()

    init {
        viewModelScope.launch {
            val hasBeenWelcomed = savedStateHandle.get<Boolean>(WELCOME_DIALOG_SHOWN_KEY) ?: false
            val currentUser = authRepository.getCurrentUser()
            _user.value = currentUser

            if (currentUser != null && !hasBeenWelcomed) {
                _showWelcomeDialogEvent.value = true
            }
        }
    }

    fun signOut() {
        savedStateHandle[WELCOME_DIALOG_SHOWN_KEY] = false
        authRepository.signOut()
    }

    fun onWelcomeDialogDismissed() {
        _showWelcomeDialogEvent.value = false
        savedStateHandle[WELCOME_DIALOG_SHOWN_KEY] = true
    }
}