package com.example.menuapp.features.ordertracking

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.menuapp.ui.theme.MenuAppTheme

enum class TrackingStatus {
    CONFIRMED, PREPARING, OUT_FOR_DELIVERY, DELIVERED
}

data class TrackingState(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val status: TrackingStatus
)

val trackingStates = listOf(
    TrackingState("Order Confirmed", "Your order has been confirmed.", Icons.Default.Check, TrackingStatus.CONFIRMED),
    TrackingState("Preparing Food", "We are preparing your order.", Icons.Default.Restaurant, TrackingStatus.PREPARING),
    TrackingState("Out for Delivery", "Estimated delivery: 20 mins", Icons.Default.LocalShipping, TrackingStatus.OUT_FOR_DELIVERY),
    TrackingState("Delivered", "Awaiting delivery completion.", Icons.Default.Done, TrackingStatus.DELIVERED)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderTrackingScreen(onBackPressed: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Track Your Order", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item { Spacer(modifier = Modifier.height(0.dp)) }
            item { OrderSummaryCard() }
            item { OtpCard() }
            item { TrackingTimeline(currentStatus = TrackingStatus.OUT_FOR_DELIVERY) }
            item { MapImage() }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun OrderSummaryCard() {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text("Order Summary", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("ID: #1234567890", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Text("$25.50", fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(top = 4.dp))
                }
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.LightGray.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Receipt, contentDescription = "Order Receipt", tint = Color.Gray)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { /* TODO: View More Action */ },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("View More", fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun OtpCard() {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Your One-Time Password (OTP)", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
            Text(
                "5 8 3 1",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 40.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            TextButton(onClick = { /* TODO: Resend OTP Action */ }) {
                Text("Resend OTP", color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
            }
        }
    }
}

@Composable
private fun TrackingTimeline(currentStatus: TrackingStatus) {
    Column {
        trackingStates.forEachIndexed { index, state ->
            val isActive = state.status.ordinal <= currentStatus.ordinal
            val isCurrent = state.status == currentStatus
            val isLast = index == trackingStates.lastIndex

            TimelineNode(
                state = state,
                isActive = isActive,
                isCurrent = isCurrent,
                isLast = isLast
            )
        }
    }
}

@Composable
private fun TimelineNode(state: TrackingState, isActive: Boolean, isCurrent: Boolean, isLast: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isActive) {
                            if (isCurrent) MaterialTheme.colorScheme.primary else Color(0xFF4CAF50)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    )
                    .then(
                        if (!isActive) Modifier.border(2.dp, Color.LightGray, CircleShape) else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = state.icon,
                    contentDescription = state.title,
                    tint = if (isActive) Color.White else Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            }
            if (!isLast) {
                val lineColor = if (isActive) Color(0xFF4CAF50) else Color.LightGray.copy(alpha = 0.5f)
                Canvas(Modifier.width(2.dp).weight(1f)) {
                    drawLine(
                        color = lineColor,
                        start = Offset(size.width / 2, 0f),
                        end = Offset(size.width / 2, size.height),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.padding(top = 8.dp, bottom = if (isLast) 0.dp else 24.dp)) {
            Text(
                text = state.title,
                fontWeight = FontWeight.Bold,
                color = if (isActive) {
                    if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                }
            )
            Text(
                text = state.subtitle,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}


@Composable
private fun MapImage() {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data("https://lh3.googleusercontent.com/aida-public/AB6AXuAnUvcdMCYl-yuZ9Y0Kqaw4k5_ZRX2PLTmkVHI_pyP-jd4UpXpOj7RJyUViRy69VndEx-pt04URrsoAYjftAkzchrSUq2QHiKiABjtMQV9p42hm9zfbp1xINcdpswQvvD9SR3MeldvQbgbIOY6I94-nDI4T_tOFJZpNWWNVRFN3IucvmT4vw8Rx4pkh0mgnwOLlM0yzGos8TsQLLCZVGuZgOrqEYR-DNaEuKHWBcu5PntF6_Gqdb27MeyaCyWVDoRb_Z4_CDOQBUMva")
            .crossfade(true)
            .build(),
        contentDescription = "Map showing delivery route",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16 / 9f)
            .clip(RoundedCornerShape(12.dp))
    )
}

@Preview(showBackground = true, name = "Light Mode")
@Composable
fun OrderTrackingScreenPreview() {
    MenuAppTheme(darkTheme = false) {
        OrderTrackingScreen(onBackPressed = {})
    }
}

@Preview(showBackground = true, name = "Dark Mode")
@Composable
fun OrderTrackingScreenDarkPreview() {
    MenuAppTheme(darkTheme = true) {
        OrderTrackingScreen(onBackPressed = {})
    }
}
