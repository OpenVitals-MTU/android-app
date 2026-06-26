package tech.mmarca.openvitals.core.presentation

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class HealthConnectSourceResolverTest {
    @Test
    fun `resolve falls back to heuristic label when package is missing`() {
        val context = mockk<Context>()
        val packageManager = mockk<PackageManager>()
        every { context.packageManager } returns packageManager
        every { packageManager.getApplicationInfo("com.samsung.health", 0) } throws PackageManager.NameNotFoundException()

        val resolved = HealthConnectSourceResolver(context).resolve("com.samsung.health")

        assertEquals("Samsung Health", resolved.label)
    }

    @Test
    fun `resolve uses package manager label when available`() {
        val context = mockk<Context>()
        val packageManager = mockk<PackageManager>()
        val appInfo = ApplicationInfo()
        every { context.packageManager } returns packageManager
        every { packageManager.getApplicationInfo("com.example.tracker", 0) } returns appInfo
        every { packageManager.getApplicationLabel(appInfo) } returns "Example Tracker"
        every { packageManager.getApplicationIcon(appInfo) } returns mockk(relaxed = true)

        val resolved = HealthConnectSourceResolver(context).resolve("com.example.tracker")

        assertEquals("Example Tracker", resolved.label)
    }
}
