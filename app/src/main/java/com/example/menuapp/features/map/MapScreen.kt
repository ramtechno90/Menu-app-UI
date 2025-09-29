package com.example.menuapp.features.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onBackPressed: () -> Unit,
    viewModel: MapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Live Location", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            if (uiState.isLoading && uiState.staffLocation == null) {
                CircularProgressIndicator()
            } else if (uiState.staffLocation != null) {
                LiveMapView(
                    staffLocation = uiState.staffLocation!!,
                    destinationLocation = uiState.destinationLocation,
                    route = uiState.route
                )
            } else {
                Text("Location data not available.")
            }
        }
    }
}

@Composable
private fun LiveMapView(
    staffLocation: LatLng,
    destinationLocation: LatLng?,
    route: List<LatLng>
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(staffLocation, 16f)
    }

    LaunchedEffect(staffLocation, destinationLocation) {
        if (destinationLocation != null) {
            val bounds = LatLngBounds.builder()
                .include(staffLocation)
                .include(destinationLocation)
                .build()
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngBounds(bounds, 150),
                durationMs = 1000
            )
        } else {
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLng(staffLocation)
            )
        }
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    ) {
        Marker(
            state = rememberMarkerState(position = staffLocation),
            title = "Delivery Staff"
        )

        if (destinationLocation != null) {
            Marker(
                state = rememberMarkerState(position = destinationLocation),
                title = "Delivery Address",
            )
        }

        if (route.isNotEmpty()) {
            Polyline(
                points = route,
                color = Color(0xFF4A80F0),
                width = 12f
            )
        }
    }
}