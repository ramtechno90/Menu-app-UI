package com.example.menuapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.menuapp.navigation.AppNavigation
import com.example.menuapp.ui.theme.MenuAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MenuAppTheme {
                AppNavigation()
            }
        }
    }
}
