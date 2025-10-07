package com.example.menuapp.features.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RamenDining
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.menuapp.features.welcome.WelcomeViewModel

@Composable
fun PhoneSignInScreen(
    onSendOtpClicked: (String) -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
    welcomeViewModel: WelcomeViewModel = hiltViewModel()
) {
    var phoneNumber by remember { mutableStateOf("") }
    val authState by authViewModel.authState.collectAsState()
    val restaurantName by welcomeViewModel.restaurantName.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.5f))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.RamenDining,
                    contentDescription = "App Logo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            }
            Text(
                text = restaurantName,
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 24.sp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            label = { Text("Phone Number") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Text("+91") }
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { onSendOtpClicked("+91$phoneNumber") },
            modifier = Modifier.fillMaxWidth(),
            enabled = authState !is AuthState.Loading
        ) {
            Text("Send OTP")
        }
        Spacer(modifier = Modifier.height(16.dp))
        when (val state = authState) {
            is AuthState.Loading -> CircularProgressIndicator()
            is AuthState.Error -> Text(
                text = state.message,
                color = MaterialTheme.colorScheme.error
            )
            else -> {}
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}