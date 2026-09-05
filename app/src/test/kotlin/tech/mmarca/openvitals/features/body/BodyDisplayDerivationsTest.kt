package tech.mmarca.openvitals.features.body

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.period.WeekPeriodMode
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.BmrEntry
import tech.mmarca.openvitals.domain.model.BodyFatEntry
import tech.mmarca.openvitals.domain.model.BodyMeasurementType
import tech.mmarca.openvitals.domain.model.HeightEntry
import tech.mmarca.openvitals.domain.model.WeightEntry
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.domain.query.BodyPeriodData

/** The body screen's derivations, through the internal helpers it calls. */
class BodyDisplayDerivationsTest {

    private val zone: ZoneId = ZoneId.systemDefault()
    private val mondayDate: LocalDate = LocalDate.of(2026, 3, 2)
    private val tuesdayDate: LocalDate = LocalDate.of(2026, 3, 3)
    private val monday: Instant = at(mondayDate, 8)
    private val tuesday: Instant = at(tuesdayDate, 8)

    private val formatter = UnitFormatter(
        unitSystemProvider = { UnitSystem.METRIC },
        localeProvider = { Locale.US },
    )

    // An empty period.

    @Test fun `an empty period has no data, no readings and no tracked metrics`() {
        val state = stateOf(BodyPeriodData())

        assertFalse(hasAnyBodyData(state, formatter))
        assertTrue(readings(state).isEmpty())
        assertTrue(trackedMetrics(state).isEmpty())
        assertNull(state.display.summary.bmi)
        // All nine metrics exist; they are simply empty.
        assertEquals(9, bodyMetricData(state, state.summary, formatter).size)
    }

    // The summary.

    @Test fun `the summary takes the latest reading, and the first weight`() {
        val state = stateOf(
            BodyPeriodData(
                weightEntries = listOf(weight(tuesday, 70.0), weight(monday, 72.0)),
                heightEntries = listOf(HeightEntry(time = monday, heightCm = 180.0, source = "test")),
                bodyFatEntries = listOf(BodyFatEntry(time = monday, percent = 20.0, source = "test")),
            )
        )

        val summary = state.display.summary
        assertEquals(70.0, summary.latestWeightKg!!, 0.0001)
        assertEquals(72.0, summary.firstWeightKg!!, 0.0001)
        assertEquals(-2.0, summary.weightChangeKg!!, 0.0001)
        assertEquals(180.0, summary.heightCm!!, 0.0001)
        // 70 / 1.8² = 21.6
        assertEquals(21.6, summary.bmi!!, 0.01)
        // Fat-free mass 56kg / 1.8² = 17.28, adjusted by 6.3 * (1.8 - 1.8) = 0.
        assertEquals(17.28, summary.ffmi!!, 0.01)
        assertEquals(17.28, summary.adjustedFfmi!!, 0.01)
    }

    // The daily series.

    @Test fun `the daily series keeps one value per day, that day's latest`() {
        val state = stateOf(
            BodyPeriodData(
                weightEntries = listOf(
                    weight(monday, 72.0),
                    weight(at(mondayDate, 18), 71.0),
                    weight(tuesday, 70.0),
                ),
            )
        )

        val weight = metric(state, R.string.metric_weight)
        assertEquals(2, weight.values.size)
        // Monday's evening reading wins; the days are in order.
        assertEquals(mondayDate, weight.values.first().date)
        assertEquals(71.0, weight.values.first().value, 0.0001)
        assertEquals(tuesdayDate, weight.values.last().date)
        assertEquals(70.0, weight.values.last().value, 0.0001)
        // The intraday samples keep every reading, oldest first.
        val samples = weight.dayValues.sortedBy { it.time }
        assertEquals(3, samples.size)
        assertEquals(72.0, samples.first().value, 0.0001)
        assertEquals(70.0, samples.last().value, 0.0001)
        assertTrue(weight.hasTrackedValues)
        assertEquals(
            listOf(R.string.metric_weight),
            trackedMetrics(state).map { it.titleRes },
        )
    }

    // BMI's series.

    @Test fun `BMI has a series only when a height is known`() {
        val withoutHeight = stateOf(BodyPeriodData(weightEntries = listOf(weight(monday, 72.0))))
        assertTrue(metric(withoutHeight, R.string.metric_bmi).values.isEmpty())
        assertTrue(bmiDayValues(withoutHeight.weightEntries, withoutHeight.summary.heightCm).isEmpty())

        val withHeight = stateOf(
            BodyPeriodData(
                weightEntries = listOf(weight(monday, 72.9)),
                heightEntries = listOf(HeightEntry(time = monday, heightCm = 180.0, source = "test")),
            )
        )
        val bmi = metric(withHeight, R.string.metric_bmi)
        assertEquals(22.5, bmi.values.single().value, 0.01)
        assertEquals(22.5, bmi.dayValues.single().value, 0.01)
        // FFMI never gets a series, in Flutter or here — only a latest value.
        assertTrue(metric(withHeight, R.string.metric_ffmi).values.isEmpty())
    }

