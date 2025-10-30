package com.example.menuapp.features.cart

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.menuapp.data.firebase.model.CartItem
import com.example.menuapp.ui.theme.MenuAppTheme
import java.text.DecimalFormat

@Composable
fun ShoppingCartScreen(
    contentPadding: PaddingValues,
    onNavigateToConfirmLocation: (Double, Double, String) -> Unit,
    viewModel: CartViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showPermissionRationale by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                viewModel.fetchAddress()
            } else {
                showPermissionRationale = true
            }
        }
    )

    if (uiState.showErrorDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.clearErrorMessage() },
            title = { Text("Error") },
            text = { Text(uiState.errorMessage ?: "An unknown error occurred.") },
            confirmButton = {
                Button(onClick = { viewModel.clearErrorMessage() }) {
                    Text("Close")
                }
            }
        )
    }

    if (showPermissionRationale) {
        AlertDialog(
            onDismissRequest = { showPermissionRationale = false },
            title = { Text("Location Permission Required") },
            text = { Text("Location permission is required to determine your delivery address. Please grant the permission in the app settings.") },
            confirmButton = {
                Button(onClick = {
                    showPermissionRationale = false
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", context.packageName, null)
                    intent.data = uri
                    context.startActivity(intent)
                }) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                Button(onClick = { showPermissionRationale = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    LaunchedEffect(uiState.locationResultForConfirmation) {
        uiState.locationResultForConfirmation?.let {
            onNavigateToConfirmLocation(it.latitude, it.longitude, it.address)
            viewModel.onNavigationToConfirmLocationDone()
        }
    }

    ShoppingCartScreenContent(
        contentPadding = contentPadding,
        uiState = uiState,
        onPlaceOrderClicked = {
            viewModel.placeOrder(uiState.deliveryAddress)
            Toast.makeText(context, "Order Placed!", Toast.LENGTH_SHORT).show()
        },
        onConfirmAddressClicked = {
            when (uiState.addressSelection) {
                AddressSelection.CURRENT_LOCATION -> locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                AddressSelection.MANUAL_ENTRY -> viewModel.geocodeManualAddress()
            }
        },
        onQuantityChange = { itemId, quantity ->
            viewModel.updateQuantity(itemId, quantity)
        },
        onNotesChange = { itemId, notes ->
            viewModel.updateNotes(itemId, notes)
        },
        onAddressSelectionChange = viewModel::onAddressSelectionChange,
        onManualAddressChange = viewModel::onManualAddressInputChange,
        onChangeLocationClicked = viewModel::changeLocation,
        onPhoneNumberChange = viewModel::onPhoneNumberChange,
        onNextStep = viewModel::nextStep,
        onPreviousStep = viewModel::previousStep,
        onPaymentMethodSelected = viewModel::onPaymentMethodSelected
    )
}

@Composable
fun ShoppingCartScreenContent(
    contentPadding: PaddingValues,
    uiState: CartUiState,
    onPlaceOrderClicked: () -> Unit,
    onConfirmAddressClicked: () -> Unit,
    onQuantityChange: (String, Int) -> Unit,
    onNotesChange: (String, String) -> Unit,
    onAddressSelectionChange: (AddressSelection) -> Unit,
    onManualAddressChange: (String) -> Unit,
    onChangeLocationClicked: () -> Unit,
    onPhoneNumberChange: (String) -> Unit,
    onNextStep: () -> Unit,
    onPreviousStep: () -> Unit,
    onPaymentMethodSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        when (uiState.cartStep) {
            CartStep.ITEMS -> {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(uiState.cartItems) { item ->
                        CartListItem(
                            item = item,
                            onQuantityChange = { newQuantity ->
                                onQuantityChange(item.id, newQuantity)
                            },
                            onNotesChange = { newNotes ->
                                onNotesChange(item.id, newNotes)
                            },
                            showImage = uiState.showImages
                        )
                    }

                    item {
                        PriceDetailsCard(uiState = uiState)
                    }

                    item {
                        Button(onClick = onNextStep, modifier = Modifier.fillMaxWidth()) {
                            Text("Proceed to Delivery")
                        }
                    }
                }
            }
            CartStep.DELIVERY -> {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    item {
                        DeliveryAddressSection(
                            uiState = uiState,
                            onConfirmAddressClicked = onConfirmAddressClicked,
                            onAddressSelectionChange = onAddressSelectionChange,
                            onManualAddressChange = onManualAddressChange,
                            onChangeLocationClicked = onChangeLocationClicked,
                            onPhoneNumberChange = onPhoneNumberChange
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onNextStep,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Proceed to Payment Options")
                            }
                            OutlinedButton(
                                onClick = onPreviousStep,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Back to Items")
                            }
                        }
                    }
                }
            }
            CartStep.PAYMENT -> {
                PaymentMethodSelection(
                    onNextStep = onNextStep,
                    onPreviousStep = onPreviousStep,
                    onPaymentMethodSelected = onPaymentMethodSelected,
                    selectedPaymentMethod = uiState.paymentMethod
                )
            }
            CartStep.SUMMARY -> {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    item {
                        Text("Items", style = MaterialTheme.typography.titleLarge)
                    }

                    items(uiState.cartItems) { item ->
                        CartListItem(
                            item = item,
                            onQuantityChange = { newQuantity ->
                                onQuantityChange(item.id, newQuantity)
                            },
                            onNotesChange = { newNotes ->
                                onNotesChange(item.id, newNotes)
                            },
                            showImage = uiState.showImages
                        )
                    }

                    item {
                        val phoneNumber =
                            uiState.phoneNumberInput.ifBlank { uiState.userDefaultPhoneNumber }

                        Text("Delivery Details", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(
                            modifier = Modifier.padding(start = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row {
                                Text(
                                    text = "Address: ",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = uiState.deliveryAddress,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Row {
                                Text(
                                    text = "Phone: ",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = phoneNumber,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Payment Method", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.padding(start = 8.dp)) {
                            Text(
                                text = "Method: ",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = uiState.paymentMethod,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    item {
                        PriceDetailsCard(uiState = uiState)
                    }

                    item {
                        Button(
                            onClick = onPlaceOrderClicked,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = uiState.cartItems.isNotEmpty() && uiState.deliveryAddress.isNotEmpty()
                        ) {
                            Text("Proceed to Place Order")
                        }
                    }

                    item {
                        OutlinedButton(onClick = onPreviousStep, modifier = Modifier.fillMaxWidth()) {
                            Text("Back to Payment Options")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PriceDetailsCard(uiState: CartUiState) {
    Column(
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                RoundedCornerShape(12.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val taxRateFormat = DecimalFormat("#.##'%'")
        val taxLabel =
            if (uiState.taxRate > 0) "Taxes (${taxRateFormat.format(uiState.taxRate)})" else "Taxes"

        SummaryRow("Subtotal", String.format("₹%.2f", uiState.subtotal))
        SummaryRow(taxLabel, String.format("₹%.2f", uiState.tax))
        SummaryRow("Delivery Fee", String.format("₹%.2f", uiState.deliveryFee))
        Divider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        )
        SummaryRow("Grand Total", String.format("₹%.2f", uiState.grandTotal), isBold = true)
    }
}

@Composable
private fun CartListItem(
    item: CartItem,
    onQuantityChange: (Int) -> Unit,
    onNotesChange: (String) -> Unit,
    showImage: Boolean
) {
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showImage && item.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(item.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(modifier = Modifier.width(16.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, fontWeight = FontWeight.Bold)
                Text(
                    String.format("₹%.2f", item.price),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            QuantityStepper(
                quantity = item.quantity,
                onQuantityChange = onQuantityChange
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = item.notes,
            onValueChange = onNotesChange,
            label = { Text("Add a note (e.g. no onions)") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
private fun QuantityStepper(
    quantity: Int,
    onQuantityChange: (Int) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconButton(
            onClick = { onQuantityChange(quantity - 1) },
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.Remove, contentDescription = "Decrease quantity")
        }
        Text(quantity.toString(), fontWeight = FontWeight.Bold, fontSize = 18.sp)
        IconButton(
            onClick = { onQuantityChange(quantity + 1) },
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Increase quantity")
        }
    }
}


@Composable
private fun SummaryRow(label: String, value: String, isBold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            fontSize = if (isBold) 18.sp else 16.sp,
            color = if (isBold) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            fontSize = if (isBold) 18.sp else 16.sp,
            color = if (isBold) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryAddressSection(
    uiState: CartUiState,
    onConfirmAddressClicked: () -> Unit,
    onAddressSelectionChange: (AddressSelection) -> Unit,
    onManualAddressChange: (String) -> Unit,
    onChangeLocationClicked: () -> Unit,
    onPhoneNumberChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        Text("Delivery Details", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.deliveryAddress.isEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = uiState.addressSelection == AddressSelection.CURRENT_LOCATION,
                    onClick = { onAddressSelectionChange(AddressSelection.CURRENT_LOCATION) }
                )
                Text("Use Current Location")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = uiState.addressSelection == AddressSelection.MANUAL_ENTRY,
                    onClick = { onAddressSelectionChange(AddressSelection.MANUAL_ENTRY) }
                )
                Text("Enter Manually")
            }

            if (uiState.addressSelection == AddressSelection.MANUAL_ENTRY) {
                OutlinedTextField(
                    value = uiState.manualAddressInput,
                    onValueChange = onManualAddressChange,
                    label = { Text("Enter your address") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onConfirmAddressClicked,
                enabled = !uiState.isFetchingAddress,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (uiState.isFetchingAddress) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onSecondary)
                } else {
                    Text("Confirm Address")
                }
            }
        } else {
            Text("Delivery Point Address", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = uiState.deliveryAddress,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onChangeLocationClicked,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Change Location")
            }

            Spacer(modifier = Modifier.height(16.dp))
            Spacer(modifier = Modifier.height(16.dp))

            Text("Confirm Phone Number", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.phoneNumberInput,
                onValueChange = { newNumber ->
                    val digitsOnly = newNumber.filter { it.isDigit() }
                    if (digitsOnly.length <= 10) {
                        onPhoneNumberChange(digitsOnly)
                    }
                },
                placeholder = { Text(uiState.userDefaultPhoneNumber) },
                label = { Text("Phone Number") },
                modifier = Modifier.fillMaxWidth(),
                prefix = { Text("+91 ") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Note: Your phone number will only be used for communication related to your order and delivery.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Preview(showBackground = true, name = "Light Mode")
@Composable
fun ShoppingCartScreenPreview() {
    MenuAppTheme {
        ShoppingCartScreenContent(
            contentPadding = PaddingValues(),
            uiState = CartUiState(),
            onPlaceOrderClicked = {},
            onConfirmAddressClicked = {},
            onQuantityChange = { _, _ -> },
            onNotesChange = { _, _ -> },
            onAddressSelectionChange = {},
            onManualAddressChange = {},
            onChangeLocationClicked = {},
            onPhoneNumberChange = {},
            onNextStep = {},
            onPreviousStep = {},
            onPaymentMethodSelected = {}
        )
    }
}

@Composable
fun PaymentMethodSelection(
    onNextStep: () -> Unit,
    onPreviousStep: () -> Unit,
    onPaymentMethodSelected: (String) -> Unit,
    selectedPaymentMethod: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Payment Method", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = selectedPaymentMethod == "Cash on Delivery",
                onClick = { onPaymentMethodSelected("Cash on Delivery") }
            )
            Column {
                Text("Cash on Delivery")
                Text(
                    "Note: UPI payments are also accepted by the delivery staff",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onNextStep,
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedPaymentMethod.isNotEmpty()
        ) {
            Text("Proceed to Summary")
        }
        OutlinedButton(
            onClick = onPreviousStep,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back to Delivery Details")
        }
    }
}
