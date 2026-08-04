package tech.mmarca.openvitals.features.devicesync.store

import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Mass
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.util.Base64
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import tech.mmarca.openvitals.features.devicesync.protocol.SyncBatch
import tech.mmarca.openvitals.features.devicesync.protocol.SyncItem
import tech.mmarca.openvitals.healthconnect.SyncedSourceOverlay

/**
 * Original-source preservation for phone-to-phone sync: the optional
 * `originPackage` wire field, its mixed-version compatibility in both
 * directions, its guaranteed absence from the content fingerprint, the
 * pass-through of an already-preserved origin on re-sync (A→B→C), and the
 * receiver-side store mapping selection.
 */
class SyncRecordOriginTest {

    private val gadgetbridge = "com.espruino.gadgetbridge.banglejs"
    private val openVitals = "tech.mmarca.openvitals"

    private fun record(clientRecordId: String = "x"): WeightRecord = WeightRecord(
        time = Instant.parse("2026-02-01T08:00:00Z"),
        zoneOffset = null,
        weight = Mass.kilograms(72.4),
        metadata = Metadata.manualEntry(
            device = Device(type = Device.TYPE_PHONE),
            clientRecordId = clientRecordId,
        ),
    )

    private fun item(originPackage: String?): SyncItem {
        val r = record()
        return SyncItem(
            key = syncFingerprint(r),
            recordType = syncRecordTypeName(r),
            payload = encodeSyncRecordPayload(r),
            originPackage = originPackage,
        )
    }

    // ── wire round-trip ──────────────────────────────────────────────────────

    @Test
    fun `origin survives a batch encode-decode round trip`() {
        val decoded = SyncBatch.decode(SyncBatch(1, listOf(item(gadgetbridge))).encode())

        assertEquals(gadgetbridge, decoded.items.single().originPackage)
    }

    @Test
    fun `an item without an origin round-trips as null`() {
        val decoded = SyncBatch.decode(SyncBatch(1, listOf(item(null))).encode())

        assertNull(decoded.items.single().originPackage)
    }

    @Test
    fun `a legacy batch without the origin field still decodes (old peer to new)`() {
        // Hand-built wire bytes exactly as a build predating the field sends
        // them: only k/t/p per item, gzipped — protocol v1 either way.
        val reference = item(null)
        val payloadB64 = Base64.getEncoder().encodeToString(reference.payload)
        val legacyJson =
            """{"seq":7,"items":[{"k":"${reference.key}","t":"${reference.recordType}","p":"$payloadB64"}]}"""
        val out = ByteArrayOutputStream()
        GZIPOutputStream(out).use { it.write(legacyJson.toByteArray(Charsets.UTF_8)) }

        val decoded = SyncBatch.decode(out.toByteArray())

        val decodedItem = decoded.items.single()
        assertEquals(reference.key, decodedItem.key)
        assertEquals(reference.recordType, decodedItem.recordType)
        assertArrayEquals(reference.payload, decodedItem.payload)
        assertNull(decodedItem.originPackage)
    }

    @Test
    fun `the origin rides as a separate ignorable field (new peer to old)`() {
        // An old build reads only k/t/p from each item object and ignores
        // unknown keys, so it is enough that those three fields are identical
        // with and without the origin — the "o" field is purely additive.
        val withOrigin = SyncBatch(1, listOf(item(gadgetbridge))).encode()
        val json = Json.parseToJsonElement(gunzip(withOrigin)).jsonObject
        val itemJson = json.getValue("items").jsonArray.single().jsonObject

        val reference = item(null)
        assertEquals(reference.key, itemJson.getValue("k").jsonPrimitive.content)
        assertEquals(reference.recordType, itemJson.getValue("t").jsonPrimitive.content)
        assertEquals(
            Base64.getEncoder().encodeToString(reference.payload),
            itemJson.getValue("p").jsonPrimitive.content,
        )
        assertEquals(gadgetbridge, itemJson.getValue("o").jsonPrimitive.content)
    }

    // ── fingerprint invariance ───────────────────────────────────────────────

    @Test
    fun `the origin never enters the fingerprint or the payload`() {
        val without = item(null)
        val with = item(gadgetbridge)

        // Same clientRecordId (dedup key) and byte-identical payload whether or
        // not the origin is attached — so records synced by builds with and
        // without the field keep converging on one Health Connect record.
        assertEquals(without.key, with.key)
        assertArrayEquals(without.payload, with.payload)
        // And the receiver-side recomputation agrees.
        val decoded = decodeSyncRecord(with.recordType, with.key, with.payload)
        assertEquals(without.key, syncFingerprint(decoded))
    }

    // ── pass-through (A→B→C) ─────────────────────────────────────────────────

    @Test
    fun `a preserved origin wins over the local dataOrigin on re-send`() {
        // On phone B the record's Health Connect dataOrigin is OpenVitals (B
        // wrote it), but B preserved Gadgetbridge for its fingerprint — C must
        // be told Gadgetbridge.
        val fingerprint = "sync_abc"

        assertEquals(
            gadgetbridge,
            resolveOriginalSource(
                clientRecordId = fingerprint,
                dataOriginPackage = openVitals,
                preservedOrigins = mapOf(fingerprint to gadgetbridge),
            ),
        )
    }

