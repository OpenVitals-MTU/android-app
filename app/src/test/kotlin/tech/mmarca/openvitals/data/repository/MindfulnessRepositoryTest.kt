package tech.mmarca.openvitals.data.repository

import android.util.Log
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.domain.model.MindfulnessSession
import tech.mmarca.openvitals.healthconnect.HealthConnectManager

class MindfulnessRepositoryTest {

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.w(any(), any<String>()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `write permissions come from Health Connect feature support`() {
        val hc = mockk<HealthConnectManager>().also { hc ->
            every { hc.mindfulnessWritePermissions } returns emptySet()
        }

        assertEquals(emptySet<String>(), MindfulnessRepositoryImpl(hc).mindfulnessWritePermissions)
    }

    @Test
    fun `loadMindfulnessSessions skips Health Connect when feature permissions are unavailable`() = runTest {
        val hc = hc(
            mindfulnessPermissions = emptySet(),
            grantedPermissions = emptySet(),
        )

        val result = MindfulnessRepositoryImpl(hc).loadMindfulnessSessions(
            LocalDate.of(2026, 7, 1),
            LocalDate.of(2026, 7, 1),
        )

        assertEquals(emptyList<MindfulnessSession>(), result)
        coVerify(exactly = 0) { hc.readMindfulnessSessions(any(), any()) }
    }

    @Test
    fun `loadMindfulnessSessions reads when mindfulness permission is granted`() = runTest {
        val permission = "read_mindfulness"
        val sessions = listOf(
            MindfulnessSession(
                id = "session-id",
                title = "Meditation",
                startTime = Instant.parse("2026-07-01T08:00:00Z"),
                endTime = Instant.parse("2026-07-01T08:10:00Z"),
                durationMs = 600_000,
                source = "test.source",
            )
        )
        val hc = hc(
            mindfulnessPermissions = setOf(permission),
            grantedPermissions = setOf(permission),
            sessions = sessions,
        )

        val result = MindfulnessRepositoryImpl(hc).loadMindfulnessSessions(
            LocalDate.of(2026, 7, 1),
            LocalDate.of(2026, 7, 1),
        )

        assertEquals(sessions, result)
        coVerify(exactly = 1) { hc.readMindfulnessSessions(any(), any()) }
    }

    private fun hc(
        mindfulnessPermissions: Set<String>,
        grantedPermissions: Set<String>,
        sessions: List<MindfulnessSession> = emptyList(),
    ): HealthConnectManager =
        mockk<HealthConnectManager>().also { hc ->
            every { hc.availability() } returns HealthConnectAvailability.AVAILABLE
            every { hc.mindfulnessPermissions } returns mindfulnessPermissions
            coEvery { hc.grantedPermissions() } returns grantedPermissions
            coEvery { hc.readMindfulnessSessions(any(), any()) } returns sessions
        }
}
