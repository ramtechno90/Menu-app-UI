package com.example.menuapp.di

import com.example.menuapp.data.service.FirebaseSettingsService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
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
}