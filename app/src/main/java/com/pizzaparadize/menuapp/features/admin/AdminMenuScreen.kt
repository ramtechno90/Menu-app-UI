package com.pizzaparadize.menuapp.features.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pizzaparadize.menuapp.data.firebase.model.Category
import com.pizzaparadize.menuapp.data.firebase.model.MenuItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMenuScreen(
    viewModel: AdminMenuViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showAddMenuItemDialog by remember { mutableStateOf(false) }
    var selectedCategoryForItem by remember { mutableStateOf<Category?>(null) }

    // For editing
    var categoryToEdit by remember { mutableStateOf<Category?>(null) }
    var menuItemToEdit by remember { mutableStateOf<MenuItem?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Menu Management") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { showAddCategoryDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Category")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (uiState.categories.isEmpty()) {
                    item {
                        Text("No categories found. Add one to start.")
                    }
                }

                items(uiState.categories) { category ->
                    CategoryItem(
                        category = category,
                        items = uiState.menuItems.filter {
                            // Handle both ID and Name matching for backward compatibility/legacy data
                            it.category == category.id || it.category == category.name
                        },
                        onEditCategory = { categoryToEdit = category },
                        onDeleteCategory = { viewModel.deleteCategory(category.id) },
                        onAddItem = {
                            selectedCategoryForItem = category
                            showAddMenuItemDialog = true
                        },
                        onEditItem = { menuItemToEdit = it },
                        onDeleteItem = { viewModel.deleteMenuItem(it.id) }
                    )
                }
            }
        }
    }

    if (showAddCategoryDialog) {
        CategoryDialog(
            onDismiss = { showAddCategoryDialog = false },
            onConfirm = { name, order ->
                viewModel.addCategory(name, order)
                showAddCategoryDialog = false
            }
        )
    }

    if (categoryToEdit != null) {
        CategoryDialog(
            category = categoryToEdit,
            onDismiss = { categoryToEdit = null },
            onConfirm = { name, order ->
                viewModel.updateCategory(categoryToEdit!!.copy(name = name, order = order))
                categoryToEdit = null
            }
        )
    }

    if (showAddMenuItemDialog && selectedCategoryForItem != null) {
        MenuItemDialog(
            categoryId = selectedCategoryForItem!!.id,
            onDismiss = {
                showAddMenuItemDialog = false
                selectedCategoryForItem = null
            },
            onConfirm = { name, desc, price, img, catId ->
                viewModel.addMenuItem(name, desc, price, catId, img)
                showAddMenuItemDialog = false
                selectedCategoryForItem = null
            }
        )
    }

    if (menuItemToEdit != null) {
        MenuItemDialog(
            menuItem = menuItemToEdit,
            categoryId = menuItemToEdit!!.category, // Pass existing category
            onDismiss = { menuItemToEdit = null },
            onConfirm = { name, desc, price, img, _ ->
                viewModel.updateMenuItem(menuItemToEdit!!.copy(
                    name = name,
                    description = desc,
                    price = price,
                    imageUrl = img
                ))
                menuItemToEdit = null
            }
        )
    }
}

@Composable
fun CategoryItem(
    category: Category,
    items: List<MenuItem>,
    onEditCategory: () -> Unit,
    onDeleteCategory: () -> Unit,
    onAddItem: () -> Unit,
    onEditItem: (MenuItem) -> Unit,
    onDeleteItem: (MenuItem) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${category.name} (${items.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row {
                    IconButton(onClick = onEditCategory) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Category", modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onDeleteCategory) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Category", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    }
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    items.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                Text("₹${item.price}", style = MaterialTheme.typography.bodyMedium)
                            }
                            Row {
                                IconButton(onClick = { onEditItem(item) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Item", modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { onDeleteItem(item) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Item", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onAddItem,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Item")
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryDialog(
    category: Category? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, Int) -> Unit
) {
    var name by remember { mutableStateOf(category?.name ?: "") }
    var order by remember { mutableStateOf(category?.order?.toString() ?: "0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (category == null) "Add Category" else "Edit Category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = order,
                    onValueChange = { if (it.all { char -> char.isDigit() }) order = it },
                    label = { Text("Order Priority") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name, order.toIntOrNull() ?: 0)
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun MenuItemDialog(
    menuItem: MenuItem? = null,
    categoryId: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Double, String, String) -> Unit
) {
    var name by remember { mutableStateOf(menuItem?.name ?: "") }
    var description by remember { mutableStateOf(menuItem?.description ?: "") }
    var price by remember { mutableStateOf(menuItem?.price?.toString() ?: "") }
    var imageUrl by remember { mutableStateOf(menuItem?.imageUrl ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (menuItem == null) "Add Menu Item" else "Edit Menu Item") },
        text = {
            Column(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") }
                )
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Price") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text("Image URL") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val priceDouble = price.toDoubleOrNull()
                    if (name.isNotBlank() && priceDouble != null) {
                        onConfirm(name, description, priceDouble, imageUrl, categoryId)
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
