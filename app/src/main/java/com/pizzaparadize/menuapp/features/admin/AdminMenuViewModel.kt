package com.pizzaparadize.menuapp.features.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pizzaparadize.menuapp.data.firebase.model.Category
import com.pizzaparadize.menuapp.data.firebase.model.MenuItem
import com.pizzaparadize.menuapp.data.repository.MenuRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminMenuUiState(
    val categories: List<Category> = emptyList(),
    val menuItems: List<MenuItem> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class AdminMenuViewModel @Inject constructor(
    private val menuRepository: MenuRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminMenuUiState(isLoading = true))
    val uiState: StateFlow<AdminMenuUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                menuRepository.getCategories(),
                menuRepository.getMenuItems()
            ) { categories, items ->
                AdminMenuUiState(categories = categories, menuItems = items, isLoading = false)
            }.collectLatest { state ->
                _uiState.value = state
            }
        }
    }

    fun addCategory(name: String, order: Int) {
        viewModelScope.launch {
            menuRepository.addCategory(Category(name = name, order = order))
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            menuRepository.updateCategory(category)
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            menuRepository.deleteCategory(categoryId)
        }
    }

    fun addMenuItem(name: String, description: String, price: Double, categoryId: String, imageUrl: String) {
        viewModelScope.launch {
            menuRepository.addMenuItem(
                MenuItem(
                    name = name,
                    description = description,
                    price = price,
                    category = categoryId,
                    imageUrl = imageUrl
                )
            )
        }
    }

    fun updateMenuItem(menuItem: MenuItem) {
        viewModelScope.launch {
            menuRepository.updateMenuItem(menuItem)
        }
    }

    fun deleteMenuItem(menuItemId: String) {
        viewModelScope.launch {
            menuRepository.deleteMenuItem(menuItemId)
        }
    }
}
