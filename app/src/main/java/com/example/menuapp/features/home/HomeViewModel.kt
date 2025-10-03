package com.example.menuapp.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.menuapp.data.firebase.model.MenuItem
import com.example.menuapp.data.repository.MenuRepository
import com.example.menuapp.data.service.FirebaseSettingsService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val menuItems: List<MenuItem> = emptyList(),
    val showImages: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val menuRepository: MenuRepository,
    private val firebaseSettingsService: FirebaseSettingsService
) : ViewModel() {

    private val _showImages = firebaseSettingsService.getShowImagesSetting()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val uiState: StateFlow<HomeUiState> = menuRepository.getMenuItems()
        .combine(_showImages) { menuItems, showImages ->
            HomeUiState(menuItems = menuItems, showImages = showImages)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState()
        )

    init {
        viewModelScope.launch {
            menuRepository.seedMenuItems()
        }
    }

    fun addToCart(menuItem: MenuItem) {
        viewModelScope.launch {
            menuRepository.addToCart(menuItem)
        }
    }
}
