package tech.mmarca.openvitals.features.manualentry.activity

import tech.mmarca.openvitals.features.manualentry.*
import tech.mmarca.openvitals.features.manualentry.activity.*
import tech.mmarca.openvitals.features.manualentry.activity.recording.*
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.*
import tech.mmarca.openvitals.features.manualentry.body.*
import tech.mmarca.openvitals.features.manualentry.hydration.*
import tech.mmarca.openvitals.features.manualentry.mindfulness.*
import tech.mmarca.openvitals.features.manualentry.vitals.*



import android.net.Uri
import androidx.health.connect.client.records.ExerciseSegment
import androidx.health.connect.client.records.ExerciseSessionRecord
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlin.math.abs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.domain.model.ActivityPauseInterval
import tech.mmarca.openvitals.domain.model.ActivityWriteRequest
import tech.mmarca.openvitals.domain.model.ExerciseLapData
import tech.mmarca.openvitals.domain.model.ExerciseRoutePoint
import tech.mmarca.openvitals.data.repository.ActivityRepository
import tech.mmarca.openvitals.data.repository.PreferencesRepository
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

    @Test fun `buildWriteRequest includes recorded pause intervals inside session`() {
        val start = Instant.parse("2026-05-26T08:30:00Z")
        val pauseStart = start.plusSeconds(600)
        val pauseEnd = start.plusSeconds(900)
        val zoneStart = start.atZone(ZoneId.systemDefault())
        val state = ActivityEntryUiState(
            startDateText = zoneStart.toLocalDate().toString(),
            startTimeText = zoneStart.toLocalTime().let { "${it.hour}:${it.minute.toString().padStart(2, '0')}" },
            durationMinutesText = "45",
            recordedPauseIntervals = listOf(
                ActivityPauseInterval(
                    startTime = pauseStart,
                    endTime = pauseEnd,
                )
            ),
        )

        val request = buildWriteRequest(state, UnitSystem.METRIC)

        requireNotNull(request)
        assertEquals(1, request.pauseIntervals.size)
        assertEquals(pauseStart, request.pauseIntervals.first().startTime)
        assertEquals(pauseEnd, request.pauseIntervals.first().endTime)
    }

    @Test fun `buildWriteRequest ignores recorded GPS metadata for non GPS activity`() {
        val start = Instant.parse("2026-05-26T08:30:00Z")
        val pauseStart = start.plusSeconds(600)
        val pauseEnd = start.plusSeconds(900)
        val zoneStart = start.atZone(ZoneId.systemDefault())
        val state = ActivityEntryUiState(
            selectedActivityType = DefaultActivityEntryTypes.first {
                it.exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
            },
            startDateText = zoneStart.toLocalDate().toString(),
            startTimeText = zoneStart.toLocalTime().let { "${it.hour}:${it.minute.toString().padStart(2, '0')}" },
            durationMinutesText = "45",
            recordedPauseIntervals = listOf(
                ActivityPauseInterval(
                    startTime = pauseStart,
                    endTime = pauseEnd,
                )
            ),
            recordedLaps = listOf(
                ExerciseLapData(
                    startTime = pauseStart,
                    endTime = pauseEnd,
                    lengthMeters = 100.0,
                )
            ),
        )

        val request = buildWriteRequest(state, UnitSystem.METRIC)

        requireNotNull(request)
        assertFalse(request.routePoints.isNotEmpty())
        assertTrue(request.pauseIntervals.isEmpty())
        assertTrue(request.laps.isEmpty())
    }

    @Test fun `buildWriteRequest writes total push-ups as one set segment`() {
        val state = ActivityEntryUiState(
            selectedActivityType = DefaultActivityEntryTypes.first { it.id == "push_ups" },
            startDateText = "2026-05-26",
            startTimeText = "8:30",
            durationMinutesText = "10",
            repetitionTotalText = "25",
            recordedPauseIntervals = listOf(
                ActivityPauseInterval(
                    startTime = Instant.parse("2026-05-26T08:35:00Z"),
                    endTime = Instant.parse("2026-05-26T08:36:00Z"),
                )
            ),
            recordedLaps = listOf(
                ExerciseLapData(
                    startTime = Instant.parse("2026-05-26T08:35:00Z"),
                    endTime = Instant.parse("2026-05-26T08:36:00Z"),
                    lengthMeters = 100.0,
                )
            ),
        )

        val request = buildWriteRequest(state, UnitSystem.METRIC)

        requireNotNull(request)
        assertEquals(ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS, request.exerciseType)
        assertEquals("Push-ups", request.title)
        assertEquals(1, request.exerciseSegments.size)
        assertEquals(ExerciseSegment.EXERCISE_SEGMENT_TYPE_OTHER_WORKOUT, request.exerciseSegments.first().segmentType)
        assertEquals(25, request.exerciseSegments.first().repetitions)
        assertEquals(0, request.exerciseSegments.first().setIndex)
        assertTrue(request.pauseIntervals.isEmpty())
        assertTrue(request.laps.isEmpty())
    }

    @Test fun `buildWriteRequest writes repetition sets and rest segments`() {
        val state = ActivityEntryUiState(
            selectedActivityType = DefaultActivityEntryTypes.first { it.id == "pull_ups" },
            startDateText = "2026-05-26",
            startTimeText = "8:30",
            durationMinutesText = "5",
            repetitionMode = ActivityRepetitionEntryMode.SETS,
            repetitionSets = listOf(
                ActivityRepetitionSetInput(repetitionsText = "8", restMinutesText = "1"),
                ActivityRepetitionSetInput(repetitionsText = "6"),
            ),
        )

        val request = buildWriteRequest(state, UnitSystem.METRIC)

        requireNotNull(request)
        assertEquals(3, request.exerciseSegments.size)
        assertEquals(8, request.exerciseSegments[0].repetitions)
        assertEquals(0, request.exerciseSegments[0].setIndex)
        assertEquals(ExerciseSegment.EXERCISE_SEGMENT_TYPE_REST, request.exerciseSegments[1].segmentType)
        assertEquals(6, request.exerciseSegments[2].repetitions)
        assertEquals(1, request.exerciseSegments[2].setIndex)
    }

    @Test fun `buildWriteRequest writes treadmill steps as steps count`() {
        val state = ActivityEntryUiState(
            selectedActivityType = DefaultActivityEntryTypes.first { it.id == "treadmill" },
            startDateText = "2026-05-26",
            startTimeText = "8:30",
            durationMinutesText = "20",
            repetitionTotalText = "2400",
        )

        val request = buildWriteRequest(state, UnitSystem.METRIC)

        requireNotNull(request)
        assertEquals(ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL, request.exerciseType)
        assertEquals(2400L, request.stepsCount)
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
        assertTrue(vm.uiState.value.saveCompleted)
    }

    @Test fun `activity entry defaults to latest recorded activity when no favorite is set`() = runTest {
        val repo = activityRepo(canWrite = true)
        val vm = ActivityEntryViewModel(
            repository = repo,
            preferencesRepository = activityPrefs(
                lastActivityExerciseType = ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
            ),
            clock = Clock.fixed(Instant.parse("2026-05-26T08:30:00Z"), ZoneId.of("UTC")),
        )
        advanceUntilIdle()

        assertEquals(ExerciseSessionRecord.EXERCISE_TYPE_BIKING, vm.uiState.value.selectedActivityType.exerciseType)
    }

    @Test fun `favorite activity overrides latest recorded activity`() = runTest {
        val repo = activityRepo(canWrite = true)
        val vm = ActivityEntryViewModel(
            repository = repo,
            preferencesRepository = activityPrefs(
                favoriteActivityExerciseType = ExerciseSessionRecord.EXERCISE_TYPE_WALKING,
                lastActivityExerciseType = ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
            ),
            clock = Clock.fixed(Instant.parse("2026-05-26T08:30:00Z"), ZoneId.of("UTC")),
        )
        advanceUntilIdle()

        assertEquals(ExerciseSessionRecord.EXERCISE_TYPE_WALKING, vm.uiState.value.selectedActivityType.exerciseType)
    }

    @Test fun `manual activity entry does not estimate calories`() = runTest {
        val repo = activityRepo(canWrite = true)
        val vm = ActivityEntryViewModel(
            repository = repo,
            clock = Clock.fixed(Instant.parse("2026-05-26T08:30:00Z"), ZoneId.of("UTC")),
        )
        advanceUntilIdle()

        vm.startManualEntry()
        advanceUntilIdle()

        assertEquals("", vm.uiState.value.activeCaloriesText)
        assertEquals("", vm.uiState.value.totalCaloriesText)
    }

    @Test fun `recorded activity without enough route points estimates calories`() = runTest {
        val repo = activityRepo(canWrite = true)
        val prefs = activityPrefs()
        val recorder = mockk<ActivityRecordingController>()
        val start = Instant.parse("2026-05-26T08:30:00Z")
        every { recorder.state } returns MutableStateFlow(ActivityRecordingState())
        every { recorder.finishRecording() } returns ActivityRecordingSnapshot(
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
            startTime = start,
            endTime = start.plusSeconds(30 * 60),
            points = emptyList(),
            pauseIntervals = emptyList(),
            distanceMeters = 0.0,
            elevationGainedMeters = 0.0,
        )
        val vm = ActivityEntryViewModel(
            repository = repo,
            activityRecorder = recorder,
            preferencesRepository = prefs,
            clock = Clock.fixed(start, ZoneId.of("UTC")),
        )
        advanceUntilIdle()

        vm.finishGpsRecording(UnitSystem.METRIC)
        advanceUntilIdle()

        assertEquals(ActivityEntryMode.MANUAL, vm.uiState.value.mode)
        assertEquals("308", vm.uiState.value.activeCaloriesText)
        assertEquals("343", vm.uiState.value.totalCaloriesText)
        verify { prefs.lastActivityExerciseType = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING }
    }

    @Test fun `finished recording draft is restored by a new activity entry view model`() = runTest {
        val repo = activityRepo(canWrite = true)
        val draftStore = ActivityRecordingDraftStore()
        val recorder = mockk<ActivityRecordingController>()
        val start = Instant.parse("2026-05-26T08:30:00Z")
        every { recorder.state } returns MutableStateFlow(ActivityRecordingState())
        every { recorder.finishRecording() } returns ActivityRecordingSnapshot(
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
            startTime = start,
            endTime = start.plusSeconds(45 * 60),
            points = listOf(routePoint(start), routePoint(start.plusSeconds(45 * 60), latitude = 59.01)),
            pauseIntervals = emptyList(),
            distanceMeters = 1200.0,
            elevationGainedMeters = 12.0,
        )
        val firstVm = ActivityEntryViewModel(
            repository = repo,
            activityRecorder = recorder,
            recordingDraftStore = draftStore,
            clock = Clock.fixed(start, ZoneId.of("UTC")),
        )
        advanceUntilIdle()
        firstVm.selectActivityType(DefaultActivityEntryTypes.first { it.exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_BIKING })
        advanceUntilIdle()

        firstVm.finishGpsRecording(UnitSystem.METRIC)
        advanceUntilIdle()

        val restoredVm = ActivityEntryViewModel(
            repository = repo,
            recordingDraftStore = draftStore,
            clock = Clock.fixed(start.plusSeconds(60), ZoneId.of("UTC")),
        )
        advanceUntilIdle()

        assertEquals(ActivityEntryMode.ROUTE_IMPORT, restoredVm.uiState.value.mode)
        assertEquals(ExerciseSessionRecord.EXERCISE_TYPE_BIKING, restoredVm.uiState.value.selectedActivityType.exerciseType)
        assertEquals("1.2", restoredVm.uiState.value.distanceText)
        assertEquals("12", restoredVm.uiState.value.elevationText)
        assertTrue(restoredVm.uiState.value.isRecordingDraft)
    }

    @Test fun `saving a restored recording draft clears it`() = runTest {
        val repo = activityRepo(canWrite = true)
        val draftStore = ActivityRecordingDraftStore()
        val recorder = mockk<ActivityRecordingController>()
        val start = Instant.parse("2026-05-26T08:30:00Z")
        every { recorder.state } returns MutableStateFlow(ActivityRecordingState())
        every { recorder.finishRecording() } returns ActivityRecordingSnapshot(
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
            startTime = start,
            endTime = start.plusSeconds(30 * 60),
            points = emptyList(),
            pauseIntervals = emptyList(),
            distanceMeters = 0.0,
            elevationGainedMeters = 0.0,
        )
        val vm = ActivityEntryViewModel(
            repository = repo,
            activityRecorder = recorder,
            recordingDraftStore = draftStore,
            clock = Clock.fixed(start, ZoneId.of("UTC")),
        )
        advanceUntilIdle()

        vm.finishGpsRecording(UnitSystem.METRIC)
        advanceUntilIdle()

        val restoredVm = ActivityEntryViewModel(
            repository = repo,
            recordingDraftStore = draftStore,
            clock = Clock.fixed(start.plusSeconds(60), ZoneId.of("UTC")),
        )
        advanceUntilIdle()

        restoredVm.addEntry(UnitSystem.METRIC)
        advanceUntilIdle()

        assertNull(draftStore.restore())
    }

    @Test fun `discarding a finished recording draft clears it and returns to source choice`() = runTest {
        val repo = activityRepo(canWrite = true)
        val draftStore = ActivityRecordingDraftStore()
        val recorder = mockk<ActivityRecordingController>()
        val start = Instant.parse("2026-05-26T08:30:00Z")
        every { recorder.state } returns MutableStateFlow(ActivityRecordingState())
        every { recorder.finishRecording() } returns ActivityRecordingSnapshot(
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
            startTime = start,
            endTime = start.plusSeconds(45 * 60),
            points = listOf(routePoint(start), routePoint(start.plusSeconds(45 * 60), latitude = 59.01)),
            pauseIntervals = emptyList(),
            distanceMeters = 1200.0,
            elevationGainedMeters = 12.0,
        )
        val vm = ActivityEntryViewModel(
            repository = repo,
            activityRecorder = recorder,
            recordingDraftStore = draftStore,
            clock = Clock.fixed(start, ZoneId.of("UTC")),
        )
        advanceUntilIdle()

        vm.finishGpsRecording(UnitSystem.METRIC)
        advanceUntilIdle()

        vm.discardRecordingDraft()
        advanceUntilIdle()

        assertNull(draftStore.restore())
        assertEquals(ActivityEntryMode.CHOOSE_SOURCE, vm.uiState.value.mode)
        assertFalse(vm.uiState.value.isRecordingDraft)
        assertNull(vm.uiState.value.importedRoute)
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
        assertEquals("113", vm.uiState.value.activeCaloriesText)
        assertEquals("126", vm.uiState.value.totalCaloriesText)
    }

    private fun activityRepo(canWrite: Boolean): ActivityRepository =
        mockk<ActivityRepository>().also { repo ->
            every { repo.activityWritePermissions() } returns ActivityWritePermissions
            every { repo.activityWritePermissions(any(), any(), any(), any(), any()) } returns ActivityWritePermissions
            every { repo.activityWritePermissions(any<ActivityWriteRequest>()) } returns ActivityWritePermissions
            coEvery { repo.hasActivityWritePermission() } returns canWrite
            coEvery { repo.hasActivityWritePermission(any(), any(), any(), any(), any()) } returns canWrite
            coEvery { repo.hasActivityWritePermission(any<ActivityWriteRequest>()) } returns canWrite
            coEvery { repo.writeActivityEntry(any()) } returns "activity-id"
        }

    private fun activityPrefs(
        favoriteActivityExerciseType: Int? = null,
        lastActivityExerciseType: Int? = null,
    ): PreferencesRepository =
        mockk<PreferencesRepository>().also { prefs ->
            every { prefs.favoriteActivityExerciseType } returns favoriteActivityExerciseType
            every { prefs.lastActivityExerciseType } returns lastActivityExerciseType
            every { prefs.lastActivityExerciseType = any() } just runs
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
