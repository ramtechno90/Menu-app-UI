package com.example.menuapp.features.map

import android.location.Geocoder
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.menuapp.data.firebase.model.Order
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext
import com.google.maps.model.TravelMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class MapUiState(
    val staffLocation: LatLng? = null,
    val destinationLocation: LatLng? = null,
    val route: List<LatLng> = emptyList(),
    val order: Order? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val geocoder: Geocoder,
    private val geoApiContext: GeoApiContext,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val orderId: String = checkNotNull(savedStateHandle["orderId"])

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private var locationListener: ListenerRegistration? = null

    init {
        fetchOrderAndListenForLocation()
    }

    private fun fetchOrderAndListenForLocation() {
        // Fetch the order details once
        firestore.collection("orders").document(orderId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val order = document.toObject(Order::class.java)
                    _uiState.value = _uiState.value.copy(order = order)
                    order?.deliveryAddress?.let { geocodeAddress(it) }
                }
            }
            .addOnFailureListener {
                it.printStackTrace()
            }

        // Listen for location updates continuously
        listenForStaffLocationUpdates()
    }

    private fun geocodeAddress(address: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val addresses = geocoder.getFromLocationName(address, 1)
                if (addresses != null && addresses.isNotEmpty()) {
                    val location = addresses[0]
                    val latLng = LatLng(location.latitude, location.longitude)
                    withContext(Dispatchers.Main) {
                        _uiState.value = _uiState.value.copy(destinationLocation = latLng)
                        _uiState.value.staffLocation?.let { staffLocation ->
                            fetchDirections(staffLocation, latLng)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun listenForStaffLocationUpdates() {
        locationListener?.remove()
        locationListener = firestore.collection("staff_locations").document(orderId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val lat = snapshot.getDouble("lat")
                    val lng = snapshot.getDouble("lng")
                    if (lat != null && lng != null) {
                        val staffLocation = LatLng(lat, lng)
                        _uiState.value = _uiState.value.copy(
                            staffLocation = staffLocation,
                            isLoading = false
                        )
                        _uiState.value.destinationLocation?.let { destLocation ->
                            fetchDirections(staffLocation, destLocation)
                        }
                    }
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            }
    }

    private fun fetchDirections(origin: LatLng, destination: LatLng) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = DirectionsApi.newRequest(geoApiContext)
                    .origin(com.google.maps.model.LatLng(origin.latitude, origin.longitude))
                    .destination(com.google.maps.model.LatLng(destination.latitude, destination.longitude))
                    .mode(TravelMode.DRIVING)
                    .await()

                if (result.routes.isNotEmpty()) {
                    val decodedPath = result.routes[0].overviewPolyline.decodePath()
                    val latLngPath = decodedPath.map { LatLng(it.lat, it.lng) }
                    withContext(Dispatchers.Main) {
                        _uiState.value = _uiState.value.copy(route = latLngPath)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        locationListener?.remove()
    }
}