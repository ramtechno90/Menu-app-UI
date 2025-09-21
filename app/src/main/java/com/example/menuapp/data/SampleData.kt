package com.example.menuapp.data

import com.example.menuapp.data.local.model.MenuItemEntity

object SampleData {
    val menuItems = listOf(
        MenuItemEntity(1, "Ghee Roast Dosa", "Crispy dosa roasted with pure ghee", 120.0, "https://www.vegrecipesofindia.com/wp-content/uploads/2021/05/ghee-roast-dosa-1.jpg", "Starters"),
        MenuItemEntity(2, "Paneer Tikka Masala", "Creamy tomato-based curry with grilled paneer", 250.0, "https://www.indianhealthyrecipes.com/wp-content/uploads/2022/03/paneer-tikka-masala-recipe.jpg", "Main Course"),
        MenuItemEntity(3, "Vegetable Biryani", "Fragrant rice dish with mixed vegetables", 180.0, "https://www.indianveggiedelight.com/wp-content/uploads/2020/04/veg-biryani-instant-pot.jpg", "Main Course"),
        MenuItemEntity(4, "Chicken Chettinad", "Spicy South Indian chicken curry", 280.0, "https://static.toiimg.com/thumb/54673639.cms?width=1200&height=900", "Main Course"),
        MenuItemEntity(5, "Naan", "Soft, leavened flatbread", 40.0, "https://www.indianhealthyrecipes.com/wp-content/uploads/2022/03/butter-naan.jpg", "Breads"),
        MenuItemEntity(6, "Gulab Jamun", "Deep-fried milk solids in syrup", 80.0, "https://www.indianhealthyrecipes.com/wp-content/uploads/2020/09/gulab-jamun-recipe.jpg", "Desserts")
    )
}
