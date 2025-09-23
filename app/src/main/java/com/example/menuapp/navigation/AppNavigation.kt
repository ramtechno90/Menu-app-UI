package com.example.menuapp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.menuapp.features.main.MainScreen
import com.example.menuapp.features.ordersummary.OrderSummaryScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.menuapp.features.confirmlocation.ConfirmLocationScreen
import com.example.menuapp.features.ordertracking.OrderTrackingScreen
import java.net.URLDecoder
import com.example.menuapp.features.roleselection.RoleSelectionScreen

@Composable
fun AppNavigation(
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.RoleSelection.route) {
        composable(Screen.RoleSelection.route) {
            RoleSelectionScreen(
                onCustomerSelected = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.RoleSelection.route) { inclusive = true }
                    }
                },
                onAdminSelected = { /* TODO */ },
                onDeliverySelected = { /* TODO */ }
            )
        }
        composable(Screen.Main.route) {
            MainScreen(
                mainNavController = navController,
                isDarkTheme = isDarkTheme,
                onThemeToggle = onThemeToggle
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
        composable(Screen.OrderTracking.route) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId")
            // In a real app, you'd use the orderId to fetch data
            OrderTrackingScreen(onBackPressed = { navController.popBackStack() })
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
                    latitude = latitude,
                    longitude = longitude,
                    address = address,
                    onConfirmClicked = {
                        // Pass the confirmed address back to the cart screen
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("confirmed_address", address)
                        navController.popBackStack()
                    },
                    onBackPressed = { navController.popBackStack() }
                )
            }
        }
    }
}
