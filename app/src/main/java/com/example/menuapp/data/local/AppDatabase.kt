package com.example.menuapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.menuapp.data.local.dao.CartDao
import com.example.menuapp.data.local.dao.MenuItemDao
import com.example.menuapp.data.local.dao.OrderDao
import com.example.menuapp.data.local.model.CartItemEntity
import com.example.menuapp.data.local.model.MenuItemEntity
import com.example.menuapp.data.local.model.OrderEntity

@Database(
    entities = [MenuItemEntity::class, CartItemEntity::class, OrderEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun menuItemDao(): MenuItemDao
    abstract fun cartDao(): CartDao
    abstract fun orderDao(): OrderDao
}
