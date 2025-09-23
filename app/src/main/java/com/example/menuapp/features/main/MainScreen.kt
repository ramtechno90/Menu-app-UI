package com.example.menuapp.features.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import com.example.menuapp.features.cart.ShoppingCartScreen
import com.example.menuapp.features.home.HomeScreen
import java.net.URLEncoder
import com.example.menuapp.features.orders.OrdersScreen
import com.example.menuapp.navigation.Screen

sealed class BottomNavItem(val title: String, val icon: ImageVector, val route: String) {
    object Home : BottomNavItem("Home", Icons.Default.Home, "home_tab")
    object Cart : BottomNavItem("Cart", Icons.Default.ShoppingCart, "cart_tab")
    object Orders : BottomNavItem("Orders", Icons.Default.ReceiptLong, "orders_tab")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    mainNavController: NavController,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit
) {
    var selectedTab by remember { mutableStateOf<BottomNavItem>(BottomNavItem.Home) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                val items = listOf(BottomNavItem.Home, BottomNavItem.Cart, BottomNavItem.Orders)
                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = selectedTab == item,
                        onClick = { selectedTab = item }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                BottomNavItem.Home -> HomeScreen(isDarkTheme = isDarkTheme, onThemeToggle = onThemeToggle)
                BottomNavItem.Cart -> ShoppingCartScreen(
                    onBackPressed = { /* Within main screen, no back press */ },
                    onNavigateToConfirmLocation = { latitude, longitude, address ->
                        val encodedAddress = URLEncoder.encode(address, "UTF-8")
                        mainNavController.navigate(
                            Screen.ConfirmLocation.createRoute(latitude, longitude, encodedAddress)
                        )
                    }
                )
                BottomNavItem.Orders -> OrdersScreen(
                    onBackPressed = { /* No back press */ },
                    onOrderClicked = { orderId ->
                        mainNavController.navigate(Screen.OrderSummary.createRoute(orderId))
                    }
                )
            }
        }
    }
}