    // The reading list.

    @Test fun `readings are newest first, indexed by day, and only OpenVitals ones are editable`() {
        val state = stateOf(
            BodyPeriodData(
                weightEntries = listOf(
                    weight(monday, 72.0, id = "w1", isOpenVitalsEntry = true),
                    // An OpenVitals entry with no id is not editable.
                    weight(at(mondayDate, 9), 71.5, isOpenVitalsEntry = true),
                    // Another app's entry never is.
                    weight(tuesday, 70.0, id = "w2"),
                ),
                bmrEntries = listOf(BmrEntry(time = at(tuesdayDate, 9), kcalPerDay = 1_800.0, source = "test")),
            )
        )

        val deleted = mutableListOf<Pair<BodyMeasurementType, String>>()
        val edited = mutableListOf<Pair<BodyMeasurementType, String>>()
        val all = readings(
            state = state,
            onEdit = { type, id -> edited += type to id },
            onDelete = { type, id -> deleted += type to id },
        )

        assertEquals(4, all.size)
        assertEquals(at(tuesdayDate, 9), all.first().time)
        assertEquals(at(mondayDate, 8), all.last().time)

        val editable = all.filter { it.onEdit != null }
        assertEquals(1, editable.size)
        assertEquals(editable, all.filter { it.onDelete != null })
        editable.single().onEdit!!.invoke()
        editable.single().onDelete!!.invoke()
        assertEquals(listOf(BodyMeasurementType.WEIGHT to "w1"), edited)
        assertEquals(listOf(BodyMeasurementType.WEIGHT to "w1"), deleted)

        assertEquals(2, all.onDate(mondayDate, zone).size)
        assertEquals(2, all.onDate(tuesdayDate, zone).size)
    }

    // The latest-value fallback.

    @Test fun `a period with a latest value but no entries still has data`() {
        // The provider can report an aggregate with no readings in the window.
        val state = stateOf(BodyPeriodData(latestWeightKg = 70.0))

        assertTrue(hasAnyBodyData(state, formatter))
        assertTrue(readings(state).isEmpty())
        assertEquals("70.0", metric(state, R.string.metric_weight).latest?.value)
        assertEquals(70.0, state.display.summary.latestWeightKg!!, 0.0001)
    }

    // Helpers.

    private fun at(date: LocalDate, hour: Int): Instant =
        date.atTime(hour, 0).atZone(zone).toInstant()

    private fun weight(
        time: Instant,
        kg: Double,
        id: String = "",
        isOpenVitalsEntry: Boolean = false,
    ) = WeightEntry(
        time = time,
        weightKg = kg,
        source = "test",
        id = id,
        isOpenVitalsEntry = isOpenVitalsEntry,
    )

    /** Mirrors what [BodyViewModel] publishes for a loaded period. */
    private fun stateOf(data: BodyPeriodData): BodyUiState {
        val query = PeriodLoadQuery(
            range = TimeRange.MONTH,
            anchorDate = mondayDate,
            weekPeriodMode = WeekPeriodMode.MONDAY_TO_SUNDAY,
        )
        val state = BodyUiState(
            isLoading = false,
            selectedRange = TimeRange.MONTH,
            selectedDate = mondayDate,
            weightEntries = data.weightEntries,
            heightEntries = data.heightEntries,
            bodyFatEntries = data.bodyFatEntries,
            leanMassEntries = data.leanMassEntries,
            bmrEntries = data.bmrEntries,
            boneMassEntries = data.boneMassEntries,
            bodyWaterMassEntries = data.bodyWaterMassEntries,
        )
        return state.copy(display = BodyPresentationMapper.build(query = query, data = data))
    }

    private fun metric(state: BodyUiState, titleRes: Int): BodyMetricData =
        bodyMetricData(state, state.summary, formatter).first { it.titleRes == titleRes }

    private fun trackedMetrics(state: BodyUiState): List<BodyMetricData> =
        bodyMetricData(state, state.summary, formatter).filter { it.hasTrackedValues }

    private fun readings(
        state: BodyUiState,
        onEdit: (BodyMeasurementType, String) -> Unit = { _, _ -> },
        onDelete: (BodyMeasurementType, String) -> Unit = { _, _ -> },
    ): List<BodyReadingItem> =
        bodyReadingItems(
            state = state,
            unitFormatter = formatter,
            weightLabel = "Weight",
            heightLabel = "Height",
            bodyFatLabel = "Body fat",
            leanMassLabel = "Lean mass",
            bmrLabel = "BMR",
            boneMassLabel = "Bone mass",
            bodyWaterMassLabel = "Body water",
            onEditBodyMeasurement = onEdit,
            onDeleteBodyMeasurement = onDelete,
        ).newestFirst()
}
