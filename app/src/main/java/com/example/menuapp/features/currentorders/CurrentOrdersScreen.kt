package com.example.menuapp.features.currentorders

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.menuapp.ui.theme.MenuAppTheme

data class CurrentOrder(
    val id: String,
    val restaurantName: String,
    val restaurantImageUrl: String,
    val status: String,
    val itemCount: Int
)

val sampleCurrentOrders = listOf(
    CurrentOrder("1234567890", "The Spice Merchant", "https://lh3.googleusercontent.com/aida-public/AB6AXuBZSX88sGc05dmgM_G9i4tu5kVvY_LpvCcuBOW0dq7DaTbtIcoxazDturElmSkbHex_YDWJIWK8VJedOWuTN3P8WlZDbsC28KIPDV_kqonXLt7d3WiDV1SlTa85MW2yl4751W3YJ5tF7HAjCtGRJKQ2GqL4Wtgsdot07AWE6xC6ncBf99GvaZls-cWtHmLh_a6f25Fi6XjpTLPqBSyQk29qX2YqAmYE4q42NDfY_nQXsCRm0OKPWswFMwzRp1AQ1MkwkLjHSTdrQQte", "Preparing Food", 2),
    CurrentOrder("9876543210", "Pizza Palace", "https://lh3.googleusercontent.com/aida-public/AB6AXuAePo43pzR3fkZzQoGwZnfS61VyXkMFzBzBc3_s3Efk223LtBTwzOivozQV8ExvUzOk3DgSxlffs0TfQOISN6_Gaq5cMEdMHS0fqdCif_kOCuRoP-TTe9yYYLTL0kTgljRskdzoLwhh5Q4C2LUmrnje38oaQpw9fxKuXlieZSGlU3xMv9-OJmmIpiC5OMyqdSsDlq237NsJkVkeOJ63LUYhE9f0hltdq8VppRCWnZQ-8XiAFaPBNS_zKX19hJv4-OI3cd-ir_15N7Ey", "Out for Delivery", 3),
    CurrentOrder("5555555555", "Sushi Central", "https://lh3.googleusercontent.com/aida-public/AB6AXuBpLaK7erYgHJFe_kJTYL--W2oqqqWwOCA6w6UlB9-p7i4_Dun0Y_-Il3cKW-amZ0IdA4ZPVbVcMjLt3pWs1iF-ZLFgE2npyfexyqMdYoTH4a-FPQWHXqxZybqE3d39ngixqdmR6Uj0p7mJs_P2Y3kzw7xJg4kb4RNYsxQP9dED2MjxKb9MSmpL5E6XE4fCeHNbPJokrai9nZakOwZfwucp6lxHdLFrWChKzr8xegnJnwzaXzVsRS5kLB_8XuCcfv6FSC3BHgWjluvY", "Awaiting Pickup", 1)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrentOrdersScreen(
    onBackPressed: () -> Unit,
    onOrderClicked: (CurrentOrder) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Current Orders", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.8f)
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(sampleCurrentOrders) { order ->
                CurrentOrderCard(order = order, onClick = { onOrderClicked(order) })
            }
        }
    }
}

@Composable
private fun CurrentOrderCard(order: CurrentOrder, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(order.restaurantImageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = order.restaurantName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(order.restaurantName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    "Order ID: #${order.id}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = order.status,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(50))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    Text(
                        "·",
                        modifier = Modifier.padding(horizontal = 8.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        "${order.itemCount} items",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View Order",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Preview(showBackground = true, name = "Light Mode")
@Composable
fun CurrentOrdersScreenPreview() {
    MenuAppTheme(darkTheme = false) {
        CurrentOrdersScreen(onBackPressed = {}, onOrderClicked = {})
    }
}

@Preview(showBackground = true, name = "Dark Mode")
@Composable
fun CurrentOrdersScreenDarkPreview() {
    MenuAppTheme(darkTheme = true) {
        CurrentOrdersScreen(onBackPressed = {}, onOrderClicked = {})
    }
}
