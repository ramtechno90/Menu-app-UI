package com.pizzaparadize.menuapp.features.common

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AppFooter(
    contactNumber: String
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

        Text(
            text = "Contact Restaurant",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.clickable {
                if (contactNumber.isNotBlank()) {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$contactNumber"))
                    context.startActivity(intent)
                }
            }
        )

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            FooterLink(text = "Terms & Conditions") {
                uriHandler.openUri("https://pizzaparadize.netlify.app/privacy/terms.html")
            }
            Text(" | ", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            FooterLink(text = "Privacy Policy") {
                uriHandler.openUri("https://pizzaparadize.netlify.app/privacy/privacy.html")
            }
            Text(" | ", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            FooterLink(text = "Data Deletion") {
                uriHandler.openUri("https://pizzaparadize.netlify.app/data_deletion")
            }
        }

        // Extra spacing at bottom for edge-to-edge
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun FooterLink(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall.copy(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            fontSize = 10.sp
        ),
        modifier = Modifier.clickable(onClick = onClick),
        textAlign = TextAlign.Center
    )
}
