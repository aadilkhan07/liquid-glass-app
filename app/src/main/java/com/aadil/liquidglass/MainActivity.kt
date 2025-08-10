package com.aadil.liquidglass

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aadil.liquidglass.ui.screens.MainScreen
import com.aadil.liquidglass.ui.theme.LiquidglassTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LiquidglassTheme {
                MainScreen()
            }
        }
    }
}