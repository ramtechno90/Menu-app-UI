package com.example.menuapp.features.ordertracking

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.menuapp.data.firebase.model.Order
import com.example.menuapp.data.repository.OrderRepository
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
    private val staffLocation: StateFlow<LatLng?> = _staffLocation.asStateFlow()
    private var locationListener: ListenerRegistration? = null

    private val orderFlow = orderRepository.getOrderById(orderId)

    val uiState: StateFlow<OrderTrackingUiState> = combine(
        orderFlow,
        staffLocation
    ) { order, location ->
        OrderTrackingUiState(order = order, isLoading = false, staffLocation = location)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = OrderTrackingUiState(isLoading = true)
    )

    init {
        // Using hardcoded staff ID as per user request for a temporary fix.
        // The permanent solution is to store the staff's document ID in the order's `assignedTo` field.
        listenForStaffLocationUpdates("WOXb8fwaipPViMKEddVk")
    }

    private fun listenForStaffLocationUpdates(staffId: String) {
        locationListener?.remove() // Remove previous listener
        locationListener = firestore.collection("staff_locations").document(staffId)
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

    override fun onCleared() {
        super.onCleared()
        locationListener?.remove()
    }
}
