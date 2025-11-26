package com.pizzaparadize.menuapp.di

import android.content.Context
import com.pizzaparadize.menuapp.data.service.FirebaseSettingsService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseSettingsService(): FirebaseSettingsService {
        return FirebaseSettingsService()
    }

    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context {
        return context
    }
}