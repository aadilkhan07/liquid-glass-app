package com.aadil.liquidglass.viewmodel

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aadil.liquidglass.R
import com.aadil.liquidglass.model.Wallpaper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EditorViewModel : ViewModel() {

    private val _wallpapers = MutableStateFlow<List<Wallpaper>>(emptyList())
    val wallpapers = _wallpapers.asStateFlow()

    // THE FIX: The error was here. The 'by' delegate needs the collectAsState import.
    // I am keeping the original structure you had which works fine with the correct import.
    private val _selectedWallpaper = MutableStateFlow<Wallpaper?>(null)
    val selectedWallpaper = _selectedWallpaper.asStateFlow()

    val blurRadius = MutableStateFlow(24f)
    val glassAlpha = MutableStateFlow(0.45f)
    val refractionHeight = MutableStateFlow(32f)
    val refractionAmount = MutableStateFlow(-40f)
    val cornerRadius = MutableStateFlow(24f)

    init {
        loadWallpapers()
    }

    private fun loadWallpapers() {
        _wallpapers.value = listOf(
            Wallpaper(1, R.drawable.wallpaper_1),
            Wallpaper(2, R.drawable.wallpaper_2),
            Wallpaper(3, R.drawable.wallpaper_3),
            Wallpaper(4, R.drawable.wallpaper_4),
            Wallpaper(5, R.drawable.wallpaper_5)
        )
    }

    fun selectWallpaper(wallpaper: Wallpaper) {
        _selectedWallpaper.value = wallpaper
    }

    fun clearSelection() {
        _selectedWallpaper.value = null
    }

    fun setWallpaper(context: Context, bitmap: Bitmap) {
        viewModelScope.launch {
            try {
                val wallpaperManager = WallpaperManager.getInstance(context)
                wallpaperManager.setBitmap(bitmap)
                Toast.makeText(context, "Wallpaper set successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to set wallpaper.", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }
}
