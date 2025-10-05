package com.example.menuapp.features.orders

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.menuapp.features.currentorders.CurrentOrdersScreen
import com.example.menuapp.ui.theme.MenuAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    onBackPressed: () -> Unit,
    onOrderClicked: (String) -> Unit,
    viewModel: OrdersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxWidth()) {
        CurrentOrdersScreen(
            onBackPressed = onBackPressed,
            onOrderClicked = { order -> onOrderClicked(order.id) },
            orders = uiState.orders,
            showImages = uiState.showImages
        )
    }
}

@Preview(showBackground = true)
@Composable
fun OrdersScreenPreview() {
    MenuAppTheme {
        OrdersScreen(onBackPressed = {}, onOrderClicked = {})
    }
}