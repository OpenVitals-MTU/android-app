package tech.mmarca.openvitals.features.homewidgets

import android.content.Context
import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.time.LocalDate
import java.util.Locale
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.insights.BodyEnergyBucketState
import tech.mmarca.openvitals.domain.insights.BodyEnergyConfidence
import tech.mmarca.openvitals.domain.insights.BodyEnergyPrimaryInfluence
import tech.mmarca.openvitals.domain.insights.BodyEnergyTimeline
import tech.mmarca.openvitals.domain.insights.BodyEnergyTimelinePoint
import tech.mmarca.openvitals.domain.preferences.UnitSystem

/**
 * A [Context] whose string lookups are deterministic in the resource id and the
 * format arguments.
 *
 * The widgets pick between strings — "Charged" or "Steady", "No data" or
 * "Today" — and it is the *choice* these tests are about, not the English. A
 * stable stand-in for each id lets a test assert the choice by naming the same
 * id, exactly as the Flutter tests name the l10n getter.
 */
internal fun stringResourceContext(): Context = mockk<Context>().also { context ->
    every { context.getString(any<Int>()) } answers { "res:${firstArg<Int>()}" }
    every { context.getString(any<Int>(), *anyVararg()) } answers {
        val formatArgs = invocation.args
            .drop(1)
            .flatMap { arg -> if (arg is Array<*>) arg.toList() else listOf(arg) }
            .joinToString(",")
        "res:${firstArg<Int>()}($formatArgs)"
    }
}

/**
 * Stubs [Uri]'s percent codec, which the route builders in `Screen` run through.
 *
 * Identity is faithful for every route argument these tests use — ISO dates,
 * metric ids and drink ids carry no reserved character.
 */
internal fun mockUriCodec() {
    mockkStatic(Uri::class)
    every { Uri.encode(any<String>()) } answers { firstArg<String>() }
    every { Uri.decode(any<String>()) } answers { firstArg<String>() }
}

internal fun unmockUriCodec() {
    unmockkStatic(Uri::class)
}

internal fun unitFormatter(
    unitSystem: UnitSystem = UnitSystem.METRIC,
): UnitFormatter = UnitFormatter({ unitSystem }, { Locale.US })

internal fun bodyEnergyTimeline(
    currentScore: Int,
    date: LocalDate = LocalDate.of(2026, 7, 10),
    startScore: Int = 70,
    charged: Int = 30,
    drained: Int = 12,
    scores: List<Int> = emptyList(),
): BodyEnergyTimeline =
    BodyEnergyTimeline(
        date = date,
        startScore = startScore,
        currentScore = currentScore,
        charged = charged,
        drained = drained,
        points = scores.mapIndexed { index, score ->
            BodyEnergyTimelinePoint(
                time = date.atStartOfDay().plusMinutes(5L * index).toInstant(java.time.ZoneOffset.UTC),
                score = score,
                delta = 0.0,
                state = BodyEnergyBucketState.REST,
                confidence = BodyEnergyConfidence.HIGH,
                charge = 0.0,
                intensityDrain = 0.0,
                activityEnergyDrain = 0.0,
                basalDrain = 0.0,
                stressDrain = 0.0,
                recoveryDebtDrain = 0.0,
                primaryInfluence = BodyEnergyPrimaryInfluence.STEADY,
            )
        },
        confidence = BodyEnergyConfidence.HIGH,
        confidenceReason = "test",
    )
