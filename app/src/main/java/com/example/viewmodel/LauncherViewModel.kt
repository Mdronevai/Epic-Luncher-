package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.LauncherRepository
import com.example.model.AppCategory
import com.example.model.AppInfo
import com.example.model.BatteryTelemetry
import com.example.model.GestureAction
import com.example.model.GestureType
import com.example.model.HomeScreenItem
import com.example.model.IconPack
import com.example.model.IconShape
import com.example.model.LauncherSettings
import com.example.model.LauncherWidget
import com.example.model.ThemePreset
import com.example.model.WallpaperOption
import com.example.model.WidgetType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class LauncherScreen {
    HOME,
    APP_DRAWER,
    SETTINGS,
    BATTERY_CENTER
}

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    val repository = LauncherRepository(application.applicationContext)

    private val _currentScreen = MutableStateFlow(LauncherScreen.HOME)
    val currentScreen: StateFlow<LauncherScreen> = _currentScreen.asStateFlow()

    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow(AppCategory.ALL)
    val selectedCategory: StateFlow<AppCategory> = _selectedCategory.asStateFlow()

    private val _editingApp = MutableStateFlow<AppInfo?>(null)
    val editingApp: StateFlow<AppInfo?> = _editingApp.asStateFlow()

    private val _showWidgetPicker = MutableStateFlow(false)
    val showWidgetPicker: StateFlow<Boolean> = _showWidgetPicker.asStateFlow()

    private val _currentHomePageIndex = MutableStateFlow(0)
    val currentHomePageIndex: StateFlow<Int> = _currentHomePageIndex.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    private val _isScreenDimmed = MutableStateFlow(false)
    val isScreenDimmed: StateFlow<Boolean> = _isScreenDimmed.asStateFlow()

    val installedApps: StateFlow<List<AppInfo>> = repository.installedApps
    val homeScreenItems: StateFlow<List<HomeScreenItem>> = repository.homeScreenItems
    val widgets: StateFlow<List<LauncherWidget>> = repository.widgets
    val settings: StateFlow<LauncherSettings> = repository.settings
    val batteryTelemetry: StateFlow<BatteryTelemetry> = repository.batteryTelemetry

    // Filtered apps for drawer based on search query, category, and hidden status
    val filteredDrawerApps: StateFlow<List<AppInfo>> = combine(
        installedApps,
        searchQuery,
        selectedCategory
    ) { apps, query, category ->
        apps.filter { app ->
            val matchesQuery = query.isBlank() ||
                    app.displayLabel.contains(query, ignoreCase = true) ||
                    app.packageName.contains(query, ignoreCase = true)

            val matchesCategory = when (category) {
                AppCategory.ALL -> !app.isHidden
                AppCategory.HIDDEN -> app.isHidden
                else -> !app.isHidden && app.category == category
            }

            matchesQuery && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dock apps: apps marked as pinned to dock up to dockCount
    val dockApps: StateFlow<List<AppInfo>> = combine(
        installedApps,
        settings
    ) { apps, s ->
        apps.filter { it.isPinnedToDock && !it.isHidden }.take(s.dockCount)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Recent / Frequently used apps (sorted by launchCount)
    val recentApps: StateFlow<List<AppInfo>> = installedApps.combine(installedApps) { apps, _ ->
        apps.filter { !it.isHidden && it.launchCount > 0 }
            .sortedByDescending { it.launchCount }
            .take(6)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun navigateTo(screen: LauncherScreen) {
        performHapticFeedback()
        _currentScreen.value = screen
        if (screen == LauncherScreen.HOME) {
            _searchQuery.value = ""
        }
    }

    fun setHomePageIndex(index: Int) {
        _currentHomePageIndex.value = index
    }

    fun setEditMode(enabled: Boolean) {
        performHapticFeedback()
        _isEditMode.value = enabled
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategory(category: AppCategory) {
        performHapticFeedback()
        _selectedCategory.value = category
    }

    fun openAppEditor(app: AppInfo) {
        performHapticFeedback()
        _editingApp.value = app
    }

    fun closeAppEditor() {
        _editingApp.value = null
    }

    fun saveAppEdit(customLabel: String?, customGlyph: String?, isHidden: Boolean) {
        val app = _editingApp.value ?: return
        repository.updateAppSettings(
            packageName = app.packageName,
            customLabel = customLabel,
            customGlyph = customGlyph,
            isHidden = isHidden
        )
        _editingApp.value = null
        emitToast("Updated ${app.displayLabel}")
    }

    fun toggleDockPin(packageName: String) {
        performHapticFeedback()
        repository.togglePinToDock(packageName)
    }

    fun addAppToHomeScreen(packageName: String, page: Int = _currentHomePageIndex.value) {
        val success = repository.addToHomeScreen(packageName, page)
        if (success) {
            performHapticFeedback()
            emitToast("Added to Home Screen")
            _currentScreen.value = LauncherScreen.HOME
        } else {
            emitToast("Current page grid is full")
        }
    }

    fun removeHomeScreenItem(itemId: String) {
        performHapticFeedback()
        repository.removeFromHomeScreen(itemId)
        emitToast("Removed from Home Screen")
    }

    fun openWidgetPicker() {
        _showWidgetPicker.value = true
    }

    fun closeWidgetPicker() {
        _showWidgetPicker.value = false
    }

    fun addWidget(type: WidgetType) {
        performHapticFeedback()
        repository.addWidget(type, _currentHomePageIndex.value)
        _showWidgetPicker.value = false
        emitToast("Added ${type.title}")
    }

    fun removeWidget(widgetId: String) {
        performHapticFeedback()
        repository.removeWidget(widgetId)
        emitToast("Widget removed")
    }

    fun cycleWidgetStyle(widgetId: String) {
        performHapticFeedback()
        repository.cycleWidgetStyle(widgetId)
    }

    fun updateGridDimensions(cols: Int, rows: Int) {
        repository.updateSettings { it.copy(gridColumns = cols, gridRows = rows) }
    }

    fun updateDockCount(count: Int) {
        repository.updateSettings { it.copy(dockCount = count) }
    }

    fun updateIconPack(pack: IconPack) {
        performHapticFeedback()
        repository.updateSettings { it.copy(iconPack = pack) }
    }

    fun updateIconShape(shape: IconShape) {
        performHapticFeedback()
        repository.updateSettings { it.copy(iconShape = shape) }
    }

    fun updateIconScale(scale: Float) {
        repository.updateSettings { it.copy(iconScale = scale) }
    }

    fun toggleLabels(show: Boolean) {
        repository.updateSettings { it.copy(showLabels = show) }
    }

    fun updateThemePreset(preset: ThemePreset) {
        performHapticFeedback()
        repository.updateSettings { it.copy(themePreset = preset) }
    }

    fun updateWallpaper(option: WallpaperOption) {
        performHapticFeedback()
        repository.updateSettings { it.copy(wallpaperOption = option) }
    }

    fun updateWallpaperDim(dim: Float) {
        repository.updateSettings { it.copy(wallpaperDim = dim) }
    }

    fun updateWallpaperBlur(blur: Float) {
        repository.updateSettings { it.copy(wallpaperBlur = blur) }
    }

    fun updateGesture(type: GestureType, action: GestureAction) {
        val currentGestures = repository.settings.value.gestures.toMutableMap()
        currentGestures[type] = action
        repository.updateSettings { it.copy(gestures = currentGestures) }
        performHapticFeedback()
        emitToast("Mapped ${type.label} → ${action.label}")
    }

    fun toggleBatterySaver() {
        performHapticFeedback()
        repository.toggleBatterySaver()
        val newState = repository.settings.value.batterySaverMode
        emitToast(if (newState) "Ultra Battery Saver Enabled" else "Standard Theme Restored")
    }

    fun toggleHaptics() {
        repository.updateSettings { it.copy(hapticFeedbackEnabled = !it.hapticFeedbackEnabled) }
    }

    fun handleGesture(gestureType: GestureType, context: Context) {
        val action = repository.settings.value.gestures[gestureType] ?: GestureAction.NONE
        performHapticFeedback()
        when (action) {
            GestureAction.OPEN_DRAWER -> {
                _currentScreen.value = LauncherScreen.APP_DRAWER
            }
            GestureAction.EXPAND_NOTIFICATIONS -> {
                expandNotificationShade(context)
            }
            GestureAction.QUICK_SEARCH -> {
                _currentScreen.value = LauncherScreen.APP_DRAWER
            }
            GestureAction.SCREEN_SLEEP -> {
                _isScreenDimmed.value = !_isScreenDimmed.value
                emitToast(if (_isScreenDimmed.value) "Tap anywhere to wake" else "Screen restored")
            }
            GestureAction.BATTERY_SAVER_TOGGLE -> {
                toggleBatterySaver()
            }
            GestureAction.OPEN_SETTINGS -> {
                _currentScreen.value = LauncherScreen.SETTINGS
            }
            GestureAction.HIDDEN_APPS_VAULT -> {
                _selectedCategory.value = AppCategory.HIDDEN
                _currentScreen.value = LauncherScreen.APP_DRAWER
                emitToast("Opened Hidden Apps Vault")
            }
            GestureAction.NONE -> {
                // Do nothing
            }
        }
    }

    fun wakeScreen() {
        if (_isScreenDimmed.value) {
            _isScreenDimmed.value = false
        }
    }

    private fun expandNotificationShade(context: Context) {
        try {
            val statusBarService = context.getSystemService("statusbar")
            val statusBarManager = Class.forName("android.app.StatusBarManager")
            val expandMethod = statusBarManager.getMethod("expandNotificationsPanel")
            expandMethod.invoke(statusBarService)
        } catch (e: Exception) {
            emitToast("Notifications panel triggered")
        }
    }

    fun launchApp(context: Context, app: AppInfo) {
        performHapticFeedback()
        val launched = repository.launchApp(context, app.packageName)
        if (!launched) {
            emitToast("Launching ${app.displayLabel}...")
        }
    }

    fun performHapticFeedback() {
        if (!repository.settings.value.hapticFeedbackEnabled) return
        try {
            val ctx = getApplication<Application>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(18, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(18)
                }
            }
        } catch (e: Exception) {
            // Ignore haptic failures
        }
    }

    private fun emitToast(msg: String) {
        viewModelScope.launch {
            _toastEvent.emit(msg)
        }
    }
}
