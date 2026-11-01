package com.example.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.util.Log
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class LauncherRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("aura_launcher_prefs", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    private val _homeScreenItems = MutableStateFlow<List<HomeScreenItem>>(emptyList())
    val homeScreenItems: StateFlow<List<HomeScreenItem>> = _homeScreenItems.asStateFlow()

    private val _widgets = MutableStateFlow<List<LauncherWidget>>(emptyList())
    val widgets: StateFlow<List<LauncherWidget>> = _widgets.asStateFlow()

    private val _settings = MutableStateFlow(LauncherSettings())
    val settings: StateFlow<LauncherSettings> = _settings.asStateFlow()

    private val _batteryTelemetry = MutableStateFlow(BatteryTelemetry())
    val batteryTelemetry: StateFlow<BatteryTelemetry> = _batteryTelemetry.asStateFlow()

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                updateBatteryInfo(intent)
            }
        }
    }

    init {
        loadSettings()
        loadWidgets()
        loadHomeScreenItems()
        loadApps()
        registerBatteryReceiver()
    }

    private fun registerBatteryReceiver() {
        try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val initialIntent = context.registerReceiver(batteryReceiver, filter)
            if (initialIntent != null) {
                updateBatteryInfo(initialIntent)
            }
        } catch (e: Exception) {
            Log.e("LauncherRepository", "Error registering battery receiver", e)
        }
    }

    private fun updateBatteryInfo(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val pct = if (level >= 0 && scale > 0) (level * 100) / scale else 82

        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val healthCode = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_GOOD)
        val healthStr = when (healthCode) {
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Critical"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            else -> "Good"
        }

        val rawTemp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 280)
        val tempC = rawTemp / 10f
        val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 4100)

        val hoursRemaining = if (isCharging) {
            (100 - pct) * 0.035f
        } else {
            (pct * 0.18f)
        }

        val tip = if (isCharging) {
            "Fast charging active — Minimalist theme reduces thermals"
        } else if (_settings.value.batterySaverMode) {
            "Aura Saver Active: AMOLED true black + static glyphs (+3.5h)"
        } else {
            "Pure AMOLED Black saves ~38% display power vs light mode"
        }

        _batteryTelemetry.value = BatteryTelemetry(
            percentage = pct,
            isCharging = isCharging,
            health = healthStr,
            temperatureCelsius = tempC,
            voltageMv = voltage,
            powerSavingsTip = tip,
            estimatedHoursRemaining = hoursRemaining
        )
    }

    fun loadApps() {
        scope.launch {
            val pm = context.packageManager
            val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }

            val resolveInfoList = try {
                pm.queryIntentActivities(launcherIntent, 0)
            } catch (e: Exception) {
                emptyList()
            }

            val appList = mutableListOf<AppInfo>()
            val customOverrides = loadCustomAppOverrides()

            for (resolveInfo in resolveInfoList) {
                val pkg = resolveInfo.activityInfo.packageName
                // Avoid listing our own launcher in the app drawer to avoid recursive launches
                if (pkg == context.packageName) continue

                val label = try {
                    resolveInfo.loadLabel(pm).toString()
                } catch (e: Exception) {
                    pkg
                }

                val override = customOverrides[pkg]
                val category = guessCategory(pkg, label)
                val isSystem = (resolveInfo.activityInfo.applicationInfo.flags and
                        android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0

                appList.add(
                    AppInfo(
                        packageName = pkg,
                        activityName = resolveInfo.activityInfo.name,
                        label = label,
                        customLabel = override?.optString("customLabel")?.takeIf { it.isNotBlank() },
                        category = category,
                        customGlyph = override?.optString("customGlyph")?.takeIf { it.isNotBlank() },
                        accentColorHex = override?.optString("accentColorHex")?.takeIf { it.isNotBlank() },
                        launchCount = override?.optInt("launchCount", 0) ?: 0,
                        isPinnedToDock = override?.optBoolean("isPinnedToDock", false) ?: false,
                        isHidden = override?.optBoolean("isHidden", false) ?: false,
                        isSystemApp = isSystem
                    )
                )
            }

            // If running on a minimal emulator or test environment with very few apps,
            // complement with rich default essential apps for immediate delight.
            if (appList.size < 6) {
                val defaults = getCuratedDefaultApps()
                for (def in defaults) {
                    if (appList.none { it.packageName == def.packageName }) {
                        val override = customOverrides[def.packageName]
                        appList.add(
                            def.copy(
                                customLabel = override?.optString("customLabel")?.takeIf { it.isNotBlank() } ?: def.customLabel,
                                isPinnedToDock = override?.optBoolean("isPinnedToDock") ?: def.isPinnedToDock,
                                isHidden = override?.optBoolean("isHidden") ?: def.isHidden,
                                launchCount = override?.optInt("launchCount") ?: def.launchCount
                            )
                        )
                    }
                }
            }

            appList.sortBy { it.displayLabel.lowercase() }
            _installedApps.value = appList

            // If home screen is empty on initial first launch, populate defaults
            if (_homeScreenItems.value.isEmpty()) {
                populateInitialHomeScreen(appList)
            }
        }
    }

    private fun guessCategory(pkg: String, label: String): AppCategory {
        val lower = "$pkg $label".lowercase()
        return when {
            lower.contains("phone") || lower.contains("dialer") || lower.contains("call") -> AppCategory.UTILITIES
            lower.contains("message") || lower.contains("sms") || lower.contains("chat") || lower.contains("telegram") || lower.contains("whatsapp") -> AppCategory.SOCIAL
            lower.contains("camera") || lower.contains("photo") || lower.contains("gallery") -> AppCategory.MEDIA
            lower.contains("music") || lower.contains("spotify") || lower.contains("sound") || lower.contains("audio") || lower.contains("youtube") -> AppCategory.MEDIA
            lower.contains("chrome") || lower.contains("browser") || lower.contains("firefox") || lower.contains("web") -> AppCategory.UTILITIES
            lower.contains("setting") || lower.contains("config") -> AppCategory.SYSTEM
            lower.contains("mail") || lower.contains("gmail") || lower.contains("note") || lower.contains("calendar") || lower.contains("task") || lower.contains("office") -> AppCategory.PRODUCTIVITY
            lower.contains("calc") || lower.contains("clock") || lower.contains("alarm") || lower.contains("file") || lower.contains("weather") -> AppCategory.TOOLS
            lower.contains("game") -> AppCategory.GAMES
            else -> AppCategory.UTILITIES
        }
    }

    private fun getCuratedDefaultApps(): List<AppInfo> {
        return listOf(
            AppInfo("com.google.android.dialer", "", "Phone", null, AppCategory.UTILITIES, "Phone", "#4CAF50", 25, isPinnedToDock = true),
            AppInfo("com.google.android.apps.messaging", "", "Messages", null, AppCategory.SOCIAL, "Chat", "#2196F3", 18, isPinnedToDock = true),
            AppInfo("com.android.chrome", "", "Browser", null, AppCategory.UTILITIES, "Language", "#00BCD4", 32, isPinnedToDock = true),
            AppInfo("com.google.android.GoogleCamera", "", "Camera", null, AppCategory.MEDIA, "PhotoCamera", "#E91E63", 14, isPinnedToDock = true),
            AppInfo("com.google.android.apps.photos", "", "Gallery", null, AppCategory.MEDIA, "Image", "#FF9800", 20, isPinnedToDock = true),
            AppInfo("com.google.android.gm", "", "Gmail", null, AppCategory.PRODUCTIVITY, "Mail", "#F44336", 15),
            AppInfo("com.google.android.calendar", "", "Calendar", null, AppCategory.PRODUCTIVITY, "CalendarToday", "#3F51B5", 9),
            AppInfo("com.google.android.apps.maps", "", "Maps", null, AppCategory.UTILITIES, "Map", "#009688", 12),
            AppInfo("com.google.android.deskclock", "", "Clock", null, AppCategory.TOOLS, "AccessTime", "#673AB7", 8),
            AppInfo("com.google.android.calculator", "", "Calculator", null, AppCategory.TOOLS, "Calculate", "#FF5722", 6),
            AppInfo("com.google.android.apps.nbu.files", "", "Files", null, AppCategory.TOOLS, "Folder", "#795548", 5),
            AppInfo("com.google.android.keep", "", "Notes", null, AppCategory.PRODUCTIVITY, "EditNote", "#FFC107", 11),
            AppInfo("com.spotify.music", "", "Spotify", null, AppCategory.MEDIA, "Audiotrack", "#1DB954", 28),
            AppInfo("com.android.settings", "", "Settings", null, AppCategory.SYSTEM, "Settings", "#607D8B", 10)
        )
    }

    private fun populateInitialHomeScreen(apps: List<AppInfo>) {
        val initialItems = mutableListOf<HomeScreenItem>()
        // Top 8 apps placed on Page 0 in grid
        val nonDockApps = apps.filter { !it.isPinnedToDock }.take(8)
        var row = 0
        var col = 0
        for (app in nonDockApps) {
            initialItems.add(
                HomeScreenItem(
                    id = UUID.randomUUID().toString(),
                    pageIndex = 0,
                    gridRow = row,
                    gridCol = col,
                    packageName = app.packageName
                )
            )
            col++
            if (col >= 4) {
                col = 0
                row++
            }
        }
        _homeScreenItems.value = initialItems
        saveHomeScreenItems()
    }

    fun launchApp(context: Context, packageName: String): Boolean {
        return try {
            val pm = context.packageManager
            val intent = pm.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                incrementLaunchCount(packageName)
                true
            } else {
                // If package is not physically on device (virtual preview item),
                // simulate successful launch and increment launch count
                incrementLaunchCount(packageName)
                false
            }
        } catch (e: Exception) {
            Log.e("LauncherRepository", "Failed to launch $packageName", e)
            false
        }
    }

    private fun incrementLaunchCount(packageName: String) {
        _installedApps.update { list ->
            list.map {
                if (it.packageName == packageName) it.copy(launchCount = it.launchCount + 1)
                else it
            }
        }
        saveAppOverride(packageName)
    }

    fun updateAppSettings(
        packageName: String,
        customLabel: String?,
        customGlyph: String?,
        isHidden: Boolean
    ) {
        _installedApps.update { list ->
            list.map {
                if (it.packageName == packageName) {
                    it.copy(
                        customLabel = customLabel,
                        customGlyph = customGlyph,
                        isHidden = isHidden
                    )
                } else it
            }
        }
        saveAppOverride(packageName)
    }

    fun togglePinToDock(packageName: String) {
        _installedApps.update { list ->
            list.map {
                if (it.packageName == packageName) {
                    it.copy(isPinnedToDock = !it.isPinnedToDock)
                } else it
            }
        }
        saveAppOverride(packageName)
    }

    fun addToHomeScreen(packageName: String, page: Int = 0): Boolean {
        val current = _homeScreenItems.value
        val itemsOnPage = current.filter { it.pageIndex == page }
        val maxCells = _settings.value.gridColumns * _settings.value.gridRows

        if (itemsOnPage.size >= maxCells) {
            return false // Page full
        }

        // Find next free cell
        val occupied = itemsOnPage.map { it.gridRow to it.gridCol }.toSet()
        var targetRow = 0
        var targetCol = 0
        var found = false
        for (r in 0 until _settings.value.gridRows) {
            for (c in 0 until _settings.value.gridColumns) {
                if ((r to c) !in occupied) {
                    targetRow = r
                    targetCol = c
                    found = true
                    break
                }
            }
            if (found) break
        }

        val newItem = HomeScreenItem(
            id = UUID.randomUUID().toString(),
            pageIndex = page,
            gridRow = targetRow,
            gridCol = targetCol,
            packageName = packageName
        )
        _homeScreenItems.value = current + newItem
        saveHomeScreenItems()
        return true
    }

    fun removeFromHomeScreen(itemId: String) {
        _homeScreenItems.update { list ->
            list.filterNot { it.id == itemId }
        }
        saveHomeScreenItems()
    }

    fun addWidget(type: WidgetType, pageIndex: Int = 0) {
        val current = _widgets.value
        val newWidget = LauncherWidget(
            id = UUID.randomUUID().toString(),
            type = type,
            pageIndex = pageIndex,
            orderIndex = current.filter { it.pageIndex == pageIndex }.size
        )
        _widgets.value = current + newWidget
        saveWidgets()
    }

    fun removeWidget(widgetId: String) {
        _widgets.update { list ->
            list.filterNot { it.id == widgetId }
        }
        saveWidgets()
    }

    fun cycleWidgetStyle(widgetId: String) {
        _widgets.update { list ->
            list.map {
                if (it.id == widgetId) it.copy(styleVariant = (it.styleVariant + 1) % 4)
                else it
            }
        }
        saveWidgets()
    }

    fun updateSettings(transform: (LauncherSettings) -> LauncherSettings) {
        val updated = transform(_settings.value)
        _settings.value = updated
        saveSettings(updated)
    }

    fun toggleBatterySaver() {
        val current = _settings.value
        val newMode = !current.batterySaverMode
        val updated = current.copy(
            batterySaverMode = newMode,
            themePreset = if (newMode) ThemePreset.AMOLED_DARK else current.themePreset,
            wallpaperOption = if (newMode) WallpaperOption.PURE_BLACK else current.wallpaperOption
        )
        _settings.value = updated
        saveSettings(updated)
    }

    // Persistence Helpers
    private fun saveSettings(s: LauncherSettings) {
        prefs.edit().apply {
            putInt("grid_cols", s.gridColumns)
            putInt("grid_rows", s.gridRows)
            putInt("dock_count", s.dockCount)
            putString("icon_pack", s.iconPack.name)
            putString("icon_shape", s.iconShape.name)
            putFloat("icon_scale", s.iconScale)
            putBoolean("show_labels", s.showLabels)
            putInt("label_font_size", s.labelFontSizeSp)
            putString("theme_preset", s.themePreset.name)
            putString("wallpaper_option", s.wallpaperOption.name)
            putFloat("wallpaper_dim", s.wallpaperDim)
            putFloat("wallpaper_blur", s.wallpaperBlur)
            putBoolean("battery_saver", s.batterySaverMode)
            putBoolean("haptics", s.hapticFeedbackEnabled)

            // Save gestures
            val gestureObj = JSONObject()
            s.gestures.forEach { (type, action) ->
                gestureObj.put(type.name, action.name)
            }
            putString("gestures_json", gestureObj.toString())
            apply()
        }
    }

    private fun loadSettings() {
        val cols = prefs.getInt("grid_cols", 4)
        val rows = prefs.getInt("grid_rows", 5)
        val dock = prefs.getInt("dock_count", 5)
        val iconPack = try {
            IconPack.valueOf(prefs.getString("icon_pack", IconPack.MINIMAL_GLYPH.name) ?: IconPack.MINIMAL_GLYPH.name)
        } catch (e: Exception) {
            IconPack.MINIMAL_GLYPH
        }
        val iconShape = try {
            IconShape.valueOf(prefs.getString("icon_shape", IconShape.SQUIRCLE.name) ?: IconShape.SQUIRCLE.name)
        } catch (e: Exception) {
            IconShape.SQUIRCLE
        }
        val iconScale = prefs.getFloat("icon_scale", 1.0f)
        val showLabels = prefs.getBoolean("show_labels", true)
        val labelFontSize = prefs.getInt("label_font_size", 11)
        val themePreset = try {
            ThemePreset.valueOf(prefs.getString("theme_preset", ThemePreset.AMOLED_DARK.name) ?: ThemePreset.AMOLED_DARK.name)
        } catch (e: Exception) {
            ThemePreset.AMOLED_DARK
        }
        val wallpaperOption = try {
            WallpaperOption.valueOf(prefs.getString("wallpaper_option", WallpaperOption.AMOLED_GEOMETRIC.name) ?: WallpaperOption.AMOLED_GEOMETRIC.name)
        } catch (e: Exception) {
            WallpaperOption.AMOLED_GEOMETRIC
        }
        val wallpaperDim = prefs.getFloat("wallpaper_dim", 0.25f)
        val wallpaperBlur = prefs.getFloat("wallpaper_blur", 0f)
        val batterySaver = prefs.getBoolean("battery_saver", false)
        val haptics = prefs.getBoolean("haptics", true)

        val gestureMap = LauncherSettings.defaultGestures().toMutableMap()
        val gestureStr = prefs.getString("gestures_json", null)
        if (gestureStr != null) {
            try {
                val json = JSONObject(gestureStr)
                for (key in json.keys()) {
                    val gType = GestureType.valueOf(key)
                    val gAction = GestureAction.valueOf(json.getString(key))
                    gestureMap[gType] = gAction
                }
            } catch (e: Exception) {
                Log.e("LauncherRepository", "Error parsing gestures json", e)
            }
        }

        _settings.value = LauncherSettings(
            gridColumns = cols,
            gridRows = rows,
            dockCount = dock,
            iconPack = iconPack,
            iconShape = iconShape,
            iconScale = iconScale,
            showLabels = showLabels,
            labelFontSizeSp = labelFontSize,
            themePreset = themePreset,
            wallpaperOption = wallpaperOption,
            wallpaperDim = wallpaperDim,
            wallpaperBlur = wallpaperBlur,
            batterySaverMode = batterySaver,
            hapticFeedbackEnabled = haptics,
            gestures = gestureMap
        )
    }

    private fun saveWidgets() {
        val array = JSONArray()
        for (w in _widgets.value) {
            val obj = JSONObject().apply {
                put("id", w.id)
                put("type", w.type.name)
                put("pageIndex", w.pageIndex)
                put("orderIndex", w.orderIndex)
                put("title", w.title)
                put("styleVariant", w.styleVariant)
            }
            array.put(obj)
        }
        prefs.edit().putString("widgets_json", array.toString()).apply()
    }

    private fun loadWidgets() {
        val json = prefs.getString("widgets_json", null)
        if (json != null) {
            try {
                val list = mutableListOf<LauncherWidget>()
                val array = JSONArray(json)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        LauncherWidget(
                            id = obj.getString("id"),
                            type = WidgetType.valueOf(obj.getString("type")),
                            pageIndex = obj.getInt("pageIndex"),
                            orderIndex = obj.getInt("orderIndex"),
                            title = obj.optString("title", ""),
                            styleVariant = obj.optInt("styleVariant", 0)
                        )
                    )
                }
                _widgets.value = list
                return
            } catch (e: Exception) {
                Log.e("LauncherRepository", "Error loading widgets", e)
            }
        }

        // Default widgets on fresh install: Minimal Clock on Page 0 + Battery Monitor on Page 0
        _widgets.value = listOf(
            LauncherWidget(
                id = "default_clock",
                type = WidgetType.CLOCK_MINIMAL,
                pageIndex = 0,
                orderIndex = 0
            ),
            LauncherWidget(
                id = "default_battery",
                type = WidgetType.BATTERY_MONITOR,
                pageIndex = 0,
                orderIndex = 1
            )
        )
    }

    private fun saveHomeScreenItems() {
        val array = JSONArray()
        for (item in _homeScreenItems.value) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("pageIndex", item.pageIndex)
                put("gridRow", item.gridRow)
                put("gridCol", item.gridCol)
                put("packageName", item.packageName)
            }
            array.put(obj)
        }
        prefs.edit().putString("home_screen_items_json", array.toString()).apply()
    }

    private fun loadHomeScreenItems() {
        val json = prefs.getString("home_screen_items_json", null) ?: return
        try {
            val list = mutableListOf<HomeScreenItem>()
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    HomeScreenItem(
                        id = obj.getString("id"),
                        pageIndex = obj.getInt("pageIndex"),
                        gridRow = obj.getInt("gridRow"),
                        gridCol = obj.getInt("gridCol"),
                        packageName = obj.getString("packageName")
                    )
                )
            }
            _homeScreenItems.value = list
        } catch (e: Exception) {
            Log.e("LauncherRepository", "Error parsing home screen items", e)
        }
    }

    private fun saveAppOverride(pkg: String) {
        val app = _installedApps.value.find { it.packageName == pkg } ?: return
        val currentJson = prefs.getString("app_overrides_json", "{}") ?: "{}"
        try {
            val root = JSONObject(currentJson)
            val appObj = JSONObject().apply {
                put("customLabel", app.customLabel ?: "")
                put("customGlyph", app.customGlyph ?: "")
                put("accentColorHex", app.accentColorHex ?: "")
                put("launchCount", app.launchCount)
                put("isPinnedToDock", app.isPinnedToDock)
                put("isHidden", app.isHidden)
            }
            root.put(pkg, appObj)
            prefs.edit().putString("app_overrides_json", root.toString()).apply()
        } catch (e: Exception) {
            Log.e("LauncherRepository", "Error saving app override", e)
        }
    }

    private fun loadCustomAppOverrides(): Map<String, JSONObject> {
        val map = mutableMapOf<String, JSONObject>()
        val json = prefs.getString("app_overrides_json", null) ?: return map
        try {
            val obj = JSONObject(json)
            for (key in obj.keys()) {
                map[key] = obj.getJSONObject(key)
            }
        } catch (e: Exception) {
            Log.e("LauncherRepository", "Error loading app overrides", e)
        }
        return map
    }
}
