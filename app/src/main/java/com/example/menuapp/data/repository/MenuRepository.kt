package com.example.menuapp.data.repository

import com.example.menuapp.data.local.dao.CartDao
import com.example.menuapp.data.local.dao.MenuItemDao
import com.example.menuapp.data.local.model.CartItemEntity
import com.example.menuapp.data.local.model.MenuItemEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MenuRepository @Inject constructor(
    private val menuItemDao: MenuItemDao,
    private val cartDao: CartDao
) {
    // Menu
    fun getMenuItems(): Flow<List<MenuItemEntity>> = menuItemDao.getMenuItems()

    suspend fun insertMenuItems(items: List<MenuItemEntity>) {
        menuItemDao.insertAll(items)
    }

    // Cart
    fun getCartItems(): Flow<List<CartItemEntity>> = cartDao.getCartItems()

    suspend fun addToCart(menuItem: MenuItemEntity) {
        val existingItem = cartDao.getById(menuItem.id)
        if (existingItem != null) {
            val updatedItem = existingItem.copy(quantity = existingItem.quantity + 1)
            cartDao.upsert(updatedItem)
        } else {
            val cartItem = CartItemEntity(
                id = menuItem.id,
                name = menuItem.name,
                price = menuItem.price,
                imageUrl = menuItem.imageUrl,
                quantity = 1
            )
            cartDao.upsert(cartItem)
        }
    }

    suspend fun updateQuantity(itemId: Int, newQuantity: Int) {
        val existingItem = cartDao.getById(itemId)
        if (existingItem != null) {
            if (newQuantity > 0) {
                val updatedItem = existingItem.copy(quantity = newQuantity)
                cartDao.upsert(updatedItem)
            } else {
                cartDao.delete(existingItem)
            }
        }
    }

    suspend fun clearCart() {
        cartDao.clearCart()
    }
}
