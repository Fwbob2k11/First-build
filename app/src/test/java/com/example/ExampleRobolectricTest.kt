package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.DeviceTelemetryRepository
import com.example.data.formatBytes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read app name from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("PulseCore", appName)
  }

  @Test
  fun `device telemetry repository builds valid profile and formats bytes`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = DeviceTelemetryRepository(context)
    val profile = repo.getDeviceProfile()
    assertTrue(profile.brandDisplay.isNotBlank())
    assertTrue(profile.cpuCores >= 1)
    assertEquals("1.00 GB", formatBytes(1024L * 1024L * 1024L))
  }
}
