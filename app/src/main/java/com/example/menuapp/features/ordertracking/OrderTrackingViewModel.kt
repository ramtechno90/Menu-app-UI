package com.example.menuapp.features.ordertracking

import android.util.Log
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
    private val TAG = "OrderTrackingViewModel"

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
        Log.d(TAG, "ViewModel initialized for orderId: $orderId")
        viewModelScope.launch {
            orderFlow.collect { order ->
                Log.d(TAG, "Order data received: $order")
                val staffId = order?.assignedTo
                if (!staffId.isNullOrBlank()) {
                    Log.d(TAG, "Found staffId: $staffId. Starting location listener.")
                    listenForStaffLocationUpdates(staffId)
                } else {
                    Log.w(TAG, "Staff ID is null or blank. Cannot start location listener.")
                }
            }
        }
    }

    private fun listenForStaffLocationUpdates(staffId: String) {
        Log.d(TAG, "Setting up listener for staffId: $staffId")
        locationListener?.remove() // Remove previous listener
        locationListener = firestore.collection("staff_locations").document(staffId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Firestore listener error", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    Log.d(TAG, "Snapshot received for staffId: $staffId")
                    val lat = snapshot.getDouble("lat")
                    val lng = snapshot.getDouble("lng")
                    Log.d(TAG, "Lat: $lat, Lng: $lng")
                    if (lat != null && lng != null) {
                        val newLocation = LatLng(lat, lng)
                        _staffLocation.value = newLocation
                        Log.d(TAG, "Staff location updated: $newLocation")
                    } else {
                        Log.w(TAG, "Lat or Lng is null in the snapshot.")
                    }
                } else {
                    Log.w(TAG, "Snapshot for staffId: $staffId is null or does not exist.")
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel cleared. Removing listener.")
        locationListener?.remove()
    }
}
