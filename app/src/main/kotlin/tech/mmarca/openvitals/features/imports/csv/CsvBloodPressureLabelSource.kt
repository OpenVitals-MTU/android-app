package tech.mmarca.openvitals.features.imports.csv

import android.content.Context
import android.content.res.Configuration
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import tech.mmarca.openvitals.core.presentation.BpBodyPositionLabels
import tech.mmarca.openvitals.core.presentation.BpMeasurementLocationLabels

/**
 * The words a body position or cuff location cell may say: the app's own
 * labels in its language, then in English. [Locale.getDefault] follows the
 * in-app language.
 */
@Singleton
class CsvBloodPressureLabelSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    fun labels(): CsvBloodPressureLabels {
        val localized = listOf(Locale.getDefault(), Locale.ENGLISH).distinct().map { locale ->
            context.createConfigurationContext(
                Configuration(context.resources.configuration).apply { setLocale(locale) },
            )
        }
        return CsvBloodPressureLabels(
            bodyPositions = localized.map { strings ->
                BpBodyPositionLabels.mapValues { (_, label) -> strings.getString(label) }
            },
            cuffLocations = localized.map { strings ->
                BpMeasurementLocationLabels.mapValues { (_, label) -> strings.getString(label) }
            },
        )
    }
}
