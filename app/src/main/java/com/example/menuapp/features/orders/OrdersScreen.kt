package com.example.menuapp.features.orders

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.menuapp.features.currentorders.CurrentOrdersScreen
import com.example.menuapp.features.orderhistory.OrderHistoryScreen
import com.example.menuapp.ui.theme.MenuAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    onBackPressed: () -> Unit,
    onOrderClicked: (String) -> Unit,
    viewModel: OrdersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Current Orders", "Order History")

    Column(modifier = Modifier.fillMaxWidth()) {
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title, fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal) }
                )
            }
        }

        // For now, both tabs will show all orders.
        // A real implementation would filter this list based on order status.
        when (selectedTabIndex) {
            0 -> CurrentOrdersScreen(
                onBackPressed = onBackPressed,
                onOrderClicked = { order -> onOrderClicked(order.id) },
                orders = uiState.orders
            )
            1 -> OrderHistoryScreen(
                onBackPressed = onBackPressed,
                onViewDetailsClicked = { order -> onOrderClicked(order.id) },
                orders = uiState.orders
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OrdersScreenPreview() {
    MenuAppTheme {
        OrdersScreen(onBackPressed = {}, onOrderClicked = {})
    }
}
