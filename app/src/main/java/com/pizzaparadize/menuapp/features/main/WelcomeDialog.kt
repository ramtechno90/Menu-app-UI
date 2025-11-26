package com.pizzaparadize.menuapp.features.main

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.pizzaparadize.menuapp.data.model.User

@Composable
fun WelcomeDialog(
    user: User,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Welcome!") },
        text = { Text(text = "Hello, ${user.username ?: "User"}!") },
        confirmButton = {
            TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)) {
                Text("OK")
            }
        }
    )
}