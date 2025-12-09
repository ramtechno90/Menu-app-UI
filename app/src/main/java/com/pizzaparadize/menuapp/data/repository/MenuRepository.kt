package com.pizzaparadize.menuapp.data.repository

import com.pizzaparadize.menuapp.data.firebase.model.CartItem
import com.pizzaparadize.menuapp.data.firebase.model.Category
import com.pizzaparadize.menuapp.data.firebase.model.MenuItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MenuRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository: com.pizzaparadize.menuapp.data.auth.AuthRepository
) {
    // Menu
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getCategories(): Flow<List<Category>> {
        return firestore.collection("categories")
            .orderBy("order", Query.Direction.ASCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.map { document ->
                    val category = document.toObject(Category::class.java)!!
                    category.id = document.id
                    category
                }
            }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
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
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getCartItems(): Flow<List<CartItem>> {
        return authRepository.getUserFlow().flatMapLatest { user ->
            if (user == null) {
                flowOf(emptyList())
            } else {
                firestore.collection("cart_items")
                    .whereEqualTo("userId", user.uid)
                    .snapshots()
                    .map { snapshot ->
                        snapshot.toObjects(CartItem::class.java)
                    }
            }
        }
    }

    suspend fun addToCart(menuItem: MenuItem) {
        val user = authRepository.getCurrentUser() ?: return
        if (menuItem.id.isEmpty()) {
            return // Do not add to cart if menu item has no id
        }
        val cartItemRef = firestore.collection("cart_items")
            .document("${user.uid}_${menuItem.id}")
        val snapshot = cartItemRef.get().await()
        if (snapshot.exists()) {
            val existingItem = snapshot.toObject(CartItem::class.java)!!
            val updatedItem = existingItem.copy(quantity = existingItem.quantity + 1)
            cartItemRef.set(updatedItem).await()
        } else {
            val cartItem = CartItem(
                menuItemId = menuItem.id,
                name = menuItem.name,
                price = menuItem.price,
                imageUrl = menuItem.imageUrl,
                quantity = 1,
                userId = user.uid
            )
            cartItemRef.set(cartItem).await()
        }
    }

    suspend fun updateQuantity(menuItemId: String, newQuantity: Int) {
        val user = authRepository.getCurrentUser() ?: return
        val cartItemRef = firestore.collection("cart_items").document("${user.uid}_${menuItemId}")
        if (newQuantity > 0) {
            cartItemRef.update("quantity", newQuantity).await()
        } else {
            cartItemRef.delete().await()
        }
    }

    suspend fun updateNotes(itemId: String, notes: String) {
        authRepository.getCurrentUser() ?: return
        val cartItemRef = firestore.collection("cart_items").document(itemId)
        cartItemRef.update("notes", notes).await()
    }

    suspend fun clearCart() {
        val user = authRepository.getCurrentUser() ?: return
        val cartItems = firestore.collection("cart_items")
            .whereEqualTo("userId", user.uid).get().await()
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
            MenuItem(name = "Tiramisu", description = "Coffee-flavoured Italian dessert.", price = 7.99, category = "Dessert", imageUrl = "https://source.unsplash.com/random/400x400?dessert"),
            MenuItem(name = "Gobi Manchurian", description = "Fried cauliflower in a spicy and tangy sauce.", price = 150.0, category = "Starters", imageUrl = "https://source.unsplash.com/random/400x400?manchurian"),
            MenuItem(name = "Chilli Paneer", description = "Stir-fried paneer with peppers and onions.", price = 180.0, category = "Starters", imageUrl = "https://source.unsplash.com/random/400x400?paneer"),
            MenuItem(name = "Dal Makhani", description = "Creamy lentils cooked with butter and spices.", price = 220.0, category = "Main Course", imageUrl = "https://source.unsplash.com/random/400x400?dal"),
            MenuItem(name = "Vegetable Biryani", description = "Aromatic rice dish with mixed vegetables.", price = 200.0, category = "Main Course", imageUrl = "https://source.unsplash.com/random/400x400?biryani"),
            MenuItem(name = "Garlic Naan", description = "Soft flatbread with garlic and butter.", price = 60.0, category = "Breads", imageUrl = "https://source.unsplash.com/random/400x400?naan"),
            MenuItem(name = "Roti", description = "Whole wheat flatbread.", price = 30.0, category = "Breads", imageUrl = "https://source.unsplash.com/random/400x400?roti"),
            MenuItem(name = "Gulab Jamun", description = "Sweet milk solids dumplings in syrup.", price = 80.0, category = "Dessert", imageUrl = "https://source.unsplash.com/random/400x400?gulabjamun")
        )
    }
}
