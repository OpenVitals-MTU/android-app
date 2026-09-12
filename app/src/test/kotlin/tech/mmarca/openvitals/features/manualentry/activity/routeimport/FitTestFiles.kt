package tech.mmarca.openvitals.features.manualentry.activity.routeimport

import java.io.ByteArrayOutputStream
import java.time.Instant

/** Builds small FIT files for tests: an activity with a route, a course, a workout. */
internal object FitTestFiles {

    fun activity(
        sport: Int,
        points: List<FitTestPoint>,
        sessionTime: Instant = points.firstOrNull()?.time ?: Instant.parse("2026-05-26T08:30:00Z"),
        elapsedSeconds: Long = points.lastOrNull()
            ?.time
            ?.epochSecond
            ?.minus(sessionTime.epochSecond)
            ?.coerceAtLeast(1L)
            ?: 60L,
        totalDistanceMeters: Double? = null,
        totalCaloriesKcal: Int? = null,
        totalAscentMeters: Int? = null,
    ): ByteArray {
        val data = ByteArrayOutputStream()
        data.writeFitFileId(fileType = 4)

        data.writeFitDefinition(
            localMessageType = 1,
            globalMessageNumber = 18,
            fields = listOf(
                FitTestFieldDefinition(number = 253, size = 4, baseType = 134),
                FitTestFieldDefinition(number = 2, size = 4, baseType = 134),
                FitTestFieldDefinition(number = 5, size = 1, baseType = 0),
                FitTestFieldDefinition(number = 7, size = 4, baseType = 134),
                FitTestFieldDefinition(number = 8, size = 4, baseType = 134),
                FitTestFieldDefinition(number = 9, size = 4, baseType = 134),
                FitTestFieldDefinition(number = 11, size = 2, baseType = 132),
                FitTestFieldDefinition(number = 21, size = 2, baseType = 132),
            ),
        )
        data.write(1)
        data.writeUInt32(sessionTime.plusSeconds(elapsedSeconds).fitTimestamp())
        data.writeUInt32(sessionTime.fitTimestamp())
        data.write(sport)
        data.writeUInt32(elapsedSeconds * 1000L)
        data.writeUInt32(elapsedSeconds * 1000L)
        data.writeNullableUInt32(totalDistanceMeters?.let { Math.round(it * 100.0) })
        data.writeNullableUInt16(totalCaloriesKcal)
        data.writeNullableUInt16(totalAscentMeters)

        val withSamples = points.any {
            it.heartRateBpm != null || it.cadence != null || it.speedMetersPerSecond != null
        }
        data.writeFitDefinition(
            localMessageType = 0,
            globalMessageNumber = 20,
            fields = buildList {
                add(FitTestFieldDefinition(number = 253, size = 4, baseType = 134))
                add(FitTestFieldDefinition(number = 0, size = 4, baseType = 133))
                add(FitTestFieldDefinition(number = 1, size = 4, baseType = 133))
                add(FitTestFieldDefinition(number = 2, size = 2, baseType = 132))
                if (withSamples) {
                    add(FitTestFieldDefinition(number = 3, size = 1, baseType = 2))
                    add(FitTestFieldDefinition(number = 4, size = 1, baseType = 2))
                    add(FitTestFieldDefinition(number = 6, size = 2, baseType = 132))
                }
            },
        )
        points.forEach { point ->
            data.write(0)
            data.writeUInt32(point.time.fitTimestamp())
            data.writeInt32(point.latitude.semicircles())
            data.writeInt32(point.longitude.semicircles())
            data.writeUInt16(point.altitudeRaw())
            if (withSamples) {
                data.write(point.heartRateBpm ?: 0xFF)
                data.write(point.cadence ?: 0xFF)
                data.writeNullableUInt16(
                    point.speedMetersPerSecond?.let { Math.round(it * 1000.0).toInt() },
                )
            }
        }

        return fitFileBytes(data)
    }

    fun course(
        name: String,
        sport: Int,
        points: List<FitTestPoint>,
        elapsedSeconds: Long,
        totalDistanceMeters: Double,
    ): ByteArray {
        val lapStart = points.firstOrNull()?.time ?: Instant.parse("2026-05-26T08:30:00Z")
        val data = ByteArrayOutputStream()
        data.writeFitFileId(fileType = 6)

        data.writeFitDefinition(
            localMessageType = 1,
            globalMessageNumber = 31,
            fields = listOf(
                FitTestFieldDefinition(number = 4, size = 1, baseType = 0),
                FitTestFieldDefinition(number = 5, size = 32, baseType = 7),
            ),
        )
        data.write(1)
        data.write(sport)
        data.writeFitString(name, size = 32)

        data.writeFitDefinition(
            localMessageType = 2,
            globalMessageNumber = 19,
            fields = listOf(
                FitTestFieldDefinition(number = 253, size = 4, baseType = 134),
                FitTestFieldDefinition(number = 2, size = 4, baseType = 134),
                FitTestFieldDefinition(number = 7, size = 4, baseType = 134),
                FitTestFieldDefinition(number = 8, size = 4, baseType = 134),
                FitTestFieldDefinition(number = 9, size = 4, baseType = 134),
                FitTestFieldDefinition(number = 21, size = 2, baseType = 132),
            ),
        )
        data.write(2)
        data.writeUInt32(lapStart.plusSeconds(elapsedSeconds).fitTimestamp())
        data.writeUInt32(lapStart.fitTimestamp())
        data.writeUInt32(elapsedSeconds * 1000L)
        data.writeUInt32(elapsedSeconds * 1000L)
        data.writeUInt32(Math.round(totalDistanceMeters * 100.0))
        data.writeUInt16(12)

        data.writeFitDefinition(
            localMessageType = 0,
            globalMessageNumber = 20,
            fields = listOf(
                FitTestFieldDefinition(number = 253, size = 4, baseType = 134),
                FitTestFieldDefinition(number = 0, size = 4, baseType = 133),
                FitTestFieldDefinition(number = 1, size = 4, baseType = 133),
                FitTestFieldDefinition(number = 2, size = 2, baseType = 132),
            ),
        )
        points.forEach { point ->
            data.write(0)
            data.writeUInt32(point.time.fitTimestamp())
            data.writeInt32(point.latitude.semicircles())
            data.writeInt32(point.longitude.semicircles())
            data.writeUInt16(point.altitudeRaw())
        }

        return fitFileBytes(data)
    }

