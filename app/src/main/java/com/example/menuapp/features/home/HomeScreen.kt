package com.example.menuapp.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

data class MenuItem(
    val id: Int,
    val name: String,
    val description: String,
    val price: String,
    val imageUrl: String
)

val sampleMenuItems = listOf(
    MenuItem(1, "Ghee Roast Dosa", "Crispy dosa roasted with pure ghee", "₹120", "https://lh3.googleusercontent.com/aida-public/AB6AXuAGEXA4GRk1EqSaxf08Pfr_GiJ04xSjFX8eNRcFId4dtz-uB5sYFxR_9hmErqyxjYwQJWkfBMFzG5PsqDew05ZusKinkAjluMIANpU_VmAA4mG0n6XmF-A8FtHGfL9j9olc3hBUGpa4aLHKAS1FH52bj0dpb4HoG81A-_C3qpqGRZovUh2E5QYXsRTAit5IkwtTbc1zusJuZgW0M8tzD9OAqNuN_wlhIdhlatstXBgpt798LVwaMZC9YFGOnIAwNqOD8v1DRAFwEHvI"),
    MenuItem(2, "Paneer Tikka Masala", "Creamy tomato-based curry with grilled paneer", "₹250", "https://lh3.googleusercontent.com/aida-public/AB6AXuCHm6QbURUkbim5OxxjFl0vMhecIeSqFxh9MrgguQkUuf54RxTafHAAi7WAOrOf834dkR0rp22VTGCbjnLmPBik2xntwpoSwWWfb4IPhV6VFXmrFOFb5FV8dca455YQ7wFUetYDHu9U4xb5nkBcQDR9mdb3UieGoOMr9qA9Js6fk2RcgDf2vMZyhrMe0EdYVmRsL1vqOmcDWHJ1J2spwYLnXZwqGJ_bII71XgvNfktzDIwo-Lqzd417BMQqvk1rhS-K34DYF5-D98_q"),
    MenuItem(3, "Vegetable Biryani", "Fragrant rice dish with mixed vegetables", "₹180", "https://lh3.googleusercontent.com/aida-public/AB6AXuCp-5jZK6dJ-wPWQRdhwzuvfFLgbquDmLnyETb6UOXCkKqONgCUIROLMrW6iXoPJQ8fczCggX_KRMkqxXf16Ip75ZdKmzNaiV6FIgk_IGhWpGtYWN15ZrkjykdX2uQmC85R8ybQAClfyc-YbAyQYEDcuntJZPIdc2SEY76LiuH0I4bbamFxTSRg8YyIr1HbbFY_nfDtrZVRKiFO_5N8BhmlVSRbURf9Qaz4au0N6PcMgMQvq86pHSqJGESwbikgvR8ULVaGV55a-ElM"),
    MenuItem(4, "Chicken Chettinad", "Spicy South Indian chicken curry", "₹280", "https://lh3.googleusercontent.com/aida-public/AB6AXuD3_Xo0x9iKjKmxSFau4pCE8wXbx-IUsQaDxgbcCaTVpd8LfhAAYojeTn-R6qhyt7s_GL4QvfJi2n2UHAJkS8Z9kqPkrJx5v9Yy2LqZX0Arpc95BAO0eRJRjpZxze1D2BSwMSV9IrdE43NT2qHI-1ycdQqUSASKEFWiIqPLiAhiuDcCW85T_JnSnlwAv53xydU3lccEW74MMRqv5ULC32FGTNhRPNTmTkJYaJ9qF5BqwZERQPXCIsFN9Hw6wOcQQqePxdlbLgUk5ktj")
)

val categories = listOf("Starters", "Main Course", "Breads", "Desserts")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf(categories.first()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Annapoorna, Coimbatore", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { /* TODO: Implement search action */ }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = onThemeToggle) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                )
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                CategorySelection(
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it }
                )
            }

            items(sampleMenuItems) { menuItem ->
                MenuItemCard(menuItem = menuItem, onAddToCart = { /* TODO: Implement add to cart */ })
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun CategorySelection(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { category ->
            val isSelected = category == selectedCategory
            Button(
                onClick = { onCategorySelected(category) },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Text(text = category, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun MenuItemCard(
    menuItem: MenuItem,
    onAddToCart: (MenuItem) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(menuItem.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(menuItem.description, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Spacer(modifier = Modifier.height(8.dp))
            Text(menuItem.price, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { onAddToCart(menuItem) },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add to cart", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add", fontWeight = FontWeight.Bold)
            }
        }
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(menuItem.imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = menuItem.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(12.dp))
        )
    }
}

@Preview(showBackground = true, name = "Light Mode")
@Composable
fun HomeScreenPreview() {
    MenuAppTheme(darkTheme = false) {
        HomeScreen(isDarkTheme = false, onThemeToggle = {})
    }
}

@Preview(showBackground = true, name = "Dark Mode")
@Composable
fun HomeScreenDarkPreview() {
    MenuAppTheme(darkTheme = true) {
        HomeScreen(isDarkTheme = true, onThemeToggle = {})
    }
}
