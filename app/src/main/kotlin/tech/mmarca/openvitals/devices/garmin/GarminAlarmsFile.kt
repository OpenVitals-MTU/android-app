package tech.mmarca.openvitals.devices.garmin

import java.io.ByteArrayOutputStream
import java.time.DayOfWeek
import java.time.Instant
import tech.mmarca.openvitals.core.fit.FitBaseType
import tech.mmarca.openvitals.core.fit.FitEncoder
import tech.mmarca.openvitals.core.fit.FitEncoderField
import tech.mmarca.openvitals.core.fit.fitTimestamp

/** How the watch announces an alarm. The order is the FIT `tone` enum. */
enum class GarminAlarmSound { OFF, SOUND, VIBRATION, SOUND_AND_VIBRATION }

/** The watch's preset alarm names. The order is the FIT `alarm_label` enum. */
enum class GarminAlarmLabel {
    NONE, WAKE_UP, WORKOUT, REMINDER, APPOINTMENT, TRAINING, CLASS, MEDITATE, BEDTIME
}

/** One alarm on a watch that has no settings tree. No [days] means it rings once. */
data class GarminAlarm(
    val hour: Int,
    val minute: Int,
    val days: Set<DayOfWeek> = emptySet(),
    val enabled: Boolean = true,
    val sound: GarminAlarmSound = GarminAlarmSound.SOUND_AND_VIBRATION,
    val backlight: Boolean = true,
    val label: GarminAlarmLabel = GarminAlarmLabel.NONE,
) {
    init {
        require(hour in 0..23 && minute in 0..59) { "Not a time of day: $hour:$minute" }
    }

    val minuteOfDay: Int get() = hour * 60 + minute

    /** Monday is bit 0 and Sunday bit 6. Bit 7 alone means once. */
    val repeatMask: Long
        get() = if (days.isEmpty()) RepeatOnce else days.sumOf { 1L shl (it.value - 1) }

    companion object {
        const val RepeatOnce = 128L
    }
}

/**
 * The FIT settings file that sets a watch's alarms, from upstream's
 * `GarminSupport.onSetAlarms` (AGPLv3). The file replaces every alarm on the
 * watch. Upstream sends no alarm record for an empty list, and so does this.
 */
object GarminAlarmsFile {

    /** What upstream offers a watch without a settings tree. */
    const val MaxAlarms = 10

