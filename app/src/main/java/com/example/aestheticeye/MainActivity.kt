package com.example.aestheticeye

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.aestheticeye.ui.screens.MainScreen
import com.example.aestheticeye.ui.theme.AestheticEyeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AestheticEyeTheme {
                MainScreen()
            }
        }
    }
}