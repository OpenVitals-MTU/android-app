package tech.mmarca.openvitals.healthconnect

import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.BloodGlucose
import androidx.health.connect.client.units.celsius
import androidx.health.connect.client.units.millimetersOfMercury
import androidx.health.connect.client.units.percent
import java.time.Instant
import java.time.ZoneId
import kotlin.reflect.KClass
import tech.mmarca.openvitals.domain.model.BpRecordValues
import tech.mmarca.openvitals.domain.model.GlucoseContext
import tech.mmarca.openvitals.domain.model.GlucoseRecordValues
import tech.mmarca.openvitals.domain.model.VitalsMeasurementEntry
import tech.mmarca.openvitals.domain.model.VitalsMeasurementType
import tech.mmarca.openvitals.domain.model.VitalsMeasurementWriteRequest
import tech.mmarca.openvitals.domain.model.Vo2MaxRecordValues
import tech.mmarca.openvitals.domain.model.bpMealContextFromClientRecordId
import tech.mmarca.openvitals.domain.model.bpPulseFromClientRecordId

/** The Health Connect record each manually entered vital is saved as. */
internal val VitalsMeasurementType.recordType: KClass<out Record>
    get() = when (this) {
        VitalsMeasurementType.BLOOD_PRESSURE -> BloodPressureRecord::class
        VitalsMeasurementType.SPO2 -> OxygenSaturationRecord::class
        VitalsMeasurementType.RESPIRATORY_RATE -> RespiratoryRateRecord::class
        VitalsMeasurementType.BODY_TEMPERATURE -> BodyTemperatureRecord::class
        VitalsMeasurementType.HRV -> HeartRateVariabilityRmssdRecord::class
        VitalsMeasurementType.BLOOD_GLUCOSE -> BloodGlucoseRecord::class
        VitalsMeasurementType.VO2_MAX -> Vo2MaxRecord::class
    }

/** The request as its record under [metadata]. An unset detail is written as Health Connect's unknown. */
internal fun VitalsMeasurementWriteRequest.toRecord(metadata: Metadata): Record {
    val zoneOffset = ZoneId.systemDefault().rules.getOffset(time)
    return when (type) {
        VitalsMeasurementType.BLOOD_PRESSURE -> BloodPressureRecord(
            time = time,
            zoneOffset = zoneOffset,
            metadata = metadata,
            systolic = value.millimetersOfMercury,
            diastolic = requireNotNull(secondaryValue).millimetersOfMercury,
            bodyPosition = bpBodyPosition ?: BpRecordValues.BODY_POSITION_UNKNOWN,
            measurementLocation = bpMeasurementLocation ?: BpRecordValues.MEASUREMENT_LOCATION_UNKNOWN,
        )
        VitalsMeasurementType.SPO2 -> OxygenSaturationRecord(
            time = time,
            zoneOffset = zoneOffset,
            percentage = value.percent,
            metadata = metadata,
        )
        VitalsMeasurementType.RESPIRATORY_RATE -> RespiratoryRateRecord(
            time = time,
            zoneOffset = zoneOffset,
            rate = value,
            metadata = metadata,
        )
        VitalsMeasurementType.BODY_TEMPERATURE -> BodyTemperatureRecord(
            time = time,
            zoneOffset = zoneOffset,
            metadata = metadata,
            temperature = value.celsius,
        )
        VitalsMeasurementType.HRV -> HeartRateVariabilityRmssdRecord(
            time = time,
            zoneOffset = zoneOffset,
            heartRateVariabilityMillis = value,
            metadata = metadata,
        )
        VitalsMeasurementType.BLOOD_GLUCOSE -> BloodGlucoseRecord(
            time = time,
            zoneOffset = zoneOffset,
            metadata = metadata,
            level = BloodGlucose.millimolesPerLiter(value),
            specimenSource = glucose.specimenSource ?: GlucoseRecordValues.SPECIMEN_SOURCE_UNKNOWN,
            mealType = glucose.mealType ?: GlucoseRecordValues.MEAL_TYPE_UNKNOWN,
            relationToMeal = glucose.relationToMeal ?: GlucoseRecordValues.RELATION_TO_MEAL_UNKNOWN,
        )
        VitalsMeasurementType.VO2_MAX -> Vo2MaxRecord(
            time = time,
            zoneOffset = zoneOffset,
            metadata = metadata,
            vo2MillilitersPerMinuteKilogram = value,
            measurementMethod = vo2MaxMeasurementMethod ?: Vo2MaxRecordValues.MEASUREMENT_METHOD_OTHER,
        )
    }
}

/** A vitals record as the entry form reads it. Health Connect's unknowns come back as null. */
internal fun Record.toVitalsMeasurementEntry(isOpenVitalsEntry: Boolean): VitalsMeasurementEntry {
    fun entry(type: VitalsMeasurementType, time: Instant, value: Double) = VitalsMeasurementEntry(
        id = metadata.id,
        type = type,
        time = time,
        value = value,
        source = SyncedSourceOverlay.displaySource(metadata),
        isOpenVitalsEntry = isOpenVitalsEntry,
    )
    return when (this) {
        is BloodPressureRecord -> entry(VitalsMeasurementType.BLOOD_PRESSURE, time, systolic.inMillimetersOfMercury).copy(
            secondaryValue = diastolic.inMillimetersOfMercury,
            bpMealContext = bpMealContextFromClientRecordId(metadata.clientRecordId),
            bpPulseBpm = bpPulseFromClientRecordId(metadata.clientRecordId),
            bpBodyPosition = bodyPosition.takeIf { it != BpRecordValues.BODY_POSITION_UNKNOWN },
            bpMeasurementLocation = measurementLocation.takeIf { it != BpRecordValues.MEASUREMENT_LOCATION_UNKNOWN },
        )
        is OxygenSaturationRecord -> entry(VitalsMeasurementType.SPO2, time, percentage.value)
        is RespiratoryRateRecord -> entry(VitalsMeasurementType.RESPIRATORY_RATE, time, rate)
        is BodyTemperatureRecord -> entry(VitalsMeasurementType.BODY_TEMPERATURE, time, temperature.inCelsius)
        is HeartRateVariabilityRmssdRecord -> entry(VitalsMeasurementType.HRV, time, heartRateVariabilityMillis)
        is BloodGlucoseRecord -> entry(VitalsMeasurementType.BLOOD_GLUCOSE, time, level.inMillimolesPerLiter).copy(
            glucose = GlucoseContext(
                relationToMeal = relationToMeal.takeIf { it != GlucoseRecordValues.RELATION_TO_MEAL_UNKNOWN },
                mealType = mealType.takeIf { it != GlucoseRecordValues.MEAL_TYPE_UNKNOWN },
                specimenSource = specimenSource.takeIf { it != GlucoseRecordValues.SPECIMEN_SOURCE_UNKNOWN },
            ),
        )
        is Vo2MaxRecord -> entry(VitalsMeasurementType.VO2_MAX, time, vo2MillilitersPerMinuteKilogram).copy(
            vo2MaxMeasurementMethod = measurementMethod.takeIf { it != Vo2MaxRecordValues.MEASUREMENT_METHOD_OTHER },
        )
        else -> error("Not a vitals record: ${this::class.simpleName}")
    }
}
