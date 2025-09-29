package com.example.menuapp.di

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.location.Geocoder
import com.google.maps.GeoApiContext
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.Locale
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LocationModule {

    @Provides
    @Singleton
    fun provideGeocoder(app: Application): Geocoder {
        return Geocoder(app, Locale.getDefault())
    }

    @Provides
    @Singleton
    fun provideGeoApiContext(app: Application): GeoApiContext {
        val ai: ApplicationInfo = app.packageManager.getApplicationInfo(app.packageName, PackageManager.GET_META_DATA)
        val apiKey = ai.metaData.getString("com.google.android.geo.API_KEY")
        return GeoApiContext.Builder()
            .apiKey(apiKey)
            .build()
    }
}