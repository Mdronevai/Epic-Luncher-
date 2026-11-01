package com.example.ui.components

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.model.AppInfo
import com.example.model.IconPack
import com.example.model.IconShape

val HexagonShape: Shape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    moveTo(w * 0.5f, 0f)
    lineTo(w, h * 0.25f)
    lineTo(w, h * 0.75f)
    lineTo(w * 0.5f, h)
    lineTo(0f, h * 0.75f)
    lineTo(0f, h * 0.25f)
    close()
}

val TeardropShape: Shape = RoundedCornerShape(
    topStart = CornerSize(50),
    topEnd = CornerSize(50),
    bottomEnd = CornerSize(50),
    bottomStart = CornerSize(4)
)

fun getComposeShape(shape: IconShape): Shape {
    return when (shape) {
        IconShape.SQUIRCLE -> RoundedCornerShape(32)
        IconShape.CIRCLE -> CircleShape
        IconShape.ROUNDED_SQUARE -> RoundedCornerShape(22)
        IconShape.HEXAGON -> HexagonShape
        IconShape.TEARDROP -> TeardropShape
    }
}

fun resolveAppGlyph(app: AppInfo): ImageVector {
    if (!app.customGlyph.isNullOrBlank()) {
        return glyphFromName(app.customGlyph)
    }
    val lower = "${app.packageName} ${app.label}".lowercase()
    return when {
        lower.contains("phone") || lower.contains("dialer") -> Icons.Default.Phone
        lower.contains("message") || lower.contains("sms") || lower.contains("chat") -> Icons.Default.Chat
        lower.contains("chrome") || lower.contains("browser") || lower.contains("web") -> Icons.Default.Language
        lower.contains("camera") -> Icons.Default.CameraAlt
        lower.contains("photo") || lower.contains("gallery") -> Icons.Default.Photo
        lower.contains("mail") || lower.contains("gmail") -> Icons.Default.Email
        lower.contains("calendar") -> Icons.Default.CalendarToday
        lower.contains("map") || lower.contains("nav") -> Icons.Default.Map
        lower.contains("clock") || lower.contains("time") -> Icons.Default.AccessTime
        lower.contains("calc") -> Icons.Default.Calculate
        lower.contains("file") || lower.contains("drive") -> Icons.Default.Folder
        lower.contains("music") || lower.contains("audio") || lower.contains("spotify") -> Icons.Default.Audiotrack
        lower.contains("setting") -> Icons.Default.Settings
        lower.contains("note") || lower.contains("keep") || lower.contains("doc") -> Icons.Default.Description
        lower.contains("code") || lower.contains("dev") -> Icons.Default.Code
        lower.contains("term") || lower.contains("shell") -> Icons.Default.Terminal
        lower.contains("game") -> Icons.Default.Gamepad
        lower.contains("search") -> Icons.Default.Search
        lower.contains("lock") || lower.contains("security") -> Icons.Default.Security
        else -> Icons.Default.Widgets
    }
}

