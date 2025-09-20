package com.example.menuapp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.menuapp.features.main.MainScreen
import com.example.menuapp.features.ordersummary.OrderSummaryScreen
import com.example.menuapp.features.ordertracking.OrderTrackingScreen
import com.example.menuapp.features.roleselection.RoleSelectionScreen

@Composable
fun AppNavigation() {
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
            MainScreen(mainNavController = navController)
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
    }
}
