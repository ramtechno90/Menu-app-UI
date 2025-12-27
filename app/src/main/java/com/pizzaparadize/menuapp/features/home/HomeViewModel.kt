package com.pizzaparadize.menuapp.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pizzaparadize.menuapp.data.firebase.model.CartItem
import com.pizzaparadize.menuapp.data.firebase.model.MenuItem
import com.pizzaparadize.menuapp.data.repository.MenuRepository
import com.pizzaparadize.menuapp.data.service.FirebaseSettingsService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val menuItems: List<MenuItem> = emptyList(),
    val categories: List<String> = emptyList(),
    val showImages: Boolean = true,
    val cartQuantities: Map<String, Int> = emptyMap(),
    val contactNumber: String = ""
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val menuRepository: MenuRepository,
    private val firebaseSettingsService: FirebaseSettingsService
) : ViewModel() {

    private val _showImages = firebaseSettingsService.getShowImagesSetting()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    private val _cartItems: StateFlow<List<CartItem>> = menuRepository.getCartItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val uiState: StateFlow<HomeUiState> = combine(
        menuRepository.getMenuItems(),
        menuRepository.getCategories(),
        _showImages,
        _cartItems,
        firebaseSettingsService.getRestaurantDetails()
    ) { menuItems, fetchedCategories, showImages, cartItems, restaurantDetails ->
        val existingCategoryNames = menuItems.map { it.category }.toSet()
        val categories = fetchedCategories
            .filter { existingCategoryNames.contains(it.name) }
            .map { it.name }

        val cartQuantities = cartItems.associate { it.menuItemId to it.quantity }
        HomeUiState(
            menuItems = menuItems,
            categories = categories,
            showImages = showImages,
            cartQuantities = cartQuantities,
            contactNumber = restaurantDetails.contactNumber ?: ""
        )
    }.stateIn(
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

    fun decrementQuantity(menuItem: MenuItem) {
        viewModelScope.launch {
            val currentQuantity = uiState.value.cartQuantities[menuItem.id] ?: 0
            if (currentQuantity > 0) {
                menuRepository.updateQuantity(menuItem.id, currentQuantity - 1)
            }
        }
    }

    fun incrementQuantity(menuItem: MenuItem) {
        viewModelScope.launch {
            val currentQuantity = uiState.value.cartQuantities[menuItem.id] ?: 0
            menuRepository.updateQuantity(menuItem.id, currentQuantity + 1)
        }
    }
}
