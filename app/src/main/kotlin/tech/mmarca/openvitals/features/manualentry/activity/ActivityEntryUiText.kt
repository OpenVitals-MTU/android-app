package tech.mmarca.openvitals.features.manualentry.activity

import tech.mmarca.openvitals.features.manualentry.*
import tech.mmarca.openvitals.features.manualentry.activity.*
import tech.mmarca.openvitals.features.manualentry.activity.recording.*
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.*
import tech.mmarca.openvitals.features.manualentry.body.*
import tech.mmarca.openvitals.features.manualentry.hydration.*
import tech.mmarca.openvitals.features.manualentry.mindfulness.*
import tech.mmarca.openvitals.features.manualentry.vitals.*



import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import tech.mmarca.openvitals.R

@Composable
internal fun FieldErrorText(errorText: String?) {
    if (errorText == null) return
    Text(
        text = errorText,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

@Composable
internal fun ActivityEntryUiState.validationErrorText(field: ActivityEntryField): String? =
    validationErrors.firstOrNull { it.field == field }?.validationMessage()

@Composable
internal fun ActivityEntryValidationError.validationMessage(): String = stringResource(
    when (this) {
        ActivityEntryValidationError.ACTIVITY_TYPE_DOES_NOT_SUPPORT_ROUTE ->
            R.string.activity_entry_error_activity_type_route
        ActivityEntryValidationError.TRAINING_PLAN_TITLE_REQUIRED ->
            R.string.activity_entry_error_training_plan_title_required
        ActivityEntryValidationError.START_DATE_INVALID -> R.string.activity_entry_error_start_date
        ActivityEntryValidationError.START_TIME_INVALID -> R.string.activity_entry_error_start_time
        ActivityEntryValidationError.START_TIME_AFTER_ROUTE_START ->
            R.string.activity_entry_error_start_time_after_route
        ActivityEntryValidationError.DURATION_INVALID -> R.string.activity_entry_error_duration
        ActivityEntryValidationError.REPETITIONS_INVALID -> R.string.activity_entry_error_repetitions
        ActivityEntryValidationError.DISTANCE_INVALID -> R.string.activity_entry_error_distance
        ActivityEntryValidationError.DISTANCE_UNSUPPORTED -> R.string.activity_entry_error_distance_unsupported
        ActivityEntryValidationError.ELEVATION_INVALID -> R.string.activity_entry_error_elevation
        ActivityEntryValidationError.ELEVATION_UNSUPPORTED -> R.string.activity_entry_error_elevation_unsupported
        ActivityEntryValidationError.ACTIVE_CALORIES_INVALID -> R.string.activity_entry_error_active_calories
        ActivityEntryValidationError.TOTAL_CALORIES_INVALID -> R.string.activity_entry_error_total_calories
        ActivityEntryValidationError.TOTAL_CALORIES_BELOW_ACTIVE ->
            R.string.activity_entry_error_total_calories_below_active
    }
)

@Composable
internal fun LocalDate.localizedDateText(): String =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        .withLocale(LocalLocale.current.platformLocale)
        .format(this)

@Composable
internal fun LocalTime.localizedTimeText(): String =
    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        .withLocale(LocalLocale.current.platformLocale)
        .format(this)

internal fun String.toStartDateOrNull(): LocalDate? =
    runCatching { LocalDate.parse(trim()) }.getOrNull()

internal fun String.toStartTimeOrNull(): LocalTime? =
    runCatching { LocalTime.parse(trim(), ActivityEntryTimeFormatter) }.getOrNull()

@Composable
internal fun activityEntryErrorText(
    error: ActivityEntryError,
    message: String?,
): String = when (error) {
    ActivityEntryError.INVALID_VALUE -> stringResource(R.string.activity_entry_invalid_value)
    ActivityEntryError.MISSING_WRITE_PERMISSION -> stringResource(R.string.activity_entry_permission_needed)
    ActivityEntryError.ROUTE_IMPORT_FAILED -> stringResource(
        R.string.activity_entry_route_import_failed,
        message ?: stringResource(R.string.unknown_error),
    )
    ActivityEntryError.LOCATION_PERMISSION_NEEDED -> stringResource(R.string.activity_entry_location_permission_needed)
    ActivityEntryError.NOTIFICATION_PERMISSION_NEEDED -> stringResource(R.string.activity_entry_notification_permission_needed)
    ActivityEntryError.ACTIVITY_RECOGNITION_PERMISSION_NEEDED ->
        stringResource(R.string.activity_entry_activity_recognition_permission_needed)
    ActivityEntryError.RECORDING_FAILED -> stringResource(
        R.string.activity_entry_recording_failed,
        message ?: stringResource(R.string.unknown_error),
    )
    ActivityEntryError.WRITE_FAILED -> stringResource(
        R.string.activity_entry_write_failed,
        message ?: stringResource(R.string.unknown_error),
    )
}

internal val ActivityEntryTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("H:mm")
