package tech.mmarca.openvitals.domain.model

/**
 * When a blood-pressure reading was taken relative to meals.
 *
 * Health Connect's BloodPressureRecord has NO field for this — only body
 * position and cuff location — so OpenVitals encodes the user's choice into
 * its own `clientRecordId` (which Health Connect stores and returns verbatim).
 * That keeps Health Connect as the only database: the context travels with
 * the record, needs no local table, and survives reinstalls. Records written
 * by other apps can never carry it and fall back to time-of-day inference.
 */
enum class BpMealContext(val token: String) {
    BEFORE_BREAKFAST("beforebreakfast"),
    AFTER_BREAKFAST("afterbreakfast"),
    BEFORE_LUNCH("beforelunch"),
    AFTER_LUNCH("afterlunch"),
    BEFORE_DINNER("beforedinner"),
    AFTER_DINNER("afterdinner"),
}

/** The marker separating the context token from the rest of the client id. */
private const val BpContextSeparator = "_bpctx-"

/**
 * Appends [context] to a clientRecordId, replacing any token already there.
 * A null context strips the token — the id goes back to its base form.
 */
fun String.withBpMealContext(context: BpMealContext?): String {
    val base = substringBefore(BpContextSeparator)
    return if (context == null) base else "$base$BpContextSeparator${context.token}"
}

/** The context encoded in [clientRecordId], or null (absent, foreign, or unknown token). */
fun bpMealContextFromClientRecordId(clientRecordId: String?): BpMealContext? {
    val token = clientRecordId
        ?.substringAfter(BpContextSeparator, missingDelimiterValue = "")
        ?.takeIf { it.isNotEmpty() }
        ?: return null
    return BpMealContext.entries.firstOrNull { it.token == token }
}
