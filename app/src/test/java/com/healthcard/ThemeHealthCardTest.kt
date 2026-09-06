package com.healthcard

import android.app.Activity
import androidx.test.core.app.ActivityScenario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ThemeHealthCardTest {

    @Test
    fun mainActivity_window_statusBarColor_is_brand_blue() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity: Activity ->
                val color = activity.window.statusBarColor
                assertEquals(
                    "MainActivity.window.statusBarColor should be brand blue 0xFF2196F3",
                    0xFF2196F3.toInt(),
                    color,
                )
                assertNotEquals(
                    "MainActivity.window.statusBarColor must not be template purple 0xFF3700B3",
                    0xFF3700B3.toInt(),
                    color,
                )
            }
        }
    }
}
