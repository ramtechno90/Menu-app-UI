package com.example.menuapp.features.orders

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.menuapp.features.currentorders.CurrentOrdersScreen
import com.example.menuapp.features.deliveredorders.DeliveredOrdersScreen
import com.example.menuapp.ui.theme.MenuAppTheme

@Composable
fun OrdersScreen(
    contentPadding: PaddingValues,
    onOrderClicked: (String) -> Unit,
    viewModel: OrdersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Current Orders", "Delivered Orders")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(contentPadding)
    ) {
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

            when (selectedTabIndex) {
                0 -> CurrentOrdersScreen(
                    onOrderClicked = { order -> onOrderClicked(order.id) },
                    orders = uiState.ongoingOrders,
                    showImages = uiState.showImages
                )
                1 -> DeliveredOrdersScreen(
                    onOrderClicked = { order -> onOrderClicked(order.id) },
                    orders = uiState.deliveredOrders
                )
            }
        }
}

@Preview(showBackground = true)
@Composable
fun OrdersScreenPreview() {
    MenuAppTheme {
        OrdersScreen(contentPadding = PaddingValues(), onOrderClicked = {})
    }
}