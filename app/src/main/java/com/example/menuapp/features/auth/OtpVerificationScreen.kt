package com.example.menuapp.features.auth

import androidx.compose.ui.tooling.preview.Preview
import com.example.menuapp.ui.theme.MenuAppTheme
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
fun OtpVerificationScreen(
    onVerifyOtpClicked: (String) -> Unit,
    welcomeViewModel: WelcomeViewModel = hiltViewModel()
) {
    var otp by remember { mutableStateOf("") }
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
            value = otp,
            onValueChange = { otp = it },
            label = { Text("OTP") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { onVerifyOtpClicked(otp) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Verify OTP")
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Preview(showBackground = true)
@Composable
fun OtpVerificationScreenPreview() {
    MenuAppTheme {
        OtpVerificationScreen(onVerifyOtpClicked = {})
    }
}