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
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.features.workoutplans.toRepetitionSetInputs
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.domain.model.ActivityPauseInterval
import tech.mmarca.openvitals.domain.model.ActivityWriteRequest
import tech.mmarca.openvitals.domain.model.BleRecordingSampleBuffer
import tech.mmarca.openvitals.domain.model.CoMapsNavigationState
import tech.mmarca.openvitals.domain.model.CoMapsRoutePolyline
import tech.mmarca.openvitals.domain.preferences.ActivityRecordingPreferences
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.domain.model.ExerciseLapData
import tech.mmarca.openvitals.domain.model.ExerciseRoutePoint
import tech.mmarca.openvitals.domain.model.ExerciseSegmentData
import tech.mmarca.openvitals.domain.model.PlannedExerciseBlockData
import tech.mmarca.openvitals.domain.model.PlannedExerciseCompletion
import tech.mmarca.openvitals.domain.model.PlannedExerciseData
import tech.mmarca.openvitals.domain.model.PlannedExerciseStepData
import tech.mmarca.openvitals.domain.model.PlannedExerciseWriteRequest
import tech.mmarca.openvitals.data.repository.contract.ActivityRepository
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

        val request = buildWriteRequest(state, ActivityEntryUnits.uniform(UnitSystem.METRIC))

        requireNotNull(request)
        assertEquals(ExerciseSessionRecord.EXERCISE_TYPE_RUNNING, request.exerciseType)
        assertEquals("Morning run", request.title)
        assertEquals("Easy effort", request.notes)
        assertEquals(10_500.0, request.distanceMeters ?: 0.0, 0.001)
        assertTrue(request.startTime.isBefore(request.endTime))
    }

    @Test fun `buildWriteRequest combines selected feeling and notes`() {
        val state = ActivityEntryUiState(
            selectedActivityType = DefaultActivityEntryTypes.first(),
            selectedFeeling = ActivityEntryFeeling.GOOD,
            notesText = "  Kept the last mile steady.  ",
            startDateText = "2026-05-26",
            startTimeText = "8:30",
            durationMinutesText = "45",
        )

        val request = buildWriteRequest(state, ActivityEntryUnits.uniform(UnitSystem.METRIC))

        requireNotNull(request)
        assertEquals("Felt good.\n\nKept the last mile steady.", request.notes)
    }

    @Test fun `buildWriteRequest ignores hidden unsupported metric values`() {
        val state = ActivityEntryUiState(
            selectedActivityType = DefaultActivityEntryTypes.first { it.id == "push_ups" },
            startDateText = "2026-05-26",
            startTimeText = "8:30",
            durationMinutesText = "30",
            distanceText = "10.5",
            elevationText = "120",
            repetitionTotalText = "25",
        )

        val request = buildWriteRequest(state, ActivityEntryUnits.uniform(UnitSystem.METRIC))

        requireNotNull(request)
        assertNull(request.distanceMeters)
        assertNull(request.elevationGainedMeters)
    }

    @Test fun `buildWriteRequest rejects total calories below active calories`() {
        val state = ActivityEntryUiState(
            startDateText = "2026-05-26",
            startTimeText = "8:30",
            durationMinutesText = "45",
            activeCaloriesText = "500",
            totalCaloriesText = "300",
        )

        assertNull(buildWriteRequest(state, ActivityEntryUnits.uniform(UnitSystem.METRIC)))
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

        val errors = validateActivityEntry(state, ActivityEntryUnits.uniform(UnitSystem.METRIC))

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
        vm.addEntry(ActivityEntryUnits.uniform(UnitSystem.METRIC))
        advanceUntilIdle()

        assertEquals(ActivityEntryError.INVALID_VALUE, vm.uiState.value.entryError)
        assertTrue(ActivityEntryValidationError.DURATION_INVALID in vm.uiState.value.validationErrors)
        assertTrue(ActivityEntryValidationError.DISTANCE_INVALID in vm.uiState.value.validationErrors)
        coVerify(exactly = 0) { repo.writeActivityEntry(any()) }
    }

    @Test fun `selecting activity clears metric fields that activity does not use`() = runTest {
        val repo = activityRepo(canWrite = true)
        val vm = ActivityEntryViewModel(
            repository = repo,
            clock = Clock.fixed(Instant.parse("2026-05-26T08:30:00Z"), ZoneId.of("UTC")),
        )
        advanceUntilIdle()

        vm.startManualEntry()
        vm.updateDistance("10.5")
        vm.updateElevation("120")
        vm.selectActivityType(DefaultActivityEntryTypes.first { it.id == "push_ups" })
        advanceUntilIdle()

        assertEquals("", vm.uiState.value.distanceText)
        assertEquals("", vm.uiState.value.elevationText)
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

        val request = buildWriteRequest(state, ActivityEntryUnits.uniform(UnitSystem.METRIC))

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

        val request = buildWriteRequest(state, ActivityEntryUnits.uniform(UnitSystem.METRIC))

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

        val request = buildWriteRequest(state, ActivityEntryUnits.uniform(UnitSystem.METRIC))

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

        val request = buildWriteRequest(state, ActivityEntryUnits.uniform(UnitSystem.METRIC))

        requireNotNull(request)
        assertFalse(request.routePoints.isNotEmpty())
        assertTrue(request.pauseIntervals.isEmpty())
        assertTrue(request.laps.isEmpty())
    }

    @Test fun `buildWriteRequest keeps BLE heart rate samples for strength training`() {
        val strengthTraining = DefaultActivityEntryTypes.first {
            it.exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING
        }
        val sampleTime = Instant.parse("2026-05-26T08:35:00Z")
        val state = ActivityEntryUiState(
            selectedActivityType = strengthTraining,
            startDateText = "2026-05-26",
            startTimeText = "8:30",
            durationMinutesText = "30",
            recordedBleSamples = BleRecordingSampleBuffer()
                .withHeartRateSample(sampleTime, 132)
                .withHeartRateSample(sampleTime.plusSeconds(30), 140),
        )

        val request = buildWriteRequest(state, ActivityEntryUnits.uniform(UnitSystem.METRIC))

        requireNotNull(request)
        assertEquals(ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING, request.exerciseType)
        assertTrue(strengthTraining.supportsLiveRecording)
        assertFalse(strengthTraining.supportsGpsRoute)
        assertFalse(strengthTraining.isRepetitionLike)
        assertTrue(request.routePoints.isEmpty())
        assertTrue(request.exerciseSegments.isEmpty())
        assertEquals(2, request.bleSamples.heartRateSamples.size)
        assertEquals(132L, request.bleSamples.heartRateSamples.first().beatsPerMinute)
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

        val request = buildWriteRequest(state, ActivityEntryUnits.uniform(UnitSystem.METRIC))

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

        val request = buildWriteRequest(state, ActivityEntryUnits.uniform(UnitSystem.METRIC))

        requireNotNull(request)
        assertEquals(3, request.exerciseSegments.size)
        assertEquals(8, request.exerciseSegments[0].repetitions)
        assertEquals(0, request.exerciseSegments[0].setIndex)
        assertEquals(ExerciseSegment.EXERCISE_SEGMENT_TYPE_REST, request.exerciseSegments[1].segmentType)
        assertEquals(6, request.exerciseSegments[2].repetitions)
        assertEquals(1, request.exerciseSegments[2].setIndex)
    }

    @Test fun `buildWriteRequest keeps BLE heart rate samples for repetition recordings`() {
        val repetitionType = DefaultActivityEntryTypes.first { it.id == "pull_ups" }
        val sampleTime = Instant.parse("2026-05-26T08:35:00Z")
        val state = ActivityEntryUiState(
            selectedActivityType = repetitionType,
            startDateText = "2026-05-26",
            startTimeText = "8:30",
            durationMinutesText = "5",
            repetitionMode = ActivityRepetitionEntryMode.SETS,
            repetitionSets = listOf(
                ActivityRepetitionSetInput(repetitionsText = "8", restMinutesText = "1"),
                ActivityRepetitionSetInput(repetitionsText = "6"),
            ),
            recordedBleSamples = BleRecordingSampleBuffer()
                .withHeartRateSample(sampleTime, 128)
                .withHeartRateSample(sampleTime.plusSeconds(30), 136),
        )

        val request = buildWriteRequest(state, ActivityEntryUnits.uniform(UnitSystem.METRIC))

        requireNotNull(request)
        assertEquals(ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS, request.exerciseType)
        assertTrue(repetitionType.supportsLiveRecording)
        assertTrue(repetitionType.isRepetitionLike)
        assertFalse(repetitionType.supportsGpsRoute)
        assertEquals(3, request.exerciseSegments.size)
        assertEquals(2, request.bleSamples.heartRateSamples.size)
        assertEquals(128L, request.bleSamples.heartRateSamples.first().beatsPerMinute)
    }

    @Test fun `buildWriteRequest links selected planned workout`() {
        val state = ActivityEntryUiState(
            selectedActivityType = DefaultActivityEntryTypes.first { it.id == "pull_ups" },
            linkedPlan = ActivityLinkedPlan("planned-id", null),
            startDateText = "2026-05-26",
            startTimeText = "8:30",
            durationMinutesText = "5",
            repetitionMode = ActivityRepetitionEntryMode.SETS,
            repetitionSets = listOf(
                ActivityRepetitionSetInput(repetitionsText = "8"),
            ),
        )

        val request = buildWriteRequest(state, ActivityEntryUnits.uniform(UnitSystem.METRIC))

        requireNotNull(request)
        assertEquals("planned-id", request.plannedExerciseSessionId)
    }

    @Test fun `buildWriteRequest writes treadmill steps as steps count`() {
        val state = ActivityEntryUiState(
            selectedActivityType = DefaultActivityEntryTypes.first { it.id == "treadmill" },
            startDateText = "2026-05-26",
            startTimeText = "8:30",
            durationMinutesText = "20",
            repetitionTotalText = "2400",
        )

        val request = buildWriteRequest(state, ActivityEntryUnits.uniform(UnitSystem.METRIC))

        requireNotNull(request)
        assertEquals(ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL, request.exerciseType)
        assertEquals(2400L, request.stepsCount)
    }

    @Test fun `buildWriteRequest writes walking steps as steps count`() {
        val state = ActivityEntryUiState(
            selectedActivityType = DefaultActivityEntryTypes.first {
                it.exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_WALKING
            },
            startDateText = "2026-05-26",
            startTimeText = "8:30",
            durationMinutesText = "20",
            distanceText = "1.6",
            repetitionTotalText = "2100",
        )

        val request = buildWriteRequest(state, ActivityEntryUnits.uniform(UnitSystem.METRIC))

        requireNotNull(request)
        assertEquals(ExerciseSessionRecord.EXERCISE_TYPE_WALKING, request.exerciseType)
        assertEquals(2100L, request.stepsCount)
        assertEquals(1_600.0, request.distanceMeters ?: 0.0, 0.001)
    }

    @Test fun `buildWriteRequest allows walking without steps`() {
        val state = ActivityEntryUiState(
            selectedActivityType = DefaultActivityEntryTypes.first {
                it.exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_WALKING
            },
            startDateText = "2026-05-26",
            startTimeText = "8:30",
            durationMinutesText = "20",
            distanceText = "1.6",
        )

        val request = buildWriteRequest(state, ActivityEntryUnits.uniform(UnitSystem.METRIC))

        requireNotNull(request)
        assertEquals(ExerciseSessionRecord.EXERCISE_TYPE_WALKING, request.exerciseType)
        assertNull(request.stepsCount)
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
        vm.addEntry(ActivityEntryUnits.uniform(UnitSystem.METRIC))
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
        vm.addEntry(ActivityEntryUnits.uniform(UnitSystem.METRIC))
        advanceUntilIdle()

        coVerify {
            repo.writeActivityEntry(match<ActivityWriteRequest> { request ->
                abs((request.distanceMeters ?: 0.0) - 5000.0) < 0.001
            })
        }
        assertFalse(vm.uiState.value.isSavingEntry)
        assertTrue(vm.uiState.value.saveCompleted)
    }

    @Test fun `the bare route opens the start hub with today's and upcoming plans`() = runTest {
        val today = plannedPullUpPlan()
        val later = today.copy(id = "later", startTime = today.startTime.plusSeconds(2 * 86_400), endTime = today.endTime.plusSeconds(2 * 86_400))
        val past = today.copy(id = "past", startTime = today.startTime.minusSeconds(86_400), endTime = today.endTime.minusSeconds(86_400))
        val repo = activityRepo(canWrite = true, plannedWorkouts = listOf(later, past, today))
        val vm = ActivityEntryViewModel(
            repository = repo,
            clock = Clock.fixed(Instant.parse("2026-05-26T08:30:00Z"), ZoneId.of("UTC")),
        )
        advanceUntilIdle()

        assertEquals(ActivityEntryMode.START_HUB, vm.uiState.value.mode)
        assertEquals(listOf("planned-id", "later"), vm.uiState.value.hubPlans.map { it.id })
        assertFalse(vm.uiState.value.isLoadingHubPlans)
        assertTrue(vm.uiState.value.hubPlansAvailable)
    }

    @Test fun `the legacy plan launch mode also lands on the hub`() = runTest {
        val vm = ActivityEntryViewModel(
            repository = activityRepo(canWrite = true, plannedWorkouts = listOf(plannedPullUpPlan())),
            clock = Clock.fixed(Instant.parse("2026-05-26T08:30:00Z"), ZoneId.of("UTC")),
            launchMode = tech.mmarca.openvitals.navigation.Screen.ActivityEntryMode.PLAN,
        )
        advanceUntilIdle()

        assertEquals(ActivityEntryMode.START_HUB, vm.uiState.value.mode)
        assertEquals(listOf("planned-id"), vm.uiState.value.hubPlans.map { it.id })
    }

    @Test fun `startWithPlan opens the requested plan directly in manual entry`() = runTest {
        val repo = activityRepo(canWrite = true, plannedWorkouts = listOf(plannedPullUpPlan(), plannedPushUpPlan()))
        val vm = ActivityEntryViewModel(
            repository = repo,
            clock = Clock.fixed(Instant.parse("2026-05-27T09:45:00Z"), ZoneId.of("UTC")),
        )
        advanceUntilIdle()

        vm.startWithPlan("planned-push-id")
        advanceUntilIdle()

        assertEquals(ActivityEntryMode.MANUAL, vm.uiState.value.mode)
        assertEquals(ActivityLinkedPlan("planned-push-id", "Push-up pyramid"), vm.uiState.value.linkedPlan)
        assertEquals("push_ups", vm.uiState.value.selectedActivityType.id)
        assertEquals("2026-05-27", vm.uiState.value.startDateText)
        assertEquals("9:45", vm.uiState.value.startTimeText)
    }

    @Test fun `a missing plan id falls back to the hub and says so`() = runTest {
        val vm = ActivityEntryViewModel(
            repository = activityRepo(canWrite = true, plannedWorkouts = listOf(plannedPullUpPlan())),
            clock = Clock.fixed(Instant.parse("2026-05-26T08:30:00Z"), ZoneId.of("UTC")),
        )
        advanceUntilIdle()

        vm.startWithPlan("gone")
        advanceUntilIdle()

        assertEquals(ActivityEntryMode.START_HUB, vm.uiState.value.mode)
        assertEquals(ActivityEntryError.PLAN_NOT_FOUND, vm.uiState.value.entryError)
        assertEquals(listOf("planned-id"), vm.uiState.value.hubPlans.map { it.id })
        assertNull(vm.uiState.value.linkedPlan)
    }

    @Test fun `logging from a hub plan prefills the set structure and links the plan`() = runTest {
        val repo = activityRepo(canWrite = true, plannedWorkouts = listOf(plannedPullUpPlan()))
        val vm = ActivityEntryViewModel(
            repository = repo,
            clock = Clock.fixed(Instant.parse("2026-05-26T08:30:00Z"), ZoneId.of("UTC")),
        )
        advanceUntilIdle()

        vm.logFromPlan("planned-id")

        assertEquals(ActivityEntryMode.MANUAL, vm.uiState.value.mode)
        assertEquals(ActivityLinkedPlan("planned-id", "Pull-up ladder"), vm.uiState.value.linkedPlan)
        assertEquals("pull_ups", vm.uiState.value.selectedActivityType.id)
        assertEquals("Pull-up ladder", vm.uiState.value.titleText)
        assertEquals(ActivityRepetitionEntryMode.SETS, vm.uiState.value.repetitionMode)
        assertEquals(
            listOf(
                ActivityRepetitionSetInput(repetitionsText = "8", restMinutesText = "60"),
                ActivityRepetitionSetInput(repetitionsText = "6"),
            ),
            vm.uiState.value.repetitionSets,
        )

        vm.clearLinkedPlan()
        assertNull(vm.uiState.value.linkedPlan)
    }

    @Test fun `a duration-only plan prefills seconds rows`() = runTest {
        val plank = plannedPullUpPlan().copy(
            id = "plank-plan",
            blocks = listOf(
                PlannedExerciseBlockData(
                    repetitions = 2,
                    description = null,
                    steps = listOf(
                        PlannedExerciseStepData(
                            exerciseType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_PLANK,
                            exercisePhase = androidx.health.connect.client.records.PlannedExerciseStep.EXERCISE_PHASE_ACTIVE,
                            description = null,
                            completion = PlannedExerciseCompletion.DurationSeconds(45),
                        ),
                    ),
                ),
            ),
        )
        val vm = ActivityEntryViewModel(
            repository = activityRepo(canWrite = true, plannedWorkouts = listOf(plank)),
            clock = Clock.fixed(Instant.parse("2026-05-26T08:30:00Z"), ZoneId.of("UTC")),
        )
        advanceUntilIdle()

        vm.logFromPlan("plank-plan")

        assertEquals(ActivityEntryMode.MANUAL, vm.uiState.value.mode)
        assertEquals(2, vm.uiState.value.repetitionSets.size)
        assertTrue(vm.uiState.value.repetitionSets.all { it.isDuration && it.repetitionsText == "45" })
    }

    @Test fun `reapplyPlan after the builder returns re-prefills from the new id`() = runTest {
        val original = plannedPullUpPlan()
        val edited = original.copy(id = "new-id", title = "Pull-up ladder v2")
        val vm = ActivityEntryViewModel(
            repository = activityRepo(canWrite = true, plannedWorkouts = listOf(original, edited)),
            clock = Clock.fixed(Instant.parse("2026-05-26T08:30:00Z"), ZoneId.of("UTC")),
        )
        advanceUntilIdle()
        vm.logFromPlan("planned-id")

        vm.reapplyPlan("new-id")
        advanceUntilIdle()

        assertEquals(ActivityLinkedPlan("new-id", "Pull-up ladder v2"), vm.uiState.value.linkedPlan)
        assertEquals("Pull-up ladder v2", vm.uiState.value.titleText)
    }

    @Test fun `editing a plan-linked session keeps the link and shows its title`() = runTest {
        val start = Instant.parse("2026-05-26T08:30:00Z")
        val workout = ExerciseData(
            id = "activity-id",
            title = "Pull-up ladder",
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS,
            startTime = start,
            endTime = start.plusSeconds(5 * 60),
            durationMs = 5 * 60 * 1000,
            source = "tech.mmarca.openvitals",
            plannedExerciseSessionId = "planned-id",
            segments = listOf(
                ExerciseSegmentData(
                    startTime = start,
                    endTime = start.plusSeconds(60),
                    segmentType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_PULL_UP,
                    repetitions = 8,
                )
            ),
            isOpenVitalsEntry = true,
        )
        val repo = activityRepo(canWrite = true, plannedWorkouts = listOf(plannedPullUpPlan()), workout = workout)
        coEvery { repo.updateActivityEntry(any(), any()) } returns Unit
        val vm = ActivityEntryViewModel(
            repository = repo,
            clock = Clock.fixed(start, ZoneId.of("UTC")),
            editActivityId = "activity-id",
        )
        advanceUntilIdle()

        vm.loadEditEntry(ActivityEntryUnits.uniform(UnitSystem.METRIC))
        advanceUntilIdle()

        assertEquals("pull_ups", vm.uiState.value.selectedActivityType.id)
        assertEquals(ActivityLinkedPlan("planned-id", "Pull-up ladder"), vm.uiState.value.linkedPlan)

        vm.addEntry(ActivityEntryUnits.uniform(UnitSystem.METRIC))
        advanceUntilIdle()

        coVerify {
            repo.updateActivityEntry("activity-id", match<ActivityWriteRequest> { it.plannedExerciseSessionId == "planned-id" })
        }
    }

    @Test fun `missing planned read permission is surfaced on the hub`() = runTest {
        val vm = ActivityEntryViewModel(
            repository = activityRepo(canWrite = true, canReadPlans = false),
            clock = Clock.fixed(Instant.parse("2026-05-26T08:30:00Z"), ZoneId.of("UTC")),
        )
        advanceUntilIdle()

        assertEquals(ActivityEntryMode.START_HUB, vm.uiState.value.mode)
        assertEquals(ScreenError.PermissionDenied, vm.uiState.value.hubPlansError)
        assertTrue(vm.uiState.value.hubPlans.isEmpty())
    }

    @Test fun `activity entry writes the linked plan id`() = runTest {
        val repo = activityRepo(canWrite = true, plannedWorkouts = listOf(plannedPullUpPlan()))
        val vm = ActivityEntryViewModel(
            repository = repo,
            clock = Clock.fixed(Instant.parse("2026-05-26T08:30:00Z"), ZoneId.of("UTC")),
        )
        advanceUntilIdle()

        vm.logFromPlan("planned-id")
        advanceUntilIdle()
        vm.addEntry(ActivityEntryUnits.uniform(UnitSystem.METRIC))
        advanceUntilIdle()

        coVerify {
            repo.writeActivityEntry(match<ActivityWriteRequest> { it.plannedExerciseSessionId == "planned-id" })
        }
    }

    @Test fun `save as plan writes a one-block plan and asks to open the builder`() = runTest {
        val repo = activityRepo(canWrite = true)
        val vm = ActivityEntryViewModel(
            repository = repo,
            clock = Clock.fixed(Instant.parse("2026-05-26T08:30:00Z"), ZoneId.of("UTC")),
        )
        advanceUntilIdle()

        vm.selectActivityType(DefaultActivityEntryTypes.first { it.id == "pull_ups" })
        vm.startManualEntry()
        vm.updateTitle("Pull-up ladder")
        vm.updateRepetitionMode(ActivityRepetitionEntryMode.SETS)
        vm.updateRepetitionSetRepetitions(0, "8")
        vm.updateRepetitionSetRest(0, "60")
        vm.addRepetitionSet()
        vm.updateRepetitionSetRepetitions(1, "6")
        vm.updateRepetitionSetRest(1, "")
        advanceUntilIdle()
        vm.saveAsPlan(ActivityEntryUnits.uniform(UnitSystem.METRIC))
        advanceUntilIdle()

        coVerify {
            repo.writePlannedWorkout(match<PlannedExerciseWriteRequest> { request ->
                request.id == null &&
                    request.title == "Pull-up ladder" &&
                    // Different rep counts do not collapse into one block of rounds.
                    request.blocks.map { it.repetitions } == listOf(1, 1) &&
                    request.blocks[0].steps.map { it.completion } == listOf(
                        PlannedExerciseCompletion.Repetitions(8),
                        PlannedExerciseCompletion.DurationSeconds(60),
                    ) &&
                    request.blocks[1].steps.map { it.completion } == listOf(PlannedExerciseCompletion.Repetitions(6))
            })
        }
        assertEquals(ActivityLinkedPlan("saved-plan-id", "Pull-up ladder"), vm.uiState.value.linkedPlan)
        assertEquals("saved-plan-id", vm.uiState.value.pendingBuilderPlanId)
        assertFalse(vm.uiState.value.isSavingAsPlan)

        vm.onBuilderNavigationHandled()
        assertNull(vm.uiState.value.pendingBuilderPlanId)
    }

    @Test fun `save as plan without a title uses the type's default title`() = runTest {
        val repo = activityRepo(canWrite = true)
        val vm = ActivityEntryViewModel(
            repository = repo,
            clock = Clock.fixed(Instant.parse("2026-05-26T08:30:00Z"), ZoneId.of("UTC")),
        )
        advanceUntilIdle()

        vm.selectActivityType(DefaultActivityEntryTypes.first { it.id == "push_ups" })
        vm.startManualEntry()
        vm.updateRepetitionTotal("10")
        advanceUntilIdle()
        vm.saveAsPlan(ActivityEntryUnits.uniform(UnitSystem.METRIC))
        advanceUntilIdle()

        coVerify {
            repo.writePlannedWorkout(match<PlannedExerciseWriteRequest> { request ->
                request.title == "Push-ups" &&
                    request.blocks.single().steps.single().completion == PlannedExerciseCompletion.Repetitions(10)
            })
        }
    }

    @Test fun `missing planned workout permission is surfaced when saving as plan`() = runTest {
        val repo = activityRepo(canWrite = true, canWritePlan = false)
        val vm = ActivityEntryViewModel(
            repository = repo,
            clock = Clock.fixed(Instant.parse("2026-05-26T08:30:00Z"), ZoneId.of("UTC")),
        )
        advanceUntilIdle()

        vm.selectActivityType(DefaultActivityEntryTypes.first { it.id == "pull_ups" })
        vm.startManualEntry()
        vm.updateRepetitionMode(ActivityRepetitionEntryMode.SETS)
        vm.updateRepetitionSetRepetitions(0, "8")
        advanceUntilIdle()
        vm.saveAsPlan(ActivityEntryUnits.uniform(UnitSystem.METRIC))
        advanceUntilIdle()

        assertEquals(ActivityEntryError.MISSING_WRITE_PERMISSION, vm.uiState.value.entryError)
        assertEquals(PlannedWorkoutWritePermissions, vm.uiState.value.writePermissions)
        assertNull(vm.uiState.value.pendingBuilderPlanId)
    }

    @Test fun `starting a plan from the hub shows the guided recording setup`() = runTest {
        val recorder = recorderMock()
        val vm = ActivityEntryViewModel(
            repository = activityRepo(canWrite = true, plannedWorkouts = listOf(plannedPushUpPlan())),
            activityRecorder = recorder,
            recordingDraftStore = ActivityRecordingDraftStore(),
            preferencesRepository = activityPrefs(),
            clock = Clock.fixed(Instant.parse("2026-05-27T09:45:00Z"), ZoneId.of("UTC")),
        )
        advanceUntilIdle()

        vm.prepareGuidedPlan("planned-push-id")
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(ActivityEntryMode.RECORDING, state.mode)
        assertEquals("planned-push-id", state.guidedPlan?.plan?.id)
        assertEquals("push_ups", state.guidedPlan?.activityType?.id)
        assertEquals(1, state.guidedPlan?.steps?.size)
        assertEquals(ActivityLinkedPlan("planned-push-id", "Push-up pyramid"), state.linkedPlan)

        every { recorder.startPlanRecording(any(), any()) } returns true
        vm.startPlanRecording()
        verify(exactly = 1) { recorder.startPlanRecording(match { it.id == "planned-push-id" }, match { it.id == "push_ups" }) }
    }

    @Test fun `the record launch mode with a plan id opens the guided setup`() = runTest {
        val vm = ActivityEntryViewModel(
            repository = activityRepo(canWrite = true, plannedWorkouts = listOf(plannedPushUpPlan())),
            activityRecorder = recorderMock(),
            recordingDraftStore = ActivityRecordingDraftStore(),
            preferencesRepository = activityPrefs(),
            clock = Clock.fixed(Instant.parse("2026-05-27T09:45:00Z"), ZoneId.of("UTC")),
            launchMode = tech.mmarca.openvitals.navigation.Screen.ActivityEntryMode.RECORD,
            launchPlanId = "planned-push-id",
        )
        advanceUntilIdle()

        assertEquals(ActivityEntryMode.RECORDING, vm.uiState.value.mode)
        assertEquals("planned-push-id", vm.uiState.value.guidedPlan?.plan?.id)
    }

    @Test fun `a plan the recorder cannot walk through falls back to the prefilled form`() = runTest {
        val runPlan = plannedPullUpPlan().copy(
            id = "run-plan",
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
            blocks = listOf(
                PlannedExerciseBlockData(
                    repetitions = 1,
                    description = null,
                    steps = listOf(
                        PlannedExerciseStepData(
                            exerciseType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_RUNNING,
                            exercisePhase = androidx.health.connect.client.records.PlannedExerciseStep.EXERCISE_PHASE_ACTIVE,
                            description = null,
                            completion = PlannedExerciseCompletion.DurationSeconds(600),
                        ),
                    ),
                ),
            ),
        )
        val vm = ActivityEntryViewModel(
            repository = activityRepo(canWrite = true, plannedWorkouts = listOf(runPlan)),
            activityRecorder = recorderMock(),
            recordingDraftStore = ActivityRecordingDraftStore(),
            preferencesRepository = activityPrefs(),
            clock = Clock.fixed(Instant.parse("2026-05-27T09:45:00Z"), ZoneId.of("UTC")),
        )
        advanceUntilIdle()

        vm.prepareGuidedPlan("run-plan")
        advanceUntilIdle()

        assertEquals(ActivityEntryMode.MANUAL, vm.uiState.value.mode)
        assertNull(vm.uiState.value.guidedPlan)
        assertEquals("run-plan", vm.uiState.value.linkedPlan?.id)
    }

    @Test fun `a finished plan run lands in the form with its exercises and the plan linked`() = runTest {
        val start = Instant.parse("2026-05-27T09:45:00Z")
        val recorder = recorderMock()
        every { recorder.finishRecording() } returns ActivityRecordingSnapshot(
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS,
            recordingKind = ActivityRecordingKind.REPETITION,
            activityTypeId = "calisthenics",
            startTime = start,
            endTime = start.plusSeconds(300),
            points = emptyList(),
            pauseIntervals = emptyList(),
            distanceMeters = 0.0,
            elevationGainedMeters = 0.0,
            repetitionCount = 10L,
            repetitionSets = listOf(
                ActivityRecordedRepetitionSet(
                    repetitions = 10L,
                    restSeconds = 60L,
                    activeMillis = 30_000L,
                    segmentType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_OTHER_WORKOUT,
                    label = "Push-ups",
                ),
                ActivityRecordedRepetitionSet(
                    repetitions = 0L,
                    restSeconds = 0L,
                    activeMillis = 44_600L,
                    segmentType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_PLANK,
                    label = "Plank",
                    isDuration = true,
                ),
            ),
            planId = "planned-id",
            planTitle = "Strength",
        )
        val vm = ActivityEntryViewModel(
            repository = activityRepo(canWrite = true),
            activityRecorder = recorder,
            recordingDraftStore = ActivityRecordingDraftStore(),
            preferencesRepository = activityPrefs(),
            clock = Clock.fixed(start, ZoneId.of("UTC")),
        )
        advanceUntilIdle()

        vm.finishGpsRecording(ActivityEntryUnits.uniform(UnitSystem.METRIC))
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(ActivityEntryMode.MANUAL, state.mode)
        assertEquals("calisthenics", state.selectedActivityType.id)
        assertEquals(ActivityLinkedPlan("planned-id", "Strength"), state.linkedPlan)
        assertEquals("Strength", state.titleText)
        assertEquals(ActivityRepetitionEntryMode.SETS, state.repetitionMode)
        assertEquals(
            listOf(
                ActivityRepetitionSetInput(repetitionsText = "10", restMinutesText = "60", label = "Push-ups"),
                ActivityRepetitionSetInput(
                    repetitionsText = "44",
                    segmentType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_PLANK,
                    label = "Plank",
                    isDuration = true,
                ),
            ),
            state.repetitionSets,
        )
        assertTrue(state.isRecordingDraft)
    }

    @Test fun `the hub lists recently completed plans to repeat, and repeat starts today's copy`() = runTest {
        val done = plannedPushUpPlan().copy(
            id = "done-id",
            completedExerciseSessionId = "session-1",
            startTime = Instant.parse("2026-05-20T07:00:00Z"),
            endTime = Instant.parse("2026-05-20T07:20:00Z"),
        )
        val repo = activityRepo(canWrite = true, plannedWorkouts = listOf(plannedPullUpPlan()))
        coEvery { repo.loadPlannedWorkouts(any(), any()) } returns listOf(done)
        coEvery { repo.loadPlannedWorkout("saved-plan-id") } returns done.copy(id = "saved-plan-id", completedExerciseSessionId = null)
        val vm = ActivityEntryViewModel(
            repository = repo,
            activityRecorder = recorderMock(),
            recordingDraftStore = ActivityRecordingDraftStore(),
            preferencesRepository = activityPrefs(),
            clock = Clock.fixed(Instant.parse("2026-05-26T08:30:00Z"), ZoneId.of("UTC")),
        )
        advanceUntilIdle()

        assertEquals(listOf("done-id"), vm.uiState.value.recentPlans.map { it.id })
        assertEquals(listOf("planned-id"), vm.uiState.value.hubPlans.map { it.id })

        vm.repeatPlan("done-id")
        advanceUntilIdle()

        coVerify {
            repo.writePlannedWorkout(match<PlannedExerciseWriteRequest> { request ->
                request.id == null &&
                    request.startTime.atZone(ZoneId.of("UTC")).toLocalDate().toString() == "2026-05-26" &&
                    request.startTime.atZone(ZoneId.of("UTC")).toLocalTime().toString() == "08:30"
            })
        }
        assertEquals(ActivityEntryMode.RECORDING, vm.uiState.value.mode)
        assertEquals("saved-plan-id", vm.uiState.value.guidedPlan?.plan?.id)
    }

    @Test fun `a mixed-exercise type composes steps from picked exercises`() = runTest {
        val vm = ActivityEntryViewModel(
            repository = activityRepo(canWrite = true),
            clock = Clock.fixed(Instant.parse("2026-05-26T08:30:00Z"), ZoneId.of("UTC")),
        )
        advanceUntilIdle()

        vm.startManualEntry()
        vm.selectActivityType(DefaultActivityEntryTypes.first { it.id == "calisthenics" })
        assertEquals(ActivityRepetitionEntryMode.SETS, vm.uiState.value.repetitionMode)

        val plank = tech.mmarca.openvitals.features.workoutplans.WorkoutPlanStepChoice(ExerciseSegment.EXERCISE_SEGMENT_TYPE_PLANK)
        vm.addExerciseStep(plank)
        // The single blank starter row is replaced, not kept in front of the first real step.
        assertEquals(1, vm.uiState.value.repetitionSets.size)
        assertEquals(ExerciseSegment.EXERCISE_SEGMENT_TYPE_PLANK, vm.uiState.value.repetitionSets.single().segmentType)
        assertTrue(vm.uiState.value.repetitionSets.single().isDuration)
        assertEquals("30", vm.uiState.value.repetitionSets.single().repetitionsText)

        vm.addRepetitionSet()
        assertEquals(2, vm.uiState.value.repetitionSets.size)
        assertEquals(vm.uiState.value.repetitionSets[0], vm.uiState.value.repetitionSets[1])

        vm.updateRepetitionSetGoalType(1, false)
        assertFalse(vm.uiState.value.repetitionSets[1].isDuration)
        assertEquals("30", vm.uiState.value.repetitionSets[1].repetitionsText)

        vm.updateRepetitionSetExercise(1, tech.mmarca.openvitals.features.workoutplans.WorkoutPlanStepCatalog.first())
        assertEquals("Push-ups", vm.uiState.value.repetitionSets[1].label)
        assertEquals(ExerciseSegment.EXERCISE_SEGMENT_TYPE_OTHER_WORKOUT, vm.uiState.value.repetitionSets[1].segmentType)
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
        every { recorder.coMapsNavigation } returns
            MutableStateFlow<CoMapsNavigationState>(CoMapsNavigationState.Disabled)
        every { recorder.coMapsRoute } returns MutableStateFlow<CoMapsRoutePolyline?>(null)
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

        vm.finishGpsRecording(ActivityEntryUnits.uniform(UnitSystem.METRIC))
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
        every { recorder.coMapsNavigation } returns
            MutableStateFlow<CoMapsNavigationState>(CoMapsNavigationState.Disabled)
        every { recorder.coMapsRoute } returns MutableStateFlow<CoMapsRoutePolyline?>(null)
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

        firstVm.finishGpsRecording(ActivityEntryUnits.uniform(UnitSystem.METRIC))
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

    @Test fun `finished walking route recording keeps recorded steps`() = runTest {
        val repo = activityRepo(canWrite = true)
        val recorder = mockk<ActivityRecordingController>()
        val start = Instant.parse("2026-05-26T08:30:00Z")
        every { recorder.state } returns MutableStateFlow(ActivityRecordingState())
        every { recorder.coMapsNavigation } returns
            MutableStateFlow<CoMapsNavigationState>(CoMapsNavigationState.Disabled)
        every { recorder.coMapsRoute } returns MutableStateFlow<CoMapsRoutePolyline?>(null)
        every { recorder.finishRecording() } returns ActivityRecordingSnapshot(
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_WALKING,
            activityTypeId = DefaultActivityEntryTypes.first {
                it.exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_WALKING
            }.id,
            startTime = start,
            endTime = start.plusSeconds(30 * 60),
            points = listOf(routePoint(start), routePoint(start.plusSeconds(30 * 60), latitude = 59.01)),
            pauseIntervals = emptyList(),
            distanceMeters = 1200.0,
            elevationGainedMeters = 12.0,
            repetitionCount = 1800L,
        )
        val vm = ActivityEntryViewModel(
            repository = repo,
            activityRecorder = recorder,
            clock = Clock.fixed(start, ZoneId.of("UTC")),
        )
        advanceUntilIdle()
        vm.selectActivityType(
            DefaultActivityEntryTypes.first { it.exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_WALKING }
        )
        advanceUntilIdle()

        vm.finishGpsRecording(ActivityEntryUnits.uniform(UnitSystem.METRIC))
        advanceUntilIdle()

        assertEquals(ActivityEntryMode.ROUTE_IMPORT, vm.uiState.value.mode)
        assertEquals(ExerciseSessionRecord.EXERCISE_TYPE_WALKING, vm.uiState.value.selectedActivityType.exerciseType)
        assertEquals("1800", vm.uiState.value.repetitionTotalText)
        assertEquals("1.2", vm.uiState.value.distanceText)
    }

    @Test fun `saving a restored recording draft clears it`() = runTest {
        val repo = activityRepo(canWrite = true)
        val draftStore = ActivityRecordingDraftStore()
        val recorder = mockk<ActivityRecordingController>()
        val start = Instant.parse("2026-05-26T08:30:00Z")
        every { recorder.state } returns MutableStateFlow(ActivityRecordingState())
        every { recorder.coMapsNavigation } returns
            MutableStateFlow<CoMapsNavigationState>(CoMapsNavigationState.Disabled)
        every { recorder.coMapsRoute } returns MutableStateFlow<CoMapsRoutePolyline?>(null)
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

        vm.finishGpsRecording(ActivityEntryUnits.uniform(UnitSystem.METRIC))
        advanceUntilIdle()

        val restoredVm = ActivityEntryViewModel(
            repository = repo,
            recordingDraftStore = draftStore,
            clock = Clock.fixed(start.plusSeconds(60), ZoneId.of("UTC")),
        )
        advanceUntilIdle()

        restoredVm.addEntry(ActivityEntryUnits.uniform(UnitSystem.METRIC))
        advanceUntilIdle()

        assertNull(draftStore.restore())
    }

    @Test fun `discarding a finished recording draft clears it and returns to the start hub`() = runTest {
        val repo = activityRepo(canWrite = true)
        val draftStore = ActivityRecordingDraftStore()
        val recorder = mockk<ActivityRecordingController>()
        val start = Instant.parse("2026-05-26T08:30:00Z")
        every { recorder.state } returns MutableStateFlow(ActivityRecordingState())
        every { recorder.coMapsNavigation } returns
            MutableStateFlow<CoMapsNavigationState>(CoMapsNavigationState.Disabled)
        every { recorder.coMapsRoute } returns MutableStateFlow<CoMapsRoutePolyline?>(null)
        every { recorder.stopBlePreview() } returns Unit
        every { recorder.clearPreparedRecording() } returns Unit
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

        vm.finishGpsRecording(ActivityEntryUnits.uniform(UnitSystem.METRIC))
        advanceUntilIdle()

        vm.discardRecordingDraft()
        advanceUntilIdle()

        assertNull(draftStore.restore())
        assertEquals(ActivityEntryMode.START_HUB, vm.uiState.value.mode)
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

        vm.importRouteFile(uri, ActivityEntryUnits.uniform(UnitSystem.IMPERIAL))
        advanceUntilIdle()

        assertEquals(ActivityEntryMode.ROUTE_IMPORT, vm.uiState.value.mode)
        assertEquals("0.4", vm.uiState.value.distanceText)
        assertEquals("12", vm.uiState.value.elevationText)
        assertEquals("11", vm.uiState.value.durationMinutesText)
        assertEquals("113", vm.uiState.value.activeCaloriesText)
        assertEquals("126", vm.uiState.value.totalCaloriesText)
    }

    @Test fun `FIT import without route fills manual activity fields`() = runTest {
        val repo = activityRepo(canWrite = true)
        val importer = mockk<RouteFileImporter>()
        val uri = mockk<Uri>()
        val start = Instant.parse("2026-05-26T08:30:00Z")
        val end = Instant.parse("2026-05-26T09:15:00Z")
        coEvery { importer.import(uri) } returns RouteFileImport(
            fileName = "Functional Strength Training.fit",
            points = emptyList(),
            distanceMeters = 0.0,
            elevationGainedMeters = 0.0,
            activeCaloriesKcal = 220.0,
            startTime = start,
            endTime = end,
            type = "training",
        )
        val vm = ActivityEntryViewModel(
            repository = repo,
            routeFileImporter = importer,
            clock = Clock.fixed(start, ZoneId.of("UTC")),
        )
        advanceUntilIdle()

        vm.importRouteFile(uri, ActivityEntryUnits.uniform(UnitSystem.METRIC))
        advanceUntilIdle()

        assertEquals(ActivityEntryMode.ROUTE_IMPORT, vm.uiState.value.mode)
        assertEquals(ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING, vm.uiState.value.selectedActivityType.exerciseType)
        assertFalse(vm.uiState.value.selectedActivityType.supportsGpsRoute)
        assertTrue(vm.uiState.value.importedRoute?.points.orEmpty().isEmpty())
        assertEquals("Functional Strength Training", vm.uiState.value.titleText)
        assertEquals("45", vm.uiState.value.durationMinutesText)
        assertEquals("220", vm.uiState.value.activeCaloriesText)
    }

    @Test fun `FIT workout import uses workout duration without changing selected time`() = runTest {
        val repo = activityRepo(canWrite = true)
        val importer = mockk<RouteFileImporter>()
        val uri = mockk<Uri>()
        val now = Instant.parse("2026-05-26T08:30:00Z")
        coEvery { importer.import(uri) } returns RouteFileImport(
            fileName = "Tempo Run.fit",
            points = emptyList(),
            distanceMeters = 0.0,
            elevationGainedMeters = 0.0,
            startTime = Instant.EPOCH,
            endTime = Instant.EPOCH.plusSeconds(15 * 60L),
            durationSeconds = 15 * 60L,
            name = "Tempo Run",
            type = "running",
            hasRecordedTimestamps = false,
            hasImportedTimeRange = false,
        )
        val vm = ActivityEntryViewModel(
            repository = repo,
            routeFileImporter = importer,
            clock = Clock.fixed(now, ZoneId.of("UTC")),
        )
        advanceUntilIdle()

        vm.importRouteFile(uri, ActivityEntryUnits.uniform(UnitSystem.METRIC))
        advanceUntilIdle()

        assertEquals("Tempo Run", vm.uiState.value.titleText)
        assertEquals("2026-05-26", vm.uiState.value.startDateText)
        assertEquals("8:30", vm.uiState.value.startTimeText)
        assertEquals("15", vm.uiState.value.durationMinutesText)
        assertEquals(ExerciseSessionRecord.EXERCISE_TYPE_RUNNING, vm.uiState.value.selectedActivityType.exerciseType)
    }

    // Command lifecycle. The save is `isSavingEntry` plus the one-shot `saveCompleted`,
    // with `entryError`/`detailError` for failure; the route import is `isImportingRoute`.

    @Test fun `the save runs, succeeds, and is consumed exactly once`() = runTest {
        val repo = activityRepo(canWrite = true)
        // The in-flight state is read from inside the repository write: the unconfined dispatcher runs the whole save there.
        var viewModel: ActivityEntryViewModel? = null
        var savingWhileWriting: Boolean? = null
        var completedWhileWriting: Boolean? = null
        coEvery { repo.writeActivityEntry(any()) } answers {
            savingWhileWriting = viewModel?.uiState?.value?.isSavingEntry
            completedWhileWriting = viewModel?.uiState?.value?.saveCompleted
            "activity-id"
        }
        val vm = ActivityEntryViewModel(
            repository = repo,
            clock = Clock.fixed(Instant.parse("2026-05-26T08:30:00Z"), ZoneId.of("UTC")),
        )
        viewModel = vm
        advanceUntilIdle()
        vm.startManualEntry()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isSavingEntry)
        assertFalse(vm.uiState.value.saveCompleted)

        vm.addEntry(ActivityEntryUnits.uniform(UnitSystem.METRIC))
        advanceUntilIdle()

        assertEquals(true, savingWhileWriting)
        assertEquals(false, completedWhileWriting)
        assertFalse(vm.uiState.value.isSavingEntry)
        assertTrue(vm.uiState.value.saveCompleted)
        assertNull(vm.uiState.value.entryError)
        assertNull(vm.uiState.value.detailError)
        coVerify(exactly = 1) { repo.writeActivityEntry(any()) }

        vm.onSaveCompletedHandled()

        assertFalse(vm.uiState.value.saveCompleted)
    }

    @Test fun `a failed write lands on the save with the screen error`() = runTest {
        val repo = activityRepo(
            canWrite = true,
            writeFailure = IllegalStateException("Health Connect said no."),
        )
        val vm = ActivityEntryViewModel(
            repository = repo,
            clock = Clock.fixed(Instant.parse("2026-05-26T08:30:00Z"), ZoneId.of("UTC")),
        )
        advanceUntilIdle()
        vm.startManualEntry()
        advanceUntilIdle()

        vm.addEntry(ActivityEntryUnits.uniform(UnitSystem.METRIC))
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isSavingEntry)
        assertFalse(vm.uiState.value.saveCompleted)
        assertEquals(ActivityEntryError.WRITE_FAILED, vm.uiState.value.entryError)
        assertEquals(ScreenError.Message("Health Connect said no."), vm.uiState.value.detailError)
    }

    @Test fun `a refused write permission is a verdict, not a failed save`() = runTest {
        val repo = activityRepo(canWrite = false)
        val vm = ActivityEntryViewModel(
            repository = repo,
            clock = Clock.fixed(Instant.parse("2026-05-26T08:30:00Z"), ZoneId.of("UTC")),
        )
        advanceUntilIdle()
        vm.startManualEntry()
        advanceUntilIdle()

        vm.addEntry(ActivityEntryUnits.uniform(UnitSystem.METRIC))
        advanceUntilIdle()

        assertEquals(ActivityEntryError.MISSING_WRITE_PERMISSION, vm.uiState.value.entryError)
        // A verdict, not a failure: no screen error, and the save never ran.
        assertNull(vm.uiState.value.detailError)
        assertFalse(vm.uiState.value.isSavingEntry)
        assertFalse(vm.uiState.value.saveCompleted)
        assertFalse(vm.uiState.value.canWrite)
        coVerify(exactly = 0) { repo.writeActivityEntry(any()) }
    }

    @Test fun `refreshPermission probes the repository and publishes the verdict`() = runTest {
        val repo = activityRepo(canWrite = true)
        val vm = ActivityEntryViewModel(
            repository = repo,
            clock = Clock.fixed(Instant.parse("2026-05-26T08:30:00Z"), ZoneId.of("UTC")),
        )
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isCheckingPermission)
        assertTrue(vm.uiState.value.canWrite)
        assertEquals(ActivityWritePermissions, vm.uiState.value.writePermissions)
        assertNull(vm.uiState.value.entryError)
        coVerify(atLeast = 1) { repo.hasActivityWritePermission() }

        vm.refreshPermission()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isCheckingPermission)
        assertTrue(vm.uiState.value.canWrite)
        coVerify(atLeast = 2) { repo.hasActivityWritePermission() }
    }

    @Test fun `a permission probe that fails surfaces the error and blocks the write`() = runTest {
        val repo = activityRepo(
            canWrite = true,
            permissionFailure = IllegalStateException("Probe exploded."),
        )
        val vm = ActivityEntryViewModel(
            repository = repo,
            clock = Clock.fixed(Instant.parse("2026-05-26T08:30:00Z"), ZoneId.of("UTC")),
        )
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isCheckingPermission)
        assertFalse(vm.uiState.value.canWrite)
        assertEquals(ActivityEntryError.WRITE_FAILED, vm.uiState.value.entryError)
        assertEquals(ScreenError.Message("Probe exploded."), vm.uiState.value.detailError)
    }

    @Test fun `the edit route prefills the form from the stored workout`() = runTest {
        val start = Instant.parse("2026-05-26T08:30:00Z")
        val workout = ExerciseData(
            id = "activity-id",
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
            startTime = start,
            endTime = start.plusSeconds(45 * 60),
            durationMs = 45 * 60 * 1000,
            source = "tech.mmarca.openvitals",
            title = "Morning run",
            notes = "Easy effort",
            totalDistanceMeters = 10_500.0,
            isOpenVitalsEntry = true,
        )
        val repo = activityRepo(canWrite = true, workout = workout)
        val vm = ActivityEntryViewModel(
            repository = repo,
            clock = Clock.fixed(start, ZoneId.of("UTC")),
            editActivityId = "activity-id",
        )
        advanceUntilIdle()

        vm.loadEditEntry(ActivityEntryUnits.uniform(UnitSystem.METRIC))
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isEditMode)
        assertEquals("activity-id", vm.uiState.value.editRecordId)
        assertEquals("Morning run", vm.uiState.value.titleText)
        assertEquals("Easy effort", vm.uiState.value.notesText)
        assertEquals("45", vm.uiState.value.durationMinutesText)
        assertEquals("10.5", vm.uiState.value.distanceText)
        assertNull(vm.uiState.value.entryError)
        assertNull(vm.uiState.value.detailError)
    }

    @Test fun `an edit prefill that cannot be read reports the failure`() = runTest {
        val repo = activityRepo(
            canWrite = true,
            loadWorkoutFailure = NoSuchElementException("Activity not found."),
        )
        val vm = ActivityEntryViewModel(
            repository = repo,
            clock = Clock.fixed(Instant.parse("2026-05-26T08:30:00Z"), ZoneId.of("UTC")),
            editActivityId = "activity-id",
        )
        advanceUntilIdle()

        vm.loadEditEntry(ActivityEntryUnits.uniform(UnitSystem.METRIC))
        advanceUntilIdle()

        assertEquals(ActivityEntryError.WRITE_FAILED, vm.uiState.value.entryError)
        assertEquals(ScreenError.Message("Activity not found."), vm.uiState.value.detailError)
    }

    @Test fun `the route import runs and returns to rest`() = runTest {
        val repo = activityRepo(canWrite = true)
        val importer = mockk<RouteFileImporter>()
        val uri = mockk<Uri>()
        val start = Instant.parse("2026-05-26T08:30:00Z")
        var viewModel: ActivityEntryViewModel? = null
        var importingWhileParsing: Boolean? = null
        coEvery { importer.import(uri) } answers {
            importingWhileParsing = viewModel?.uiState?.value?.isImportingRoute
            RouteFileImport(
                fileName = "run.gpx",
                points = listOf(
                    routePoint(start),
                    routePoint(start.plusSeconds(30 * 60), latitude = 59.01),
                ),
                distanceMeters = 5000.0,
                elevationGainedMeters = 20.0,
                startTime = start,
                endTime = start.plusSeconds(30 * 60),
            )
        }
        val vm = ActivityEntryViewModel(
            repository = repo,
            routeFileImporter = importer,
            clock = Clock.fixed(start, ZoneId.of("UTC")),
        )
        viewModel = vm
        advanceUntilIdle()

        vm.importRouteFile(uri, ActivityEntryUnits.uniform(UnitSystem.METRIC))
        advanceUntilIdle()

        assertEquals(true, importingWhileParsing)
        assertFalse(vm.uiState.value.isImportingRoute)
        assertEquals(ActivityEntryMode.ROUTE_IMPORT, vm.uiState.value.mode)
        assertEquals(2, vm.uiState.value.importedRoute?.points?.size ?: 0)
        assertNull(vm.uiState.value.entryError)
        // The import is its own command: it never touches the save.
        assertFalse(vm.uiState.value.saveCompleted)
        assertFalse(vm.uiState.value.isSavingEntry)
    }

    @Test fun `a route file that will not parse fails its own command`() = runTest {
        val repo = activityRepo(canWrite = true)
        val importer = mockk<RouteFileImporter>()
        val uri = mockk<Uri>()
        coEvery { importer.import(uri) } throws IllegalArgumentException("Unsupported file.")
        val vm = ActivityEntryViewModel(
            repository = repo,
            routeFileImporter = importer,
            clock = Clock.fixed(Instant.parse("2026-05-26T08:30:00Z"), ZoneId.of("UTC")),
        )
        advanceUntilIdle()

        vm.importRouteFile(uri, ActivityEntryUnits.uniform(UnitSystem.METRIC))
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isImportingRoute)
        assertEquals(ActivityEntryError.ROUTE_IMPORT_FAILED, vm.uiState.value.entryError)
        val detailError = vm.uiState.value.detailError
        assertTrue(detailError is ScreenError.Message && detailError.text.contains("Unsupported file."))
        assertNull(vm.uiState.value.importedRoute)
        assertFalse(vm.uiState.value.saveCompleted)
        assertFalse(vm.uiState.value.isSavingEntry)
    }

    /** The three failable calls take an optional throwable. The device-bound ActivityRecordingController is mocked on purpose. */

    private fun recorderMock(
        startResult: Boolean = true,
        errorMessage: String? = null,
        state: MutableStateFlow<ActivityRecordingState> = MutableStateFlow(ActivityRecordingState()),
    ) = mockk<ActivityRecordingController>().also { recorder ->
        every { recorder.state } returns state
        every { recorder.coMapsNavigation } returns
            MutableStateFlow<CoMapsNavigationState>(CoMapsNavigationState.Disabled)
        every { recorder.coMapsRoute } returns MutableStateFlow<CoMapsRoutePolyline?>(null)
        every { recorder.startRecording(any(), any(), any(), any()) } answers {
            if (startResult) {
                state.value = ActivityRecordingState(status = ActivityRecordingStatus.RECORDING)
            } else {
                state.value = ActivityRecordingState(errorMessage = errorMessage)
            }
            startResult
        }
        every { recorder.pauseRecording() } answers {
            state.value = state.value.copy(status = ActivityRecordingStatus.PAUSED)
        }
        every { recorder.resumeRecording() } answers {
            state.value = state.value.copy(status = ActivityRecordingStatus.RECORDING)
        }
        every { recorder.discardRecording() } answers { state.value = ActivityRecordingState() }
        every { recorder.stopBlePreview() } returns Unit
        every { recorder.previewBleConnections() } returns Unit
        every { recorder.clearPreparedRecording() } returns Unit
        every { recorder.startPlanRecording(any(), any()) } answers {
            state.value = ActivityRecordingState(status = ActivityRecordingStatus.RECORDING)
            true
        }
        every { recorder.finishRecording() } returns null
    }

    private fun recordingViewModel(recorder: ActivityRecordingController) =
        ActivityEntryViewModel(
            repository = activityRepo(canWrite = true),
            activityRecorder = recorder,
            preferencesRepository = activityPrefs(),
            clock = Clock.fixed(Instant.parse("2026-05-26T08:30:00Z"), ZoneId.of("UTC")),
        )

    @Test fun `a started recording succeeds and republishes the session`() = runTest {
        val recorder = recorderMock(startResult = true)
        val vm = recordingViewModel(recorder)
        advanceUntilIdle()

        vm.startGpsRecording()
        advanceUntilIdle()

        assertEquals(ActivityEntryMode.RECORDING, vm.uiState.value.mode)
        assertNull(vm.uiState.value.entryError)
        verify(exactly = 1) { recorder.startRecording(any(), any(), any(), any()) }
        // The controller's session is what the screen shows.
        assertEquals(ActivityRecordingStatus.RECORDING, recorder.state.value.status)
    }

    @Test fun `a refused start fails with the reason the service gave`() = runTest {
        val recorder = recorderMock(startResult = false, errorMessage = "Waiting for GPS")
        val vm = recordingViewModel(recorder)
        advanceUntilIdle()

        vm.startGpsRecording()
        advanceUntilIdle()

        assertEquals(ActivityEntryError.RECORDING_FAILED, vm.uiState.value.entryError)
        assertEquals(ScreenError.Message("Waiting for GPS"), vm.uiState.value.detailError)
        // A refusal is not a session: nothing may look like it is recording.
        assertFalse(recorder.state.value.isActive)
    }

    @Test fun `pause and resume reach the service and republish its status`() = runTest {
        val recorder = recorderMock(startResult = true)
        val vm = recordingViewModel(recorder)
        advanceUntilIdle()
        vm.startGpsRecording()
        advanceUntilIdle()

        vm.pauseGpsRecording()
        verify(exactly = 1) { recorder.pauseRecording() }
        assertEquals(ActivityRecordingStatus.PAUSED, recorder.state.value.status)

        vm.resumeGpsRecording()
        verify(exactly = 1) { recorder.resumeRecording() }
        assertEquals(ActivityRecordingStatus.RECORDING, recorder.state.value.status)
    }

    @Test fun `stopping with nothing recording fails loudly, not silently`() = runTest {
        // The snapshot is the only copy of the workout: a stop that produces none must not look like it worked.
        val recorder = recorderMock(startResult = true)
        val vm = recordingViewModel(recorder)
        advanceUntilIdle()

        vm.finishGpsRecording(ActivityEntryUnits.uniform(UnitSystem.METRIC))
        advanceUntilIdle()

        assertEquals(ActivityEntryError.RECORDING_FAILED, vm.uiState.value.entryError)
        assertEquals(
            ScreenError.Message("No active activity recording was found."),
            vm.uiState.value.detailError,
        )
    }

    @Test fun `discarding clears the session and returns to the start hub`() = runTest {
        val recorder = recorderMock(startResult = false, errorMessage = "Waiting for GPS")
        val draftStore = ActivityRecordingDraftStore()
        val vm = ActivityEntryViewModel(
            repository = activityRepo(canWrite = true),
            activityRecorder = recorder,
            recordingDraftStore = draftStore,
            preferencesRepository = activityPrefs(),
            clock = Clock.fixed(Instant.parse("2026-05-26T08:30:00Z"), ZoneId.of("UTC")),
        )
        advanceUntilIdle()
        vm.startGpsRecording()
        advanceUntilIdle()
        assertEquals(ActivityEntryError.RECORDING_FAILED, vm.uiState.value.entryError)

        vm.discardGpsRecording()
        advanceUntilIdle()

        verify(exactly = 1) { recorder.discardRecording() }
        assertNull(draftStore.restore())
        assertEquals(ActivityEntryMode.START_HUB, vm.uiState.value.mode)
        assertNull(vm.uiState.value.entryError)
        assertNull(vm.uiState.value.detailError)
        assertFalse(recorder.state.value.isActive)
    }

    @Test fun `focus mode needs a session that can actually use it`() {
        // An idle recorder has nothing to focus on.
        assertFalse(ActivityRecordingState().canUseFocusMode)

        assertTrue(
            ActivityRecordingState(status = ActivityRecordingStatus.RECORDING).canUseFocusMode,
        )

        // A repetition session has no focus mode in the first place.
        assertFalse(
            ActivityRecordingState(
                status = ActivityRecordingStatus.RECORDING,
                recordingKind = ActivityRecordingKind.REPETITION,
            ).canUseFocusMode,
        )
    }

    private fun activityRepo(
        canWrite: Boolean,
        plannedWorkouts: List<PlannedExerciseData> = emptyList(),
        workout: ExerciseData? = null,
        canReadPlans: Boolean = true,
        canWritePlan: Boolean = true,
        permissionFailure: Throwable? = null,
        writeFailure: Throwable? = null,
        loadWorkoutFailure: Throwable? = null,
    ): ActivityRepository =
        mockk<ActivityRepository>().also { repo ->
            every { repo.activityWritePermissions() } returns ActivityWritePermissions
            every { repo.activityWritePermissions(any(), any(), any(), any(), any()) } returns ActivityWritePermissions
            every { repo.activityWritePermissions(any<ActivityWriteRequest>()) } returns ActivityWritePermissions
            every { repo.plannedWorkoutWritePermissions() } returns PlannedWorkoutWritePermissions
            coEvery { repo.hasActivityWritePermission() } answers {
                if (permissionFailure != null) throw permissionFailure else canWrite
            }
            coEvery { repo.hasActivityWritePermission(any(), any(), any(), any(), any()) } answers {
                if (permissionFailure != null) throw permissionFailure else canWrite
            }
            coEvery { repo.hasActivityWritePermission(any<ActivityWriteRequest>()) } answers {
                if (permissionFailure != null) throw permissionFailure else canWrite
            }
            coEvery { repo.writeActivityEntry(any()) } answers {
                if (writeFailure != null) throw writeFailure else "activity-id"
            }
            coEvery { repo.loadWorkout(any()) } answers {
                if (loadWorkoutFailure != null) throw loadWorkoutFailure else workout
            }
            coEvery { repo.loadPlannedWorkout(any()) } answers {
                if (!canReadPlans) throw SecurityException("Missing Health Connect planned exercise read permission.")
                plannedWorkouts.firstOrNull { it.id == firstArg() }
            }
            coEvery { repo.loadExistingPlannedWorkouts(any()) } answers {
                if (canReadPlans) plannedWorkouts else throw SecurityException("Missing Health Connect planned exercise read permission.")
            }
            coEvery { repo.writePlannedWorkout(any()) } answers {
                if (canWritePlan) "saved-plan-id" else throw SecurityException("Missing Health Connect planned exercise write permission.")
            }
        }

    private fun activityPrefs(
        favoriteActivityExerciseType: Int? = null,
        lastActivityExerciseType: Int? = null,
    ): PreferencesRepository =
        mockk<PreferencesRepository>().also { prefs ->
            every { prefs.favoriteActivityExerciseType } returns favoriteActivityExerciseType
            every { prefs.lastActivityExerciseType } returns lastActivityExerciseType
            every { prefs.lastActivityExerciseType = any() } just runs
            // Integration off: the CoMaps start gate stays out of these tests' way.
            every { prefs.activityRecordingPreferences() } returns ActivityRecordingPreferences()
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
        private val PlannedWorkoutWritePermissions = setOf(
            "read_planned",
            "write_planned",
        )
    }

    @Test fun `toRepetitionSetInputs ignores timed active steps instead of treating them as rest`() {
        val plan = plannedPullUpPlan().copy(
            blocks = listOf(
                PlannedExerciseBlockData(
                    repetitions = 1,
                    description = null,
                    steps = listOf(
                        PlannedExerciseStepData(
                            exerciseType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_OTHER_WORKOUT,
                            exercisePhase = androidx.health.connect.client.records.PlannedExerciseStep.EXERCISE_PHASE_ACTIVE,
                            description = "Push-ups",
                            completion = PlannedExerciseCompletion.Repetitions(10),
                        ),
                        PlannedExerciseStepData(
                            exerciseType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_REST,
                            exercisePhase = androidx.health.connect.client.records.PlannedExerciseStep.EXERCISE_PHASE_REST,
                            description = null,
                            completion = PlannedExerciseCompletion.DurationSeconds(60),
                        ),
                        PlannedExerciseStepData(
                            exerciseType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_PLANK,
                            exercisePhase = androidx.health.connect.client.records.PlannedExerciseStep.EXERCISE_PHASE_ACTIVE,
                            description = null,
                            completion = PlannedExerciseCompletion.DurationSeconds(45),
                        ),
                    ),
                ),
            ),
        )

        val sets = plan.toRepetitionSetInputs(ownSegmentType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_OTHER_WORKOUT)

        assertEquals(
            listOf(
                ActivityRepetitionSetInput(repetitionsText = "10", restMinutesText = "60", label = "Push-ups"),
                ActivityRepetitionSetInput(
                    repetitionsText = "45",
                    segmentType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_PLANK,
                    isDuration = true,
                ),
            ),
            sets,
        )
    }

    @Test fun `a plan mixing exercises maps to the generic set type of its session type`() {
        val mixed = plannedPullUpPlan().copy(
            blocks = listOf(
                PlannedExerciseBlockData(
                    repetitions = 1,
                    description = null,
                    steps = listOf(
                        PlannedExerciseStepData(
                            exerciseType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_OTHER_WORKOUT,
                            exercisePhase = androidx.health.connect.client.records.PlannedExerciseStep.EXERCISE_PHASE_ACTIVE,
                            description = "Push-ups",
                            completion = PlannedExerciseCompletion.Repetitions(10),
                        ),
                        PlannedExerciseStepData(
                            exerciseType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_PLANK,
                            exercisePhase = androidx.health.connect.client.records.PlannedExerciseStep.EXERCISE_PHASE_ACTIVE,
                            description = null,
                            completion = PlannedExerciseCompletion.DurationSeconds(45),
                        ),
                    ),
                ),
            ),
        )

        assertEquals("calisthenics", mixed.toActivityEntryType()?.id)
        assertEquals(
            "strength_sets",
            mixed.copy(exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING).toActivityEntryType()?.id,
        )
        // A single-exercise plan still resolves to that exercise.
        assertEquals("pull_ups", plannedPullUpPlan().toActivityEntryType()?.id)
    }

    @Test fun `timed sets take their own seconds and segment type when building segments`() {
        val state = ActivityEntryUiState(
            selectedActivityType = DefaultActivityEntryTypes.first { it.id == "push_ups" },
            startDateText = "2026-08-26",
            startTimeText = "8:00",
            durationMinutesText = "5",
            repetitionMode = ActivityRepetitionEntryMode.SETS,
            repetitionSets = listOf(
                ActivityRepetitionSetInput(repetitionsText = "10", restMinutesText = "60"),
                ActivityRepetitionSetInput(
                    repetitionsText = "45",
                    restMinutesText = "30",
                    segmentType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_PLANK,
                    isDuration = true,
                ),
                ActivityRepetitionSetInput(repetitionsText = "10"),
            ),
        )
        val (start, end) = requireNotNull(activityEntrySessionRange(state))

        val segments = requireNotNull(buildActivityExerciseSegments(state, start, end))

        // 300 s total - 90 s rest - 45 s plank = 165 s shared by the two rep sets.
        assertEquals(5, segments.size)
        val plank = segments[2]
        assertEquals(ExerciseSegment.EXERCISE_SEGMENT_TYPE_PLANK, plank.segmentType)
        assertEquals(0, plank.repetitions)
        assertEquals(45L, java.time.Duration.between(plank.startTime, plank.endTime).seconds)
        assertEquals(ExerciseSegment.EXERCISE_SEGMENT_TYPE_OTHER_WORKOUT, segments[0].segmentType)
        assertEquals(10, segments[0].repetitions)
        assertEquals(83L, java.time.Duration.between(segments[0].startTime, segments[0].endTime).seconds)
        assertEquals(82L, java.time.Duration.between(segments[4].startTime, segments[4].endTime).seconds)
        assertEquals(end, segments.last().endTime)
    }
}

