package com.pizzaparadize.menuapp.features.delivery

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pizzaparadize.menuapp.data.firebase.model.Order
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryOrdersScreen(
    viewModel: DeliveryOrdersViewModel = hiltViewModel(),
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Active", "History")

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {}
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Delivery Dashboard") },
                actions = {
                    IconButton(onClick = {
                        viewModel.logout()
                        onLogout()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, "Logout")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.error != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Error: ${uiState.error}", color = MaterialTheme.colorScheme.error)
                }
            } else {
                val orders = if (selectedTabIndex == 0) uiState.activeOrders else uiState.historyOrders

                if (orders.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(if (selectedTabIndex == 0) "No active orders." else "No delivery history.")
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(orders) { order ->
                            DeliveryOrderCard(
                                order = order,
                                isHistory = selectedTabIndex == 1,
                                onMarkPickedUp = { viewModel.markPickedUp(order.id) },
                                onVerifyOtp = { otp -> viewModel.verifyOtp(order.id, otp) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeliveryOrderCard(
    order: Order,
    isHistory: Boolean,
    onMarkPickedUp: () -> Unit,
    onVerifyOtp: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Order #${order.id.take(5)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = order.customerName,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    StatusChip(status = order.status)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (expanded) "Show Less" else "Details",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Date: ${SimpleDateFormat.getDateTimeInstance().format(Date(order.orderDate))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Phone Link
                    if (!order.customerPhoneNumber.isNullOrEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${order.customerPhoneNumber}"))
                                    context.startActivity(intent)
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Phone, "Phone", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(order.customerPhoneNumber.orEmpty(), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Address Link
                    if (order.deliveryAddress.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val uri = Uri.parse("geo:0,0?q=${Uri.encode(order.deliveryAddress)}")
                                    val intent = Intent(Intent.ACTION_VIEW, uri)
                                    intent.setPackage("com.google.android.apps.maps")
                                    if (intent.resolveActivity(context.packageManager) != null) {
                                        context.startActivity(intent)
                                    } else {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(order.deliveryAddress)}")))
                                    }
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.LocationOn, "Address", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(order.deliveryAddress, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Items", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))

                    order.items.forEach { item ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${item.name} x${item.quantity}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                if (item.notes.isNotEmpty()) {
                                    Text("Note: ${item.notes}", style = MaterialTheme.typography.bodyLarge, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Normal)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                         Text("Total", fontWeight = FontWeight.Bold)
                         Text("₹${String.format("%.2f", order.grandTotal)}", fontWeight = FontWeight.Bold)
                    }

                    if (!isHistory) {
                        Spacer(modifier = Modifier.height(24.dp))
                        if (order.status == "READY_FOR_DELIVERY") {
                            Button(
                                onClick = onMarkPickedUp,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Mark as Picked Up")
                            }
                        } else if (order.status == "PICKED_UP") {
                            if (order.otpVerified) {
                                Text("✓ Verified & Delivered", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                            } else {
                                OtpVerificationSection(onVerify = onVerifyOtp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OtpVerificationSection(onVerify: (String) -> Unit) {
    var otp by remember { mutableStateOf("") }

    Column {
        Text("Enter Delivery OTP", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = otp,
                onValueChange = { if (it.length <= 4) otp = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("4 Digits") }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { onVerify(otp) },
                enabled = otp.length == 4,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Verify")
            }
        }
    }
}

@Composable
fun StatusChip(status: String) {
    val (containerColor, contentColor) = when (status) {
        "READY_FOR_DELIVERY" -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        "PICKED_UP" -> Color(0xFFFFF3E0) to Color(0xFFE65100)
        "DELIVERED" -> Color(0xFFE0F2F1) to Color(0xFF00695C)
        else -> MaterialTheme.colorScheme.surface to MaterialTheme.colorScheme.onSurface
    }

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text(
            text = status.replace("_", " "),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
    }
}
