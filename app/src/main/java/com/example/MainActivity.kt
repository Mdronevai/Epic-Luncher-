package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.WallpaperView
import com.example.ui.dialogs.AppEditDialog
import com.example.ui.dialogs.WidgetPickerSheet
import com.example.ui.screens.AppDrawerView
import com.example.ui.screens.BatteryEfficiencyScreen
import com.example.ui.screens.HomeScreenView
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AuraLauncherTheme
import com.example.viewmodel.LauncherScreen
import com.example.viewmodel.LauncherViewModel
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: LauncherViewModel = viewModel()
            val settings by viewModel.settings.collectAsState()
            val currentScreen by viewModel.currentScreen.collectAsState()
            val isScreenDimmed by viewModel.isScreenDimmed.collectAsState()
            val editingApp by viewModel.editingApp.collectAsState()
            val showWidgetPicker by viewModel.showWidgetPicker.collectAsState()
            val context = LocalContext.current

            LaunchedEffect(Unit) {
                viewModel.toastEvent.collectLatest { message ->
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            }

            AuraLauncherTheme(preset = settings.themePreset) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Wallpaper Background
                    WallpaperView(
                        wallpaperOption = settings.wallpaperOption,
                        dimAlpha = settings.wallpaperDim,
                        isBatterySaver = settings.batterySaverMode
                    )

                    // Navigation View Transitions
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            when (targetState) {
                                LauncherScreen.APP_DRAWER -> {
                                    (slideInVertically { it / 3 } + fadeIn())
                                        .togetherWith(slideOutVertically { -it / 6 } + fadeOut())
                                }
                                LauncherScreen.HOME -> {
                                    (fadeIn()).togetherWith(slideOutVertically { it / 3 } + fadeOut())
                                }
                                else -> {
                                    fadeIn().togetherWith(fadeOut())
                                }
                            }
                        },
                        label = "screen_transition",
                        modifier = Modifier.fillMaxSize()
                    ) { screen ->
                        when (screen) {
                            LauncherScreen.HOME -> {
                                HomeScreenView(viewModel = viewModel)
                            }
                            LauncherScreen.APP_DRAWER -> {
                                AppDrawerView(viewModel = viewModel)
                            }
                            LauncherScreen.SETTINGS -> {
                                SettingsScreen(viewModel = viewModel)
                            }
                            LauncherScreen.BATTERY_CENTER -> {
                                BatteryEfficiencyScreen(viewModel = viewModel)
                            }
                        }
                    }

                    // Screen Sleep / Dim overlay (triggered by Double Tap gesture)
                    if (isScreenDimmed) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.96f))
                                .clickable { viewModel.wakeScreen() }
                        ) {
                            Text(
                                text = "SCREEN ASLEEP • TAP TO WAKE",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )
                        }
                    }

                    // Edit App Dialog
                    editingApp?.let { app ->
                        AppEditDialog(
                            app = app,
                            onDismiss = { viewModel.closeAppEditor() },
                            onSave = { customLabel, customGlyph, isHidden ->
                                viewModel.saveAppEdit(customLabel, customGlyph, isHidden)
                            }
                        )
                    }

                    // Widget Picker Bottom Sheet
                    if (showWidgetPicker) {
                        WidgetPickerSheet(
                            onDismiss = { viewModel.closeWidgetPicker() },
                            onSelectWidget = { widgetType ->
                                viewModel.addWidget(widgetType)
                            }
                        )
                    }
                }
            }
        }
    }
}
