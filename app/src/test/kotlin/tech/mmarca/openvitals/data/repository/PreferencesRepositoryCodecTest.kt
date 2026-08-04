package tech.mmarca.openvitals.data.repository

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.devices.FakeSharedPreferences
import tech.mmarca.openvitals.domain.preferences.ActivityRecordingDashboardField
import tech.mmarca.openvitals.domain.preferences.ActivityRecordingDashboardItemSize
import tech.mmarca.openvitals.domain.preferences.ActivityRecordingDashboardLayout
import tech.mmarca.openvitals.domain.preferences.ActivityRecordingDashboardTemplate
import tech.mmarca.openvitals.domain.preferences.ActivityRecordingPreferences

/**
 * Port of the Flutter `preferences_repository_codec_test.dart` suite:
 * characterization tests for the two hand-rolled string codecs inside
 * [PreferencesRepository]. They are written against the public API
 * (set -> reload from a fresh repository -> get), so the assertion goes
 * through the encoder AND the decoder, not an in-memory cache.
 */
class PreferencesRepositoryCodecTest {

    private fun contextFor(prefs: SharedPreferences): Context = mockk {
        every {
            getSharedPreferences(PreferencesRepository.PREFS_FILE, Context.MODE_PRIVATE)
        } returns prefs
    }

    private fun newRepo(initial: Map<String, String> = emptyMap()): Pair<PreferencesRepository, FakeSharedPreferences> {
        val prefs = FakeSharedPreferences()
        val editor = prefs.edit()
        initial.forEach { (key, value) -> editor.putString(key, value) }
        editor.apply()
        return PreferencesRepository(contextFor(prefs)) to prefs
    }

    /**
     * Writes through one repository, then reads through a fresh one — the
     * analog of the Dart `_roundTrip` helper.
     */
    private fun <T> roundTrip(
        write: (PreferencesRepository) -> Unit,
        read: (PreferencesRepository) -> T,
    ): T {
        val (writer, prefs) = newRepo()
        write(writer)
        return read(PreferencesRepository(contextFor(prefs)))
    }

    // region activity recording dashboard layout codec

    @Test fun `every template normalizes to LARGE_TOP - the others are storage-only`() {
        // Pinning what the code ACTUALLY does: `normalized()` resolves the grid
        // through the hardcoded LARGE_TOP recording template rather than the
        // instance's own `template` field, so a stored TWO_BY_FOUR comes back
        // as LARGE_TOP. Written down here so nobody "fixes" the round-trip into
        // a feature that does not exist.
        ActivityRecordingDashboardTemplate.entries.forEach { template ->
            val result = roundTrip(
                write = { repo ->
                    repo.setActivityRecordingDashboardLayout(
                        "running",
                        ActivityRecordingDashboardLayout(template = template),
                    )
                },
                read = { repo -> repo.activityRecordingDashboardLayout("running") },
            )
            assertEquals(
                "stored as ${template.name}",
                ActivityRecordingDashboardTemplate.LARGE_TOP,
                result.template,
            )
        }
    }

    @Test fun `round-trips the field order`() {
        val fields = listOf(
            ActivityRecordingDashboardField.SPEED,
            ActivityRecordingDashboardField.HEART_RATE,
            ActivityRecordingDashboardField.DISTANCE,
        )

        val result = roundTrip(
            write = { repo ->
                repo.setActivityRecordingDashboardLayout(
                    "running",
                    ActivityRecordingDashboardLayout(
                        template = ActivityRecordingDashboardTemplate.TWO_BY_FOUR,
                        fields = fields,
                    ),
                )
            },
            read = { repo -> repo.activityRecordingDashboardLayout("running") },
        )

        // Order is the whole point of the layout — it is what the user dragged.
        assertEquals(fields, result.fields.take(fields.size))
    }

    @Test fun `round-trips per-field sizes`() {
        val result = roundTrip(
            write = { repo ->
                repo.setActivityRecordingDashboardLayout(
                    "running",
                    ActivityRecordingDashboardLayout(
                        template = ActivityRecordingDashboardTemplate.LARGE_TOP,
                        fields = listOf(
                            ActivityRecordingDashboardField.HEART_RATE,
                            ActivityRecordingDashboardField.SPEED,
                        ),
                        sizes = mapOf(
                            ActivityRecordingDashboardField.HEART_RATE to
                                ActivityRecordingDashboardItemSize(columnSpan = 2, rowSpan = 2),
                        ),
                    ),
                )
            },
            read = { repo -> repo.activityRecordingDashboardLayout("running") },
        )

        val heartRate = result.items.first {
            it.field == ActivityRecordingDashboardField.HEART_RATE
        }
        assertEquals(2, heartRate.size.columnSpan)
        assertEquals(2, heartRate.size.rowSpan)
    }

