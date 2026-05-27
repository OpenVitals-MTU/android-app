package tech.mmarca.openvitals.features.manualentry

import android.net.Uri
import androidx.health.connect.client.records.ExerciseSessionRecord
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlin.math.abs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.core.preferences.UnitSystem
import tech.mmarca.openvitals.data.model.ActivityWriteRequest
import tech.mmarca.openvitals.data.model.ExerciseRoutePoint
import tech.mmarca.openvitals.data.repository.ActivityRepository
import tech.mmarca.openvitals.util.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class ActivityEntryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test fun `buildWriteRequest converts metric distance and trims text`() {
        val state = ActivityEntryUiState(
            selectedActivityType = DefaultActivityEntryTypes.first(),
            titleText = "  Morning run  ",
            notesText = "  Easy effort  ",
            startDateText = "2026-05-26",
            startTimeText = "8:30",
            durationMinutesText = "45",
            distanceText = "10.5",
        )

        val request = buildWriteRequest(state, UnitSystem.METRIC)

        requireNotNull(request)
        assertEquals(ExerciseSessionRecord.EXERCISE_TYPE_RUNNING, request.exerciseType)
        assertEquals("Morning run", request.title)
        assertEquals("Easy effort", request.notes)
        assertEquals(10_500.0, request.distanceMeters ?: 0.0, 0.001)
        assertTrue(request.startTime.isBefore(request.endTime))
    }

    @Test fun `buildWriteRequest rejects total calories below active calories`() {
        val state = ActivityEntryUiState(
            startDateText = "2026-05-26",
            startTimeText = "8:30",
            durationMinutesText = "45",
            activeCaloriesText = "500",
            totalCaloriesText = "300",
        )

        assertNull(buildWriteRequest(state, UnitSystem.METRIC))
    }

    @Test fun `validateActivityEntry returns field specific errors`() {
        val state = ActivityEntryUiState(
            startDateText = "",
            startTimeText = "25:99",
            durationMinutesText = "0",
            distanceText = "-1",
            activeCaloriesText = "abc",
            totalCaloriesText = "0",
        )

        val errors = validateActivityEntry(state, UnitSystem.METRIC)

        assertTrue(ActivityEntryValidationError.START_DATE_INVALID in errors)
        assertTrue(ActivityEntryValidationError.START_TIME_INVALID in errors)
        assertTrue(ActivityEntryValidationError.DURATION_INVALID in errors)
        assertTrue(ActivityEntryValidationError.DISTANCE_INVALID in errors)
        assertTrue(ActivityEntryValidationError.ACTIVE_CALORIES_INVALID in errors)
        assertTrue(ActivityEntryValidationError.TOTAL_CALORIES_INVALID in errors)
    }

    @Test fun `activity entry exposes field errors and skips write for invalid values`() = runTest {
        val repo = activityRepo(canWrite = true)
        val vm = ActivityEntryViewModel(
            repository = repo,
            clock = Clock.fixed(Instant.parse("2026-05-26T08:30:00Z"), ZoneId.of("UTC")),
        )
        advanceUntilIdle()

        vm.startManualEntry()
        vm.updateDurationMinutes("0")
        vm.updateDistance("-1")
        vm.addEntry(UnitSystem.METRIC)
        advanceUntilIdle()

        assertEquals(ActivityEntryError.INVALID_VALUE, vm.uiState.value.entryError)
        assertTrue(ActivityEntryValidationError.DURATION_INVALID in vm.uiState.value.validationErrors)
        assertTrue(ActivityEntryValidationError.DISTANCE_INVALID in vm.uiState.value.validationErrors)
        coVerify(exactly = 0) { repo.writeActivityEntry(any()) }
    }

    @Test fun `buildWriteRequest uses imported route distance and adjusts end after last point`() {
        val start = Instant.parse("2026-05-26T08:30:00Z")
        val last = Instant.parse("2026-05-26T09:00:00Z")
        val route = RouteFileImport(
            fileName = "run.gpx",
            points = listOf(routePoint(start), routePoint(last, latitude = 59.01)),
            distanceMeters = 1200.0,
            elevationGainedMeters = 12.0,
            startTime = start,
            endTime = last,
        )
        val state = ActivityEntryUiState(
            startDateText = start.atZone(ZoneId.systemDefault()).toLocalDate().toString(),
            startTimeText = start.atZone(ZoneId.systemDefault()).toLocalTime().let { "${it.hour}:${it.minute.toString().padStart(2, '0')}" },
            durationMinutesText = "30",
            importedRoute = route,
        )

        val request = buildWriteRequest(state, UnitSystem.METRIC)

        requireNotNull(request)
        assertEquals(2, request.routePoints.size)
        assertEquals(1200.0, request.distanceMeters ?: 0.0, 0.001)
        assertEquals(12.0, request.elevationGainedMeters ?: 0.0, 0.001)
        assertTrue(last.isBefore(request.endTime))
    }

    @Test fun `buildWriteRequest retimes imported route without recorded timestamps`() {
        val originalStart = Instant.EPOCH
        val originalLast = Instant.EPOCH.plusSeconds(20)
        val route = RouteFileImport(
            fileName = "route.kml",
            points = listOf(routePoint(originalStart), routePoint(originalLast, latitude = 59.01)),
            distanceMeters = 1200.0,
            elevationGainedMeters = 12.0,
            startTime = originalStart,
            endTime = originalLast,
            hasRecordedTimestamps = false,
            hasImportedTimeRange = false,
        )
        val state = ActivityEntryUiState(
            startDateText = "2026-05-26",
            startTimeText = "8:30",
            durationMinutesText = "30",
            importedRoute = route,
        )

        val request = buildWriteRequest(state, UnitSystem.METRIC)

        requireNotNull(request)
        val expectedStart = java.time.LocalDateTime.of(
            java.time.LocalDate.parse("2026-05-26"),
            java.time.LocalTime.parse("8:30", java.time.format.DateTimeFormatter.ofPattern("H:mm")),
        ).atZone(ZoneId.systemDefault()).toInstant()
        assertEquals(expectedStart, request.routePoints.first().time)
        assertTrue(request.routePoints.last().time.isBefore(request.endTime))
        assertTrue(request.routePoints.first().time != originalStart)
    }

    @Test fun `missing activity write permission prevents write`() = runTest {
        val repo = activityRepo(canWrite = false)
        val vm = ActivityEntryViewModel(
            repository = repo,
            clock = Clock.fixed(Instant.parse("2026-05-26T08:30:00Z"), ZoneId.of("UTC")),
        )
        advanceUntilIdle()

        vm.startManualEntry()
        advanceUntilIdle()
        vm.addEntry(UnitSystem.METRIC)
        advanceUntilIdle()

        assertEquals(ActivityEntryError.MISSING_WRITE_PERMISSION, vm.uiState.value.entryError)
        coVerify(exactly = 0) { repo.writeActivityEntry(any()) }
    }

    @Test fun `activity entry writes request when permission is granted`() = runTest {
        val repo = activityRepo(canWrite = true)
        val vm = ActivityEntryViewModel(
            repository = repo,
            clock = Clock.fixed(Instant.parse("2026-05-26T08:30:00Z"), ZoneId.of("UTC")),
        )
        advanceUntilIdle()

        vm.startManualEntry()
        advanceUntilIdle()
        vm.updateDistance("5")
        vm.refreshPermission()
        advanceUntilIdle()
        vm.addEntry(UnitSystem.METRIC)
        advanceUntilIdle()

        coVerify {
            repo.writeActivityEntry(match<ActivityWriteRequest> { request ->
                abs((request.distanceMeters ?: 0.0) - 5000.0) < 0.001
            })
        }
        assertFalse(vm.uiState.value.isSavingEntry)
    }

    @Test fun `activity entry keeps full write permissions when optional fields change`() = runTest {
        val repo = activityRepo(canWrite = true)
        val vm = ActivityEntryViewModel(
            repository = repo,
            clock = Clock.fixed(Instant.parse("2026-05-26T08:30:00Z"), ZoneId.of("UTC")),
        )
        advanceUntilIdle()

        vm.startManualEntry()
        advanceUntilIdle()
        vm.updateDistance("5")
        vm.updateElevation("20")
        vm.updateActiveCalories("300")
        vm.updateTotalCalories("350")

        assertEquals(ActivityWritePermissions, vm.uiState.value.writePermissions)
        assertTrue(vm.uiState.value.canWrite)
    }

    @Test fun `route import fills distance and elevation fields in current unit system`() = runTest {
        val repo = activityRepo(canWrite = true)
        val importer = mockk<RouteFileImporter>()
        val uri = mockk<Uri>()
        val start = Instant.parse("2026-05-26T08:30:00Z")
        val last = Instant.parse("2026-05-26T08:40:00Z")
        coEvery { importer.import(uri) } returns RouteFileImport(
            fileName = "run.kmz",
            points = listOf(routePoint(start), routePoint(last, latitude = 59.01)),
            distanceMeters = 0.4 * 1609.344,
            elevationGainedMeters = 12.0 * 0.3048,
            startTime = start,
            endTime = last,
        )
        val vm = ActivityEntryViewModel(
            repository = repo,
            routeFileImporter = importer,
            clock = Clock.fixed(start, ZoneId.of("UTC")),
        )
        advanceUntilIdle()

        vm.importRouteFile(uri, UnitSystem.IMPERIAL)
        advanceUntilIdle()

        assertEquals(ActivityEntryMode.ROUTE_IMPORT, vm.uiState.value.mode)
        assertEquals("0.4", vm.uiState.value.distanceText)
        assertEquals("12", vm.uiState.value.elevationText)
        assertEquals("11", vm.uiState.value.durationMinutesText)
    }

    private fun activityRepo(canWrite: Boolean): ActivityRepository =
        mockk<ActivityRepository>().also { repo ->
            every { repo.activityWritePermissions() } returns ActivityWritePermissions
            every { repo.activityWritePermissions(any(), any(), any(), any(), any()) } returns ActivityWritePermissions
            every { repo.activityWritePermissions(any<ActivityWriteRequest>()) } returns ActivityWritePermissions
            coEvery { repo.hasActivityWritePermission() } returns canWrite
            coEvery { repo.hasActivityWritePermission(any(), any(), any(), any(), any()) } returns canWrite
            coEvery { repo.writeActivityEntry(any()) } returns "activity-id"
        }

    private fun routePoint(
        time: Instant,
        latitude: Double = 59.0,
        longitude: Double = 24.0,
    ): ExerciseRoutePoint =
        ExerciseRoutePoint(
            time = time,
            latitude = latitude,
            longitude = longitude,
            altitudeMeters = 10.0,
            horizontalAccuracyMeters = null,
            verticalAccuracyMeters = null,
        )

    private companion object {
        private val ActivityWritePermissions = setOf(
            "write_activity",
            "write_route",
            "write_distance",
            "write_elevation",
            "write_active_calories",
            "write_total_calories",
        )
    }
}
