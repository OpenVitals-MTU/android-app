package tech.mmarca.openvitals.features.imports.applehealth

/**
 * The text report of an Apple Health analysis or import, to copy, save or send.
 * Structure only: counts, types, units and reasons, and the import log, whose
 * error text is masked. Never a record's time or value, a file location, a
 * source's name, or which workouts lost their route: the card lists those.
 */
internal fun buildAppleHealthReportText(
    parsed: AppleParsedExport,
    imported: Int,
    selectedCategories: Set<AppleHealthImportCategory>?,
    summaries: List<AppleHealthImportTypeSummary>,
    categorySummaries: List<AppleHealthImportCategorySummary>,
    diagnostics: List<AppleHealthImportDiagnostic>,
    diagnosticSummaries: List<AppleHealthImportDiagnosticSummary>,
    importLogs: List<String>,
): String = buildString {
    appendAppleHealthReportHeader()
    appendLine()
    appendLine("Summary")
    appendLine("Parsed records: ${parsed.parsedRecords}")
    appendLine("Parsed workouts: ${parsed.parsedWorkouts}")
    appendLine("Parsed correlations: ${parsed.parsedCorrelations}")
    appendLine("Parsed activity summaries: ${parsed.parsedActivitySummaries}")
    val totals = summaries.totals()
    appendLine("Converted Health Connect records: ${totals.converted}")
    appendLine("Imported Health Connect records: $imported")
    appendLine("Duplicate skipped: ${totals.duplicateSkipped}")
    appendLine("Not selected: ${totals.notSelected}")
    appendLine("Unsupported: ${totals.unsupported}")
    appendLine("Skipped: ${totals.skipped}")
    appendLine("Failed: ${totals.failed}")
    appendLine("Workout routes incomplete: ${parsed.workoutRouteArchiveFailure != null}")
    if (selectedCategories != null) {
        appendLine("Selected categories: ${selectedCategories.joinToString { it.reportName }}")
    }
    appendWorkoutsWithoutRoute(diagnosticSummaries)
    appendLine()
    appendLine("Logs")
    if (importLogs.isEmpty()) {
        appendLine("No import log entries were recorded.")
    } else {
        importLogs.forEach { entry -> appendLine(entry) }
    }
    appendLine()
    appendLine("By Import Category")
    if (categorySummaries.isEmpty()) {
        appendLine("No Health Connect-compatible categories were detected.")
    } else {
        categorySummaries.forEach { summary ->
            val routeText = if (summary.routeSessions > 0) ", routeSessions=${summary.routeSessions}" else ""
            appendLine("- ${summary.category.reportName}: converted=${summary.convertedRecords}$routeText")
        }
    }
    appendLine()
    appendLine("By Apple Type")
    summaries.forEach { summary ->
        appendLine(
            "- ${summary.appleType}: parsed=${summary.parsed}, converted=${summary.converted}, " +
                "imported=${summary.imported}, duplicate=${summary.duplicateSkipped}, " +
                "notSelected=${summary.notSelected}, unsupported=${summary.unsupported}, " +
                "skipped=${summary.skipped}, failed=${summary.failed}",
        )
    }
    appendDiagnosticSummary(diagnosticSummaries)
    appendRawDiagnostics(diagnostics)
}

private fun StringBuilder.appendWorkoutsWithoutRoute(diagnosticSummaries: List<AppleHealthImportDiagnosticSummary>) {
    val byType = diagnosticSummaries
        .filter { it.reasonCode == "workout_route_unavailable" }
        .groupBy { it.appleType }
        .mapValues { (_, grouped) -> grouped.sumOf { it.count } }
        .toSortedMap()
    if (byType.isEmpty()) return
    appendLine()
    appendLine("Workouts imported without their route")
    appendLine(
        "${byType.values.sum()} workout(s) refer to a route file that is not in the export. The import card lists " +
            "them. Import export.zip rather than export.xml, or import each route with the GPX/KML/KMZ/TCX importer.",
    )
    byType.forEach { (appleType, count) -> appendLine("- $appleType: $count") }
}

private fun StringBuilder.appendDiagnosticSummary(diagnosticSummaries: List<AppleHealthImportDiagnosticSummary>) {
    appendLine()
    appendLine("Diagnostic Summary")
    if (diagnosticSummaries.isEmpty()) {
        appendLine("No failures, skips, duplicates, or unsupported entries were recorded.")
        return
    }
    val reasonSummaries = diagnosticSummaries.groupBy { it.reasonCode }
        .mapValues { (_, grouped) -> grouped.sumOf { it.count } }
        .toSortedMap()
    appendLine(
        "Grouped diagnostic types: ${diagnosticSummaries.size}; " +
            reasonSummaries.entries.joinToString(", ") { (reason, count) -> "$reason=$count" },
    )
    diagnosticSummaries
        .sortedWith(
            compareByDescending<AppleHealthImportDiagnosticSummary> { it.count }
                .thenBy { it.reasonCode }
                .thenBy { it.appleType },
        )
        .forEachIndexed { index, diagnostic ->
            val unitText = diagnostic.exampleUnit?.let { "; unit=$it" }.orEmpty()
            appendLine(
                "${index + 1}. count=${diagnostic.count}; reason=${diagnostic.reasonCode}; appleType=${diagnostic.appleType}; " +
                    "target=${diagnostic.targetType ?: "none"}; detail=${diagnostic.detail}$unitText",
            )
        }
}

private fun StringBuilder.appendRawDiagnostics(diagnostics: List<AppleHealthImportDiagnostic>) {
    appendLine()
    appendLine("Raw Diagnostic Log")
    if (diagnostics.isEmpty()) {
        appendLine("No raw diagnostics were recorded.")
        return
    }
    if (diagnostics.size >= MaxRawDiagnostics) {
        appendLine(
            "Raw diagnostics were capped at $MaxRawDiagnostics per source; " +
                "see Diagnostic Summary above for complete counts.",
        )
    }
    diagnostics.forEachIndexed { index, diagnostic ->
        appendLine(
            "${index + 1}. reason=${diagnostic.reasonCode}; appleType=${diagnostic.appleType}; " +
                "target=${diagnostic.targetType ?: "none"}; unit=${diagnostic.unit ?: "none"}; detail=${diagnostic.detail}",
        )
    }
}