fun glyphFromName(name: String): ImageVector {
    return when (name) {
        "Phone" -> Icons.Default.Phone
        "Chat" -> Icons.Default.Chat
        "Language" -> Icons.Default.Language
        "PhotoCamera" -> Icons.Default.CameraAlt
        "Image" -> Icons.Default.Photo
        "Mail" -> Icons.Default.Email
        "CalendarToday" -> Icons.Default.CalendarToday
        "Map" -> Icons.Default.Map
        "AccessTime" -> Icons.Default.AccessTime
        "Calculate" -> Icons.Default.Calculate
        "Folder" -> Icons.Default.Folder
        "Audiotrack" -> Icons.Default.Audiotrack
        "Settings" -> Icons.Default.Settings
        "EditNote" -> Icons.Default.Description
        "Code" -> Icons.Default.Code
        "Terminal" -> Icons.Default.Terminal
        "Star" -> Icons.Default.Star
        "Security" -> Icons.Default.Security
        "Explore" -> Icons.Default.Explore
        "Gamepad" -> Icons.Default.Gamepad
        else -> Icons.Default.Widgets
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun AppIconItem(
    app: AppInfo,
    iconPack: IconPack,
    iconShape: IconShape,
    iconScale: Float = 1.0f,
    showLabel: Boolean = true,
    labelFontSizeSp: Int = 11,
    isEditMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val shape = remember(iconShape) { getComposeShape(iconShape) }
    val glyph = remember(app.packageName, app.customGlyph) { resolveAppGlyph(app) }
    val animatedScale by animateFloatAsState(targetValue = iconScale, label = "iconScale")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(72.dp)
            .padding(vertical = 4.dp)
            .testTag("app_item_${app.packageName}")
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(54.dp)
                .scale(animatedScale)
        ) {
            when (iconPack) {
                IconPack.MINIMAL_GLYPH -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(shape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                shape = shape
                            )
                    ) {
                        Icon(
                            imageVector = glyph,
                            contentDescription = app.displayLabel,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                IconPack.MONOCHROME -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(shape)
                            .background(Color(0xFF16181D))
                            .border(1.dp, Color(0xFFE6EDF3).copy(alpha = 0.3f), shape)
                    ) {
                        Icon(
                            imageVector = glyph,
                            contentDescription = app.displayLabel,
                            tint = Color.White,
                            modifier = Modifier.size(23.dp)
                        )
                    }
                }

                IconPack.NEON_OUTLINE -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(shape)
                            .background(Color(0xFF070B14))
                            .border(
                                width = 1.8.dp,
                                brush = Brush.linearGradient(
                                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                                ),
                                shape = shape
                            )
                    ) {
                        Icon(
                            imageVector = glyph,
                            contentDescription = app.displayLabel,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                IconPack.PASTEL_SQUIRCLE -> {
                    val pastelBg = remember(app.packageName) {
                        val colors = listOf(
                            Color(0xFF80CBC4),
                            Color(0xFFB39DDB),
                            Color(0xFFFFAB91),
                            Color(0xFF81D4FA),
                            Color(0xFFA5D6A7),
                            Color(0xFFFFF59D)
                        )
                        val idx = kotlin.math.abs(app.packageName.hashCode()) % colors.size
                        colors[idx]
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(shape)
                            .background(pastelBg)
                    ) {
                        Icon(
                            imageVector = glyph,
                            contentDescription = app.displayLabel,
                            tint = Color(0xFF1E1E24),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                IconPack.MATERIAL_YOU -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(shape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(
                            imageVector = glyph,
                            contentDescription = app.displayLabel,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                IconPack.SYSTEM_STOCK -> {
                    val systemIconBitmap = remember(app.packageName) {
                        loadSystemAppIconBitmap(context, app.packageName)
                    }
                    if (systemIconBitmap != null) {
                        Image(
                            bitmap = systemIconBitmap,
                            contentDescription = app.displayLabel,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(shape)
                        )
                    } else {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(shape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(
                                imageVector = glyph,
                                contentDescription = app.displayLabel,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            // Edit badge indicator if in edit mode
            if (isEditMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error)
                        .border(1.5.dp, Color.Black, CircleShape)
                ) {
                    Text(
                        text = "×",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }

        if (showLabel) {
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = app.displayLabel,
                fontSize = labelFontSizeSp.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(68.dp)
            )
        }
    }
}

private fun loadSystemAppIconBitmap(context: Context, packageName: String): androidx.compose.ui.graphics.ImageBitmap? {
    return try {
        val pm = context.packageManager
        val drawable: Drawable = pm.getApplicationIcon(packageName)
        val bitmap = drawable.toBitmap(width = 120, height = 120)
        bitmap.asImageBitmap()
    } catch (e: Exception) {
        null
    }
}
