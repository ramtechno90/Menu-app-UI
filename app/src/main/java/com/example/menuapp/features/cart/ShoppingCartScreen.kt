package com.example.menuapp.features.cart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.menuapp.ui.theme.MenuAppTheme

data class CartItem(
    val id: Int,
    val name: String,
    val price: Double,
    val tax: Double,
    val quantity: Int,
    val imageUrl: String
)

val sampleCartItems = listOf(
    CartItem(1, "Spicy Chicken Sandwich", 975.00, 78.00, 1, "https://lh3.googleusercontent.com/aida-public/AB6AXuCQjvvQ3dXf1Sal3Q5M-WUGGNr9lSlAvumw6Sy1VFahGhso6GExGFVnUiat3_Vvn3q0sHlVa3a0gdpay6jCDzn5IXidio11yZD79R9j73orkkm_LVHwLfPScuV8rikNHeN0A85O5CotkAMDxWCnDLOXG6T3odTWcppX_iZHhmoJBegi3SQuUvkqcG36JyHsfvuY4O8KtGShHFmgpis0ta9Djc8Vghp738lDGynfPphwouFE39K_OHSDX1Bb5TTgIb9PyqHUSsNlcqYQ"),
    CartItem(2, "Crispy Fries", 750.00, 60.00, 1, "https://lh3.googleusercontent.com/aida-public/AB6AXuCkPAIjcGUTRH-pFXoNfZMQYoojsrslKramwzugiboI9udughxOFgDFCsen1GJWpDv4t44scnYsIpmGlW2VQcZ0rxIaahLP7rzeWJYEbZPuP2jf7c2zx-uyqWDogk_W8U3hKJ2GvJ9mIsdntsdGmz226mj58VgdObuScO3BzjCHq0zYpt0rehiLUZIB1iL8ZuPLe2ZGLUnyMHDLxERZ8qdqcR7J6QEVsYEUWPeZ7vcXyyAsLL-q_dKiBAAvcAox0jrl0FHiW4zXsni6"),
    CartItem(3, "Classic Burger", 450.00, 36.00, 1, "https://lh3.googleusercontent.com/aida-public/AB6AXuC3B-gXyLJl0rOyRdvreCivOGJ1nInHXZUOeDdDKoYfThqE6108sQzIIryji43a_4IU_0OlwG2pBBlfkyUXU9ryh118t7POc3vJ8pfIBKLS4LDB8_JfDk9Fkalao71qtwrEdiyRvHuxgjql_aTRH2nEvT61kXcw0t1XrE4dHuYrtpt94K10wrF9OdsLnmHeQJmKymgVDcwobCWI7k-iMlkjTvqL7kSmihlAwTSDtwaVfKyvzmJqZyKQK47H9rZPjkWRQJPi30nPL7yc")
)

@OptIn(ExperimentalMaterial3ai::class)
@Composable
fun ShoppingCartScreen(onBackPressed: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Cart", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            CheckoutFooter(cartItems = sampleCartItems)
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(sampleCartItems) { item ->
                CartListItem(
                    item = item,
                    onQuantityChange = { /* TODO: Implement quantity change */ }
                )
            }
        }
    }
}

@Composable
private fun CartListItem(
    item: CartItem,
    onQuantityChange: (Int) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(item.imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(12.dp))
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(item.name, fontWeight = FontWeight.Bold)
            Text(String.format("₹%.2f", item.price), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Text(String.format("Tax (8%%): ₹%.2f", item.tax), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
        QuantityStepper(
            quantity = item.quantity,
            onQuantityChange = onQuantityChange
        )
    }
}

@Composable
private fun QuantityStepper(
    quantity: Int,
    onQuantityChange: (Int) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconButton(
            onClick = { if (quantity > 1) onQuantityChange(quantity - 1) },
            modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
            colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.Remove, contentDescription = "Decrease quantity")
        }
        Text(quantity.toString(), fontWeight = FontWeight.Bold, fontSize = 18.sp)
        IconButton(
            onClick = { onQuantityChange(quantity + 1) },
            modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
            colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Increase quantity")
        }
    }
}

@Composable
private fun CheckoutFooter(cartItems: List<CartItem>) {
    val subtotal = cartItems.sumOf { it.price * it.quantity }
    val totalTax = cartItems.sumOf { it.tax * it.quantity }
    val deliveryFee = 225.00 // From HTML
    val grandTotal = subtotal + totalTax + deliveryFee

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 8.dp)
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SummaryRow("Subtotal", String.format("₹%.2f", subtotal))
            SummaryRow("Taxes", String.format("₹%.2f", totalTax))
            SummaryRow("Delivery Fee", String.format("₹%.2f", deliveryFee))
            Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            SummaryRow("Grand Total", String.format("₹%.2f", grandTotal), isBold = true)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { /* TODO: Implement place order */ },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Proceed to Place Order", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, isBold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            fontSize = if (isBold) 18.sp else 16.sp,
            color = if (isBold) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            fontSize = if (isBold) 18.sp else 16.sp,
            color = if (isBold) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}


@Preview(showBackground = true, name = "Light Mode")
@Composable
fun ShoppingCartScreenPreview() {
    MenuAppTheme(darkTheme = false) {
        ShoppingCartScreen(onBackPressed = {})
    }
}

@Preview(showBackground = true, name = "Dark Mode")
@Composable
fun ShoppingCartScreenDarkPreview() {
    MenuAppTheme(darkTheme = true) {
        ShoppingCartScreen(onBackPressed = {})
    }
}
