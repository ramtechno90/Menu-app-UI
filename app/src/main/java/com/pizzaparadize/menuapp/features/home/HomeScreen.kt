package com.pizzaparadize.menuapp.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
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
import com.pizzaparadize.menuapp.data.firebase.model.Category
import com.pizzaparadize.menuapp.data.firebase.model.MenuItem
import com.pizzaparadize.menuapp.features.common.AppFooter
import com.pizzaparadize.menuapp.ui.theme.MenuAppTheme

@Composable
fun HomeScreen(
    contentPadding: PaddingValues,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedCategoryId by remember { mutableStateOf("") }

    if (uiState.categories.isNotEmpty() && selectedCategoryId.isBlank()) {
        selectedCategoryId = uiState.categories.first().id
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (uiState.categories.isNotEmpty()) {
            item {
                CategorySelection(
                    categories = uiState.categories,
                    selectedCategoryId = selectedCategoryId,
                    onCategorySelected = { selectedCategoryId = it }
                )
            }

            items(uiState.menuItems.filter { it.category == selectedCategoryId }) { menuItem ->
                MenuItemCard(
                    menuItem = menuItem,
                    quantity = uiState.cartQuantities[menuItem.id] ?: 0,
                    onAddToCart = { viewModel.addToCart(menuItem) },
                    onIncrement = { viewModel.incrementQuantity(menuItem) },
                    onDecrement = { viewModel.decrementQuantity(menuItem) },
                    showImage = uiState.showImages
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                AppFooter(contactNumber = uiState.contactNumber)
            }
        }
    }
}

@Composable
private fun CategorySelection(
    categories: List<Category>,
    selectedCategoryId: String,
    onCategorySelected: (String) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { category ->
            val isSelected = category.id == selectedCategoryId
            Button(
                onClick = { onCategorySelected(category.id) },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Text(text = category.name, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun MenuItemCard(
    menuItem: MenuItem,
    quantity: Int,
    onAddToCart: (MenuItem) -> Unit,
    onIncrement: (MenuItem) -> Unit,
    onDecrement: (MenuItem) -> Unit,
    showImage: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatMenuItemName(menuItem.name),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f, fill = false)
                )
                AddToCartButton(
                    quantity = quantity,
                    onAddToCart = { onAddToCart(menuItem) },
                    onIncrement = { onIncrement(menuItem) },
                    onDecrement = { onDecrement(menuItem) }
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "₹${menuItem.price}",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = menuItem.description,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
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

private fun formatMenuItemName(name: String): String {
    return name.trim().split(Regex("\\s+")).chunked(3).joinToString("\n") { it.joinToString(" ") }
}

@Composable
private fun AddToCartButton(
    quantity: Int,
    onAddToCart: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    if (quantity == 0) {
        Button(
            onClick = onAddToCart,
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Text(text = "+ Add", fontWeight = FontWeight.Bold)
        }
    } else {
        QuantitySelector(
            quantity = quantity,
            onIncrement = onIncrement,
            onDecrement = onDecrement
        )
    }
}

@Composable
private fun QuantitySelector(
    quantity: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(
            onClick = onDecrement,
            enabled = quantity > 0,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
        ) {
            Icon(Icons.Default.Remove, contentDescription = "Decrement", tint = MaterialTheme.colorScheme.primary)
        }
        Text(quantity.toString(), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        IconButton(
            onClick = onIncrement,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Increment", tint = MaterialTheme.colorScheme.primary)
        }
    }
}


@Preview(showBackground = true, name = "Light Mode")
@Composable
fun HomeScreenPreview() {
    MenuAppTheme {
        HomeScreen(contentPadding = PaddingValues(0.dp))
    }
}
