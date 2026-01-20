package com.pizzaparadize.menuapp.features.admin

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pizzaparadize.menuapp.data.firebase.model.DeliveryStaff
import com.pizzaparadize.menuapp.data.firebase.model.Order
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminOrdersScreen(
    viewModel: AdminOrdersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            // Handle permission result if needed
        }
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Admin Orders") }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.orders.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No orders found.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(uiState.orders) { order ->
                    AdminOrderCard(
                        order = order,
                        deliveryStaffList = uiState.deliveryStaffList,
                        onStatusUpdate = { status -> viewModel.updateOrderStatus(order.id, status) },
                        onAssignStaff = { uid, name -> viewModel.assignOrder(order.id, name, uid) }
                    )
                }
            }
        }
    }
}

@Composable
fun AdminOrderCard(
    order: Order,
    deliveryStaffList: List<DeliveryStaff>,
    onStatusUpdate: (String) -> Unit,
    onAssignStaff: (String, String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Order #${order.id.take(5)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text("Customer: ${order.customerName}")
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = order.status,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                         text = if (expanded) "Collapse ▲" else "Expand ▼",
                         fontSize = 12.sp,
                         color = Color.Gray
                    )
                }
            }

            // Expanded Details
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Divider()
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Date: ${SimpleDateFormat.getDateTimeInstance().format(Date(order.orderDate))}")

                    if (!order.customerPhoneNumber.isNullOrEmpty()) {
                        Text(
                            text = "Phone: ${order.customerPhoneNumber}",
                            modifier = Modifier.clickable {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${order.customerPhoneNumber}"))
                                context.startActivity(intent)
                            },
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (order.deliveryAddress.isNotEmpty()) {
                         Text(
                            text = "Address: ${order.deliveryAddress}",
                            modifier = Modifier.clickable {
                                val uri = Uri.parse("geo:0,0?q=${Uri.encode(order.deliveryAddress)}")
                                val intent = Intent(Intent.ACTION_VIEW, uri)
                                intent.setPackage("com.google.android.apps.maps")
                                if (intent.resolveActivity(context.packageManager) != null) {
                                    context.startActivity(intent)
                                } else {
                                    // Fallback to browser if maps app not installed
                                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(order.deliveryAddress)}"))
                                    context.startActivity(browserIntent)
                                }
                            },
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Items:", fontWeight = FontWeight.Bold)
                    order.items.forEach { item ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${item.name} x${item.quantity}")
                                if (item.notes.isNotEmpty()) {
                                    Text("Note: ${item.notes}", style = LocalTextStyle.current.copy(fontStyle = FontStyle.Italic, fontSize = 12.sp))
                                }
                            }
                            Text("$${String.format("%.2f", item.price * item.quantity)}")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Subtotal")
                        Text("$${String.format("%.2f", order.subtotal)}")
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Tax")
                        Text("$${String.format("%.2f", order.tax)}")
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Delivery Fee")
                        Text("$${String.format("%.2f", order.deliveryFee)}")
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total", fontWeight = FontWeight.Bold)
                        Text("$${String.format("%.2f", order.grandTotal)}", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Actions:", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Status Dropdown
                    StatusDropdown(currentStatus = order.status, onStatusChange = onStatusUpdate)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Assign Staff Dropdown
                    AssignStaffDropdown(
                        currentStaffUid = order.assignedToUid,
                        staffList = deliveryStaffList,
                        onAssign = onAssignStaff
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusDropdown(currentStatus: String, onStatusChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val statuses = listOf("PENDING", "PREPARING", "READY_FOR_DELIVERY", "REJECTED")
    // If current status is not in the list (e.g. COMPLETED), we still show it as selected but maybe disable changing back?
    // Admin request: "able to change order status".

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = currentStatus,
            onValueChange = {},
            readOnly = true,
            label = { Text("Status") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            statuses.forEach { status ->
                DropdownMenuItem(
                    text = { Text(status) },
                    onClick = {
                        onStatusChange(status)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignStaffDropdown(
    currentStaffUid: String?,
    staffList: List<DeliveryStaff>,
    onAssign: (String, String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val currentStaffName = staffList.find { it.uid == currentStaffUid }?.name ?: "Unassigned"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = currentStaffName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Assign to") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Unassigned") },
                onClick = {
                    onAssign("", "Unassigned") // Assuming empty string means unassign
                    expanded = false
                }
            )
            staffList.forEach { staff ->
                DropdownMenuItem(
                    text = { Text(staff.name) },
                    onClick = {
                        onAssign(staff.uid, staff.name)
                        expanded = false
                    }
                )
            }
        }
    }
}
