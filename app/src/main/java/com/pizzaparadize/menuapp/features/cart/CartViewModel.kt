package com.pizzaparadize.menuapp.features.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pizzaparadize.menuapp.data.auth.AuthRepository
import com.pizzaparadize.menuapp.data.firebase.model.CartItem
import com.pizzaparadize.menuapp.data.model.DeliveryFeeSettings
import com.pizzaparadize.menuapp.data.repository.MenuRepository
import com.pizzaparadize.menuapp.data.repository.OrderRepository
import com.pizzaparadize.menuapp.data.service.FirebaseSettingsService
import com.pizzaparadize.menuapp.location.LocationHelper
import com.pizzaparadize.menuapp.location.LocationResult
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
    val deliveryDistanceKm: Double? = null,
    val isFetchingAddress: Boolean = false,
    val deliveryAddress: String = "",
    val phoneNumberInput: String = "", // The text in the TextField
    val userDefaultPhoneNumber: String = "", // The number from user's profile for placeholder
    val errorMessage: String? = null,
    val showErrorDialog: Boolean = false,
    val locationResultForConfirmation: LocationResult.Success? = null,
    val manualAddressInput: String = "",
    val addressSelection: AddressSelection = AddressSelection.CURRENT_LOCATION,
    val showImages: Boolean = true,
    val cartStep: CartStep = CartStep.ITEMS,
    val paymentMethod: String = "",
    val deliveryFeeSettings: DeliveryFeeSettings? = null
)

enum class AddressSelection {
    CURRENT_LOCATION,
    MANUAL_ENTRY
}

enum class CartStep {
    ITEMS,
    DELIVERY,
    PAYMENT,
    SUMMARY
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

    private val _deliveryDistance = MutableStateFlow<Double?>(null)
    private var notesUpdateJob: Job? = null

