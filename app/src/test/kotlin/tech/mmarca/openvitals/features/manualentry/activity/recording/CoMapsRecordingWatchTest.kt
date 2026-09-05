package tech.mmarca.openvitals.features.manualentry.activity.recording

import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.comaps.CoMapsGuidanceFeed
import tech.mmarca.openvitals.data.repository.contract.CoMapsNavigationRepository
import tech.mmarca.openvitals.domain.model.CoMapsNavigationSnapshot
import tech.mmarca.openvitals.domain.model.CoMapsNavigationState
import tech.mmarca.openvitals.domain.model.CoMapsRoutePolyline
import tech.mmarca.openvitals.util.MainDispatcherRule

/**
 * Guidance on a watch and guidance in a recording each answer to their own switch.
 * Either keeps the shared feed up, and neither can make the other show anything.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CoMapsRecordingWatchTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val provider = MutableSharedFlow<CoMapsNavigationState>(replay = 1)

    private class FakeRepository(private val provider: Flow<CoMapsNavigationState>) :
        CoMapsNavigationRepository {
        /** How many subscriptions the feed is holding — must never exceed one. */
        var subscriptions = 0

        override suspend fun readLive(): CoMapsNavigationState = CoMapsNavigationState.NotNavigating

        override fun watchLive(): Flow<CoMapsNavigationState> = flow {
            subscriptions++
            try {
                provider.collect { emit(it) }
            } finally {
                subscriptions--
            }
        }

        override suspend fun readRouteGeometry(revision: Int): CoMapsRoutePolyline? = null

        override fun permissionName(): String? = null

        override fun hasPermission(): Boolean = true

        override fun onPermissionChanged() = Unit

        override fun canLaunchCoMaps(): Boolean = false

        override fun launchForPlanning(latitude: Double?, longitude: Double?): Boolean = false

        override fun saveSamples(activityId: String, samples: List<CoMapsNavigationSnapshot>) = Unit

        override fun loadSamples(activityId: String): List<CoMapsNavigationSnapshot> = emptyList()

        override fun deleteSamples(activityId: String) = Unit
    }

    private fun guidance(street: String) = CoMapsNavigationState.Active(
        snapshot = CoMapsNavigationSnapshot(
            sampledAt = Instant.parse("2026-08-20T10:00:00Z"),
            sessionState = "OnRoute",
            nextStreet = street,
            distanceToTurn = "200 m",
        ),
    )

    private val idle = ActivityRecordingState()

    private val gpsRecording = ActivityRecordingState(
        status = ActivityRecordingStatus.RECORDING,
        recordingKind = ActivityRecordingKind.GPS_ROUTE,
        startTime = Instant.parse("2026-08-20T09:00:00Z"),
    )

    private class Fixture(
        val repository: FakeRepository,
        val feed: CoMapsGuidanceFeed,
        val watch: CoMapsRecordingWatch,
        val scope: CoroutineScope,
    )

    private fun fixture(
        recordingIntegrationOn: Boolean,
        savingOn: Boolean = true,
    ): Fixture {
        val repository = FakeRepository(provider)
        val feed = CoMapsGuidanceFeed(repository)
        val scope = CoroutineScope(UnconfinedTestDispatcher(mainDispatcherRule.testDispatcher.scheduler))
        return Fixture(
            repository = repository,
            feed = feed,
            watch = CoMapsRecordingWatch(
                repository = repository,
                feed = feed,
                scope = scope,
                isEnabled = { recordingIntegrationOn },
                isSavingEnabled = { savingOn },
            ),
            scope = scope,
        )
    }

    @Test
    fun `a watch alone reaches the wrist and nothing else`() = runTest {
        val fixture = fixture(recordingIntegrationOn = false)
        fixture.watch.sync(idle)
        assertEquals(0, fixture.repository.subscriptions)

        fixture.feed.request(CoMapsGuidanceFeed.Reason.WATCH, true)
        provider.emit(guidance("Kalevala"))

        assertEquals(1, fixture.repository.subscriptions)
        // The vendor layer sees it...
        assertEquals(
            "Kalevala",
            (fixture.feed.guidance.value as CoMapsNavigationState.Active).snapshot.nextStreet,
        )
        // ...and the recording screen does not, because nobody asked it to.
        assertTrue(fixture.watch.navigation.value is CoMapsNavigationState.Disabled)
        assertTrue(fixture.watch.samples().isEmpty())

        fixture.scope.cancel()
    }

    @Test
    fun `a recording alone still shows and still banks`() = runTest {
        val fixture = fixture(recordingIntegrationOn = true)

        fixture.watch.sync(gpsRecording)
        provider.emit(guidance("Mannerheimintie"))

        assertEquals(1, fixture.repository.subscriptions)
        assertTrue(fixture.watch.navigation.value is CoMapsNavigationState.Active)
        assertEquals(1, fixture.watch.samples().size)
        assertEquals("Mannerheimintie", fixture.watch.samples().first().nextStreet)

        fixture.scope.cancel()
    }

    @Test
    fun `both on share one subscription and both see it`() = runTest {
        val fixture = fixture(recordingIntegrationOn = true)

        fixture.feed.request(CoMapsGuidanceFeed.Reason.WATCH, true)
        fixture.watch.sync(gpsRecording)
        provider.emit(guidance("Aleksanterinkatu"))

        assertEquals(1, fixture.repository.subscriptions)
        assertTrue(fixture.feed.guidance.value is CoMapsNavigationState.Active)
        assertTrue(fixture.watch.navigation.value is CoMapsNavigationState.Active)

        // The recording ends. The wrist keeps the feed; the recording view goes dark.
        fixture.watch.sync(idle)
        assertEquals(1, fixture.repository.subscriptions)
        assertTrue(fixture.feed.guidance.value is CoMapsNavigationState.Active)
        assertTrue(fixture.watch.navigation.value is CoMapsNavigationState.Disabled)
        assertTrue(fixture.watch.samples().isEmpty())

        // Guidance arriving afterwards is the wrist's alone.
        provider.emit(guidance("Esplanadi"))
        assertTrue(fixture.watch.navigation.value is CoMapsNavigationState.Disabled)
        assertTrue(fixture.watch.samples().isEmpty())

        fixture.scope.cancel()
    }

    @Test
    fun `the last one to let go takes the feed down`() = runTest {
        val fixture = fixture(recordingIntegrationOn = true)

        fixture.feed.request(CoMapsGuidanceFeed.Reason.WATCH, true)
        fixture.watch.sync(gpsRecording)
        provider.emit(guidance("Bulevardi"))
        assertEquals(1, fixture.repository.subscriptions)

        fixture.feed.request(CoMapsGuidanceFeed.Reason.WATCH, false)
        assertEquals(1, fixture.repository.subscriptions)
        assertTrue(fixture.watch.navigation.value is CoMapsNavigationState.Active)

        fixture.watch.sync(idle)
        assertEquals(0, fixture.repository.subscriptions)
        assertTrue(fixture.feed.guidance.value is CoMapsNavigationState.Disabled)
        assertTrue(fixture.watch.navigation.value is CoMapsNavigationState.Disabled)

        fixture.scope.cancel()
    }
}
