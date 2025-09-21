package com.example.menuapp.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.menuapp.data.local.model.MenuItemEntity
import com.example.menuapp.data.repository.MenuRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val menuItems: List<MenuItemEntity> = emptyList()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val menuRepository: MenuRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = menuRepository.getMenuItems()
        .map { menuItems -> HomeUiState(menuItems = menuItems) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState()
        )

    fun addToCart(menuItem: MenuItemEntity) {
        viewModelScope.launch {
            menuRepository.addToCart(menuItem)
        }
    }
}
