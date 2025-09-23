package com.example.menuapp.features.confirmlocation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.menuapp.location.LocationHelper
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConfirmLocationUiState(
    val markerPosition: LatLng? = null,
    val address: String = "",
    val isLoading: Boolean = false
)

@HiltViewModel
class ConfirmLocationViewModel @Inject constructor(
    private val locationHelper: LocationHelper,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConfirmLocationUiState())
    val uiState: StateFlow<ConfirmLocationUiState> = _uiState.asStateFlow()

    init {
        val latitude = savedStateHandle.get<Float>("latitude")?.toDouble()
        val longitude = savedStateHandle.get<Float>("longitude")?.toDouble()
        val address = savedStateHandle.get<String>("address") ?: ""

        if (latitude != null && longitude != null) {
            _uiState.value = ConfirmLocationUiState(
                markerPosition = LatLng(latitude, longitude),
                address = address
            )
        }
    }

    fun updateAddressFromCoordinates(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val newAddress = locationHelper.getAddressFromCoordinates(latitude, longitude)
            _uiState.update {
                it.copy(
                    address = newAddress,
                    markerPosition = LatLng(latitude, longitude),
                    isLoading = false
                )
            }
        }
    }
}
