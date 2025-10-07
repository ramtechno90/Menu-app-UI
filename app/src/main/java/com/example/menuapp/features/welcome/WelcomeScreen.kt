package com.example.menuapp.features.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RamenDining
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.menuapp.ui.theme.MenuAppTheme

@Composable
fun WelcomeScreen(
    onNavigateToSignIn: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    viewModel: WelcomeViewModel = hiltViewModel()
) {
    val restaurantName by viewModel.restaurantName.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(1f))

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

            Column(
                modifier = Modifier.fillMaxWidth(0.8f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = onNavigateToSignIn,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sign In")
                }
                OutlinedButton(
                    onClick = onNavigateToSignUp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sign Up")
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WelcomeScreenPreview() {
    MenuAppTheme {
        WelcomeScreen({}, {})
    }
}