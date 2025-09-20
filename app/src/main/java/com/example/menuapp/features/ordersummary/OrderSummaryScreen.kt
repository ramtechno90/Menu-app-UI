package com.example.menuapp.features.ordersummary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.menuapp.ui.theme.MenuAppTheme

data class OrderItem(
    val id: Int,
    val name: String,
    val quantity: Int,
    val price: Double,
    val tax: Double,
    val imageUrl: String
)

val sampleOrderItems = listOf(
    OrderItem(1, "Spicy Chicken Sandwich", 1, 9.99, 0.80, "https://lh3.googleusercontent.com/aida-public/AB6AXuDNOwK6xhYL2iCZB-3pT8RQOT_6KmNsGLCaBfImN2SUl2AnfjSjjgcUA03gEyq0u5WydwwOgPLXVRr0-QXy0CTrSIsG1wnZ7ZvokHtS38FCR2nZKCPDxU8zYdbfyDnNxv39_J4cov9jeA1DnvEbAYrWuLpVqp0z3uBThJaEdETy1lHTwDkPVUYrfQIvUtfQevu6y2yAkgIGYH2ptgbgjSclJw22E_uxkTARLojlgj_bNgHCez9GlC32rOYMaJ72SENsc-4k8MNmym6o"),
    OrderItem(2, "Fries", 1, 3.99, 0.32, "https://lh3.googleusercontent.com/aida-public/AB6AXuAa2GOcZ8ZWt6sFbKBF0cMqs_WF9lyLkPINHAMQiErWM5fXaIxJQXo-SKs_AUdlVZd2qGAoRdkQKpaNMi2NTSxw_xT-v5knjUsI0ETq7EFZoOkJMp-VYtePF17zner49MyGB0GKNzChsAx9s7Nca2gMcCrbwePNa42Yz22lm4ppVOUsy58zrfgTmeKRB27ePTGnnyEKkcn_jV0vXzkZMaxwby_mrhNXjaNWC9W9pwTgEmsNsuo5yodld5uqeStbW1zH0Olm3xHw56RS"),
    OrderItem(3, "Coke", 1, 1.99, 0.16, "https://lh3.googleusercontent.com/aida-public/AB6AXuCi7Z_ZllEbLoCEXVAsbB_oyomT8_e5GbGeUs7aXBNHwIGJn5Ss_7Am1XU6sjcJmmuEanaM8urL4vMSu_ojiIAhuDJMoR-SbKUOv_rEOT_L_icJhyWOlU1PZ0DwDXVVM_lxQdMhKTug50gGR5bmAf5-RHWJf4BbgW7bvEjx1Umnqkia96Jg7wsS532jlVVv4OMaO32Z24PgdEmND8b446FRDjff_0kg2Ku-BbBeLTCIWpjlcUmXH3jGPEs3Vu7qcVyaSyXSYA9_LQj_")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderSummaryScreen(onBackPressed: () -> Unit, onTrackOrderClicked: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Order Summary", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            OrderActionsFooter(onTrackOrderClicked = onTrackOrderClicked)
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { OrderStatusHeader() }
            item {
                Text(
                    "Items in Your Order",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
            items(sampleOrderItems) { item ->
                OrderItemCard(item = item)
            }
            item {
                val subtotal = sampleOrderItems.sumOf { it.price }
                val taxes = sampleOrderItems.sumOf { it.tax }
                SummaryCard(subtotal = subtotal, taxes = taxes)
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun OrderStatusHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("Order #123456789", fontSize = 14.sp)
            Text("Out for Delivery", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
        Icon(
            imageVector = Icons.Default.LocalShipping,
            contentDescription = "Delivery Status",
            tint = Color(0xFF2E7D32), // Green color for shipping
            modifier = Modifier.size(40.dp)
        )
    }
}

@Composable
private fun OrderItemCard(item: OrderItem) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(item.imageUrl).crossfade(true).build(),
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.name, fontWeight = FontWeight.Bold)
                    Text("Quantity: ${item.quantity}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    Text(String.format("$%.2f", item.price), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                }
            }
            Divider(modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Tax (8%)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Text(String.format("$%.2f", item.tax), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
            }
        }
    }
}

@Composable
private fun SummaryCard(subtotal: Double, taxes: Double) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Subtotal", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                Text(String.format("$%.2f", subtotal), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Taxes", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                Text(String.format("$%.2f", taxes), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
            }
        }
    }
}


@Composable
private fun OrderActionsFooter(onTrackOrderClicked: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onTrackOrderClicked,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Track Order", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Button(
            onClick = { /* TODO: Implement contact support */ },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text("Contact Support", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Preview(showBackground = true, name = "Light Mode")
@Composable
fun OrderSummaryScreenPreview() {
    MenuAppTheme(darkTheme = false) {
        OrderSummaryScreen(onBackPressed = {}, onTrackOrderClicked = {})
    }
}

@Preview(showBackground = true, name = "Dark Mode")
@Composable
fun OrderSummaryScreenDarkPreview() {
    MenuAppTheme(darkTheme = true) {
        OrderSummaryScreen(onBackPressed = {}, onTrackOrderClicked = {})
    }
}
