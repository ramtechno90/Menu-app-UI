package com.pizzaparadize.menuapp.features.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pizzaparadize.menuapp.data.service.FirebaseSettingsService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class MainUiState(
    val selectedTab: BottomNavItem = BottomNavItem.Home,
    val restaurantName: String = "Menu App" // Default name
)

@HiltViewModel
class MainViewModel @Inject constructor(
    firebaseSettingsService: FirebaseSettingsService
) : ViewModel() {

    private val _selectedTab = MutableStateFlow<BottomNavItem>(BottomNavItem.Home)

    val uiState: StateFlow<MainUiState> = combine(
        _selectedTab,
        firebaseSettingsService.getRestaurantDetails()
    ) { tab, details ->
        MainUiState(
            selectedTab = tab,
            restaurantName = details.name.ifBlank { "Menu App" }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainUiState()
    )

    fun onTabSelected(tab: BottomNavItem) {
        _selectedTab.value = tab
    }
}