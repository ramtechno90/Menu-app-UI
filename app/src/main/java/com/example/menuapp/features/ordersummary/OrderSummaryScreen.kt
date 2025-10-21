package com.example.menuapp.features.ordersummary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.menuapp.data.firebase.model.CartItem
import com.example.menuapp.data.firebase.model.Order
import com.example.menuapp.ui.theme.MenuAppTheme
import com.example.menuapp.utils.OrderStatusMapper
import java.text.DecimalFormat
import androidx.compose.foundation.BorderStroke

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderSummaryScreen(
    onBackPressed: () -> Unit,
    onTrackOrderClicked: () -> Unit,
    viewModel: OrderSummaryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Order Summary", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            OrderActionsFooter(onTrackOrderClicked = onTrackOrderClicked)
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.order != null) {
            val order = uiState.order!!
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { OrderStatusHeader(order) }
                item { DeliveryAddressCard(order) }
                item {
                    Text(
                        "Items in Your Order",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
                items(order.items) { item ->
                    OrderItemCard(item = item, showImage = uiState.showImages)
                }
                item {
                    SummaryCard(order = order)
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Order not found.")
            }
        }
    }
}

@Composable
private fun DeliveryAddressCard(order: Order) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Delivery Address",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(order.deliveryAddress)
        }
    }
}

@Composable
private fun OrderStatusHeader(order: Order) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("Order #${order.id.take(8)}", fontSize = 14.sp)
            Text(OrderStatusMapper.mapOrderStatus(order.status), fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
        Icon(
            imageVector = Icons.Default.LocalShipping,
            contentDescription = "Delivery Status",
            tint = Green,
            modifier = Modifier.size(40.dp)
        )
    }
}

@Composable
private fun OrderItemCard(item: CartItem, showImage: Boolean) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showImage && item.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(item.imageUrl).crossfade(true).build(),
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.width(16.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.name, fontWeight = FontWeight.Bold)
                Text("Quantity: ${item.quantity}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                Text(String.format("₹%.2f", item.price), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                if (item.notes.isNotBlank()) {
                    Text(
                        text = "Notes: ${item.notes}",
                        fontSize = 14.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(order: Order) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val taxRate = if (order.subtotal > 0) (order.tax / order.subtotal) * 100 else 0.0
            val taxRateFormat = DecimalFormat("#.##'%'")
            val taxLabel = if (taxRate > 0) "Taxes (${taxRateFormat.format(taxRate)})" else "Taxes"

            SummaryRow("Subtotal", String.format("₹%.2f", order.subtotal))
            SummaryRow(taxLabel, String.format("₹%.2f", order.tax))
            SummaryRow("Delivery Fee", String.format("₹%.2f", order.deliveryFee))
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            SummaryRow("Grand Total", String.format("₹%.2f", order.grandTotal), isBold = true)
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
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
        Text(
            text = value,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun OrderActionsFooter(onTrackOrderClicked: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onTrackOrderClicked,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Track Order", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
                                OutlinedButton(
                                    onClick = { /* TODO: Implement contact support */ },
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                ) {            Text("Contact Support", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Preview(showBackground = true, name = "Light Mode")
@Composable
fun OrderSummaryScreenPreview() {
    MenuAppTheme {
        // This preview will be in a loading state as it has no ViewModel
        OrderSummaryScreen(onBackPressed = {}, onTrackOrderClicked = {})
    }
}
