package com.example.menuapp.features.ordertracking

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.menuapp.data.firebase.model.Order
import com.example.menuapp.data.repository.OrderRepository
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class OrderTrackingUiState(
    val order: Order? = null,
    val isLoading: Boolean = true,
    val staffLocation: LatLng? = null
)

@HiltViewModel
class OrderTrackingViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val firestore: FirebaseFirestore,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val orderId: String = checkNotNull(savedStateHandle["orderId"])

    private val _staffLocation = MutableStateFlow<LatLng?>(null)
    val staffLocation: StateFlow<LatLng?> = _staffLocation.asStateFlow()

    val uiState: StateFlow<OrderTrackingUiState> = combine(
        orderRepository.getOrderById(orderId),
        staffLocation
    ) { order, location ->
        OrderTrackingUiState(order = order, isLoading = false, staffLocation = location)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = OrderTrackingUiState(isLoading = true)
    )

    init {
        listenForStaffLocationUpdates()
    }

    private fun listenForStaffLocationUpdates() {
        // staffId is hardcoded as per requirement
        val staffId = "staff123"
        firestore.collection("staff_locations").document(staffId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    // Handle error
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val lat = snapshot.getDouble("lat")
                    val lng = snapshot.getDouble("lng")
                    if (lat != null && lng != null) {
                        _staffLocation.value = LatLng(lat, lng)
                    }
                }
            }
    }
}
