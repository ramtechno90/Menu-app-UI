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
import androidx.navigation.NavHostController
import com.example.menuapp.features.auth.OtpVerificationScreen
import com.example.menuapp.features.auth.SignInScreen
import com.example.menuapp.features.auth.SignUpScreen
import com.example.menuapp.features.map.MapScreen
import com.example.menuapp.features.welcome.WelcomeScreen

@Composable
fun AppNavigation(
    startDestination: String,
    onSendOtpClicked: (String) -> Unit,
    onVerifyOtpClicked: (String, String) -> Unit,
    navController: NavHostController
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.SignIn.route) {
            SignInScreen(
                onSignInSuccess = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                onNavigateToSignUp = { navController.navigate(Screen.SignUp.route) },
                onSendOtpClicked = onSendOtpClicked
            )
        }
        composable(Screen.SignUp.route) {
            SignUpScreen(
                onSignUpSuccess = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                onNavigateToSignIn = { navController.popBackStack() }
            )
        }
        composable(Screen.OtpVerification.route) { backStackEntry ->
            val verificationId = backStackEntry.arguments?.getString("verificationId")
            if (verificationId != null) {
                OtpVerificationScreen(onVerifyOtpClicked = { otp ->
                    onVerifyOtpClicked(verificationId, otp)
                })
            }
        }
        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onNavigateToSignIn = { navController.navigate(Screen.SignIn.route) },
                onNavigateToSignUp = { navController.navigate(Screen.SignUp.route) }
            )
        }
        composable(Screen.Main.route) {
            MainScreen(
                mainNavController = navController
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
        composable(
            route = Screen.Map.route,
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) {
            MapScreen(onBackPressed = { navController.popBackStack() })
        }
    }
}
