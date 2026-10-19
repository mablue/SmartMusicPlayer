package com.example.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.ui.viewmodel.MusicPlayerViewModel

@Composable
fun HomeScreen(viewModel: MusicPlayerViewModel) {
    // Minimal placeholder UI so project compiles; replace with real UI later.
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "Welcome to SmartMusicPlayer")
    }
}