    fun workout(
        name: String,
        sport: Int,
        timeStepSeconds: List<Int>,
    ): ByteArray {
        val data = ByteArrayOutputStream()
        data.writeFitFileId(fileType = 5)

        data.writeFitDefinition(
            localMessageType = 1,
            globalMessageNumber = 26,
            fields = listOf(
                FitTestFieldDefinition(number = 4, size = 1, baseType = 0),
                FitTestFieldDefinition(number = 6, size = 2, baseType = 132),
                FitTestFieldDefinition(number = 8, size = 32, baseType = 7),
            ),
        )
        data.write(1)
        data.write(sport)
        data.writeUInt16(timeStepSeconds.size)
        data.writeFitString(name, size = 32)

        data.writeFitDefinition(
            localMessageType = 0,
            globalMessageNumber = 27,
            fields = listOf(
                FitTestFieldDefinition(number = 254, size = 2, baseType = 132),
                FitTestFieldDefinition(number = 1, size = 1, baseType = 0),
                FitTestFieldDefinition(number = 2, size = 4, baseType = 134),
            ),
        )
        timeStepSeconds.forEachIndexed { index, seconds ->
            data.write(0)
            data.writeUInt16(index)
            data.write(0)
            data.writeUInt32(seconds * 1000L)
        }

        return fitFileBytes(data)
    }

    private fun ByteArrayOutputStream.writeFitFileId(fileType: Int) {
        writeFitDefinition(
            localMessageType = 3,
            globalMessageNumber = 0,
            fields = listOf(FitTestFieldDefinition(number = 0, size = 1, baseType = 0)),
        )
        write(3)
        write(fileType)
    }

    private fun fitFileBytes(data: ByteArrayOutputStream): ByteArray {
        val dataBytes = data.toByteArray()
        return ByteArrayOutputStream().apply {
            write(14)
            write(16)
            writeUInt16(0)
            writeUInt32(dataBytes.size.toLong())
            write(byteArrayOf('.'.code.toByte(), 'F'.code.toByte(), 'I'.code.toByte(), 'T'.code.toByte()))
            writeUInt16(0)
            write(dataBytes)
            writeUInt16(0)
        }.toByteArray()
    }

    private fun ByteArrayOutputStream.writeFitDefinition(
        localMessageType: Int,
        globalMessageNumber: Int,
        fields: List<FitTestFieldDefinition>,
    ) {
        write(0x40 or localMessageType)
        write(0)
        write(0)
        writeUInt16(globalMessageNumber)
        write(fields.size)
        fields.forEach { field ->
            write(field.number)
            write(field.size)
            write(field.baseType)
        }
    }

    private fun ByteArrayOutputStream.writeUInt16(value: Int) {
        write(value and 0xFF)
        write((value ushr 8) and 0xFF)
    }

    private fun ByteArrayOutputStream.writeUInt32(value: Long) {
        write((value and 0xFF).toInt())
        write(((value ushr 8) and 0xFF).toInt())
        write(((value ushr 16) and 0xFF).toInt())
        write(((value ushr 24) and 0xFF).toInt())
    }

    private fun ByteArrayOutputStream.writeNullableUInt32(value: Long?) {
        writeUInt32(value ?: 0xFFFFFFFFL)
    }

    private fun ByteArrayOutputStream.writeNullableUInt16(value: Int?) {
        writeUInt16(value ?: 0xFFFF)
    }

    private fun ByteArrayOutputStream.writeInt32(value: Int) {
        writeUInt32(value.toLong() and 0xFFFFFFFFL)
    }

    private fun ByteArrayOutputStream.writeFitString(value: String, size: Int) {
        val bytes = value.toByteArray(Charsets.UTF_8)
        val output = ByteArray(size)
        bytes.copyInto(output, endIndex = minOf(bytes.size, size - 1))
        write(output)
    }

    private fun Instant.fitTimestamp(): Long =
        epochSecond - 631_065_600L

    private fun Double.semicircles(): Int =
        Math.round(this * 2_147_483_648.0 / 180.0).toInt()

    private fun FitTestPoint.altitudeRaw(): Int =
        Math.round((altitudeMeters + 500.0) * 5.0).toInt()

    private data class FitTestFieldDefinition(
        val number: Int,
        val size: Int,
        val baseType: Int,
    )
}

internal data class FitTestPoint(
    val time: Instant,
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double,
    val heartRateBpm: Int? = null,
    val cadence: Int? = null,
    val speedMetersPerSecond: Double? = null,
)
