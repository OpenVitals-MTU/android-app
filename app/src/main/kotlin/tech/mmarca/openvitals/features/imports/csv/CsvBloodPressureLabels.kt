package tech.mmarca.openvitals.features.imports.csv

import android.content.Context
import android.content.res.Configuration
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import tech.mmarca.openvitals.core.presentation.BpBodyPositions
import tech.mmarca.openvitals.core.presentation.BpMeasurementLocations
import tech.mmarca.openvitals.core.presentation.bpBodyPositionLabelRes
import tech.mmarca.openvitals.core.presentation.bpMeasurementLocationLabelRes

/**
 * What a body position or cuff location cell can say, as normalised label
 * text mapped to its [BpRecordValues] constant.
 */
data class CsvBloodPressureLabels(
    val bodyPositions: Map<String, Int> = emptyMap(),
    val cuffLocations: Map<String, Int> = emptyMap(),
) {
    /** The body position [text] names, or null when it matches no label. */
    fun bodyPosition(text: String): Int? = bodyPositions[normalizeCsvLabel(text)]

    /** The cuff location [text] names, or null when it matches no label. */
    fun cuffLocation(text: String): Int? = cuffLocations[normalizeCsvLabel(text)]
}

/** Lowercase letters and digits only, so case, whitespace and punctuation do not matter. */
internal fun normalizeCsvLabel(text: String): String =
    text.lowercase(Locale.ROOT).filter { it.isLetterOrDigit() }

/**
 * Builds the lookup from [labelSets], most preferred first: a label two sets
 * share keeps the constant the earlier set gave it.
 */
internal fun csvLabelLookup(labelSets: List<Map<Int, String>>): Map<String, Int> {
    val lookup = linkedMapOf<String, Int>()
    for (labels in labelSets) {
        for ((code, label) in labels) lookup.putIfAbsent(normalizeCsvLabel(label), code)
    }
    return lookup
}

/** The labels in the app language, then English as the fallback. */
@Singleton
class CsvBloodPressureLabelSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    fun labels(): CsvBloodPressureLabels {
        val locales = listOf(Locale.getDefault(), Locale.ENGLISH).distinct()
        val strings = locales.map { locale ->
            context.createConfigurationContext(
                Configuration(context.resources.configuration).apply { setLocale(locale) },
            )
        }
        return CsvBloodPressureLabels(
            bodyPositions = csvLabelLookup(
                strings.map { localized ->
                    CsvBodyPositionLabelRes.mapValues { localized.getString(it.value) }
                },
            ),
            cuffLocations = csvLabelLookup(
                strings.map { localized ->
                    CsvCuffLocationLabelRes.mapValues { localized.getString(it.value) }
                },
            ),
        )
    }
}

/** The options a user can pick as a default, in the order the dropdown lists them. */
internal val CsvBodyPositionLabelRes: Map<Int, Int> =
    BpBodyPositions.associateWith(::bpBodyPositionLabelRes)

internal val CsvCuffLocationLabelRes: Map<Int, Int> =
    BpMeasurementLocations.associateWith(::bpMeasurementLocationLabelRes)
