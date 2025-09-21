package com.example.menuapp.data.local

import androidx.room.TypeConverter
import com.example.menuapp.data.local.model.CartItemEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    @TypeConverter
    fun fromCartItemList(value: List<CartItemEntity>): String {
        val gson = Gson()
        val type = object : TypeToken<List<CartItemEntity>>() {}.type
        return gson.toJson(value, type)
    }

    @TypeConverter
    fun toCartItemList(value: String): List<CartItemEntity> {
        val gson = Gson()
        val type = object : TypeToken<List<CartItemEntity>>() {}.type
        return gson.fromJson(value, type)
    }
}
