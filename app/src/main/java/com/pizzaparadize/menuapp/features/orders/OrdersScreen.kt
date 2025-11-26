package com.pizzaparadize.menuapp.features.orders

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
import com.pizzaparadize.menuapp.features.currentorders.CurrentOrdersScreen
import com.pizzaparadize.menuapp.ui.theme.MenuAppTheme

@Composable
fun OrdersScreen(
    onOrderClicked: (String) -> Unit,
    viewModel: OrdersViewModel = hiltViewModel(),
    contentPadding: PaddingValues = PaddingValues()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(contentPadding)
    ) {
        CurrentOrdersScreen(
            onOrderClicked = { order -> onOrderClicked(order.id) },
            orders = uiState.ongoingOrders,
            showImages = uiState.showImages
        )
    }
}

@Preview(showBackground = true)
@Composable
fun OrdersScreenPreview() {
    MenuAppTheme {
        OrdersScreen(onOrderClicked = {}, contentPadding = PaddingValues())
    }
}