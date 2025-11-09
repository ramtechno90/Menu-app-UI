package com.example.menuapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.example.menuapp.data.auth.AuthRepository
import com.example.menuapp.features.auth.AuthState
import com.example.menuapp.features.auth.AuthViewModel
import com.example.menuapp.navigation.AppNavigation
import com.example.menuapp.navigation.Screen
import com.example.menuapp.ui.theme.MenuAppTheme
import com.example.menuapp.utils.UpdateManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository
    private lateinit var updateManager: UpdateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        updateManager = UpdateManager(this)

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
        updateManager.checkForUpdate(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == UpdateManager.UPDATE_REQUEST_CODE) {
            if (resultCode != RESULT_OK) {
                updateManager.checkForUpdate(this)
            }
        }
    }
}