    @Test fun `layouts are per activity type, not global`() {
        val (writer, prefs) = newRepo()
        writer.setActivityRecordingDashboardLayout(
            "running",
            ActivityRecordingDashboardLayout(
                fields = listOf(ActivityRecordingDashboardField.HEART_RATE),
            ),
        )
        writer.setActivityRecordingDashboardLayout(
            "cycling",
            ActivityRecordingDashboardLayout(
                fields = listOf(ActivityRecordingDashboardField.CADENCE),
            ),
        )

        val repo = PreferencesRepository(contextFor(prefs))
        assertEquals(
            ActivityRecordingDashboardField.HEART_RATE,
            repo.activityRecordingDashboardLayout("running").fields.first(),
        )
        assertEquals(
            ActivityRecordingDashboardField.CADENCE,
            repo.activityRecordingDashboardLayout("cycling").fields.first(),
        )
    }

    @Test fun `an unknown activity type falls back to the default layout`() {
        val (repo, _) = newRepo()
        assertEquals(
            ActivityRecordingDashboardLayout().template,
            repo.activityRecordingDashboardLayout("never-configured").template,
        )
    }

    @Test fun `a corrupt stored string degrades to the default, never throws`() {
        // The decoder parses a bespoke separator format. Garbage in the store —
        // a downgrade, a half-written value, a future format — must not take
        // the recording screen down with it.
        listOf(
            "",
            "NOT_A_TEMPLATE",
            "NOT_A_TEMPLATE|HEART_RATE=1x1",
            "|||",
        ).forEach { corrupt ->
            val (repo, _) = newRepo(
                mapOf("activity_recording_dashboard_layout_running" to corrupt),
            )
            val layout = runCatching { repo.activityRecordingDashboardLayout("running") }
            assertTrue("corrupt: \"$corrupt\"", layout.isSuccess)
            assertNotNull("corrupt: \"$corrupt\"", layout.getOrNull())
        }
    }

    @Test fun `an unknown field in a stored layout is dropped, not fatal`() {
        // Forward compatibility: a layout written by a NEWER build may name a
        // field this build has never heard of.
        val (repo, _) = newRepo(
            mapOf(
                "activity_recording_dashboard_layout_running" to
                    "LARGE_TOP|HEART_RATE=1x1,WARP_DRIVE_RPM=1x1",
            ),
        )

        val layout = repo.activityRecordingDashboardLayout("running")
        assertTrue(layout.fields.contains(ActivityRecordingDashboardField.HEART_RATE))
    }

    // endregion

    // region activity recording preferences — the null sentinels

    // These are stored as 0 meaning "off"/"null". Losing the distinction turns
    // "no route-gap limit" into "a route-gap limit of zero metres", which would
    // break the drawn line on every single fix.

    @Test fun `null route gap survives a round-trip as null, not zero`() {
        val result = roundTrip(
            write = { repo ->
                repo.setActivityRecordingPreferences(
                    ActivityRecordingPreferences(routeGapMeters = null),
                )
            },
            read = { repo -> repo.activityRecordingPreferences() },
        )
        assertNull(result.routeGapMeters)
    }

    @Test fun `null distance interval survives as null, not zero`() {
        val result = roundTrip(
            write = { repo ->
                repo.setActivityRecordingPreferences(
                    ActivityRecordingPreferences(recordingDistanceIntervalMeters = null),
                )
            },
            read = { repo -> repo.activityRecordingPreferences() },
        )
        assertNull(result.recordingDistanceIntervalMeters)
    }

    @Test fun `null voice intervals survive as null, not zero`() {
        val result = roundTrip(
            write = { repo ->
                repo.setActivityRecordingPreferences(
                    ActivityRecordingPreferences(
                        voiceAnnouncementTimeIntervalMinutes = null,
                        voiceAnnouncementDistanceIntervalMeters = null,
                    ),
                )
            },
            read = { repo -> repo.activityRecordingPreferences() },
        )
        assertNull(result.voiceAnnouncementTimeIntervalMinutes)
        assertNull(result.voiceAnnouncementDistanceIntervalMeters)
    }

    @Test fun `a real value round-trips as itself, and is not read as null`() {
        val result = roundTrip(
            write = { repo ->
                repo.setActivityRecordingPreferences(
                    ActivityRecordingPreferences(
                        routeGapMeters = 50,
                        recordingDistanceIntervalMeters = 10,
                    ).normalized(),
                )
            },
            read = { repo -> repo.activityRecordingPreferences() },
        )
        assertNotNull(result.routeGapMeters)
        assertNotNull(result.recordingDistanceIntervalMeters)
    }

    @Test fun `the booleans and the timeout round-trip`() {
        val result = roundTrip(
            write = { repo ->
                repo.setActivityRecordingPreferences(
                    ActivityRecordingPreferences(
                        autoIdleEnabled = false,
                        keepScreenOnDuringRecording = false,
                        voiceAnnouncementsEnabled = false,
                        restTimerBellEnabled = false,
                        autoIdleTimeoutSeconds = 45,
                    ).normalized(),
                )
            },
            read = { repo -> repo.activityRecordingPreferences() },
        )

        assertFalse(result.autoIdleEnabled)
        assertFalse(result.keepScreenOnDuringRecording)
        assertFalse(result.voiceAnnouncementsEnabled)
        assertFalse(result.restTimerBellEnabled)
        assertEquals(45, result.autoIdleTimeoutSeconds)
    }

    // endregion
}
