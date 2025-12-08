package com.pizzaparadize.menuapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
            val navController = rememberNavController()

            MenuAppTheme {
                AppNavigation(
                    startDestination = if (isAuthenticated) Screen.Main.route else Screen.Welcome.route,
                    navController = navController
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateManager.checkForUpdate(updateLauncher)
    }
}
