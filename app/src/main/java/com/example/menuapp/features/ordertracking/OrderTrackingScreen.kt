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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.menuapp.data.firebase.model.Order
import com.example.menuapp.ui.theme.MenuAppTheme
import com.example.menuapp.utils.OrderStatusMapper

enum class TrackingStatus {
    PLACED, CONFIRMED, PREPARING, COMPLETED, OUT_FOR_DELIVERY, DELIVERED
}

data class TrackingState(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val status: TrackingStatus
)

val trackingStates = listOf(
    TrackingState(OrderStatusMapper.mapOrderStatus("PENDING"), "Your order has been placed.", Icons.Default.Check, TrackingStatus.PLACED),
    TrackingState(OrderStatusMapper.mapOrderStatus("ACCEPTED"), "Your order has been confirmed.", Icons.Default.Check, TrackingStatus.CONFIRMED),
    TrackingState(OrderStatusMapper.mapOrderStatus("PREPARING"), "We are preparing your order.", Icons.Default.Restaurant, TrackingStatus.PREPARING),
    TrackingState(OrderStatusMapper.mapOrderStatus("COMPLETED"), "Your order is completed.", Icons.Default.Done, TrackingStatus.COMPLETED),
    TrackingState(OrderStatusMapper.mapOrderStatus("OUT_FOR_DELIVERY"), "Estimated delivery: 20 mins", Icons.Default.LocalShipping, TrackingStatus.OUT_FOR_DELIVERY),
    TrackingState(OrderStatusMapper.mapOrderStatus("DELIVERED"), "Your order has been delivered.", Icons.Default.CheckCircle, TrackingStatus.DELIVERED)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderTrackingScreen(
    onBackPressed: () -> Unit,
    onNavigateToMap: (String) -> Unit,
    viewModel: OrderTrackingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.order != null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item { Spacer(modifier = Modifier.height(0.dp)) }
                item { OrderSummaryCard(uiState.order!!) }
                if (uiState.order != null) {
                    item { OtpCard(order = uiState.order!!) }
                }
                item { TrackingTimeline(order = uiState.order!!) }
                item {
                    val orderId = uiState.order?.id
                    if (!orderId.isNullOrBlank()) {
                        Button(
                            onClick = { onNavigateToMap(orderId) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = "Map Icon",
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("View Live Map")
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Order not found.")
            }
        }
    }
}

@Composable
private fun OrderSummaryCard(order: Order) {
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
                    Text("ID: #${order.id.take(8)}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Text(String.format("₹%.2f", order.grandTotal), fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(top = 4.dp))
                }
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(order.items.firstOrNull()?.imageUrl ?: "")
                            .crossfade(true)
                            .build(),
                        contentDescription = "Order Image",
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

@Composable
private fun OtpCard(order: Order) {
    val green = Color(0xFF4CAF50)

    val surfaceColor = when {
        order.otpVerified -> green.copy(alpha = 0.1f)
        order.otpInvalid -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = surfaceColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val titleColor = when {
                order.otpVerified -> green
                order.otpInvalid -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.primary
            }

            Text(
                text = "Delivery OTP",
                color = titleColor,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (order.otpVerified) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Verified",
                        tint = green,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "OTP Verified",
                        color = green,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                }
                Text(
                    text = "Your order is successfully delivered.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else {
                // Always show OTP if not yet verified
                Text(
                    text = order.otp?.chunked(1)?.joinToString(" ") ?: "----",
                    color = if (order.otpInvalid) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 40.sp,
                    letterSpacing = 8.sp,
                    modifier = Modifier
                        .border(
                            1.dp,
                            (if (order.otpInvalid) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary).copy(
                                alpha = 0.3f
                            ),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Show invalid message or helper text
                if (order.otpInvalid) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = "Invalid",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Invalid OTP. Please try again.",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    Text(
                        text = "Show this to the delivery person at the door.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackingTimeline(order: Order) {
    val currentStatus = order.status.toTrackingStatus()

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
    val green = Color(0xFF4CAF50)

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
                        if (isCurrent) green else MaterialTheme.colorScheme.surface
                    )
                    .then(
                        when {
                            isCurrent -> Modifier
                            isActive -> Modifier.border(2.dp, green, CircleShape)
                            else -> Modifier.border(2.dp, Color.LightGray, CircleShape)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = state.icon,
                    contentDescription = state.title,
                    tint = when {
                        isCurrent -> Color.White
                        isActive -> green
                        else -> Color.Gray
                    },
                    modifier = Modifier.size(24.dp)
                )
            }
            if (!isLast) {
                val lineColor = if (isActive) green else Color.LightGray.copy(alpha = 0.5f)
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
                color = when {
                    isCurrent -> green
                    isActive -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
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


private fun String?.toTrackingStatus(): TrackingStatus = when (this) {
    "PENDING" -> TrackingStatus.PLACED
    "ACCEPTED" -> TrackingStatus.CONFIRMED
    "PREPARING" -> TrackingStatus.PREPARING
    "COMPLETED", "READY_FOR_DELIVERY" -> TrackingStatus.COMPLETED
    "OUT_FOR_DELIVERY", "PICKED_UP" -> TrackingStatus.OUT_FOR_DELIVERY
    "DELIVERED" -> TrackingStatus.DELIVERED
    else -> TrackingStatus.PLACED
}

@Preview(showBackground = true, name = "Light Mode")
@Composable
fun OrderTrackingScreenPreview() {
    MenuAppTheme(darkTheme = false) {
        OrderTrackingScreen(onBackPressed = {}, onNavigateToMap = {})
    }
}