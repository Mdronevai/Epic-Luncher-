package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.model.WallpaperOption

@Composable
fun WallpaperView(
    wallpaperOption: WallpaperOption,
    dimAlpha: Float,
    isBatterySaver: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (isBatterySaver || wallpaperOption == WallpaperOption.PURE_BLACK) {
            // Absolute Pure Black #000000 for zero OLED pixel power consumption
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            )
        } else {
            when (wallpaperOption) {
                WallpaperOption.AMOLED_GEOMETRIC -> {
                    Image(
                        painter = painterResource(id = R.drawable.wallpaper_amoled_geometric_1793505339684),
                        contentDescription = "AMOLED Geometric Wallpaper",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                WallpaperOption.SUBTLE_GRADIENT -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFF0D1117),
                                        Color(0xFF161B22),
                                        Color(0xFF05070A)
                                    )
                                )
                            )
                    )
                }

                WallpaperOption.MESH_DARK -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF1A233A),
                                        Color(0xFF0C101D),
                                        Color(0xFF04060A)
                                    )
                                )
                            )
                    )
                }

                WallpaperOption.SYSTEM_DEFAULT -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0B0E14))
                    )
                }

                WallpaperOption.PURE_BLACK -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                    )
                }
            }
        }

        // Dim overlay layer
        if (dimAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = dimAlpha.coerceIn(0f, 0.9f)))
            )
        }
    }
}
