package com.example.aestheticeye

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aestheticeye.data.FavoritesDatabase
import com.example.aestheticeye.ui.screens.MainScreen
import com.example.aestheticeye.ui.theme.AestheticEyeTheme
import com.example.aestheticeye.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db  = FavoritesDatabase.getInstance(this)
        val dao = db.favoritesDao()

        setContent {
            AestheticEyeTheme {
                val viewModel: MainViewModel = viewModel(
                    factory = MainViewModel.Factory(dao)
                )
                MainScreen(viewModel = viewModel)
            }
        }
    }
}