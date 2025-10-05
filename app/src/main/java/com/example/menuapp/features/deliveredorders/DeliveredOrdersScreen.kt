package com.example.menuapp.features.deliveredorders

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.menuapp.data.firebase.model.Order
import com.example.menuapp.utils.formatTimestamp

@Composable
fun DeliveredOrdersScreen(
    onBackPressed: () -> Unit,
    onOrderClicked: (Order) -> Unit,
    orders: List<Order>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(orders, key = { it.id }) { order ->
            DeliveredOrderCard(
                order = order,
                onOrderClicked = { onOrderClicked(order) }
            )
        }
    }
}

@Composable
fun DeliveredOrderCard(
    order: Order,
    onOrderClicked: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOrderClicked() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Order #${order.id.take(6)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Delivered on:")
                Text(text = formatTimestamp(order.deliveryDate ?: order.orderDate))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Total:")
                Text(text = String.format("₹%.2f", order.grandTotal), fontWeight = FontWeight.Bold)
            }
        }
    }
}