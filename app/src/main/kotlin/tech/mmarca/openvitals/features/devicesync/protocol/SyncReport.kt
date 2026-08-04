package tech.mmarca.openvitals.features.devicesync.protocol

import java.time.Instant

/**
 * Outcome + progress models for a sync session, mirroring the Apple Health
 * import's counters so the report screen can render them the same way.
 */

/** Coarse phase of a session, for the progress label. */
enum class SyncPhase {
    HANDSHAKE,
    AUTHENTICATING,
    EXCHANGING,
    WRITING,
    COMPLETE,
    ABORTED,
}

/** Live progress emitted during a session (drives the progress screen). */
data class SyncProgress(
    val phase: SyncPhase,
    /** Our records handed to the peer so far. */
    val itemsSent: Int = 0,
    /** Peer records received so far. */
    val itemsReceived: Int = 0,
    /** Peer records actually written to Health Connect so far (post-dedup). */
    val itemsWritten: Int = 0,
)

/** Per-record-type tally in the final report. */
data class SyncTypeSummary(
    val recordType: String,
    val received: Int = 0,
    val imported: Int = 0,
    val duplicateSkipped: Int = 0,
)

/**
 * The final result of a session, shown on the report screen. Each device
 * reports what IT wrote (bidirectional merge means both sides have one).
 */
data class SyncReport(
    /** True if the session finished cleanly; false if it aborted. */
    val completed: Boolean,
    val peerDeviceName: String,
    val negotiatedTypes: List<String>,
    /** Our records sent to the peer. */
    val itemsSent: Int,
    /** Peer records received. */
    val itemsReceived: Int,
    /** Peer records written to Health Connect (new). */
    val imported: Int,
    /** Peer records skipped because Health Connect already had them. */
    val duplicateSkipped: Int,
    val typeSummaries: List<SyncTypeSummary>,
    /** Set when [completed] is false — why the session ended early. */
    val abortReason: String? = null,
)

/** Accumulates per-type tallies during a run and folds them into a [SyncReport]. */
class SyncReportBuilder {
    private val byType = mutableMapOf<String, SyncTypeSummary>()
    var itemsSent: Int = 0
    var itemsReceived: Int = 0
        private set
    var imported: Int = 0
        private set
    var duplicateSkipped: Int = 0
        private set

    /**
     * Records one received item. [imported] is true only when it was actually
     * written to Health Connect (a fresh item whose write succeeded);
     * [duplicate] is true when it was skipped as already present. A fresh item
     * whose write FAILED is neither — counted as received but not imported, so
     * the report cannot claim "imported N" when nothing landed.
     */
    fun recordReceived(recordType: String, imported: Boolean = false, duplicate: Boolean = false) {
        itemsReceived += 1
        if (imported) this.imported += 1
        if (duplicate) duplicateSkipped += 1
        val current = byType[recordType] ?: SyncTypeSummary(recordType = recordType)
        byType[recordType] = current.copy(
            received = current.received + 1,
            imported = current.imported + if (imported) 1 else 0,
            duplicateSkipped = current.duplicateSkipped + if (duplicate) 1 else 0,
        )
    }

    fun build(
        completed: Boolean,
        peerDeviceName: String,
        negotiatedTypes: List<String>,
        abortReason: String? = null,
    ): SyncReport = SyncReport(
        completed = completed,
        peerDeviceName = peerDeviceName,
        negotiatedTypes = negotiatedTypes,
        itemsSent = itemsSent,
        itemsReceived = itemsReceived,
        imported = imported,
        duplicateSkipped = duplicateSkipped,
        typeSummaries = byType.values.sortedBy { it.recordType },
        abortReason = abortReason,
    )
}

/**
 * Formats a [SyncReport] as the shareable plain-text report shown on the report
 * screen and written to `DeviceSyncReportStore`. English by design, like the
 * Apple Health import report file — it is a technical artifact, not UI text.
 * [generatedAt] is injected so the output is deterministic in tests.
 */
fun buildSyncReportText(report: SyncReport, generatedAt: Instant): String = buildString {
    appendLine("OpenVitals — Sync With Another Phone report")
    appendLine("Generated: $generatedAt")
    appendLine("Peer: ${report.peerDeviceName}")
    appendLine("Status: ${if (report.completed) "completed" else "aborted"}")
    if (!report.completed && report.abortReason != null) {
        appendLine("Reason: ${report.abortReason}")
    }
    appendLine()
    appendLine("Summary")
    appendLine("Sent: ${report.itemsSent}")
    appendLine("Received: ${report.itemsReceived}")
    appendLine("Imported: ${report.imported}")
    appendLine("Already had (skipped): ${report.duplicateSkipped}")
    appendLine()
    appendLine("By data type")
    if (report.typeSummaries.isEmpty()) {
        appendLine("(none)")
    } else {
        report.typeSummaries.forEach { summary ->
            appendLine(
                "${summary.recordType}: received ${summary.received}, " +
                    "imported ${summary.imported}, skipped ${summary.duplicateSkipped}",
            )
        }
    }
}
