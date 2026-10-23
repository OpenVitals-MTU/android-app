package tech.mmarca.openvitals.features.imports.applehealth

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.io.ByteArrayInputStream
import kotlinx.coroutines.test.runTest
import org.junit.Test
import tech.mmarca.openvitals.core.privacy.PrivacySentinels
import tech.mmarca.openvitals.data.repository.AppleHealthImportRepository

/**
 * The report is meant to be shared, so no record's value or time, no source's
 * name and no file location may reach it: not through a refused record, a
 * duplicate, a workout without its route, or a failed write.
 */
class AppleHealthImportReportPrivacyTest {

    private val janesWatch = "${PrivacySentinels.Name}'s Watch"

    private val xml = """
        <HealthData>
            <Record type="HKQuantityTypeIdentifierHeight" sourceName="$janesWatch" unit="cm" value="-${PrivacySentinels.Value}"
                startDate="${PrivacySentinels.Date} 07:30:00 +0000" endDate="${PrivacySentinels.Date} 07:30:00 +0000" />
            <Record type="HKCategoryTypeIdentifierMenstrualFlow" sourceName="$janesWatch" value="HKCategoryValueMenstrualFlowMedium"
                startDate="${PrivacySentinels.Date} 07:30:00 +0000" endDate="${PrivacySentinels.Date} 07:30:00 +0000" />
            <Record type="HKCategoryTypeIdentifierMenstrualFlow" sourceName="$janesWatch" value="HKCategoryValueMenstrualFlowMedium"
                startDate="${PrivacySentinels.Date} 07:30:00 +0000" endDate="${PrivacySentinels.Date} 07:30:00 +0000" />
            <Record type="HKQuantityTypeIdentifierBodyMass" sourceName="$janesWatch" unit="kg" value="${PrivacySentinels.Value}"
                startDate="${PrivacySentinels.Date} 07:30:00 +0000" endDate="${PrivacySentinels.Date} 07:30:00 +0000" />
            <Workout workoutActivityType="HKWorkoutActivityTypeRunning" sourceName="$janesWatch"
                startDate="${PrivacySentinels.Date} 07:30:00 +0000" endDate="${PrivacySentinels.Date} 08:00:00 +0000"
                duration="30" durationUnit="min">
                <WorkoutRoute sourceName="$janesWatch">
                    <FileReference path="/workout-routes/route_${PrivacySentinels.Date}_jane-export.gpx" />
                </WorkoutRoute>
            </Workout>
        </HealthData>
    """.trimIndent()

    @Test
    fun `an import whose records are refused, repeated, route-less and unwritable reports none of their data`() = runTest {
        val export = PickedExport(xml.toByteArray())
        // As Health Connect words a refusal: values, times and identifiers, never a person's name.
        coEvery { export.repository.insertImportedRecords(any()) } throws IllegalArgumentException(
            "value ${PrivacySentinels.Value} at ${PrivacySentinels.Instant} for ${PrivacySentinels.Email} " +
                "from ${PrivacySentinels.Uri}\nweight ${PrivacySentinels.Value} refused",
        )

        val result = export.service.importAppleHealthExport(export.uri)

        assertThat(result.failedRecords).isGreaterThan(0)
        assertThat(result.diagnostics.map { it.reasonCode })
            .containsAtLeast("invalid", "duplicate_in_file", "workout_route_unavailable", "insert_failed")
        PrivacySentinels.assertNoneIn(result.shareableReportText)
        assertThat(result.shareableReportText).doesNotContain("Watch")
        assertThat(result.shareableReportText).contains("insert_failed")
        assertThat(result.workoutsWithoutRoute).hasSize(1)
    }

    @Test
    fun `an analysis reports none of the data either`() = runTest {
        val export = PickedExport(xml.toByteArray())

        val result = export.service.analyzeAppleHealthExport(export.uri)

        PrivacySentinels.assertNoneIn(result.shareableReportText)
        assertThat(result.shareableReportText).doesNotContain("Watch")
    }

    @Test
    fun `a failed import's report masks the error`() {
        val error = IllegalStateException(
            "write failed for ${PrivacySentinels.Path}",
            IllegalArgumentException("${PrivacySentinels.Sentence}\nsecond line by ${PrivacySentinels.Name}"),
        )

        val report = buildFailureReportText(error, workerLogs = listOf("2026-01-01T08:00:00Z [WORKER] started"))

        PrivacySentinels.assertNoneIn(report, except = setOf(PrivacySentinels.Name))
        assertThat(report).doesNotContain("second line")
        assertThat(report).contains("Caused by: java.lang.IllegalArgumentException: value #")
    }

    @Test
    fun `the workouts without a route survive a round trip through their file`() {
        val workouts = listOf(
            AppleWorkoutWithoutRoute("HKWorkoutActivityTypeRunning", java.time.Instant.parse(PrivacySentinels.Instant), java.time.Instant.parse("2026-03-14T08:00:00Z")),
        )

        assertThat(decodeWorkoutsWithoutRoute(encodeWorkoutsWithoutRoute(workouts))).isEqualTo(workouts)
        assertThat(decodeWorkoutsWithoutRoute("not\ta\tworkout\nnor this")).isEmpty()
    }

    private class PickedExport(bytes: ByteArray) {
        val uri = mockk<Uri>()
        val repository = mockk<AppleHealthImportRepository>()
        val service: AppleHealthImportService

        init {
            val resolver = mockk<ContentResolver>()
            val context = mockk<Context>()
            every { context.contentResolver } returns resolver
            every { resolver.openInputStream(uri) } answers { ByteArrayInputStream(bytes) }
            every { repository.isMindfulnessAvailable() } returns true
            coEvery { repository.findMatchingImportedClientRecordIds(any(), any(), any(), any()) } returns emptySet()
            coEvery { repository.insertImportedRecords(any()) } returns Unit
            service = AppleHealthImportService(context, repository)
        }
    }
}