    fun build(alarms: List<GarminAlarm>, now: Instant): ByteArray {
        require(alarms.size <= MaxAlarms) { "A watch holds at most $MaxAlarms alarms." }
        val timestamp = fitTimestamp(now)
        val encoder = FitEncoder()

        encoder.defineMessage(
            FitLocalFileId, FitFileIdMessage,
            listOf(
                FitEncoderField(FitFileIdTypeField, FitBaseType.ENUM),
                FitEncoderField(FitFileIdManufacturerField, FitBaseType.UINT16),
                FitEncoderField(FitFileIdProductField, FitBaseType.UINT16),
                FitEncoderField(FitFileIdTimeCreatedField, FitBaseType.UINT32),
                FitEncoderField(FitFileIdSerialNumberField, FitBaseType.UINT32Z),
                FitEncoderField(FitFileIdNumberField, FitBaseType.UINT16),
            ),
        )
        encoder.writeMessage(
            FitLocalFileId,
            values = mapOf(
                FitFileIdTypeField to FitFileTypeSettings,
                FitFileIdManufacturerField to FitManufacturerGarmin,
                FitFileIdProductField to FitProductConnect,
                FitFileIdTimeCreatedField to timestamp,
                FitFileIdSerialNumberField to 1L,
                FitFileIdNumberField to 1L,
            ),
        )

        if (alarms.isEmpty()) return encoder.toBytes()

        encoder.defineMessage(
            FitLocalAlarm, FitAlarmSettingsMessage,
            listOf(
                FitEncoderField(FitAlarmTimeField, FitBaseType.UINT16),
                FitEncoderField(FitAlarmRepeatField, FitBaseType.UINT32Z),
                FitEncoderField(FitAlarmEnabledField, FitBaseType.ENUM),
                FitEncoderField(FitAlarmSoundField, FitBaseType.ENUM),
                FitEncoderField(FitAlarmBacklightField, FitBaseType.ENUM),
                FitEncoderField(FitAlarmTimeCreatedField, FitBaseType.UINT32),
                FitEncoderField(FitAlarmSnoozeField, FitBaseType.UINT8),
                FitEncoderField(FitAlarmLabelField, FitBaseType.ENUM),
                FitEncoderField(FitMessageIndexField, FitBaseType.UINT16),
            ),
        )
        alarms.forEachIndexed { index, alarm ->
            encoder.writeMessage(
                FitLocalAlarm,
                values = mapOf(
                    FitAlarmTimeField to alarm.minuteOfDay.toLong(),
                    FitAlarmRepeatField to alarm.repeatMask,
                    FitAlarmEnabledField to alarm.enabled.toFitBoolean(),
                    FitAlarmSoundField to alarm.sound.ordinal.toLong(),
                    FitAlarmBacklightField to alarm.backlight.toFitBoolean(),
                    FitAlarmTimeCreatedField to timestamp,
                    FitAlarmSnoozeField to 0L,
                    FitAlarmLabelField to alarm.label.ordinal.toLong(),
                    FitMessageIndexField to index.toLong(),
                ),
            )
        }

        // Upstream writes the alarms a second time, as device-settings arrays.
        val count = alarms.size
        encoder.defineMessage(
            FitLocalDeviceSettings, FitDeviceSettingsMessage,
            listOf(
                FitEncoderField(FitSettingsAlarmsTimeField, FitBaseType.UINT16, size = count * 2),
                FitEncoderField(FitSettingsAlarmsModeField, FitBaseType.ENUM, size = count),
                FitEncoderField(FitSettingsAlarmsEnabledField, FitBaseType.ENUM, size = count),
                FitEncoderField(FitSettingsAlarmsRepeatField, FitBaseType.UINT32Z, size = count * 4),
            ),
        )
        encoder.writeMessage(
            FitLocalDeviceSettings,
            arrays = mapOf(
                FitSettingsAlarmsTimeField to alarms.map { it.minuteOfDay.toLong() },
                FitSettingsAlarmsModeField to alarms.map { FitAlarmModeCustom },
                FitSettingsAlarmsEnabledField to alarms.map { it.enabled.toFitBoolean() },
                FitSettingsAlarmsRepeatField to alarms.map { it.repeatMask },
            ),
        )

        return encoder.toBytes()
    }
}

private fun FitEncoder.toBytes(): ByteArray = ByteArrayOutputStream().also(::writeTo).toByteArray()

private fun Boolean.toFitBoolean(): Long = if (this) 1L else 0L

// The FIT vocabulary this file writes, from Garmin's FIT profile.
private const val FitLocalFileId = 0
private const val FitLocalAlarm = 1
private const val FitLocalDeviceSettings = 2

private const val FitFileIdMessage = 0
private const val FitDeviceSettingsMessage = 2
private const val FitAlarmSettingsMessage = 222

private const val FitMessageIndexField = 254

private const val FitFileIdTypeField = 0
private const val FitFileIdManufacturerField = 1
private const val FitFileIdProductField = 2
private const val FitFileIdSerialNumberField = 3
private const val FitFileIdTimeCreatedField = 4
private const val FitFileIdNumberField = 5

private const val FitAlarmTimeField = 0
private const val FitAlarmRepeatField = 1
private const val FitAlarmEnabledField = 2
private const val FitAlarmSoundField = 3
private const val FitAlarmBacklightField = 4
private const val FitAlarmTimeCreatedField = 5
private const val FitAlarmSnoozeField = 7
private const val FitAlarmLabelField = 8

private const val FitSettingsAlarmsTimeField = 8
private const val FitSettingsAlarmsModeField = 9
private const val FitSettingsAlarmsEnabledField = 28
private const val FitSettingsAlarmsRepeatField = 92

private const val FitFileTypeSettings = 2L

/** `alarm_mode` custom: the repeat mask says which days. */
private const val FitAlarmModeCustom = 5L

/** Upstream's values: the watch is known to accept a file that claims them. */
private const val FitManufacturerGarmin = 1L
private const val FitProductConnect = 65534L
