package com.example.menuapp.features.main

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.menuapp.features.cart.CartViewModel
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
    onThemeToggle: () -> Unit,
    mainViewModel: MainViewModel = hiltViewModel(),
    cartViewModel: CartViewModel = hiltViewModel(),
    authAwareViewModel: AuthAwareViewModel = hiltViewModel()
) {
    val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val selectedTab = uiState.selectedTab
    val user by authAwareViewModel.user.collectAsStateWithLifecycle()
    val showWelcomeDialogEvent by authAwareViewModel.showWelcomeDialogEvent.collectAsStateWithLifecycle()

    val currentUser = user
    if (showWelcomeDialogEvent && currentUser != null) {
        WelcomeDialog(
            user = currentUser,
            onDismiss = { authAwareViewModel.onWelcomeDialogDismissed() }
        )
    }

    val savedStateHandle = mainNavController.currentBackStackEntry?.savedStateHandle
    LaunchedEffect(savedStateHandle) {
        savedStateHandle?.getLiveData<String>("confirmed_address")?.observeForever { address ->
            address?.let {
                cartViewModel.onLocationConfirmed(it)
                savedStateHandle.remove<String>("confirmed_address")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val title = when (selectedTab) {
                        BottomNavItem.Home -> "Menu App"
                        BottomNavItem.Cart -> "Your Cart"
                        BottomNavItem.Orders -> "My Orders"
                    }
                    Text(text = title)
                },
                actions = {
                    if (selectedTab == BottomNavItem.Home) {
                        IconButton(onClick = onThemeToggle) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Toggle Theme"
                            )
                        }
                        TextButton(onClick = { authAwareViewModel.signOut() }) {
                            Icon(Icons.Default.Logout, contentDescription = "Sign Out")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Logout")
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                val items = listOf(BottomNavItem.Home, BottomNavItem.Cart, BottomNavItem.Orders)
                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = selectedTab == item,
                        onClick = { mainViewModel.onTabSelected(item) }
                    )
                }
            }
        }
    ) { innerPadding ->
        // The content of each tab is rendered here. The `innerPadding` is passed
        // to the respective screen to handle the space needed for the TopAppBar and BottomNavBar.
        when (selectedTab) {
            BottomNavItem.Home -> HomeScreen(
                contentPadding = innerPadding
            )

            BottomNavItem.Cart -> ShoppingCartScreen(
                contentPadding = innerPadding,
                onNavigateToConfirmLocation = { latitude, longitude, address ->
                    val encodedAddress = URLEncoder.encode(address, "UTF-8")
                    mainNavController.navigate(
                        Screen.ConfirmLocation.createRoute(latitude, longitude, encodedAddress)
                    )
                },
                viewModel = cartViewModel
            )

            BottomNavItem.Orders -> OrdersScreen(
                contentPadding = innerPadding,
                onOrderClicked = { orderId ->
                    mainNavController.navigate(Screen.OrderSummary.createRoute(orderId))
                }
            )
        }
    }
}