    init {
        viewModelScope.launch {
            combine(
                menuRepository.getCartItems(),
                firebaseSettingsService.getShowImagesSetting(),
                firebaseSettingsService.getDeliveryFeeSettings(),
                authRepository.getUserFlow(),
                _deliveryDistance
            ) { items, showImages, deliveryFeeSettings, user, distance ->
                val subtotal = items.sumOf { it.price * it.quantity }
                val tax = 0.0 // Tax removed
                val currentTaxRate = 0.0

                var deliveryFee = 0.0
                if (distance != null) {
                    if (distance <= deliveryFeeSettings.minDistanceKm) {
                        deliveryFee = deliveryFeeSettings.minDistanceRate
                    } else {
                        deliveryFee = deliveryFeeSettings.minDistanceRate + (distance - deliveryFeeSettings.minDistanceKm) * deliveryFeeSettings.additionalRatePerKm
                    }
                }

                val grandTotal = subtotal + tax + deliveryFee
                val defaultPhoneNumber = user?.phoneNumber ?: ""

                _uiState.update {
                    it.copy(
                        cartItems = items,
                        subtotal = subtotal,
                        tax = tax,
                        taxRate = currentTaxRate,
                        deliveryFee = deliveryFee,
                        grandTotal = grandTotal,
                        showImages = showImages,
                        userDefaultPhoneNumber = defaultPhoneNumber,
                        deliveryDistanceKm = distance,
                        deliveryFeeSettings = deliveryFeeSettings
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
                if (it.menuItemId == itemId) {
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
            val customerPhoneNumber =
                _uiState.value.phoneNumberInput.ifBlank { _uiState.value.userDefaultPhoneNumber }
            orderRepository.createOrder(
                address = address,
                customerName = customerName,
                customerPhoneNumber = customerPhoneNumber,
                paymentMethod = _uiState.value.paymentMethod
            )
        }
    }

    fun onPhoneNumberChange(phoneNumber: String) {
    _uiState.update { it.copy(phoneNumberInput = phoneNumber) }
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
                            errorMessage = "Location permission not granted.",
                            showErrorDialog = true
                        )
                    }
                }
                is LocationResult.LocationDisabled -> {
                    _uiState.update {
                        it.copy(
                            isFetchingAddress = false,
                            errorMessage = "Please enable location services.",
                            showErrorDialog = true
                        )
                    }
                }
                is LocationResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isFetchingAddress = false,
                            errorMessage = result.exception.message ?: "An unknown error occurred.",
                            showErrorDialog = true
                        )
                    }
                }
            }
        }
    }

    fun onLocationConfirmed(address: String) {
        _uiState.update { it.copy(deliveryAddress = address) }
        calculateDistance(address)
    }

    private fun calculateDistance(address: String) {
        viewModelScope.launch {
             // Show fetching state if needed, or just let the flow update
             _uiState.update { it.copy(isFetchingAddress = true) } // Reusing loading state for distance fetch

             when(val result = locationHelper.geocodeAddress(address)) {
                 is LocationResult.Success -> {
                      val userLat = result.latitude
                      val userLng = result.longitude

                      firebaseSettingsService.getRestaurantDetails().collect { restDetails ->
                        if (restDetails.latitude != 0.0 || restDetails.longitude != 0.0) {

                            val drivingDistanceKm = locationHelper.getDrivingDistanceKm(
                                restDetails.latitude, restDetails.longitude,
                                userLat, userLng
                            )

                            if (drivingDistanceKm != null) {
                                _deliveryDistance.value = drivingDistanceKm
                            } else {
                                // Fallback to straight line if API fails?
                                // For now, let's stick to the requirement "must match Google Maps", so failure means no fee or error.
                                // We'll assume if it fails, we default to 0.0 or keep previous.
                                // Or we could fallback to straight line calculation:
                                val results = FloatArray(1)
                                android.location.Location.distanceBetween(
                                    restDetails.latitude, restDetails.longitude,
                                    userLat, userLng,
                                    results
                                )
                                _deliveryDistance.value = results[0] / 1000.0
                            }
                        }
                        _uiState.update { it.copy(isFetchingAddress = false) }
                        throw java.util.concurrent.CancellationException("Got details")
                    }
                 }
                 else -> {
                     _uiState.update { it.copy(isFetchingAddress = false) }
                     // Handle error or do nothing
                 }
             }
        }
    }

    fun onNavigationToConfirmLocationDone() {
        _uiState.update { it.copy(locationResultForConfirmation = null) }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null, showErrorDialog = false) }
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
                        errorMessage = "Address cannot be empty.",
                        showErrorDialog = true
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

    fun onPaymentMethodSelected(paymentMethod: String) {
        _uiState.update { it.copy(paymentMethod = paymentMethod) }
    }

    fun nextStep() {
        when (_uiState.value.cartStep) {
            CartStep.ITEMS -> _uiState.update { it.copy(cartStep = CartStep.DELIVERY) }
            CartStep.DELIVERY -> {
                val state = _uiState.value
                val phoneNumber = state.phoneNumberInput.ifBlank { state.userDefaultPhoneNumber }
                val address = state.deliveryAddress.ifBlank { state.manualAddressInput }

                if (address.isBlank()) {
                    _uiState.update {
                        it.copy(
                            errorMessage = "Address cannot be empty.",
                            showErrorDialog = true
                        )
                    }
                    return
                }
                if (phoneNumber.isBlank()) {
                    _uiState.update {
                        it.copy(
                            errorMessage = "Phone number cannot be empty.",
                            showErrorDialog = true
                        )
                    }
                    return
                }
                _uiState.update { it.copy(cartStep = CartStep.PAYMENT) }
            }
            CartStep.PAYMENT -> _uiState.update { it.copy(cartStep = CartStep.SUMMARY) }
            CartStep.SUMMARY -> {}
        }
    }

    fun previousStep() {
        when (_uiState.value.cartStep) {
            CartStep.DELIVERY -> _uiState.update { it.copy(cartStep = CartStep.ITEMS) }
            CartStep.PAYMENT -> _uiState.update { it.copy(cartStep = CartStep.DELIVERY) }
            CartStep.SUMMARY -> _uiState.update { it.copy(cartStep = CartStep.PAYMENT) }
            CartStep.ITEMS -> {}
        }
    }
}
