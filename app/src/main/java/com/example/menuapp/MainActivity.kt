package com.example.menuapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    private val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(this, gso)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var isDarkTheme by remember { mutableStateOf(false) }
            val isAuthenticated by authRepository.isAuthenticated.collectAsState(initial = false)
            val viewModel: AuthViewModel = hiltViewModel()
            val navController = rememberNavController()

            val googleSignInLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    if (account != null) {
                        viewModel.signInWithGoogle(account)
                    }
                } catch (e: ApiException) {
                    // Handle error
                }
            }

            LaunchedEffect(viewModel.authState) {
                viewModel.authState.collect { state ->
                    if (state is AuthState.OtpSent) {
                        navController.navigate(Screen.OtpVerification.createRoute(state.verificationId))
                    }
                }
            }

            MenuAppTheme(darkTheme = isDarkTheme) {
                AppNavigation(
                    isDarkTheme = isDarkTheme,
                    onThemeToggle = { isDarkTheme = !isDarkTheme },
                    startDestination = if (isAuthenticated) Screen.Main.route else Screen.RoleSelection.route,
                    onGoogleSignInClicked = { googleSignInLauncher.launch(googleSignInClient.signInIntent) },
                    onSendOtpClicked = { phoneNumber -> viewModel.sendOtp(phoneNumber, this) },
                    onVerifyOtpClicked = { verificationId, otp -> viewModel.verifyOtp(verificationId, otp) },
                    navController = navController
                )
            }
        }
    }
}
