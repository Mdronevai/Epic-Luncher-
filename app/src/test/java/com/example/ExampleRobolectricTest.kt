package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.GestureAction
import com.example.model.GestureType
import com.example.model.IconPack
import com.example.model.LauncherSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read app name string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Aura Launcher", appName)
  }

  @Test
  fun `launcher settings default gestures initialized`() {
    val settings = LauncherSettings()
    assertEquals(IconPack.MINIMAL_GLYPH, settings.iconPack)
    assertEquals(4, settings.gridColumns)
    assertEquals(5, settings.gridRows)
    assertEquals(GestureAction.OPEN_DRAWER, settings.gestures[GestureType.SWIPE_UP])
    assertEquals(GestureAction.EXPAND_NOTIFICATIONS, settings.gestures[GestureType.SWIPE_DOWN])
  }
}
