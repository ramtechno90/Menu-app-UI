package com.example.menuapp.features.map

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.example.menuapp.data.firebase.model.Order
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class MapUiState(
    val staffLocation: LatLng? = null,
    val destinationLocation: LatLng? = null,
    val route: List<LatLng> = emptyList(),
    val order: Order? = null,
    val isLoading: Boolean = true
)

import android.location.Geocoder
import androidx.lifecycle.viewModelScope
import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext
import com.google.maps.model.TravelMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class MapViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val geocoder: Geocoder,
    private val geoApiContext: GeoApiContext,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val staffId: String = checkNotNull(savedStateHandle["staffId"])

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private var locationListener: ListenerRegistration? = null

    init {
        fetchOrderAndListenForLocation()
    }

    private fun fetchOrderAndListenForLocation() {
        firestore.collection("orders")
            .whereEqualTo("assignedTo", staffId)
            .whereIn("status", listOf("PICKED_UP", "OUT_FOR_DELIVERY"))
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    return@addOnSuccessListener
                }
                val order = documents.documents[0].toObject(Order::class.java)
                _uiState.value = _uiState.value.copy(order = order)
                order?.deliveryAddress?.let { geocodeAddress(it) }
                listenForStaffLocationUpdates()
            }
            .addOnFailureListener {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
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
        locationListener = firestore.collection("staff_locations").document(staffId)
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