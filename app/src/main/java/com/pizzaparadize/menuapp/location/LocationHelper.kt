package com.pizzaparadize.menuapp.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.maps.GeoApiContext
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

interface LocationHelper {
    suspend fun getCurrentLocation(): LocationResult
    suspend fun geocodeAddress(address: String): LocationResult
    suspend fun getAddressFromCoordinates(latitude: Double, longitude: Double): String
}

sealed class LocationResult {
    data class Success(val latitude: Double, val longitude: Double, val address: String) : LocationResult()
    object NoPermission : LocationResult()
    object LocationDisabled : LocationResult()
    data class Error(val exception: Exception) : LocationResult()
}

@Singleton
class LocationHelperImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient,
    private val geocoder: Geocoder
) : LocationHelper {

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): LocationResult {
        if (!hasLocationPermission()) {
            return LocationResult.NoPermission
        }
        if (!isLocationEnabled()) {
            return LocationResult.LocationDisabled
        }
        return try {
            val location = fetchRawLocation()
            if (location != null) {
                val address = getAddressFromCoordinates(location.latitude, location.longitude)
                LocationResult.Success(location.latitude, location.longitude, address)
            } else {
                LocationResult.Error(Exception("Failed to get location"))
            }
        } catch (e: Exception) {
            LocationResult.Error(e)
        }
    }

    override suspend fun geocodeAddress(address: String): LocationResult {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { continuation ->
                    try {
                        geocoder.getFromLocationName(address, 1) { addresses ->
                            if (addresses.isNotEmpty()) {
                                val location = addresses[0]
                                continuation.resume(
                                    LocationResult.Success(
                                        location.latitude,
                                        location.longitude,
                                        location.getAddressLine(0)
                                    )
                                )
                            } else {
                                continuation.resume(LocationResult.Error(Exception("No location found for the address")))
                            }
                        }
                    } catch (e: Exception) {
                        continuation.resume(LocationResult.Error(e))
                    }
                }
            } else {
                val addresses = withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocationName(address, 1)
                }
                if (addresses?.isNotEmpty() == true) {
                    val location = addresses[0]
                    LocationResult.Success(location.latitude, location.longitude, location.getAddressLine(0))
                } else {
                    LocationResult.Error(Exception("No location found for the address"))
                }
            }
        } catch (e: Exception) {
            LocationResult.Error(e)
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun fetchRawLocation(): android.location.Location? {
        return suspendCancellableCoroutine { continuation ->
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setWaitForAccurateLocation(true)
                .setMinUpdateIntervalMillis(5000)
                .setMaxUpdateDelayMillis(10000)
                .build()

            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: com.google.android.gms.location.LocationResult) {
                    fusedLocationClient.removeLocationUpdates(this)
                    continuation.resume(locationResult.lastLocation)
                }
            }

            fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())

            continuation.invokeOnCancellation {
                fusedLocationClient.removeLocationUpdates(locationCallback)
            }
        }
    }

    @Suppress("DEPRECATION")
    override suspend fun getAddressFromCoordinates(latitude: Double, longitude: Double): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { continuation ->
                    try {
                        geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                            val address =
                                addresses.firstOrNull()?.getAddressLine(0) ?: "Unknown address"
                            continuation.resume(address)
                        }
                    } catch (e: Exception) {
                        continuation.resume("Could not get address")
                    }
                }
            } else {
                withContext(Dispatchers.IO) {
                    geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
                        ?.getAddressLine(0) ?: "Unknown address"
                }
            }
        } catch (e: Exception) {
            "Could not get address"
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object LocationModule {

    @Provides
    @Singleton
    fun provideFusedLocationProviderClient(@ApplicationContext context: Context): FusedLocationProviderClient {
        return LocationServices.getFusedLocationProviderClient(context)
    }

    @Provides
    @Singleton
    fun provideGeocoder(@ApplicationContext context: Context): Geocoder {
        return Geocoder(context, Locale.getDefault())
    }

    @Provides
    @Singleton
    fun provideLocationHelper(impl: LocationHelperImpl): LocationHelper {
        return impl
    }

    @Provides
    @Singleton
    fun provideGeoApiContext(@ApplicationContext context: Context): GeoApiContext {
        val ai = context.packageManager
            .getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
        val apiKey = ai.metaData.getString("com.google.android.geo.API_KEY")
        return GeoApiContext.Builder()
            .apiKey(apiKey)
            .build()
    }
}
