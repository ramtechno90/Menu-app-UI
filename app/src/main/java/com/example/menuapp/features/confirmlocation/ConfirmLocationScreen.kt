package com.example.menuapp.features.confirmlocation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmLocationScreen(
    latitude: Double,
    longitude: Double,
    address: String,
    onConfirmClicked: () -> Unit,
    onBackPressed: () -> Unit
) {
    val location = remember { LatLng(latitude, longitude) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(location, 15f)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            Marker(
                state = MarkerState(position = location),
                title = "Your Location"
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            Text(
                text = "Confirm Your Delivery Address",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = address, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onConfirmClicked,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Confirm Location")
            }
            OutlinedButton(
                onClick = onBackPressed,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Go Back")
            }
        }
    }
}
