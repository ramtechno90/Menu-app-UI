package com.pizzaparadize.menuapp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pizzaparadize.menuapp.features.main.MainScreen
import com.pizzaparadize.menuapp.features.ordersummary.OrderSummaryScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.pizzaparadize.menuapp.features.confirmlocation.ConfirmLocationScreen
import com.pizzaparadize.menuapp.features.ordertracking.OrderTrackingScreen
import java.net.URLDecoder
import androidx.navigation.NavHostController
import com.pizzaparadize.menuapp.features.welcome.WelcomeScreen
import com.pizzaparadize.menuapp.features.roleselection.RoleSelectionScreen
import com.pizzaparadize.menuapp.features.admin.AdminOrdersScreen
import com.pizzaparadize.menuapp.features.admin.AdminLoginScreen
import com.pizzaparadize.menuapp.features.delivery.DeliveryLoginScreen
import com.pizzaparadize.menuapp.features.delivery.DeliveryOrdersScreen

@Composable
fun AppNavigation(
    startDestination: String,
    navController: NavHostController,
    isAuthenticated: Boolean,
    isAdmin: Boolean
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.RoleSelection.route) {
            RoleSelectionScreen(
                onCustomerClicked = {
                    val destination = if (isAuthenticated) Screen.Main.route else Screen.Welcome.route
                    navController.navigate(destination) {
                        popUpTo(Screen.RoleSelection.route) { inclusive = true }
                    }
                },
                onAdminClicked = {
                    if (isAdmin) {
                        navController.navigate(Screen.AdminOrders.route)
                    } else {
                        navController.navigate(Screen.AdminLogin.route)
                    }
                },
                onDeliveryStaffClicked = {
                    if (isAdmin) {
                        navController.navigate(Screen.DeliveryOrders.route)
                    } else {
                        navController.navigate(Screen.DeliveryLogin.route)
                    }
                }
            )
        }
        composable(Screen.DeliveryLogin.route) {
            DeliveryLoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.DeliveryOrders.route) {
                        popUpTo(Screen.RoleSelection.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.DeliveryOrders.route) {
            DeliveryOrdersScreen(
                onLogout = {
                    navController.navigate(Screen.RoleSelection.route) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(Screen.AdminLogin.route) {
            AdminLoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.AdminOrders.route) {
                        popUpTo(Screen.RoleSelection.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.AdminOrders.route) {
            AdminOrdersScreen(
                onLogout = {
                    navController.navigate(Screen.RoleSelection.route) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onNameEntered = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Main.route) {
            MainScreen(
                mainNavController = navController,
                onLogout = {
                    navController.navigate(Screen.RoleSelection.route) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(Screen.OrderSummary.route) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId")
            // In a real app, you'd use the orderId to fetch data and pass it to the screen
            OrderSummaryScreen(
                onBackPressed = { navController.popBackStack() },
                onTrackOrderClicked = {
                    // Assuming the same orderId is used for tracking
                    if (orderId != null) {
                        navController.navigate(Screen.OrderTracking.createRoute(orderId))
                    }
                }
            )
        }
        composable(Screen.OrderTracking.route) {
            OrderTrackingScreen(
                onBackPressed = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.ConfirmLocation.route,
            arguments = listOf(
                navArgument("latitude") { type = NavType.FloatType },
                navArgument("longitude") { type = NavType.FloatType },
                navArgument("address") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val latitude = backStackEntry.arguments?.getFloat("latitude")?.toDouble()
            val longitude = backStackEntry.arguments?.getFloat("longitude")?.toDouble()
            val address = backStackEntry.arguments?.getString("address")?.let {
                URLDecoder.decode(it, "UTF-8")
            }

            if (latitude != null && longitude != null && address != null) {
                ConfirmLocationScreen(
                    onConfirmClicked = { newAddress ->
                        // Pass the confirmed address back to the cart screen
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("confirmed_address", newAddress)
                        navController.popBackStack()
                    },
                    onBackPressed = { navController.popBackStack() }
                )
            }
        }
    }
}
