package com.pizzaparadize.menuapp

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.pizzaparadize.menuapp.data.auth.AuthRepository
import com.pizzaparadize.menuapp.navigation.AppNavigation
import com.pizzaparadize.menuapp.navigation.Screen
import com.pizzaparadize.menuapp.ui.theme.MenuAppTheme
import com.pizzaparadize.menuapp.utils.UpdateManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository
    private lateinit var updateManager: UpdateManager
    private lateinit var updateLauncher: ActivityResultLauncher<IntentSenderRequest>

    private val destinationFlow = MutableStateFlow<String?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra("destination")?.let {
            destinationFlow.value = it
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            intent.getStringExtra("destination")?.let {
                destinationFlow.value = it
            }
        }

        updateLauncher = registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            if (result.resultCode != RESULT_OK) {
                updateManager.checkForUpdate(updateLauncher)
            }
        }

        updateManager = UpdateManager(this)

        enableEdgeToEdge()
        setContent {
            val isAuthenticated by authRepository.isAuthenticated.collectAsState(initial = false)
            val isAdmin by authRepository.isAdmin.collectAsState(initial = false)
            val isDeliveryStaff by authRepository.isDeliveryStaff.collectAsState(initial = false)
            val navController = rememberNavController()
            val destination by destinationFlow.collectAsState()

            var currentUiUid by remember { mutableStateOf<String?>(null) }
            // To prevent multiple navigations or loops
            var hasRedirected by remember { mutableStateOf(false) }

            LaunchedEffect(destination, isAdmin, isAuthenticated) {
                if (destination == "admin_orders" && isAuthenticated && isAdmin) {
                    navController.navigate(Screen.AdminOrders.route)
                    destinationFlow.value = null
                }
            }

            LaunchedEffect(isAuthenticated, isAdmin, isDeliveryStaff) {
                if (isAuthenticated && !hasRedirected) {
                    if (isAdmin) {
                        navController.navigate(Screen.AdminOrders.route) {
                            popUpTo(Screen.RoleSelection.route) { inclusive = true }
                        }
                        hasRedirected = true
                    } else if (isDeliveryStaff) {
                         navController.navigate(Screen.DeliveryOrders.route) {
                            popUpTo(Screen.RoleSelection.route) { inclusive = true }
                        }
                        hasRedirected = true
                    }
                }
            }

            LaunchedEffect(isAuthenticated) {
                if (isAuthenticated) {
                    try {
                        val user = authRepository.getCurrentUser()
                        if (user != null) {
                            val messaging = com.google.firebase.messaging.FirebaseMessaging.getInstance()

                            // Subscribe to personal staff topic
                            messaging.subscribeToTopic("staff_${user.uid}")
                            currentUiUid = user.uid

                            if (isAdmin) {
                                messaging.subscribeToTopic("admin_notifications")
                            } else {
                                messaging.unsubscribeFromTopic("admin_notifications")
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    val messaging = com.google.firebase.messaging.FirebaseMessaging.getInstance()
                    currentUiUid?.let { uid ->
                        messaging.unsubscribeFromTopic("staff_$uid")
                    }
                    messaging.unsubscribeFromTopic("admin_notifications")
                    currentUiUid = null
                }
            }

            MenuAppTheme {
                AppNavigation(
                    startDestination = Screen.Welcome.route,
                    navController = navController,
                    isAdmin = isAdmin
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateManager.checkForUpdate(updateLauncher)
    }
}
