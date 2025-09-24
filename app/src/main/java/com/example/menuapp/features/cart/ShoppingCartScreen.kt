package com.example.menuapp.features.cart

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.menuapp.data.firebase.model.CartItem
import com.example.menuapp.ui.theme.MenuAppTheme

@Composable
fun ShoppingCartScreen(
    onBackPressed: () -> Unit,
    onNavigateToConfirmLocation: (Double, Double, String) -> Unit,
    viewModel: CartViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                viewModel.fetchAddress()
            } else {
                Toast.makeText(context, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    )

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearErrorMessage()
        }
    }

    LaunchedEffect(uiState.locationResultForConfirmation) {
        uiState.locationResultForConfirmation?.let {
            onNavigateToConfirmLocation(it.latitude, it.longitude, it.address)
            viewModel.onNavigationToConfirmLocationDone()
        }
    }

    ShoppingCartScreenContent(
        uiState = uiState,
        onBackPressed = onBackPressed,
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
        onAddressSelectionChange = viewModel::onAddressSelectionChange,
        onManualAddressChange = viewModel::onManualAddressInputChange,
        onChangeLocationClicked = viewModel::changeLocation
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingCartScreenContent(
    uiState: CartUiState,
    onBackPressed: () -> Unit,
    onPlaceOrderClicked: () -> Unit,
    onConfirmAddressClicked: () -> Unit,
    onQuantityChange: (String, Int) -> Unit,
    onAddressSelectionChange: (AddressSelection) -> Unit,
    onManualAddressChange: (String) -> Unit,
    onChangeLocationClicked: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Cart", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            CheckoutFooter(
                uiState = uiState,
                onPlaceOrderClicked = onPlaceOrderClicked
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(uiState.cartItems) { item ->
                CartListItem(
                    item = item,
                    onQuantityChange = { newQuantity ->
                        onQuantityChange(item.id, newQuantity)
                    }
                )
            }

            item {
                DeliveryAddressSection(
                    uiState = uiState,
                    onConfirmAddressClicked = onConfirmAddressClicked,
                    onAddressSelectionChange = onAddressSelectionChange,
                    onManualAddressChange = onManualAddressChange,
                    onChangeLocationClicked = onChangeLocationClicked
                )
            }
        }
    }
}

@Composable
private fun CartListItem(
    item: CartItem,
    onQuantityChange: (Int) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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
        Column(modifier = Modifier.weight(1f)) {
            Text(item.name, fontWeight = FontWeight.Bold)
            Text(String.format("₹%.2f", item.price), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        }
        QuantityStepper(
            quantity = item.quantity,
            onQuantityChange = onQuantityChange
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
            modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
            colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.Remove, contentDescription = "Decrease quantity")
        }
        Text(quantity.toString(), fontWeight = FontWeight.Bold, fontSize = 18.sp)
        IconButton(
            onClick = { onQuantityChange(quantity + 1) },
            modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
            colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Increase quantity")
        }
    }
}

@Composable
private fun CheckoutFooter(uiState: CartUiState, onPlaceOrderClicked: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 8.dp)
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SummaryRow("Subtotal", String.format("₹%.2f", uiState.subtotal))
            SummaryRow("Taxes", String.format("₹%.2f", uiState.tax))
            SummaryRow("Delivery Fee", String.format("₹%.2f", uiState.deliveryFee))
            Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            SummaryRow("Grand Total", String.format("₹%.2f", uiState.grandTotal), isBold = true)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onPlaceOrderClicked,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            enabled = uiState.cartItems.isNotEmpty() && uiState.deliveryAddress.isNotEmpty()
        ) {
            Text("Proceed to Place Order", fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
    onChangeLocationClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        Text("Delivery Address", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(8.dp))

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
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isFetchingAddress) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Confirm Address")
                }
            }
        } else {
            Text(
                text = uiState.deliveryAddress,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onChangeLocationClicked,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Change Location")
            }
        }
    }
}

@Preview(showBackground = true, name = "Light Mode")
@Composable
fun ShoppingCartScreenPreview() {
    MenuAppTheme(darkTheme = false) {
        ShoppingCartScreenContent(
            uiState = CartUiState(),
            onBackPressed = {},
            onPlaceOrderClicked = {},
            onConfirmAddressClicked = {},
            onQuantityChange = { _, _ -> },
            onAddressSelectionChange = {},
            onManualAddressChange = {},
            onChangeLocationClicked = {}
        )
    }
}
