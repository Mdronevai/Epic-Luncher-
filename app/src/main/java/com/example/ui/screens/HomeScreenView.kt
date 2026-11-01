package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppInfo
import com.example.model.GestureType
import com.example.model.HomeScreenItem
import com.example.model.LauncherWidget
import com.example.model.WidgetType
import com.example.ui.components.AppIconItem
import com.example.ui.components.BatteryMonitorWidget
import com.example.ui.components.DailyFocusWidget
import com.example.ui.components.MediaMinimalWidget
import com.example.ui.components.MinimalClockWidget
import com.example.ui.components.QuickSearchWidget
import com.example.ui.components.WeatherGlanceWidget
import com.example.ui.components.WidgetCardContainer
import com.example.ui.gestures.launcherGestureHandler
import com.example.viewmodel.LauncherScreen
import com.example.viewmodel.LauncherViewModel

@Composable
fun HomeScreenView(
    viewModel: LauncherViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val isEditMode by viewModel.isEditMode.collectAsState()
    val widgets by viewModel.widgets.collectAsState()
    val homeItems by viewModel.homeScreenItems.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()
    val dockApps by viewModel.dockApps.collectAsState()
    val batteryTelemetry by viewModel.batteryTelemetry.collectAsState()

    val pageCount = 2
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { pageCount })

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            viewModel.setHomePageIndex(page)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .launcherGestureHandler(
                onGesture = { gestureType ->
                    viewModel.handleGesture(gestureType, context)
                },
                onDoubleTap = {
                    viewModel.handleGesture(GestureType.DOUBLE_TAP, context)
                },
                onLongPress = {
                    viewModel.setEditMode(!isEditMode)
                }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top Action Header (Status, Edit mode banner, and Quick Launchers)
            HomeTopBar(
                isEditMode = isEditMode,
                isBatterySaver = settings.batterySaverMode,
                onToggleEditMode = { viewModel.setEditMode(!isEditMode) },
                onOpenSearch = { viewModel.navigateTo(LauncherScreen.APP_DRAWER) },
                onAddWidget = { viewModel.openWidgetPicker() },
                onOpenSettings = { viewModel.navigateTo(LauncherScreen.SETTINGS) },
                onOpenBatteryCenter = { viewModel.navigateTo(LauncherScreen.BATTERY_CENTER) }
            )

            // Horizontal Pager for Home Pages
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                val pageWidgets = widgets.filter { it.pageIndex == page }
                val pageItems = homeItems.filter { it.pageIndex == page }

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Widgets Section on this page
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        for (w in pageWidgets) {
                            WidgetCardContainer(
                                widget = w,
                                isEditMode = isEditMode,
                                onCycleStyle = { viewModel.cycleWidgetStyle(w.id) },
                                onRemove = { viewModel.removeWidget(w.id) }
                            ) {
                                when (w.type) {
                                    WidgetType.CLOCK_MINIMAL -> {
                                        MinimalClockWidget(
                                            styleVariant = w.styleVariant,
                                            batteryPct = batteryTelemetry.percentage,
                                            onClick = {
                                                if (isEditMode) viewModel.cycleWidgetStyle(w.id)
                                                else viewModel.navigateTo(LauncherScreen.SETTINGS)
                                            }
                                        )
                                    }
                                    WidgetType.BATTERY_MONITOR -> {
                                        BatteryMonitorWidget(
                                            telemetry = batteryTelemetry,
                                            isBatterySaverActive = settings.batterySaverMode,
                                            onToggleBatterySaver = { viewModel.toggleBatterySaver() },
                                            onOpenDetails = { viewModel.navigateTo(LauncherScreen.BATTERY_CENTER) }
                                        )
                                    }
                                    WidgetType.QUICK_SEARCH -> {
                                        QuickSearchWidget(
                                            onSearchClick = { viewModel.navigateTo(LauncherScreen.APP_DRAWER) },
                                            onVoiceClick = { viewModel.navigateTo(LauncherScreen.APP_DRAWER) },
                                            onSettingsClick = { viewModel.navigateTo(LauncherScreen.SETTINGS) }
                                        )
                                    }
                                    WidgetType.WEATHER_GLANCE -> {
                                        WeatherGlanceWidget()
                                    }
                                    WidgetType.MEDIA_MINIMAL -> {
                                        MediaMinimalWidget()
                                    }
                                    WidgetType.DAILY_FOCUS -> {
                                        DailyFocusWidget()
                                    }
                                }
                            }
                        }

                        if (isEditMode && pageWidgets.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 8.dp)
                                    .clickable { viewModel.openWidgetPicker() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Add Minimalist Widget",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    // Home Screen App Grid
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(settings.gridColumns),
                            contentPadding = PaddingValues(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(pageItems, key = { it.id }) { item ->
                                val app = installedApps.find { it.packageName == item.packageName }
                                if (app != null) {
                                    AppIconItem(
                                        app = app,
                                        iconPack = settings.iconPack,
                                        iconShape = settings.iconShape,
                                        iconScale = settings.iconScale,
                                        showLabel = settings.showLabels,
                                        labelFontSizeSp = settings.labelFontSizeSp,
                                        isEditMode = isEditMode,
                                        onClick = {
                                            if (isEditMode) {
                                                viewModel.removeHomeScreenItem(item.id)
                                            } else {
                                                viewModel.launchApp(context, app)
                                            }
                                        },
                                        onLongClick = {
                                            viewModel.openAppEditor(app)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Page Indicator Dots
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                repeat(pageCount) { iteration ->
                    val isSelected = pagerState.currentPage == iteration
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (isSelected) 8.dp else 5.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                    )
                }
            }

            // Dock & Drawer Swipe Pill
            HomeDock(
                dockApps = dockApps,
                settings = settings,
                isEditMode = isEditMode,
                onAppClick = { app -> viewModel.launchApp(context, app) },
                onAppLongClick = { app -> viewModel.openAppEditor(app) },
                onOpenDrawer = { viewModel.navigateTo(LauncherScreen.APP_DRAWER) }
            )
        }
    }
}

@Composable
fun HomeTopBar(
    isEditMode: Boolean,
    isBatterySaver: Boolean,
    onToggleEditMode: () -> Unit,
    onOpenSearch: () -> Unit,
    onAddWidget: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenBatteryCenter: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        if (isEditMode) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "EDIT MODE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.width(10.dp))
                IconButton(
                    onClick = onAddWidget,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .testTag("add_widget_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Widgets,
                        contentDescription = "Add Widget",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onOpenBatteryCenter)
                    .background(
                        if (isBatterySaver) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (isBatterySaver) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isBatterySaver) "AURA SAVER" else "AURA LAUNCHER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isEditMode) {
                IconButton(
                    onClick = onToggleEditMode,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Done Editing",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                IconButton(
                    onClick = onOpenSearch,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Open Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onToggleEditMode,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Home Screen",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(19.dp)
                    )
                }
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun HomeDock(
    dockApps: List<AppInfo>,
    settings: com.example.model.LauncherSettings,
    isEditMode: Boolean,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    onOpenDrawer: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        // App Drawer Swipe handle pill
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier
                .padding(bottom = 6.dp)
                .clickable(onClick = onOpenDrawer)
                .testTag("drawer_handle")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Open App Drawer",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "SWIPE UP",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Dock background container
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                for (app in dockApps) {
                    AppIconItem(
                        app = app,
                        iconPack = settings.iconPack,
                        iconShape = settings.iconShape,
                        iconScale = settings.iconScale * 0.95f,
                        showLabel = false,
                        isEditMode = isEditMode,
                        onClick = { onAppClick(app) },
                        onLongClick = { onAppLongClick(app) }
                    )
                }
            }
        }
    }
}
