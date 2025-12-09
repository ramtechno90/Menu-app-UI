package com.pizzaparadize.menuapp.features.welcome

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.pizzaparadize.menuapp.R
import com.pizzaparadize.menuapp.features.auth.AuthState
import com.pizzaparadize.menuapp.features.auth.AuthViewModel
import com.pizzaparadize.menuapp.ui.theme.GoldenYellow
import com.pizzaparadize.menuapp.ui.theme.MenuAppTheme
import com.pizzaparadize.menuapp.ui.theme.NewWelcomeBackgroundColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(
    onNameEntered: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var name by remember { mutableStateOf("") }
    var isTermsAccepted by remember { mutableStateOf(false) }
    val authState by viewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            onNameEntered()
        }
    }

    MenuAppTheme(statusBarColor = NewWelcomeBackgroundColor) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NewWelcomeBackgroundColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "App Logo",
                    modifier = Modifier
                        .size(300.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "Welcome to Pizza Paradize",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(32.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { newName ->
                        if (newName.all { it.isLetter() || it.isWhitespace() }) {
                            name = newName
                        }
                    },
                    label = { Text("Enter Your Name") },
                    isError = authState is AuthState.Error,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                        unfocusedTextColor = MaterialTheme.colorScheme.onPrimary,
                        cursorColor = MaterialTheme.colorScheme.onPrimary,
                        focusedBorderColor = MaterialTheme.colorScheme.onPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                        focusedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                    )
                )
                if (authState is AuthState.Error) {
                    Text(
                        text = (authState as AuthState.Error).message,
                        color = MaterialTheme.colorScheme.onError,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isTermsAccepted = !isTermsAccepted },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isTermsAccepted,
                        onCheckedChange = null,
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.onPrimary,
                            uncheckedColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                            checkmarkColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    val checkboxLabel = buildAnnotatedString {
                        append("I accept the ")

                        val termsLink = LinkAnnotation.Url(
                            "https://pizzaparadize.netlify.app/terms.html",
                            styles = androidx.compose.ui.text.TextLinkStyles(
                                style = SpanStyle(
                                    color = GoldenYellow,
                                    textDecoration = TextDecoration.Underline,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        )
                        withLink(termsLink) {
                            append("Terms and Conditions")
                        }

                        append(" and ")

                        val privacyLink = LinkAnnotation.Url(
                            "https://pizzaparadize.netlify.app/privacy.html",
                            styles = androidx.compose.ui.text.TextLinkStyles(
                                style = SpanStyle(
                                    color = GoldenYellow,
                                    textDecoration = TextDecoration.Underline,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        )
                        withLink(privacyLink) {
                            append("Privacy Policy")
                        }
                    }

                    Text(
                        text = checkboxLabel,
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onPrimary),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.saveUsername(name) },
                    enabled = name.isNotBlank() && isTermsAccepted && authState !is AuthState.Loading,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onPrimary,
                        contentColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f),
                        disabledContentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                ) {
                    if (authState is AuthState.Loading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    } else {
                        Text("Start Ordering")
                    }
                }
            }

            val footerText = buildAnnotatedString {
                append("View ")

                val termsLink = LinkAnnotation.Url(
                    "https://pizzaparadize.netlify.app/terms.html",
                    styles = androidx.compose.ui.text.TextLinkStyles(
                        style = SpanStyle(
                            color = GoldenYellow,
                            textDecoration = TextDecoration.Underline,
                            fontWeight = FontWeight.Bold
                        )
                    )
                )
                withLink(termsLink) {
                    append("Terms and Conditions")
                }

                append(" and ")

                val privacyLink = LinkAnnotation.Url(
                    "https://pizzaparadize.netlify.app/privacy.html",
                    styles = androidx.compose.ui.text.TextLinkStyles(
                        style = SpanStyle(
                            color = GoldenYellow,
                            textDecoration = TextDecoration.Underline,
                            fontWeight = FontWeight.Bold
                        )
                    )
                )
                withLink(privacyLink) {
                    append("Privacy Policy")
                }
            }

            Text(
                text = footerText,
                style = MaterialTheme.typography.bodySmall.copy(color = Color.White),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
            )
        }
    }
}
