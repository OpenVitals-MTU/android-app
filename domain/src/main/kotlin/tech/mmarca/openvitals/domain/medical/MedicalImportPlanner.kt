package tech.mmarca.openvitals.domain.medical

import java.time.LocalDate
import tech.mmarca.openvitals.domain.model.MedicalRecordSource

/** Where a group's records go. */
sealed interface MedicalImportTarget {
    /** One of this app's sources, found by base URI. Re-importing the same file updates it. */
    data class Existing(val source: MedicalRecordSource) : MedicalImportTarget

    /** A source to create. [baseUri] is fixed by the file, or comes from [name] when the file names no origin. */
    data class New(val baseUri: String, val name: String, val fromName: Boolean) : MedicalImportTarget
}

/**
 * One group's plan. [fhirVersion] is the matched source's when there is one: a data source
 * has one version. [otherApp] is another app's source for the same origin; such a group
 * is skipped unless the user includes it, so a re-import never duplicates it.
 */
data class MedicalImportGroupPlan(
    val group: FhirImportGroup,
    val target: MedicalImportTarget,
    val fhirVersion: String,
    val otherApp: MedicalRecordSource? = null,
    val include: Boolean = otherApp == null,
) {
    val targetName: String get() = when (target) {
        is MedicalImportTarget.Existing -> target.source.displayName
        is MedicalImportTarget.New -> target.name
    }
}

/**
 * Matches each group to a data source. A source is matched by base URI, never by name.
 * When the file states the group's FHIR version, the version must match too: one origin
 * in two versions is two sources. Health Connect keeps display names unique per app and
 * never renames a source, so a new source whose name is taken gets the import date.
 */
object MedicalImportPlanner {

    /** [otherSources] are the sources of the other apps this app can read. */
    fun plan(
        analysis: FhirImportAnalysis,
        ownSources: List<MedicalRecordSource>,
        otherSources: List<MedicalRecordSource>,
    ): List<MedicalImportGroupPlan> = analysis.groups.map { group ->
        val otherApp = group.baseUri?.let { base -> otherSources.firstOrNull { FhirSourceGrouper.sameBase(it.fhirBaseUri, base) } }
        target(group, group.suggestedName, ownSources).let { target ->
            MedicalImportGroupPlan(group, target, versionFor(target, group), otherApp)
        }
    }

    /** The plan after the user renames a new source. A source the file names keeps its base URI. */
    fun rename(plan: MedicalImportGroupPlan, name: String, ownSources: List<MedicalRecordSource>): MedicalImportGroupPlan {
        val target = target(plan.group, name.trim().ifEmpty { plan.group.suggestedName }, ownSources)
        return plan.copy(target = target, fhirVersion = versionFor(target, plan.group))
    }

    /** [name], or [name] with the date when this app already has a source called that. */
    fun uniqueName(name: String, ownSources: List<MedicalRecordSource>, today: LocalDate): String {
        val taken = ownSources.map { it.displayName }.toSet()
        if (name !in taken) return name
        val dated = "$name ($today)"
        if (dated !in taken) return dated
        return generateSequence(2) { it + 1 }.map { "$dated $it" }.first { it !in taken }
    }

    private fun target(group: FhirImportGroup, name: String, ownSources: List<MedicalRecordSource>): MedicalImportTarget {
        val fromName = group.baseUri == null
        val baseUri = group.baseUri ?: FhirSourceGrouper.importBaseUri(name)
        return ownSources.firstOrNull { matches(it, baseUri, group) }
            ?.let { MedicalImportTarget.Existing(it) }
            ?: MedicalImportTarget.New(baseUri, name, fromName)
    }

    private fun matches(source: MedicalRecordSource, baseUri: String, group: FhirImportGroup): Boolean =
        FhirSourceGrouper.sameBase(source.fhirBaseUri, baseUri) && (!group.versionStated || source.fhirVersion == group.fhirVersion)

    private fun versionFor(target: MedicalImportTarget, group: FhirImportGroup): String =
        (target as? MedicalImportTarget.Existing)?.source?.fhirVersion ?: group.fhirVersion
}
