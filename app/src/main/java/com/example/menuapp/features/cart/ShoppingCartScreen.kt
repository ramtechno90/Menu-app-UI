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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.menuapp.data.local.model.CartItemEntity
import java.net.URLEncoder
import com.example.menuapp.navigation.Screen
import com.example.menuapp.ui.theme.MenuAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingCartScreen(
    onBackPressed: () -> Unit,
    onNavigateToConfirmLocation: (Double, Double, String) -> Unit,
    viewModel: CartViewModel = hiltViewModel()
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
                onPlaceOrderClicked = {
                    viewModel.placeOrder(uiState.deliveryAddress)
                    Toast.makeText(context, "Order Placed!", Toast.LENGTH_SHORT).show()
                }
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
                        viewModel.updateQuantity(item.id, newQuantity)
                    }
                )
            }

            item {
                DeliveryAddressSection(
                    uiState = uiState,
                    onFetchAddressClicked = {
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                )
            }
        }
    }
}

@Composable
private fun CartListItem(
    item: CartItemEntity,
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

@Composable
fun DeliveryAddressSection(
    uiState: CartUiState,
    onFetchAddressClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        Text("Delivery Address", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = uiState.deliveryAddress.ifEmpty { "No address set" },
                modifier = Modifier.weight(1f),
                color = if (uiState.deliveryAddress.isNotEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onFetchAddressClicked,
                enabled = !uiState.isFetchingAddress
            ) {
                if (uiState.isFetchingAddress) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Default.MyLocation, contentDescription = "Get current location")
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Light Mode")
@Composable
fun ShoppingCartScreenPreview() {
    MenuAppTheme(darkTheme = false) {
        ShoppingCartScreen(
            onBackPressed = {},
            onNavigateToConfirmLocation = { _, _, _ -> }
        )
    }
}
