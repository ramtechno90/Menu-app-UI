package com.pizzaparadize.menuapp.features.main

import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.pizzaparadize.menuapp.ui.theme.GoldenYellow
import com.pizzaparadize.menuapp.ui.theme.MenuAppTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.pizzaparadize.menuapp.features.cart.CartViewModel
import com.pizzaparadize.menuapp.features.cart.ShoppingCartScreen
import com.pizzaparadize.menuapp.features.home.HomeScreen
import com.pizzaparadize.menuapp.features.orders.OrdersScreen
import com.pizzaparadize.menuapp.features.orders.OrdersViewModel
import com.pizzaparadize.menuapp.navigation.Screen
import java.net.URLEncoder

sealed class BottomNavItem(val title: String, val icon: ImageVector, val route: String) {
    object Home : BottomNavItem("Home", Icons.Default.Home, "home_tab")
    object Cart : BottomNavItem("Cart", Icons.Default.ShoppingCart, "cart_tab")
    object Orders : BottomNavItem("Orders", Icons.AutoMirrored.Filled.ReceiptLong, "orders_tab")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    mainNavController: NavController,
    mainViewModel: MainViewModel = hiltViewModel(),
    cartViewModel: CartViewModel = hiltViewModel(),
    ordersViewModel: OrdersViewModel = hiltViewModel(),
    authAwareViewModel: AuthAwareViewModel = hiltViewModel()
) {
    val mainUiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val selectedTab = mainUiState.selectedTab
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
                        BottomNavItem.Home -> mainUiState.restaurantName
                        BottomNavItem.Cart -> "Your Cart"
                        BottomNavItem.Orders -> "Order Summary"
                    }
                    Text(text = title)
                },
                actions = {
                    if (selectedTab == BottomNavItem.Home) {
                        TextButton(onClick = { authAwareViewModel.signOut() }) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign Out")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Logout")
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                val cartUiState by cartViewModel.uiState.collectAsStateWithLifecycle()
                val cartItemCount = cartUiState.cartItems.sumOf { it.quantity }
                val items = listOf(BottomNavItem.Home, BottomNavItem.Cart, BottomNavItem.Orders)

                items.forEach { item ->
                    NavigationBarItem(
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.onPrimary,
                            unselectedIconColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                            unselectedTextColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                            indicatorColor = MaterialTheme.colorScheme.primary
                        ),
                        icon = {
                            if (item.route == BottomNavItem.Cart.route && cartItemCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            containerColor = GoldenYellow
                                        ) { Text(cartItemCount.toString()) }
                                    }
                                ) {
                                    Icon(item.icon, contentDescription = item.title)
                                }
                            } else {
                                Icon(item.icon, contentDescription = item.title)
                            }
                        },
                        label = { Text(item.title) },
                        selected = selectedTab == item,
                        onClick = { mainViewModel.onTabSelected(item) }
                    )
                }
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            BottomNavItem.Home -> HomeScreen(contentPadding = innerPadding)
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
                },
                viewModel = ordersViewModel
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    MenuAppTheme {
        MainScreen(mainNavController = rememberNavController())
    }
}
