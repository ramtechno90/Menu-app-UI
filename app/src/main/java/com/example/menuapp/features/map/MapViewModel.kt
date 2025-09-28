package com.example.menuapp.features.map

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val isLoading: Boolean = true
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // The staffId from nav args is unused due to the temporary hardcoding below.
    // private val staffId: String = checkNotNull(savedStateHandle["staffId"])

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private var locationListener: ListenerRegistration? = null

    init {
        listenForStaffLocationUpdates()
    }

    private fun listenForStaffLocationUpdates() {
        // Using hardcoded staff ID as per user request for a temporary fix.
        val staffId = "WOXb8fwaipPViMKEddVk"
        locationListener?.remove()
        locationListener = firestore.collection("staff_locations").document(staffId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    // Handle error, maybe update UI state with an error message
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val lat = snapshot.getDouble("lat")
                    val lng = snapshot.getDouble("lng")
                    if (lat != null && lng != null) {
                        _uiState.value = MapUiState(
                            staffLocation = LatLng(lat, lng),
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        locationListener?.remove()
    }
}