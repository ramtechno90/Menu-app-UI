package com.example.menuapp.di

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.menuapp.data.SampleData
import com.example.menuapp.data.local.dao.MenuItemDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

class DatabaseCallback @Inject constructor(
    private val menuItemDaoProvider: Provider<MenuItemDao>
) : RoomDatabase.Callback() {

    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        CoroutineScope(Dispatchers.IO).launch {
            // If the database is empty, populate it with sample data.
            if (menuItemDaoProvider.get().getMenuItemsCount() == 0) {
                populateDatabase()
            }
        }
    }

    private suspend fun populateDatabase() {
        menuItemDaoProvider.get().insertAll(SampleData.menuItems)
    }
}
