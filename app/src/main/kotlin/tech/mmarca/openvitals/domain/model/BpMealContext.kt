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

/** The marker separating the pulse token from the rest of the client id. */
private const val BpPulseSeparator = "_bppulse-"

/** The suffix that marks the heart rate record carrying a reading's pulse. */
private const val BpPulseSuffix = "_pulse"

/** The client id with both tokens stripped. */
private fun String.bpBaseClientRecordId(): String =
    substringBefore(BpContextSeparator).substringBefore(BpPulseSeparator)

private fun String.withBpTokens(context: BpMealContext?, pulseBpm: Int?): String = buildString {
    append(this@withBpTokens)
    if (context != null) append(BpContextSeparator).append(context.token)
    if (pulseBpm != null) append(BpPulseSeparator).append(pulseBpm)
}

/** Appends [context] to a clientRecordId, replacing any token and keeping the pulse. Null strips it. */
fun String.withBpMealContext(context: BpMealContext?): String =
    bpBaseClientRecordId().withBpTokens(context, bpPulseFromClientRecordId(this))

/**
 * Appends the pulse [bpm] to a clientRecordId, replacing any token and keeping the meal
 * context. Null strips it. The pulse also lives in a heart rate record of its own, see
 * [bpPulseClientRecordId]; the token is what lets a list of readings show it without a
 * second read.
 */
fun String.withBpPulse(bpm: Int?): String =
    bpBaseClientRecordId().withBpTokens(bpMealContextFromClientRecordId(this), bpm)

/**
 * The clientRecordId of the heart rate record holding the pulse of the blood pressure
 * record [bpClientRecordId]. Derived from the id with its tokens stripped, so an edit of
 * the context or the pulse keeps pointing at the same pulse record.
 */
fun bpPulseClientRecordId(bpClientRecordId: String): String =
    bpClientRecordId.bpBaseClientRecordId() + BpPulseSuffix

/** The context encoded in [clientRecordId], or null (absent, foreign, or unknown token). */
fun bpMealContextFromClientRecordId(clientRecordId: String?): BpMealContext? {
    val token = clientRecordId
        ?.substringAfter(BpContextSeparator, missingDelimiterValue = "")
        ?.substringBefore(BpPulseSeparator)
        ?.takeIf { it.isNotEmpty() }
        ?: return null
    return BpMealContext.entries.firstOrNull { it.token == token }
}

/** The pulse encoded in [clientRecordId], or null (absent, foreign, or not a number). */
fun bpPulseFromClientRecordId(clientRecordId: String?): Int? =
    clientRecordId
        ?.substringAfter(BpPulseSeparator, missingDelimiterValue = "")
        ?.substringBefore(BpContextSeparator)
        ?.toIntOrNull()
        ?.takeIf { it > 0 }
