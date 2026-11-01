package com.example.model

enum class AppCategory(val displayName: String) {
    ALL("All"),
    UTILITIES("Utilities"),
    SOCIAL("Social"),
    PRODUCTIVITY("Productivity"),
    MEDIA("Media"),
    TOOLS("Tools"),
    GAMES("Games"),
    SYSTEM("System"),
    HIDDEN("Hidden")
}

data class AppInfo(
    val packageName: String,
    val activityName: String = "",
    val label: String,
    val customLabel: String? = null,
    val category: AppCategory = AppCategory.UTILITIES,
    val customGlyph: String? = null,
    val accentColorHex: String? = null,
    val launchCount: Int = 0,
    val isPinnedToDock: Boolean = false,
    val isHidden: Boolean = false,
    val isSystemApp: Boolean = false
) {
    val displayLabel: String
        get() = customLabel?.takeIf { it.isNotBlank() } ?: label
}

enum class WidgetType(val title: String, val description: String) {
    CLOCK_MINIMAL("Minimalist Clock", "Digital typography with date & glanceable time"),
    BATTERY_MONITOR("Battery & Efficiency Hub", "Live power drain, battery health & saver switch"),
    QUICK_SEARCH("Quick Search Bar", "Instant app lookup & global launch"),
    WEATHER_GLANCE("Weather Glance", "Minimalist temperature & forecast overview"),
    MEDIA_MINIMAL("Audio Player Glance", "Clean audio playback controls & track info"),
    DAILY_FOCUS("Daily Focus & Habit", "Clean minimalist quote & task checklist")
}

data class LauncherWidget(
    val id: String,
    val type: WidgetType,
    val pageIndex: Int = 0,
    val orderIndex: Int = 0,
    val title: String = type.title,
    val styleVariant: Int = 0
)

enum class GestureType(val label: String, val description: String) {
    SWIPE_UP("Swipe Up", "Swipe up on empty home space"),
    SWIPE_DOWN("Swipe Down", "Swipe down on empty home space"),
    DOUBLE_TAP("Double Tap", "Double tap on blank home area"),
    PINCH_IN("Pinch In", "Pinch inward with two fingers"),
    PINCH_OUT("Pinch Out", "Pinch outward with two fingers"),
    TWO_FINGER_SWIPE_UP("2-Finger Swipe Up", "Swipe up with two fingers"),
    TWO_FINGER_SWIPE_DOWN("2-Finger Swipe Down", "Swipe down with two fingers"),
    LONG_PRESS("Long Press", "Hold down on home screen wallpaper")
}

enum class GestureAction(val label: String, val iconName: String) {
    OPEN_DRAWER("Open App Drawer", "Apps"),
    EXPAND_NOTIFICATIONS("Expand Notifications", "Notifications"),
    QUICK_SEARCH("Open Quick Search", "Search"),
    SCREEN_SLEEP("Lock / Dim Screen", "Bedtime"),
    BATTERY_SAVER_TOGGLE("Toggle Battery Saver", "BatteryChargingFull"),
    OPEN_SETTINGS("Open Launcher Settings", "Tune"),
    HIDDEN_APPS_VAULT("Hidden Apps Vault", "Lock"),
    NONE("Do Nothing", "Block")
}

enum class IconPack(val title: String, val subtitle: String) {
    MINIMAL_GLYPH("Minimal Glyph", "Pure minimalist vector line art"),
    MONOCHROME("Monochrome Ink", "High-contrast stark black & white"),
    NEON_OUTLINE("Cyber Neon", "Luminescent vibrant neon outlines"),
    PASTEL_SQUIRCLE("Pastel Soft", "Muted calming pastel colors"),
    MATERIAL_YOU("Dynamic M3", "Adaptive tonal system colors"),
    SYSTEM_STOCK("Device Native", "Default Android application icons")
}

enum class IconShape(val label: String) {
    SQUIRCLE("Squircle"),
    CIRCLE("Circle"),
    ROUNDED_SQUARE("Rounded Square"),
    HEXAGON("Hexagon"),
    TEARDROP("Teardrop")
}

