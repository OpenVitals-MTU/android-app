package tech.mmarca.openvitals.domain.model

/**
 * When a blood-pressure reading was taken relative to meals. Health Connect
 * has no field for it, so OpenVitals encodes it in its own `clientRecordId`.
 * Foreign records fall back to time-of-day inference.
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

/** Appends [context] to a clientRecordId, replacing any token. Null strips it. */
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
