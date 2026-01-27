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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
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
    viewModel: AdminOrdersViewModel = hiltViewModel(),
    onLogout: () -> Unit = {},
    onMenuManagementClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { _ ->
            // Handle permission result if needed
        }
    )

    // State for filtering
    var selectedStatus by remember { mutableStateOf("PENDING") }

    // Hardcoded important statuses to ensure they appear
    val importantStatuses = listOf("PENDING", "PREPARING", "READY_FOR_DELIVERY", "OUT_FOR_DELIVERY", "DELIVERED", "REJECTED")

    // Dynamic statuses from orders + important ones
    val allStatuses = remember(uiState.orders) {
        (importantStatuses + uiState.orders.map { it.status }).distinct()
    }

    val filteredOrders = remember(uiState.orders, selectedStatus) {
        if (selectedStatus == "All") {
            uiState.orders
        } else {
            uiState.orders.filter { it.status == selectedStatus }
        }
    }

    // Confirmation dialog for "Delete All"
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("Delete All Orders?") },
            text = { Text("Are you sure you want to delete all orders in '$selectedStatus' status? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteOrdersByStatus(selectedStatus)
                        showDeleteAllDialog = false
                    }
                ) {
                    Text("Delete All", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Admin Orders") },
                navigationIcon = {
                    IconButton(onClick = onMenuManagementClick) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu Management"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.logout()
                        onLogout()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Logout"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filter Row
            if (!uiState.isLoading) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allStatuses) { status ->
                        val count = uiState.orders.count { it.status == status }
                        FilterChip(
                            selected = selectedStatus == status,
                            onClick = { selectedStatus = status },
                            label = { Text("${status.replace("_", " ")} ($count)") }
                        )
                    }
                }

                // Show "Clear All" button for Delivered/Rejected
                if (selectedStatus == "DELIVERED" || selectedStatus == "REJECTED") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { showDeleteAllDialog = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear All")
                        }
                    }
                }

                HorizontalDivider()
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filteredOrders.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No orders found.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredOrders) { order ->
                        AdminOrderCard(
                            order = order,
                            deliveryStaffList = uiState.deliveryStaffList,
                            onStatusUpdate = { status -> viewModel.updateOrderStatus(order.id, status) },
                            onAssignStaff = { uid, name -> viewModel.assignOrder(order.id, name, uid) },
                            onDelete = { viewModel.deleteOrder(order.id) }
                        )
                    }
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
    onAssignStaff: (String, String) -> Unit,
    onDelete: () -> Unit
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
            // Header
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

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (order.status == "DELIVERED" || order.status == "REJECTED") {
                             IconButton(
                                onClick = onDelete,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        Text(
                             text = if (expanded) "Show Less" else "Details",
                             style = MaterialTheme.typography.labelLarge,
                             color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Expanded Details
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Date: ${SimpleDateFormat.getDateTimeInstance().format(Date(order.orderDate))}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (!order.customerPhoneNumber.isNullOrEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val intent = Intent(
                                        Intent.ACTION_DIAL,
                                        Uri.parse("tel:${order.customerPhoneNumber}")
                                    )
                                    context.startActivity(intent)
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Phone",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = order.customerPhoneNumber.orEmpty(),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (order.deliveryAddress.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val uri =
                                        Uri.parse("geo:0,0?q=${Uri.encode(order.deliveryAddress)}")
                                    val intent = Intent(Intent.ACTION_VIEW, uri)
                                    intent.setPackage("com.google.android.apps.maps")
                                    if (intent.resolveActivity(context.packageManager) != null) {
                                        context.startActivity(intent)
                                    } else {
                                        val browserIntent = Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(order.deliveryAddress)}")
                                        )
                                        context.startActivity(browserIntent)
                                    }
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Address",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = order.deliveryAddress,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Text("Items", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))

                    order.items.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${item.name} x${item.quantity}",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                if (item.notes.isNotEmpty()) {
                                    Text(
                                        text = "Note: ${item.notes}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontStyle = FontStyle.Italic,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = "₹${String.format("%.2f", item.price * item.quantity)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(8.dp))

                    PriceRow(label = "Subtotal", amount = order.subtotal)
                    PriceRow(label = "Delivery Fee", amount = order.deliveryFee)

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = "₹${String.format("%.2f", order.grandTotal)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Actions
                    Text("Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(12.dp))

                    StatusDropdown(currentStatus = order.status, onStatusChange = onStatusUpdate)
                    Spacer(modifier = Modifier.height(12.dp))

                    if (order.status == "READY_FOR_DELIVERY") {
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
}

@Composable
fun PriceRow(label: String, amount: Double) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("₹${String.format("%.2f", amount)}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun StatusChip(status: String) {
    val (containerColor, contentColor) = when (status) {
        "PENDING" -> Color(0xFFFFF3E0) to Color(0xFFE65100) // Orange
        "PREPARING" -> Color(0xFFE3F2FD) to Color(0xFF1565C0) // Blue
        "READY_FOR_DELIVERY" -> Color(0xFFE8F5E9) to Color(0xFF2E7D32) // Green
        "REJECTED" -> Color(0xFFFFEBEE) to Color(0xFFC62828) // Red
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
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusDropdown(currentStatus: String, onStatusChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val statuses = listOf("PENDING", "PREPARING", "READY_FOR_DELIVERY", "REJECTED")
    val isEditable = currentStatus in statuses

    ExposedDropdownMenuBox(
        expanded = expanded && isEditable,
        onExpandedChange = { if (isEditable) expanded = !expanded }
    ) {
        OutlinedTextField(
            value = currentStatus.replace("_", " "),
            onValueChange = {},
            readOnly = true,
            enabled = isEditable,
            label = { Text("Update Status") },
            trailingIcon = { if (isEditable) ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            statuses.forEach { status ->
                DropdownMenuItem(
                    text = { Text(status.replace("_", " ")) },
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
            label = { Text("Assign Staff") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Unassigned") },
                onClick = {
                    onAssign("", "Unassigned")
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