    @Test
    fun `a native record announces its Health Connect dataOrigin`() {
        assertEquals(
            gadgetbridge,
            resolveOriginalSource(
                clientRecordId = null,
                dataOriginPackage = gadgetbridge,
                preservedOrigins = emptyMap(),
            ),
        )
        // A synced record with no preserved origin (landed from an old-version
        // peer) still announces the local attribution rather than nothing.
        assertEquals(
            openVitals,
            resolveOriginalSource(
                clientRecordId = "sync_abc",
                dataOriginPackage = openVitals,
                preservedOrigins = emptyMap(),
            ),
        )
    }

    // ── receiver-side mapping selection ──────────────────────────────────────

    @Test
    fun `only a foreign non-blank origin is worth persisting`() {
        assertEquals(gadgetbridge, persistableOrigin(gadgetbridge, openVitals))
        // The receiver's default attribution is already right for these.
        assertNull(persistableOrigin(openVitals, openVitals))
        assertNull(persistableOrigin("", openVitals))
        assertNull(persistableOrigin(null, openVitals))
    }

    // ── device provenance (Metadata.device across the wire) ──────────────────

    private fun watchRecord(): WeightRecord = WeightRecord(
        time = Instant.parse("2026-02-01T08:00:00Z"),
        zoneOffset = null,
        weight = Mass.kilograms(72.4),
        metadata = Metadata.manualEntry(
            device = Device(manufacturer = "Garmin", model = "Venu 3", type = Device.TYPE_WATCH),
            clientRecordId = "x",
        ),
    )

    @Test
    fun `the recording device survives the payload round trip verbatim`() {
        val original = watchRecord()

        val decoded = decodeSyncRecord(
            recordType = syncRecordTypeName(original),
            clientRecordId = syncFingerprint(original),
            payload = encodeSyncRecordPayload(original),
        )

        val device = decoded.metadata.device
        assertEquals("Garmin", device?.manufacturer)
        assertEquals("Venu 3", device?.model)
        assertEquals(Device.TYPE_WATCH, device?.type)
    }

    @Test
    fun `a legacy payload without a device decodes with the pre-field phone default`() {
        // Strip the `device` key to reproduce a payload from a build that
        // predates it — the receiver must fall back, not fail.
        val original = watchRecord()
        val legacyPayload = encodeSyncRecordPayload(original)
            .toString(Charsets.UTF_8)
            .replace(Regex(""","device":\{[^}]*\}"""), "")
            .toByteArray(Charsets.UTF_8)

        val decoded = decodeSyncRecord(
            recordType = syncRecordTypeName(original),
            clientRecordId = syncFingerprint(original),
            payload = legacyPayload,
        )

        assertEquals(Device.TYPE_PHONE, decoded.metadata.device?.type)
    }

    @Test
    fun `an unrecognised device type clamps to unknown`() {
        val original = watchRecord()
        val payload = encodeSyncRecordPayload(original)
            .toString(Charsets.UTF_8)
            .replace(""""t":${Device.TYPE_WATCH}""", """"t":99""")
            .toByteArray(Charsets.UTF_8)

        val decoded = decodeSyncRecord(
            recordType = syncRecordTypeName(original),
            clientRecordId = syncFingerprint(original),
            payload = payload,
        )

        assertEquals(Device.TYPE_UNKNOWN, decoded.metadata.device?.type)
    }

    @Test
    fun `the device never enters the fingerprint`() {
        // Identical content, different recording hardware — the dedup key must
        // agree or a re-sync after a device-metadata change would duplicate.
        assertEquals(syncFingerprint(record()), syncFingerprint(watchRecord()))
    }

    // ── display overlay ──────────────────────────────────────────────────────

    @Test
    fun `the overlay substitutes the preserved origin for a synced record`() {
        val synced = record(clientRecordId = "sync_abc")
        val native = record(clientRecordId = "openvitals_manual_1")
        try {
            SyncedSourceOverlay.update(mapOf("sync_abc" to gadgetbridge))

            assertEquals(gadgetbridge, SyncedSourceOverlay.displaySource(synced.metadata))
            assertEquals(true, SyncedSourceOverlay.isSyncedRecord("sync_abc"))
            // Unmapped records keep their Health Connect attribution.
            assertEquals(
                native.metadata.dataOrigin.packageName,
                SyncedSourceOverlay.displaySource(native.metadata),
            )
            assertEquals(false, SyncedSourceOverlay.isSyncedRecord("openvitals_manual_1"))
            assertEquals(false, SyncedSourceOverlay.isSyncedRecord(null))
        } finally {
            SyncedSourceOverlay.update(emptyMap())
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun gunzip(bytes: ByteArray): String =
        GZIPInputStream(bytes.inputStream()).readBytes().toString(Charsets.UTF_8)
}
