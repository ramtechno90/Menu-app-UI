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
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.menuapp.data.firebase.model.MenuItem
import com.example.menuapp.ui.theme.MenuAppTheme

val categories = listOf("Starters", "Main Course", "Breads", "Pizza", "Salad", "Pasta", "Dessert")

@Composable
fun HomeScreen(
    contentPadding: PaddingValues,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedCategory by remember { mutableStateOf(categories.first()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
            item {
                CategorySelection(
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it }
                )
            }

            items(uiState.menuItems.filter { it.category == selectedCategory }) { menuItem ->
                MenuItemCard(
                    menuItem = menuItem,
                    onAddToCart = { viewModel.addToCart(menuItem) },
                    showImage = uiState.showImages
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
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
    onAddToCart: (MenuItem) -> Unit,
    showImage: Boolean
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
            Text("₹${menuItem.price}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
        if (showImage && menuItem.imageUrl.isNotBlank()) {
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
}

@Preview(showBackground = true, name = "Light Mode")
@Composable
fun HomeScreenPreview() {
    MenuAppTheme(darkTheme = false) {
        // This preview won't have a real ViewModel, so it will show an empty list.
        // For a more complete preview, a fake ViewModel/repository could be provided.
        HomeScreen(contentPadding = PaddingValues(0.dp))
    }
}