enum class ThemePreset(
    val title: String,
    val primaryHex: Long,
    val secondaryHex: Long,
    val surfaceHex: Long,
    val backgroundHex: Long
) {
    AMOLED_DARK("AMOLED Pure Black", 0xFF00E5FF, 0xFF76FF03, 0xFF12141A, 0xFF000000),
    CYBER_NEON("Cyberpunk Neon", 0xFF00F0FF, 0xFFFF007F, 0xFF101222, 0xFF080914),
    NORD_FROST("Nordic Frost", 0xFF88C0D0, 0xFF81A1C1, 0xFF2E3440, 0xFF1E222A),
    MONOCHROME_INK("Minimal Ink", 0xFFFFFFFF, 0xFFAAAAAA, 0xFF181818, 0xFF0C0C0C),
    SUNSET_AMBER("Sunset Amber", 0xFFFF9100, 0xFFFFD180, 0xFF221712, 0xFF0E0B09),
    PASTEL_DREAM("Pastel Dream", 0xFFB39DDB, 0xFF80CBC4, 0xFF1C1A24, 0xFF110F17)
}

enum class WallpaperOption(val title: String, val isDark: Boolean) {
    AMOLED_GEOMETRIC("AMOLED Geometric (Generated)", true),
    SUBTLE_GRADIENT("Minimal Deep Gradient", true),
    PURE_BLACK("OLED True Black (Max Battery)", true),
    MESH_DARK("Dark Aurora Mesh", true),
    SYSTEM_DEFAULT("System Wallpaper / Transparent", true)
}

data class BatteryTelemetry(
    val percentage: Int = 85,
    val isCharging: Boolean = false,
    val health: String = "Good",
    val temperatureCelsius: Float = 28.5f,
    val voltageMv: Int = 4120,
    val powerSavingsTip: String = "AMOLED Black + 60Hz saves ~42% display power",
    val estimatedHoursRemaining: Float = 14.2f
)

data class HomeScreenItem(
    val id: String,
    val pageIndex: Int,
    val gridRow: Int,
    val gridCol: Int,
    val packageName: String
)

data class LauncherSettings(
    val gridColumns: Int = 4,
    val gridRows: Int = 5,
    val dockCount: Int = 5,
    val iconPack: IconPack = IconPack.MINIMAL_GLYPH,
    val iconShape: IconShape = IconShape.SQUIRCLE,
    val iconScale: Float = 1.0f,
    val showLabels: Boolean = true,
    val labelFontSizeSp: Int = 11,
    val themePreset: ThemePreset = ThemePreset.AMOLED_DARK,
    val wallpaperOption: WallpaperOption = WallpaperOption.AMOLED_GEOMETRIC,
    val wallpaperDim: Float = 0.25f,
    val wallpaperBlur: Float = 0f,
    val batterySaverMode: Boolean = false,
    val hapticFeedbackEnabled: Boolean = true,
    val gestures: Map<GestureType, GestureAction> = defaultGestures()
) {
    companion object {
        fun defaultGestures(): Map<GestureType, GestureAction> = mapOf(
            GestureType.SWIPE_UP to GestureAction.OPEN_DRAWER,
            GestureType.SWIPE_DOWN to GestureAction.EXPAND_NOTIFICATIONS,
            GestureType.DOUBLE_TAP to GestureAction.SCREEN_SLEEP,
            GestureType.PINCH_IN to GestureAction.OPEN_SETTINGS,
            GestureType.PINCH_OUT to GestureAction.QUICK_SEARCH,
            GestureType.TWO_FINGER_SWIPE_UP to GestureAction.BATTERY_SAVER_TOGGLE,
            GestureType.TWO_FINGER_SWIPE_DOWN to GestureAction.HIDDEN_APPS_VAULT,
            GestureType.LONG_PRESS to GestureAction.OPEN_SETTINGS
        )
    }
}
