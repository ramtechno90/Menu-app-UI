package com.example.menuapp.data.repository

import com.example.menuapp.data.firebase.model.CartItem
import com.example.menuapp.data.firebase.model.MenuItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MenuRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    // Menu
    fun getMenuItems(): Flow<List<MenuItem>> {
        return firestore.collection("menu_items")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.map { document ->
                    val menuItem = document.toObject(MenuItem::class.java)!!
                    menuItem.id = document.id
                    menuItem
                }
            }
    }

    suspend fun seedMenuItems() {
        val menuItemsCollection = firestore.collection("menu_items")
        val snapshot = menuItemsCollection.limit(1).get().await()
        if (snapshot.isEmpty) {
            for (item in getSampleMenuItems()) {
                menuItemsCollection.add(item).await()
            }
        }
    }

    // Cart
    fun getCartItems(): Flow<List<CartItem>> {
        return firestore.collection("cart_items")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.map { document ->
                    val cartItem = document.toObject(CartItem::class.java)!!
                    cartItem.id = document.id
                    cartItem
                }
            }
    }

    suspend fun addToCart(menuItem: MenuItem) {
        if (menuItem.id.isEmpty()) {
            return // Do not add to cart if menu item has no id
        }
        val cartItemRef = firestore.collection("cart_items").document(menuItem.id)
        val snapshot = cartItemRef.get().await()
        if (snapshot.exists()) {
            val existingItem = snapshot.toObject(CartItem::class.java)!!
            val updatedItem = existingItem.copy(quantity = existingItem.quantity + 1)
            cartItemRef.set(updatedItem).await()
        } else {
            val cartItem = CartItem(
                id = menuItem.id,
                name = menuItem.name,
                price = menuItem.price,
                imageUrl = menuItem.imageUrl,
                quantity = 1
            )
            cartItemRef.set(cartItem).await()
        }
    }

    suspend fun updateQuantity(itemId: String, newQuantity: Int) {
        val cartItemRef = firestore.collection("cart_items").document(itemId)
        if (newQuantity > 0) {
            cartItemRef.update("quantity", newQuantity).await()
        } else {
            cartItemRef.delete().await()
        }
    }

    suspend fun clearCart() {
        val cartItems = firestore.collection("cart_items").get().await()
        for (document in cartItems.documents) {
            document.reference.delete().await()
        }
    }

    private fun getSampleMenuItems(): List<MenuItem> {
        return listOf(
            MenuItem(name = "Margherita Pizza", description = "Classic pizza with tomato, mozzarella, and basil.", price = 12.99, category = "Pizza", imageUrl = "https://source.unsplash.com/random/400x400?pizza"),
            MenuItem(name = "Pepperoni Pizza", description = "Pizza with pepperoni and mozzarella cheese.", price = 14.99, category = "Pizza", imageUrl = "https://source.unsplash.com/random/400x400?pizza"),
            MenuItem(name = "Caesar Salad", description = "Fresh romaine lettuce with Caesar dressing, croutons, and Parmesan cheese.", price = 9.99, category = "Salad", imageUrl = "https://source.unsplash.com/random/400x400?salad"),
            MenuItem(name = "Spaghetti Carbonara", description = "Pasta with eggs, cheese, pancetta, and black pepper.", price = 15.99, category = "Pasta", imageUrl = "https://source.unsplash.com/random/400x400?pasta"),
            MenuItem(name = "Tiramisu", description = "Coffee-flavoured Italian dessert.", price = 7.99, category = "Dessert", imageUrl = "https://source.unsplash.com/random/400x400?dessert")
        )
    }
}
