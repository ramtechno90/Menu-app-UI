package com.example.menuapp.features.orderhistory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.menuapp.ui.theme.MenuAppTheme

enum class PastOrderStatus {
    DELIVERED, CANCELLED
}

data class PastOrder(
    val id: String,
    val itemCount: Int,
    val totalPrice: Double,
    val orderPlacedDate: String,
    val finalStatusDate: String,
    val status: PastOrderStatus
)

val samplePastOrders = listOf(
    PastOrder("1234567890", 3, 25.50, "05/12/2024 12:30 PM", "05/12/2024 01:05 PM", PastOrderStatus.DELIVERED),
    PastOrder("9876543210", 5, 32.75, "05/10/2024 07:45 PM", "05/10/2024 07:50 PM", PastOrderStatus.CANCELLED),
    PastOrder("1122334455", 2, 18.99, "05/08/2024 02:15 PM", "05/08/2024 02:45 PM", PastOrderStatus.DELIVERED),
    PastOrder("5544332211", 7, 45.20, "05/05/2024 06:00 PM", "05/05/2024 06:35 PM", PastOrderStatus.DELIVERED)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderHistoryScreen(
    onBackPressed: () -> Unit,
    onViewDetailsClicked: (PastOrder) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Order History", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.8f)
                )
            )
        },
        bottomBar = {
            // The screenshot shows a bottom nav bar, similar to the home screen
            BottomNavigationBar()
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(samplePastOrders) { order ->
                OrderHistoryCard(order = order, onViewDetails = { onViewDetailsClicked(order) })
            }
        }
    }
}

@Composable
private fun OrderHistoryCard(order: PastOrder, onViewDetails: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text("Order #${order.id}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(
                        "${order.itemCount} items",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Text(String.format("$%.2f", order.totalPrice), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Divider(modifier = Modifier.padding(vertical = 12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Order Placed: ${order.orderPlacedDate}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                val statusText = if (order.status == PastOrderStatus.DELIVERED) "Delivered" else "Cancelled"
                val statusColor = if (order.status == PastOrderStatus.DELIVERED) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                Text(
                    "$statusText: ${order.finalStatusDate}",
                    fontSize = 12.sp,
                    color = statusColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onViewDetails,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("View Details", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun BottomNavigationBar() {
    // This is a replica of the one in HomeScreen, with "Orders" selected
    var selectedItem by remember { mutableStateOf(2) } // 2 corresponds to Orders
    val items = listOf("Home", "Cart", "Orders")
    val icons = listOf(Icons.Default.Home, Icons.Default.ShoppingCart, Icons.Default.ReceiptLong)

    NavigationBar {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                icon = { Icon(icons[index], contentDescription = item) },
                label = { Text(item) },
                selected = selectedItem == index,
                onClick = {
                    selectedItem = index
                    // TODO: Implement navigation
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            )
        }
    }
}

@Preview(showBackground = true, name = "Light Mode")
@Composable
fun OrderHistoryScreenPreview() {
    MenuAppTheme(darkTheme = false) {
        OrderHistoryScreen(onBackPressed = {}, onViewDetailsClicked = {})
    }
}

@Preview(showBackground = true, name = "Dark Mode")
@Composable
fun OrderHistoryScreenDarkPreview() {
    MenuAppTheme(darkTheme = true) {
        OrderHistoryScreen(onBackPressed = {}, onViewDetailsClicked = {})
    }
}
