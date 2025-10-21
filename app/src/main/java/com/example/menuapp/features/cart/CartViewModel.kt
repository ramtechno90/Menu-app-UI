package com.example.menuapp.features.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.menuapp.data.auth.AuthRepository
import com.example.menuapp.data.firebase.model.CartItem
import com.example.menuapp.data.repository.MenuRepository
import com.example.menuapp.data.repository.OrderRepository
import com.example.menuapp.data.service.FirebaseSettingsService
import com.example.menuapp.location.LocationHelper
import com.example.menuapp.location.LocationResult
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CartUiState(
    val cartItems: List<CartItem> = emptyList(),
    val subtotal: Double = 0.0,
    val tax: Double = 0.0,
    val taxRate: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val grandTotal: Double = 0.0,
    val isFetchingAddress: Boolean = false,
    val deliveryAddress: String = "",
    val errorMessage: String? = null,
    val locationResultForConfirmation: LocationResult.Success? = null,
    val manualAddressInput: String = "",
    val addressSelection: AddressSelection = AddressSelection.CURRENT_LOCATION,
    val showImages: Boolean = true
)

enum class AddressSelection {
    CURRENT_LOCATION,
    MANUAL_ENTRY
}

@HiltViewModel
class CartViewModel @Inject constructor(
    private val menuRepository: MenuRepository,
    private val orderRepository: OrderRepository,
    private val locationHelper: LocationHelper,
    private val authRepository: AuthRepository,
    private val firebaseSettingsService: FirebaseSettingsService
) : ViewModel() {

    private val _uiState = MutableStateFlow(CartUiState())
    val uiState: StateFlow<CartUiState> = _uiState.asStateFlow()

    private var notesUpdateJob: Job? = null

    init {
        viewModelScope.launch {
            combine(
                menuRepository.getCartItems(),
                firebaseSettingsService.getTaxSettings(),
                firebaseSettingsService.getShowImagesSetting(),
                firebaseSettingsService.getDeliveryFeeSettings()
            ) { items, taxSettings, showImages, deliveryFeeSettings ->
                val subtotal = items.sumOf { it.price * it.quantity }
                val currentTaxRate = if (taxSettings.universalTax) taxSettings.taxRate else 8.0
                val tax = subtotal * (currentTaxRate / 100.0)
                val deliveryFee = if (items.isNotEmpty()) deliveryFeeSettings.fee else 0.0
                val grandTotal = subtotal + tax + deliveryFee

                _uiState.update {
                    it.copy(
                        cartItems = items,
                        subtotal = subtotal,
                        tax = tax,
                        taxRate = currentTaxRate,
                        deliveryFee = deliveryFee,
                        grandTotal = grandTotal,
                        showImages = showImages
                    )
                }
            }.collect()
        }
    }

    fun updateQuantity(itemId: String, quantity: Int) {
        viewModelScope.launch {
            menuRepository.updateQuantity(itemId, quantity)
        }
    }

    fun updateNotes(itemId: String, notes: String) {
        // Cancel the previous job if it's still running
        notesUpdateJob?.cancel()

        // Update the UI state immediately
        _uiState.update { currentState ->
            val updatedItems = currentState.cartItems.map {
                if (it.id == itemId) {
                    it.copy(notes = notes)
                } else {
                    it
                }
            }
            currentState.copy(cartItems = updatedItems)
        }

        // Launch a new job to update Firestore after a delay
        notesUpdateJob = viewModelScope.launch {
            delay(500L) // Debounce time
            menuRepository.updateNotes(itemId, notes)
        }
    }

    fun removeItem(itemId: String) {
        updateQuantity(itemId, 0)
    }

    fun placeOrder(address: String) {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser()
            val customerName = user?.username ?: "Guest"
            val customerPhoneNumber = user?.phoneNumber
            orderRepository.createOrder(address, customerName, customerPhoneNumber)
        }
    }

    fun fetchAddress() {
        viewModelScope.launch {
            _uiState.update { it.copy(isFetchingAddress = true, errorMessage = null) }
            when (val result = locationHelper.getCurrentLocation()) {
                is LocationResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isFetchingAddress = false,
                            locationResultForConfirmation = result
                        )
                    }
                }
                is LocationResult.NoPermission -> {
                    _uiState.update {
                        it.copy(
                            isFetchingAddress = false,
                            errorMessage = "Location permission not granted."
                        )
                    }
                }
                is LocationResult.LocationDisabled -> {
                    _uiState.update {
                        it.copy(
                            isFetchingAddress = false,
                            errorMessage = "Please enable location services."
                        )
                    }
                }
                is LocationResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isFetchingAddress = false,
                            errorMessage = result.exception.message ?: "An unknown error occurred."
                        )
                    }
                }
            }
        }
    }

    fun onLocationConfirmed(address: String) {
        _uiState.update { it.copy(deliveryAddress = address) }
    }

    fun onNavigationToConfirmLocationDone() {
        _uiState.update { it.copy(locationResultForConfirmation = null) }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onAddressSelectionChange(selection: AddressSelection) {
        _uiState.update { it.copy(addressSelection = selection) }
    }

    fun onManualAddressInputChange(address: String) {
        _uiState.update { it.copy(manualAddressInput = address) }
    }

    fun geocodeManualAddress() {
        viewModelScope.launch {
            _uiState.update { it.copy(isFetchingAddress = true, errorMessage = null) }
            val address = _uiState.value.manualAddressInput
            if (address.isBlank()) {
                _uiState.update {
                    it.copy(
                        isFetchingAddress = false,
                        errorMessage = "Address cannot be empty."
                    )
                }
                return@launch
            }
            when (val result = locationHelper.geocodeAddress(address)) {
                is LocationResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isFetchingAddress = false,
                            locationResultForConfirmation = result
                        )
                    }
                }
                is LocationResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isFetchingAddress = false,
                            errorMessage = result.exception.message ?: "An unknown error occurred."
                        )
                    }
                }
                else -> {
                    _uiState.update {
                        it.copy(
                            isFetchingAddress = false,
                            errorMessage = "An unexpected error occurred."
                        )
                    }
                }
            }
        }
    }

    fun changeLocation() {
        _uiState.update {
            it.copy(
                deliveryAddress = "",
                locationResultForConfirmation = null
            )
        }
    }
}
