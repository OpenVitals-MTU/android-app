package tech.mmarca.openvitals.features.imports.applehealth

import androidx.health.connect.client.records.SleepSessionRecord

internal fun AppleHealthImportConverter.convertSleep(
    records: List<AppleRecord>,
    trackConsumedRecords: Boolean = true,
): List<ConvertedAppleRecord> {
    val sleepRecords = records.filter { it.type == AppleSleepAnalysis }
    if (sleepRecords.isEmpty()) return emptyList()
    if (trackConsumedRecords) {
        sleepRecords.forEach { consumedRecordFingerprints += it.sourceFingerprint }
    }

    val groups = sleepRecords
        .mapNotNull { record ->
            val start = record.startDate ?: return@mapNotNull null.also { invalid(record, "Sleep record is missing startDate.") }
            val end = record.endDate ?: return@mapNotNull null.also { invalid(record, "Sleep record is missing endDate.") }
            val stage = record.rawValue.toSleepStageType()
                ?: return@mapNotNull null.also { invalid(record, "Sleep stage value is unsupported.") }
            SleepStageCandidate(
                record = record,
                start = start,
                end = end,
                stage = stage,
                inBedOnly = record.rawValue == AppleSleepInBed,
            )
        }
        .groupBy { "${it.record.sourceName.orEmpty()}|${it.record.device.orEmpty()}" }

    return groups.values.flatMap { candidates ->
        candidates.sortedBy { it.start.instant }.splitSleepSessions().mapNotNull { session ->
            val sessionStart = session.minOf { it.start.instant }
            val sessionEnd = session.maxOf { it.end.instant }
            if (!sessionEnd.isAfter(sessionStart)) {
                invalid(AppleSleepAnalysis, "Sleep session has no positive duration.", "$sessionStart..$sessionEnd")
                return@mapNotNull null
            }
            val detailedStages = session.filterNot { it.inBedOnly }.takeIf { it.isNotEmpty() } ?: session
            val stages = detailedStages
                .sortedBy { it.start.instant }
                .fold(mutableListOf<SleepSessionRecord.Stage>()) { acc, candidate ->
                    val clippedStart = maxOf(candidate.start.instant, acc.lastOrNull()?.endTime ?: sessionStart)
                    val clippedEnd = minOf(candidate.end.instant, sessionEnd)
                    if (clippedEnd.isAfter(clippedStart)) {
                        acc += SleepSessionRecord.Stage(
                            startTime = clippedStart,
                            endTime = clippedEnd,
                            stage = candidate.stage,
                        )
                    }
                    acc
                }
            if (stages.isEmpty()) {
                invalid(AppleSleepAnalysis, "Sleep session did not contain any valid non-overlapping stages.", "$sessionStart..$sessionEnd")
                return@mapNotNull null
            }
            val first = session.first().record
            val fingerprint = buildStableClientRecordId(
                "sleep",
                listOf("sleep", sessionStart.toString(), sessionEnd.toString(), session.joinToString(";") { it.record.stableParts() }),
            )
            markConverted(AppleSleepAnalysis)
            ConvertedAppleRecord(
                appleType = AppleSleepAnalysis,
                targetType = "SleepSessionRecord",
                fingerprint = fingerprint,
                recordType = SleepSessionRecord::class,
                record = SleepSessionRecord(
                    startTime = sessionStart,
                    startZoneOffset = first.startDate?.offset,
                    endTime = sessionEnd,
                    endZoneOffset = first.endDate?.offset ?: first.startDate?.offset,
                    metadata = appleMetadata("SleepSessionRecord", fingerprint),
                    title = "Apple Health sleep",
                    notes = null,
                    stages = stages,
                ),
                sourceTimeRange = AppleImportTimeRange(sessionStart, sessionEnd),
                unit = null,
                value = "stages=${stages.size}",
            )
        }
    }
}
