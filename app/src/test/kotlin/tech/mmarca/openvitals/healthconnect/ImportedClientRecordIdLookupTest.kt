package tech.mmarca.openvitals.healthconnect

import android.content.Context
import android.os.Process
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.testing.FakeHealthConnectClient
import androidx.health.connect.client.units.Mass
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import tech.mmarca.openvitals.features.imports.csv.buildCsvClientRecordId

/**
 * The importers' duplicate lookup. The two importers namespace their ids differently
 * (`apple_health_` and `csv_`), and the Kotlin port added a prefix guard.
 */
class ImportedClientRecordIdLookupTest {

    private val fake = FakeHealthConnectClient()

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>(), any()) } returns 0
        every { Log.e(any(), any<String>(), any()) } returns 0
        mockkStatic(Process::class)
        every { Process.myUid() } returns 10_000
        mockkObject(HealthConnectClient.Companion)
        every { HealthConnectClient.getOrCreate(any(), any()) } returns fake
    }

    @After
    fun tearDown() {
        unmockkObject(HealthConnectClient.Companion)
        unmockkStatic(Process::class)
        unmockkStatic(Log::class)
    }

    @Test
    fun `an apple health id already in Health Connect comes back as a match`() = runBlocking {
        val id = "apple_health_weightrecord_0123456789abcdef0123456789abcdef"
        fake.insertRecords(listOf(weightWith(id)))

        val matched = manager().findMatchingImportedClientRecordIds(
            recordType = WeightRecord::class,
            start = TIME.minusSeconds(1),
            end = TIME.plusSeconds(1),
            wantedIds = setOf(id, "apple_health_weightrecord_never_written"),
        )

        assertThat(matched).containsExactly(id)
        Unit
    }

    /**
     * FAILS TODAY. The lookup drops every id not starting with `apple_health_`, so
     * `CsvImportService.countExisting` never finds anything.
     */
    @Test
    fun `a csv id already in Health Connect comes back as a match`() = runBlocking {
        val id = buildCsvClientRecordId(targetType = "WeightRecord", utc = TIME)
        fake.insertRecords(listOf(weightWith(id)))

        val matched = manager().findMatchingImportedClientRecordIds(
            recordType = WeightRecord::class,
            start = TIME.minusSeconds(1),
            end = TIME.plusSeconds(1),
            wantedIds = setOf(id),
        )

        assertThat(matched).containsExactly(id)
        Unit
    }

    /** An id nobody asked about is never reported, whatever its namespace. */
    @Test
    fun `an id outside the wanted set is not reported`() = runBlocking {
        fake.insertRecords(
            listOf(weightWith("apple_health_weightrecord_aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")),
        )

        val matched = manager().findMatchingImportedClientRecordIds(
            recordType = WeightRecord::class,
            start = TIME.minusSeconds(1),
            end = TIME.plusSeconds(1),
            wantedIds = setOf("apple_health_weightrecord_bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"),
        )

        assertThat(matched).isEmpty()
    }

    private fun manager(): HealthConnectManager {
        val context = mockk<Context>(relaxed = true)
        every { context.packageName } returns APP_PACKAGE
        return HealthConnectManager(
            context = context,
            syncGate = mockk(relaxed = true),
            mindfulnessGate = mockk(relaxed = true),
        )
    }

    private fun weightWith(clientRecordId: String) = WeightRecord(
        time = TIME,
        zoneOffset = ZoneOffset.UTC,
        weight = Mass.kilograms(78.4),
        metadata = Metadata.manualEntry(
            device = Device(type = Device.TYPE_PHONE),
            clientRecordId = clientRecordId,
        ),
    )

    private companion object {
        val TIME: Instant = Instant.parse("2026-07-01T08:12:00Z")
        const val APP_PACKAGE = "tech.mmarca.openvitals"
    }
}
