package com.example.menuapp.navigation

sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object EnterName : Screen("enter_name")
    object Main : Screen("main") // This will be a container for screens with bottom nav
    object Home : Screen("home")
    object ShoppingCart : Screen("shopping_cart")
    object Orders : Screen("orders") // A container for Current and History
    object OrderSummary : Screen("order_summary/{orderId}") {
        fun createRoute(orderId: String) = "order_summary/$orderId"
    }
    object OrderTracking : Screen("order_tracking/{orderId}") {
        fun createRoute(orderId: String) = "order_tracking/$orderId"
    }
    object ConfirmLocation : Screen("confirm_location/{latitude}/{longitude}/{address}") {
        fun createRoute(latitude: Double, longitude: Double, address: String) = "confirm_location/$latitude/$longitude/$address"
    }
}
