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
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
}