private fun plannedPushUpPlan(): PlannedExerciseData =
    PlannedExerciseData(
        id = "planned-push-id",
        title = "Push-up pyramid",
        exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS,
        startTime = Instant.parse("2026-05-26T09:30:00Z"),
        endTime = Instant.parse("2026-05-26T09:35:00Z"),
        hasExplicitTime = true,
        completedExerciseSessionId = null,
        notes = "Slow tempo",
        blockCount = 1,
        source = "tech.mmarca.openvitals",
        blocks = listOf(
            PlannedExerciseBlockData(
                repetitions = 1,
                description = "Main set",
                steps = listOf(
                    PlannedExerciseStepData(
                        exerciseType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_OTHER_WORKOUT,
                        exercisePhase = androidx.health.connect.client.records.PlannedExerciseStep.EXERCISE_PHASE_ACTIVE,
                        description = "Set 1",
                        completion = PlannedExerciseCompletion.Repetitions(12),
                    ),
                ),
            )
        ),
    )

private fun plannedPullUpPlan(): PlannedExerciseData =
    PlannedExerciseData(
        id = "planned-id",
        title = "Pull-up ladder",
        exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS,
        startTime = Instant.parse("2026-05-26T08:30:00Z"),
        endTime = Instant.parse("2026-05-26T08:35:00Z"),
        hasExplicitTime = true,
        completedExerciseSessionId = null,
        notes = "Strict reps",
        blockCount = 1,
        source = "tech.mmarca.openvitals",
        blocks = listOf(
            PlannedExerciseBlockData(
                repetitions = 1,
                description = "Main set",
                steps = listOf(
                    PlannedExerciseStepData(
                        exerciseType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_PULL_UP,
                        exercisePhase = androidx.health.connect.client.records.PlannedExerciseStep.EXERCISE_PHASE_ACTIVE,
                        description = "Set 1",
                        completion = PlannedExerciseCompletion.Repetitions(8),
                    ),
                    PlannedExerciseStepData(
                        exerciseType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_REST,
                        exercisePhase = androidx.health.connect.client.records.PlannedExerciseStep.EXERCISE_PHASE_REST,
                        description = "Rest",
                        completion = PlannedExerciseCompletion.DurationSeconds(60),
                    ),
                    PlannedExerciseStepData(
                        exerciseType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_PULL_UP,
                        exercisePhase = androidx.health.connect.client.records.PlannedExerciseStep.EXERCISE_PHASE_ACTIVE,
                        description = "Set 2",
                        completion = PlannedExerciseCompletion.Repetitions(6),
                    ),
                ),
            )
        ),
    )
