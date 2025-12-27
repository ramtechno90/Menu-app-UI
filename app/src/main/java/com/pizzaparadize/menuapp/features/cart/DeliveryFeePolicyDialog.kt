package com.pizzaparadize.menuapp.features.cart

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pizzaparadize.menuapp.data.model.DeliveryFeeSettings
import java.text.DecimalFormat

@Composable
fun DeliveryFeePolicyDialog(
    deliveryFeeSettings: DeliveryFeeSettings,
    onDismiss: () -> Unit
) {
    val decimalFormat = DecimalFormat("0.##")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delivery Fee Policy") },
        text = {
            Column {
                Text(
                    text = "How we calculate your delivery fee",
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "The delivery fee is calculated based on the driving distance from our restaurant to your delivery address, as determined by Google Maps."
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Fee Structure:",
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Base Zone: For distances up to ${decimalFormat.format(deliveryFeeSettings.minDistanceKm)} km, a flat fee of \u20B9${decimalFormat.format(deliveryFeeSettings.minDistanceRate)} applies."
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Extended Zone: For distances beyond ${decimalFormat.format(deliveryFeeSettings.minDistanceKm)} km, an additional charge of \u20B9${decimalFormat.format(deliveryFeeSettings.additionalRatePerKm)} per km is applied."
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Note: The final fee is calculated automatically at checkout based on your precise location."
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
