# Medical Records Through Health Connect

> **Status:** Proposal draft, 2026-09-25. Revised on 2026-09-26 after a review pass. Nothing is implemented.
> **Sources:** the Android Medical Records guides, the Jetpack `connect-client` 1.2.0-alpha06 sources, the Play policy pages, the SMART Health Cards and HL7 CDA specifications, and the current app code. Links are at the end.
> **Scope:** phase 1 is import ("upload"), view, export ("download"), and manual entry of FHIR records through OpenVitals, with Health Connect as the store, plus optional saved documents and phone-to-phone sync. Phase 2 adds document parsers, each a feature of its own.
> **Implementation map:** [Feature map](../features/feature-map.md), [Feature playbook](../engineering/feature-playbook.md), [Health Connect](../app/health-connect.md), [Permissions](../app/permissions.md), [Privacy](../app/privacy.md).

## Summary

Health Connect can hold medical records: vaccines, allergies, conditions, medications, lab results, procedures, visits, and more. Apps write them with one permission and read them with one permission per category. OpenVitals already ships the Jetpack library that exposes this API, so the toolchain is ready.

Phase 1 adds a Medical records area to OpenVitals, reached from a dashboard tile, with five jobs:

- **View.** Show the records Health Connect holds, grouped by category, as readable cards.
- **Import.** Bring FHIR records into Health Connect from FHIR files and from the clinical records inside an Apple Health export. FHIR only. No parsing of other formats.
- **Export.** Save or share the records as a FHIR file, and later as a section of the PDF health report.
- **Enter.** Type in a vaccine, an allergy, a medication, or a condition by hand.
- **Keep and carry.** Optionally keep the imported file on the phone. Carry the records to another phone with phone-to-phone sync.

Phase 2 turns documents that are not FHIR into records: health-card QR codes, CDA XML, the Estonian portal export, PDFs with text, scans. Each parser is a feature on its own, with its own proposal page when it is picked up. Phase 1 leaves the seams for them.

## Decisions Taken

Recorded 2026-09-25 and 2026-09-26:

| Question | Decision |
|---|---|
| Sensitive categories (pregnancy, social history, personal details, practitioner details) | All twelve categories ship in phase 1. One Play declaration covers them all. |
| FHIR vital signs on the existing vitals screens | No. Medical records stay self-contained. OpenVitals never writes Health Connect vitals as FHIR or the reverse. |
| Entry point | A dashboard tile called Medical Records, like Steps or Distance. Imports stay under Settings, Data Importers, and are also reachable from the records home. |
| Document parsers | Phase 2. Phase 1 imports FHIR directly and nothing else. Each parser (health cards, CDA, Estonian export, PDF text, OCR) is a feature of its own. |
| Estonian export | Added to phase 2 as its own feature. The portal hands out XML plus PDF. Needs a sample export to pin the XML schema. |
| Manual entry codes | Free text only. An optional code field, nothing bundled, no licence work. |
| Tile content | Static: glyph, title, and "Tap to browse". No reads, no summary, no staleness. |
| SMART Health Card signatures (phase 2a) | Not verified. Every imported card is labelled "signature not verified". No issuer-key snapshot. |
| Keeping the source document | The user chooses per import. The switch starts off every time and nothing is remembered. FHIR files and parsed documents can be kept, files with no records cannot. A saved copy lives in the app's private storage with a Room index, and the wizard says it takes space on the phone. Ships in phase 1 as step 1.4. |
| Apple clinical records | One data source per provider, from the `ClinicalRecord` entries in export.xml. DSTU2 records are skipped before any write. |
| Export and re-import | The export keeps each record's source in `fullUrl`. The importer splits a bundle by source and matches existing sources by base URI. |
| Files without ids | The importer assigns ids, lifts contained resources, and rewrites references. |
| Patient model | One person. One Patient per OpenVitals data source. A file for someone else stops at a patient check. |
| Permissions | All thirteen medical permissions are asked once, on the first tap of the tile. |
| Play declaration | Filed when the nightly upload to Play blocks on the new permissions. |
| Release | View, import, and export ship together as step 1.1. |
| Lab and vital values | Value, unit, and range as text. High or Low only when the lab set it. OpenVitals computes nothing. |
| Repo rules | Reads in a `MedicalRecordsHealthReader` like the other readers. Writes in a `MedicalRecordsWriter`, with the AGENTS.md write rule amended in the same change. |
| Declined categories | Still list OpenVitals' own records, with a note about other apps' records and one screen-level re-ask. |
| Apple `export_cda.xml` | Open. A real export is checked before feature 2b touches it. |
| PDF text library | Provisional. Settled in the 2d proposal. |
| Category list rows | Events are rows. Places, organisations, and drug definitions show inside the records that point at them. |
| Record status | Every record shows with its status label, entered-in-error included. |
| FHIR version | Detected from R4B hints, else R4. A matched source's version wins. |
| Import report | Full detail, like the Apple importer's, with a review warning before sharing. |
| Play refusal | Decided if it happens. |
| Phone-to-phone sync | Medical records sync in full, as step 1.5. |

## Documents Versus Records

Vocabulary first, because it changes the feature.

Health Connect does not store documents. It stores structured records in FHIR format, as JSON. The platform rejects binary content of any kind:

- No `DocumentReference`, no `Binary`, no `DiagnosticReport`.
- No attachments. PDFs, images, and scans are not accepted.
- No contained (nested) resources.

So "upload a document" cannot mean "keep this PDF in Health Connect". It can only mean "extract the structured records from this file and store those". That extraction is phase 2. Phase 1 only moves records that are already FHIR.

This draft uses **record** for the FHIR unit Health Connect stores, and **file** or **document** for what the user picks or saves.

Health Connect itself has a browser and a separate permission screen for medical records. Users can inspect and revoke without OpenVitals. What OpenVitals adds is import, export, and a friendlier reading view, and later the parsers.

## What The Platform Provides

### Data model

| Term | Meaning |
|------|---------|
| `MedicalDataSource` | Where records come from: a hospital, a portal, or an app. Created by the writing app with `fhirBaseUri`, `displayName`, and `fhirVersion`. Cannot be edited after creation. The display name must be unique per app. |
| `MedicalResource` | One FHIR resource. Fields: `type` (the category, assigned by Health Connect), `id` (a `MedicalResourceId` made of data source id, FHIR resource type, and FHIR resource id), `dataSourceId`, `fhirVersion`, `fhirResource` (the JSON). |
| Category (`MEDICAL_RESOURCE_TYPE_*`) | Assigned by Health Connect from the FHIR resource type and, for `Observation`, from its category or LOINC codes. |

Supported FHIR versions: R4 (4.0.1) and R4B (4.3.0).

Supported resource types: `AllergyIntolerance`, `Condition`, `Encounter`, `Immunization`, `Location`, `Medication`, `MedicationRequest`, `MedicationStatement`, `Observation`, `Organization`, `Patient`, `Practitioner`, `PractitionerRole`, `Procedure`. Any other type is rejected.

### Categories and permissions

| Category | FHIR resources | Read permission (`android.permission.health.`) |
|---|---|---|
| Vaccines | Immunization | `READ_MEDICAL_DATA_VACCINES` |
| Allergies | AllergyIntolerance | `READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES` |
| Conditions | Condition | `READ_MEDICAL_DATA_CONDITIONS` |
| Medications | Medication, MedicationRequest, MedicationStatement | `READ_MEDICAL_DATA_MEDICATIONS` |
| Laboratory results | Observation with the `laboratory` category | `READ_MEDICAL_DATA_LABORATORY_RESULTS` |
| Procedures | Procedure | `READ_MEDICAL_DATA_PROCEDURES` |
| Visits | Encounter, Location, Organization | `READ_MEDICAL_DATA_VISITS` |
| Vital signs | Observation with the `vital-signs` category or LOINC codes | `READ_MEDICAL_DATA_VITAL_SIGNS` |
| Personal details | Patient | `READ_MEDICAL_DATA_PERSONAL_DETAILS` |
| Practitioner details | Practitioner, PractitionerRole | `READ_MEDICAL_DATA_PRACTITIONER_DETAILS` |
| Pregnancy | Observation with pregnancy LOINC codes | `READ_MEDICAL_DATA_PREGNANCY` |
| Social history | Observation with the `social-history` category or LOINC codes | `READ_MEDICAL_DATA_SOCIAL_HISTORY` |

Writing needs one permission for everything: `android.permission.health.WRITE_MEDICAL_DATA`.

Ownership rules:

- An app that holds the write permission reads its own records without a read permission. It reads other apps' records only with the category permission, and only in the foreground unless it holds `READ_HEALTH_DATA_IN_BACKGROUND`.
- An app can delete only records and data sources it wrote.
- Health Connect stores records for one person. The platform recommends a single Patient resource but does not enforce it. See [Patient model](#patient-model).

### API surface

All calls live on `HealthConnectClient`, are `suspend`, carry `@ExperimentalPersonalHealthRecordApi`, and throw `UnsupportedOperationException` when the feature is unavailable.

| Call | Purpose |
|---|---|
| `features.getFeatureStatus(FEATURE_PERSONAL_HEALTH_RECORD)` | Availability gate. |
| `createMedicalDataSource(CreateMedicalDataSourceRequest)` | One per origin. |
| `getMedicalDataSources(GetMedicalDataSourcesRequest(packageNames))`, `getMedicalDataSources(ids)` | List sources, ours or others'. |
| `upsertMedicalResources(List<UpsertMedicalResourceRequest>)` | Insert or update by (source, type, id). Transactional: all or nothing. |
| `readMedicalResources(ReadMedicalResourcesInitialRequest)`, then `ReadMedicalResourcesPageRequest` | Paged read by category, optionally filtered by sources. |
| `readMedicalResources(List<MedicalResourceId>)` | Read by id. |
| `deleteMedicalResources(ids)`, `deleteMedicalResources(DeleteMedicalResourcesRequest)` | Delete own records. |
| `deleteMedicalDataSourceWithData(id)` | Delete a source and everything in it. |

Validation on write, enforced today: valid JSON, a supported version and type, an `id` that matches the FHIR id rule, uniqueness per (source, type), no `contained`, top-level fields that exist in the spec with the right JSON types, and at most one choice field (`effectiveDateTime` or `effectivePeriod`, never both). Deeper checks on primitives and complex types are announced as coming.

Missing from the platform today:

- No changelog API for medical records. Incremental sync is not possible. Every view is a fresh paged read.
- No count call and no documented sort order for paged reads. Counting and sorting are the client's job.
- No published numbers for rate limits. The general Health Connect quotas apply, stricter in the background.
- The API is experimental and may change.

### Availability

The feature needs Android 14 or newer with Health Connect module SDK extension 16 or newer. Android 13 and older never get it. The check is `getFeatureStatus`, which the app already uses for mindfulness, history, background reads, skin temperature, and planned exercise in `healthconnect/HealthConnectPermissionService.kt`.

## Where OpenVitals Stands

| Fact | Consequence |
|---|---|
| `connect-client` 1.2.0-alpha06 in `gradle/libs.versions.toml`, compileSdk 37, targetSdk 36, minSdk 26 | The medical classes are already in the AAR. No dependency change for phase 1. |
| No medical permission in `AndroidManifest.xml` | 13 new `<uses-permission>` lines and a Play Console declaration. |
| `connect-testing` 1.0.0-alpha04: `FakeHealthConnectClient` has no medical methods | The app needs its own fake behind a small interface. |
| kotlinx-serialization-json is used only through `JsonElement`, no plugin, no `@Serializable` | FHIR handling can stay on the tree API. No FHIR SDK. |
| The Apple Health importer already opens the export zip in `features/imports/applehealth/AppleHealthImportParser.kt` and skips unknown entries. Apple's export.xml lists each clinical record as a `ClinicalRecord` element with `type`, `identifier`, `sourceName`, `sourceURL`, `fhirVersion`, `receivedDate`, and `resourceFilePath` | The provider and FHIR version of every record are known before its JSON is read. What `export_cda.xml` holds is unverified. It likely holds Apple's own measurements, and importing those as FHIR would break the self-contained vitals decision, so feature 2b checks a real export first. |
| The PDF report is built on the platform `PdfDocument` in `features/reports/pdf/`, with per-section "missing" notes | A medical records section fits the existing model. The app writes PDFs but has no PDF reader, which matters in phase 2. |
| File flows use `OpenDocument`, `CreateDocument`, and the app `FileProvider` through `core/export/ExportStaging.kt` | Import and export reuse the same plumbing. |
| Dashboard tiles are `DashboardWidgetId` entries. Non-metric tiles such as the watch and cycle tiles render through `DashboardPillWidget`, which wraps `MetricStatCard`. New ids are appended to saved layouts by `dashboardWidgetIdsWithNewOnesAppended` | A Medical Records tile renders as a pill with no value and appears for existing users without a reset. |
| Shared shell: `WithHealthConnectFeatureScreen`, `HealthConnectFeature`, `rememberHealthConnectPermissionLauncher`. Policy: asking happens at point of use, never on the dashboard | The tile never prompts by itself. Its first tap opens the records home, which asks once for every medical permission. |
| No `CAMERA` permission and no Google dependency for functionality. Distributed on Google Play and on F-Droid and Codeberg | Phase 2 parsers that need a camera or a proprietary library are separate decisions. Play's health records policy applies to the Play build. |
| No `INTERNET` permission. Health Connect is the source of truth. No local mirror of records | Import and export are file based. No FHIR server client. No Room copy of records. |

## Phase 1: Proposed User Experience

### Entry point: a dashboard tile

A **Medical Records** tile, `DashboardWidgetId.MEDICAL_RECORDS`, placed after the Cycle tile in `DefaultDashboardWidgetIds`. Existing users get it appended to their layout, as the watch tile was.

The tile is static. It is a `DashboardPillWidget` with a medical records glyph, the title, and the message "Tap to browse", the way the cycle tile says browse when it has nothing to show. No value, no subtitle, no reads. The platform has no count call, and a summary would need daily reads and a staleness story, so the tile carries none. It also stays out of the metric-group coalescer, which is where a wedged group has stuck tiles on Loading before.

The tile still needs an entry in `display.widgets`, because `dashboardWidgetSpecs` skips any id without one. The entry carries no value.

The tile never asks for permissions by itself. Its first tap is the point of use: it opens the records home, which asks for every medical permission at once. See [Permissions](#permissions).

Gating. `toDashboardMetricOrNull()` returns null, so provider support never hides the tile. Feature status does. When `FEATURE_PERSONAL_HEALTH_RECORD` is unavailable, the presentation mapper leaves the tile out of `display.widgets`, so `dashboardWidgetSpecs` skips it. Edit mode materialises every id for the add tray through `includeUnsupported`, so the mapper needs its own availability check there as well. Users on Android 13, and on Android 14 without the module update, never see the tile.

### Permissions

The first time the medical area opens, OpenVitals makes one Health Connect request with all thirteen medical permissions: the twelve read permissions and write. The first tap of the tile is the usual trigger. Opening medical import from Settings, choosing clinical records in the Apple Health importer, or choosing medical records in phone-to-phone sync triggers the same request if it has not happened yet.

- The request holds only medical permissions. It never mixes them with fitness permissions, so Health Connect shows its own medical permission screen. The user can turn off any permission there.
- It goes through the shared shell. A `HealthConnectFeature.MEDICAL_RECORDS` entry requests all thirteen but requires none, so a screen with partial access still renders. No row or card asks on its own.
- After the first request, OpenVitals does not ask again by itself. A category the user turned off still lists the records OpenVitals itself imported, which write access lets it read, with a line saying records from other apps need access. One screen-level action re-asks for everything missing, through the shell.
- Write is granted up front, so the import wizard does not stop mid-way. If the user declined write, the wizard asks for it before the analyze step, because matching existing data sources needs it.

### Records home

One row per category, in two blocks: **Care** (vaccines, allergies, conditions, medications, lab results, procedures, visits, vital signs) and **Sensitive** (pregnancy, social history, personal details, practitioner details). All twelve ship in phase 1. The split is presentation only. A category with permission reads one page of size 1 to show "has records" or "empty". A declined category follows the rule in [Permissions](#permissions).

Two actions at the top: Import and Export all.

### Category list

One `SettingsListItem`-style row per record: leading category glyph, title (vaccine name, allergen, test name), supporting text (date, source display name, status). The screen pages through the category at 100 records a page, sorts locally, and shows progress while paging. Personal record volumes are tens to low hundreds, so loading a category fully is acceptable. The spike should confirm this.

- **Rows are events.** Immunizations, allergies, conditions, medication requests and statements, observations, procedures, and encounters are rows. Locations, organisations, and bare drug definitions show inside the records that point at them, and become rows only when nothing points at them. Personal details and Practitioner details list their Patient and practitioner records, since those categories hold nothing else.
- **References.** A reference shows the referenced record's name when it is readable, else the reference's own `display` text, else "needs access to <category>".
- **Sorting.** Newest first, by each type's own date: `occurrence[x]` for immunizations, onset or `recordedDate` for allergies and conditions, `authoredOn` or `effective[x]` for medications, `effective[x]` for observations, `performed[x]` for procedures, `period.start` for encounters. A partial date such as "2020" sorts as its first day and shows as written. Records without a date sort last, by name.
- **Status.** Every record shows, each with its status label, including entered-in-error, refuted, inactive, and resolved. The label is a word, not only a colour.

### Record detail

`DetailRow`s in a `Card`: the curated fields for the resource type (see FHIR handling), a **Source** row with the data source display name and FHIR base URI, and an expandable **Raw FHIR** section with the pretty-printed JSON as selectable text. Actions: share this record as a FHIR JSON file; delete when OpenVitals wrote it, otherwise a note that another app owns it with a link to Health Connect settings.

Lab results and vital signs show the value, the unit, and the reference range as text. A High or Low flag appears only when the lab set it in `interpretation`. OpenVitals computes nothing, which keeps the promise that it does not interpret records. No trend charts. Vital signs stay in this area and never join the vitals detail screens.

### Import (the "upload")

Lives under Settings, Data Importers, next to CSV and Apple Health, and is also reachable from the records home. It is a stepped screen built on `StepBar`, like CSV import. Phase 1 accepts FHIR only.

1. **Choose a file.** `OpenDocument` for `application/json`, `application/fhir+json`, `application/x-ndjson`, `text/plain`, and `application/octet-stream`. Accepted shapes: a single FHIR resource, a FHIR `Bundle` of any type (entries are unwrapped), or NDJSON with one resource per line. Resources without ids, bundles that link entries by `urn:uuid` URLs, and resources with contained resources are all accepted. See [Ids and references](#ids-and-references). Anything else is refused with one line that names the phase 2 parsers as not available yet. If write access is missing, the wizard asks for it here.
2. **Analyze.** Parse, assign ids, rewrite references, and group the records by source. Show counts per category and the FHIR version, detected from R4B hints such as `meta.profile` URLs and otherwise R4. When a group matches an existing source, that source's version wins. Resources of types Health Connect does not support, such as Composition, DiagnosticReport, and DocumentReference, show as skipped with a count per type. Records that will be rejected are listed with the reason, such as an `Observation` without a category the platform can classify. The step names the data source each group goes to, runs the [patient check](#patient-model), and marks groups already in Health Connect from another app. Nothing is written yet.
3. **Confirm.** Confirm or edit the display name of each new data source.
4. **Import.** Upsert in batches with progress. The spike sets the batch size. A batch is transactional, so one bad record fails its batch. The importer then retries that batch one record at a time to isolate the bad one, and keeps going.
5. **Result.** Written, updated, skipped, and rejected counts, grouped by reason, and a copy or save report, like CSV import. "Updated" comes from reading each batch's ids before the write, because upsert does not say. The report includes record content, like the Apple importer's, and the result step says to review it before sharing.

Phase 2 parsers plug in behind step 1 and add a review table to step 2. Steps 3 to 5 do not change. The `MedicalImportScreen` is built with that seam from the start: a `MedicalImportSource` that yields proposed resources, with the FHIR file source as its only phase 1 implementation.

**Apple Health export.** The Apple Health importer gains an optional "Clinical records" category in its analyze step. It runs the same pipeline as a FHIR file, with the grouping taken from Apple's own index:

- While it parses export.xml, the importer collects every `ClinicalRecord` element: `sourceName`, `sourceURL`, `fhirVersion`, and `resourceFilePath`.
- A second pass over the zip reads the `clinical-records/` files those elements point to. The zip may list the files before export.xml, and the picked URI can be reopened, so two passes are simpler than buffering.
- One data source per provider. `fhirBaseUri` is the provider's `sourceURL` and the display name is its `sourceName`. Two hospitals never share a source, so their ids cannot collide.
- `fhirVersion` decides per record. 4.0.1 and 4.3.0 continue. 1.0.2, which is DSTU2, and any other version are skipped before any write, with the reason "FHIR DSTU2, not supported by Health Connect". Mapping DSTU2 to R4 is phase 2 feature 2f.
- A provider with records in both R4 and R4B gets one source per version, because a data source has one version. The second one gets the version as a name suffix.
- A file with no `ClinicalRecord` element, or an element with no file, is skipped and reported.

### Export (the "download")

- **FHIR file.** From the records home, "Export all" builds a FHIR `Bundle` of type `collection` with every record the app can read and saves it through `CreateDocument` as `openvitals-medical-records-<date>.json`, or shares it through the FileProvider. From a category or a record, the same for that subset. The bundle keeps where each record came from, so a re-import restores the same sources and ids:
  - Each entry's `fullUrl` is the data source's `fhirBaseUri` followed by `<type>/<id>`, which is what FHIR defines `fullUrl` to be. Records from different sources can share an id without clashing.
  - Each entry carries an OpenVitals extension with the data source's display name and the package that wrote it.
  - The bundle's `meta.tag` marks it as an OpenVitals export, with the app version.
  - Resources are written exactly as Health Connect returns them. Ids and references inside them are not changed.
  - A shared export is staged in a new `medical_exports` cache path in `file_paths.xml`. It is pruned at the next export or app start, not after the usual day.
- **PDF report section**, later in phase 1. The health report builder gets a "Medical records" section: vaccines, allergies, medications with their recorded status, conditions, and lab results in the date range, with the existing honesty rule when a category has no read permission.

### Delete

Only records OpenVitals wrote. Per record from the detail screen. Per data source from a "Written by OpenVitals" list, with a confirmation that names the record count. Re-import never deletes. It upserts.

### Manual entry

Its own feature, after the import work. Simple forms for the four categories a person can reasonably type: a vaccine, an allergy, a medication, a condition. OpenVitals authors the FHIR resource with a UUID id, `meta.source` set to the app, the name as free text in `code.text`, and a reference to the owner's Patient record. An optional code field exists for users who have one, but nothing is bundled and no vocabulary ships. [Patient model](#patient-model) covers the patient and the other required fields. This is direct FHIR, not parsing, so it stays in phase 1, but it can move if phase 1 needs to be smaller.

### Saved documents

Step 1.4. The user decides, per import, whether OpenVitals keeps the file the records came from. The switch starts off on every import, and nothing is remembered.

**Which files.** FHIR files from the import wizard, and every phase 2 document that produces records. A kept FHIR file holds what Health Connect drops on import, such as DiagnosticReport, DocumentReference, and Composition resources. For an Apple Health export, the switch keeps only the clinical records files, packed as one zip, not the whole export. A file that yields no records cannot be kept. OpenVitals is not a general document store.

**Where the choice is made.** The Confirm step gets a switch, "Keep a copy of this document on the phone". Under it: the file's size, the total space saved documents already use, and one line: "Saved documents take space on this phone. They stay in the app's private storage, are not backed up, and are deleted when the app is uninstalled." The install size of the app does not change, only its data does.

**Storage.** Files go under the app's private files directory, `files/medical_documents/<uuid>.<ext>`, never the cache, which `ExportStaging` prunes. A Room table `medical_documents` indexes them: id, original file name, MIME type, size, SHA-256, import time, data source id, and source display name. A second table, `medical_document_records`, links a document to the `MedicalResourceId`s imported from it. The same file twice, by hash, keeps one copy. This is a bump to the next Room version, with its migration, the committed schema file, and the architecture doc row, per the playbook. It qualifies under the playbook rule because a document file is data Health Connect cannot hold. `file_paths.xml` gains a `files-path` for the folder.

**Where documents are seen.** The records home gets a "Saved documents" row: a list with name, date, size, and the number of records that came from each file, plus the total space used and "Delete all". A document opens in whatever viewer the phone has for its type through the `FileProvider` (`ACTION_VIEW`), can be saved elsewhere through `CreateDocument`, or shared. A record whose document was kept gets an "Open original document" action on its detail screen. Deleting a document does not delete its records, and the confirmation says so. Deleting records does not delete the document. A link to a record that no longer exists in Health Connect is dropped when the list loads.

**Privacy.** The file can hold more than the records extracted from it: names, identifiers, free text. The privacy page says so. Opening a document hands it to another app's viewer, which the privacy page also says. Device file-based encryption covers the private directory. No extra encryption layer in the first version.

Saved documents do not travel with phone-to-phone sync. Only records do.

### Phone-to-phone sync

Step 1.5. Medical records travel with [Sync with another phone](../features/device-sync.md).

- **Category.** The sync wizard's category picker gains "Medical records". It appears when this phone has the medical feature, holds write access, and can read at least one medical category.
- **Range.** Medical records ignore the "how far back" choice and always sync in full, because a vaccination history cut at one year is not useful. The picker says so under the category.
- **Payload.** The sender sends the same bundle the FHIR export writes, with each record's source kept in `fullUrl` and the OpenVitals extension. It is a new message type in the existing exchange. The category is offered only when both phones support it, so an older build does not see it.
- **Receiving.** The receiver runs the import pipeline without the wizard: source grouping, matching by base URI, the rule that skips sources another app owns, and pre-flight. Identity is the data source and the resource id, not the content fingerprint other records use, so a re-sync upserts and counts matches as already present.
- **Patient check.** The receiver compares the incoming Patient records with its own. A mismatch holds back every medical record from that session, and the report says why. The user can still move them with an export and a checked import.
- **Report.** The sync report gains a medical records line: written, already present, skipped, and rejected.

## Phase 1: Design

### Packages and classes

Health Connect access stays in `healthconnect/`, as the AGENTS.md reader rule requires. `HealthConnectLayeringTest` also stops those classes from importing repositories.

| Piece | Location | Notes |
|---|---|---|
| `MedicalRecordsHealthReader` and `MedicalRecordsWriter` | `healthconnect/` | Created inside `HealthConnectManager` on `HealthConnectReaderSupport`, like the other readers. The only classes with `@OptIn(ExperimentalPersonalHealthRecordApi::class)`. They wrap the calls listed above, with the feature check inside. The AGENTS.md write rule is amended in the same change: medical writes go through `MedicalRecordsWriter`, because FHIR records have no `clientRecordId`. |
| `MedicalFakeHealthConnectClient` | `app/src/test/.../healthconnect/` | Wraps `FakeHealthConnectClient` the way `AggregatingFakeHealthConnectClient` does, and adds the medical calls on an in-memory map keyed by `MedicalResourceId`. Fills the gap in `connect-testing`. |
| `MedicalRecordsRepository` | `data/repository/contract/` and `data/repository/MedicalRecordsRepositoryImpl.kt` | Paged reads by category, own-source listing, upsert batches, delete. Bound in `di/RepositoryModule.kt`. |
| FHIR parsing and summaries | `domain/medical/` | `FhirFileParser` (file to resources, unwraps Bundle and NDJSON), `FhirIdAssigner` (ids, contained resources, reference rewriting), `FhirSourceGrouper` (splits by `fullUrl` base, `meta.source`, or Apple provider), `PatientCheck`, `FhirPreflight` (the local checks), `FhirSummary` per resource type, `FhirBundleWriter` for export. Pure Kotlin on `JsonElement`, unit-tested on fixtures. |
| Import source seam | `domain/medical/MedicalImportSource.kt` | Interface: a file in, proposed resources with optional source snippets out. `FhirFileImportSource` is the phase 1 implementation. Phase 2 parsers implement it. |
| Category model | `domain/model/MedicalCategory.kt` | Enum with title, glyph, and block (care or sensitive), free of Health Connect types. The mapping to `MEDICAL_RESOURCE_TYPE_*` and permission strings lives in `healthconnect/`, because the playbook keeps Health Connect permissions below the repository layer. |
| Sync | `features/devicesync/` | A medical records category, a message type that carries the export bundle, and the receiving side of the import pipeline. |
| Tile | `features/dashboard/` | `DashboardWidgetId.MEDICAL_RECORDS`, a branch in `dashboardWidgetSpecs` next to the cycle branch, a title and meta entry, and a feature-available flag passed into `DashboardPresentationMapper.build` the way the watch display is, so the mapper can leave the tile out. No display model of its own. |
| Screens | `features/medical/` and `features/imports/medical/` | `MedicalRecordsScreen` (home), `MedicalCategoryScreen`, `MedicalRecordDetailScreen`, `MedicalImportScreen`. ViewModels are `@HiltViewModel`. State holds display models, never raw resources. |
| Navigation | `navigation/Screen.kt`, `AppNavigationMetricRoutes.kt`, `AppNavigationSettingsRoutes.kt` | `Screen.MedicalRecords`, `Screen.MedicalCategory`, `Screen.MedicalRecordDetail`, `Screen.SettingsMedicalImport`. The tile maps to `Screen.MedicalRecords` where the other widget ids map to their routes. Add each to `Screen.all`. |
| Permissions | `healthconnect/HealthConnectFeature.kt`, `HealthConnectPermissionService.kt`, the manifest | One `HealthConnectFeature.MEDICAL_RECORDS` entry that requests all thirteen permissions and requires none, and a `medicalRecordsAvailable()` check next to the existing feature checks. The shell may need a way to mark permissions as requested but not required. |

### Data source strategy

One `MedicalDataSource` per origin, never one per import run, and never two origins in one source:

- A bundle whose entries have absolute `fullUrl`s, which includes every OpenVitals export: one source per base URI, the part of `fullUrl` before `<type>/<id>`. The display name comes from the OpenVitals extension when present, otherwise from the URI's host.
- A file whose resources carry `meta.source`: one source per distinct value.
- Otherwise: `fhirBaseUri = openvitals://import/<slug>`, with a display name the user types and the file name pre-filled. `Bundle.identifier` is not used, because it is usually a UUID.
- Apple Health export: one source per provider, from `sourceURL` and `sourceName`.
- Manual entry: `openvitals://manual`, "Entered in OpenVitals".

**Matching.** Before creating a source, the wizard lists the app's own sources with `getMedicalDataSources(GetMedicalDataSourcesRequest(listOf(packageName)))` and matches by `fhirBaseUri`, not by display name. A match reuses the source, so re-importing the same file upserts and stays idempotent. Display names must be unique per app and cannot be changed, so a new source whose name is taken gets the import date as a suffix. No Room table is needed to remember sources.

**Other apps' records.** The wizard also lists the sources of every app it can read. A group whose base URI matches a source another app owns is skipped by default, with the reason "already in Health Connect from <app>". A re-import on the same phone then never duplicates another app's records. On a new phone nothing matches, so everything imports.

### Ids and references

Health Connect needs an id on every resource, unique per type within a data source. Real files often break that. The EU patient summary links bundle entries by `urn:uuid` URLs and leaves ids out, and many exports nest resources inside others as `contained`. The importer fixes this in the analyze step, before pre-flight, and keeps every reference pointing at the right record. The platform docs ask for the same treatment when records are merged.

- A valid id is kept as it is. Re-imports stay idempotent and references stay intact.
- A bundle entry with a `urn:uuid:` `fullUrl` and no id takes the UUID as its id. A UUID is 36 characters and passes the FHIR id rule.
- Any other resource without an id, or with an id that breaks the rule, gets `ov-` followed by the first 40 hex characters of a SHA-256 over the source's base URI, the resource type, and the resource's JSON with keys sorted and `id` and `meta` removed. The same file always yields the same ids.
- Each contained resource becomes a top-level resource with the id `<parent id>-<local id>`, or a hash as above when that is too long or breaks the rule. A contained resource of a type Health Connect does not support is dropped, and the reference to it keeps only its display text.
- Every `reference` in the group is then rewritten to `<type>/<id>`: `urn:uuid:` references, `#local` references to contained resources, and absolute references to a `fullUrl` in the same file. References to records outside the file stay as they are.
- Two resources in one group with the same type and id: the later one wins, and the report lists the duplicate.

### Patient model

Health Connect holds medical records for one person: the phone's owner. The platform recommends a single Patient resource but does not enforce it. FHIR R4 requires a patient reference on immunizations, allergies, conditions, medication requests, and medication statements. OpenVitals follows these rules:

- **One Patient per data source.** References resolve inside a data source, so each source OpenVitals writes holds at most one Patient, and its records point at it. The Personal details category shows every Patient record in Health Connect as one person, with the sources listed under it.
- **Imports keep their Patient.** A file with one Patient keeps it, with its id handled by the id rules. A file with records but no Patient keeps its references as they are. Health Connect checks that required fields are present, not that references resolve.
- **The patient check.** The analyze step compares each Patient in the file with the Patient records already in Health Connect, by name and birth date. Names compare without case, accents, or extra given names. A match passes silently, and so does a first import into an empty store. A mismatch, or a file with more than one Patient, stops the import and shows both identities side by side. The user can confirm "These records are mine" or cancel. Without access to personal details, the check cannot compare, so it shows the file's patient and asks for the same confirmation. OpenVitals does not hold records for other people, so a child's vaccination card is cancelled, not filed under the owner.
- **Manual entry.** The manual source gets one Patient with the id `self`, created with the first manual entry. Its name and birth date come from an existing Patient record when one is readable. Otherwise the first manual entry asks for them once. Every manual record references `Patient/self` and fills the other fields FHIR requires or expects: `status` and the occurrence date on an immunization, `clinicalStatus` on an allergy and a condition, `status` on a medication statement.
- **No local copy.** The owner's identity is read from Health Connect when needed. OpenVitals keeps none of it.

### FHIR handling

No FHIR SDK. HAPI FHIR and the Android FHIR SDK are large, and both assume a server workflow. The app needs to read a JSON tree, check a few top-level fields, and pick display fields per type. `kotlinx.serialization.json.JsonElement` does that and is already in the app without the compiler plugin.

Curated display fields, first version:

| Resource | Title | Supporting | Detail rows |
|---|---|---|---|
| Immunization | `vaccineCode` | `occurrenceDateTime`, `status` | lot number, site, route, performer, dose number |
| AllergyIntolerance | `code` | `clinicalStatus`, `criticality` | category, reactions from `reaction[].manifestation`, `recordedDate`, onset |
| Condition | `code` | `clinicalStatus`, onset | verification status, severity, `recordedDate`, body site |
| MedicationRequest, MedicationStatement, Medication | `medicationCodeableConcept`, or the referenced Medication | `status`, `authoredOn` or effective date | dosage text, requester, reason |
| Observation | `code` | value with unit, `effectiveDateTime` | `interpretation`, `referenceRange`, `component[]` (blood pressure), performer, note |
| Procedure | `code` | `performedDateTime` or period, `status` | body site, performer, reason, outcome |
| Encounter | `type` or `class` | `period`, service provider | participants, reason, location |
| Patient | name | birth date | identifiers hidden by default |
| Practitioner, PractitionerRole, Organization, Location | name | specialty or type | contact |

Every field is optional. A resource with none of them still renders as "Untitled" plus its type and the raw JSON. A coded value shows `text` first, then `coding[0].display`, then `coding[0].code` with the system as a caption.

Pre-flight runs after id assignment and mirrors the platform's enforced rules, so rejections are explained before the write, not after: the JSON parses, `resourceType` is supported, `id` matches `[A-Za-z0-9\-\.]{1,64}`, there is no `contained`, and an `Observation` carries a category or a code the platform can classify. After id assignment, the id and contained checks are a safety net. Pre-flight knows the three category codes the platform names: `laboratory`, `vital-signs`, and `social-history`. The platform's LOINC lists for pregnancy, social history, and vital signs are not all published, so an Observation that passes pre-flight can still be rejected on write. The report then gives the platform's reason.

### No local copy of records

Health Connect stays the source of truth, as everywhere else in the app. No Room table holds records, and no record data goes in preferences. The only stored value is the shell's note that the first permission request has happened. The tile is static, so there is no summary to keep. Saved documents, step 1.4, are files with an index, not records.

### Errors, limits, threading

- Throwables go through `toScreenError()`. `SecurityException` already maps to `PermissionDenied`, and the shell renders the grant affordance. The `UnsupportedOperationException` from the feature gate needs a case that renders as "not available on this device", new if none fits.
- Reads happen in the foreground only, paged at 100, through `HealthConnectReaderSupport`, so the existing rate-limit guard and logging apply.
- Import is user-started work. A file import runs in the ViewModel scope. The Apple Health path already runs as a foreground service and inherits that.
- NDJSON is read line by line. A single JSON file is parsed as one tree, so the spike sets a size limit, and the wizard refuses larger files with a message that says so.
- Nothing on the main thread. The ratchet tests still apply.

### Tests

- Unit: parser and pre-flight on fixture files (the HL7 example resources are CC0 and can be vendored under `app/src/test/resources/fhir/`), summaries per type, repository against the fake client, ViewModel tests for paging and permission-denied states.
- Id assignment: a `urn:uuid` document bundle, contained resources, invalid ids, and duplicate ids, with every reference checked after rewriting.
- Grouping: a synthetic Apple export with two providers that reuse the same ids, and one DSTU2 record.
- Round trip: export from the fake client, re-import into an empty one, and get the same sources, ids, and references back. Re-import into the same client and get no duplicates.
- Patient check: a match, an empty store, a mismatch, a file with two patients, and no access to personal details.
- Sync: two fake clients exchange medical records, a re-sync writes no duplicates, and a patient mismatch holds the records back.
- `ArchitectureDocTest`: add the new packages to `docs/engineering/architecture.md` in the same change.
- Instrumented: one golden for the records home, one for a detail screen, one for the tile.
- On the Pixel: confirm Health Connect shows the medical records feature, grant permissions, import an HL7 sample bundle, and check the Health Connect browser shows the same records.

## Phase 2: Document Parsers

Each parser below is a feature of its own: its own proposal page under `docs/proposals/`, its own feature page when shipped, its own fixture corpus, and its own release. They share three things that phase 1 leaves in place: the `MedicalImportSource` seam, the import wizard, and a review table in the analyze step that shows every proposed record next to its source snippet with a keep or skip switch.

The rule that holds for every parser: a record extracted from a document is a **proposal** until the user has seen it beside its source and kept it. Extracted records carry `meta.source` pointing at the import, and a `note` that says OpenVitals extracted them from a named file and the user reviewed them. Extracted records go through the same [id and reference rules](#ids-and-references) and the same [patient check](#patient-model) as FHIR files, so re-importing the same document updates rather than duplicates. For parsed records the id hash covers the source file and the snippet's position, not the edited values, so a correction in the review table updates the same record instead of adding one. Types without a `note` field, such as Patient, Encounter, and Organization, carry the provenance in `meta.tag` instead.

The candidates, ranked by how deterministic they are. Deterministic first, because every step down the list adds a way to store a wrong number in someone's medical history.

### 2a. SMART Health Cards

The format pharmacies and many US and Canadian health systems use for vaccination and lab result cards. The QR text starts with `shc:/` followed by digit pairs. Decoding is small: digits to a JWS, the JWS payload is raw DEFLATE (no zlib header), and the JSON inside holds `vc.credentialSubject.fhirBundle`, a FHIR R4 Bundle. `java.util.zip.Inflater` and Base64 cover it, about a hundred lines. Long cards are split across several QR codes with an ordinal prefix, so the importer accepts several images. Card resources carry no ids and link by `resource:0` style references, which the id rules handle like `urn:uuid` references.

Signature verification needs the issuer's keys from the network. With no `INTERNET` permission, the import decodes the card, marks it "signature not verified", and says so on every record from it. No issuer-key snapshot ships. Decided.

Reading the QR from a file: ZXing core (Apache 2.0, pure Java) decodes a bitmap. Health-card PDFs embed the QR as an image, so a PDF page rendered with the platform `PdfRenderer` and passed to ZXing covers that path too. A live camera scanner needs the `CAMERA` permission and is a separate decision.

The EU Digital COVID Certificate (HCERT: base45, zlib, COSE, CBOR) maps to `Immunization`, needs a CBOR library, and is mostly historical. Not planned unless asked.

### 2b. CDA documents

HL7 CDA R2 is XML. It is what US patient portals hand out as "download my record" (C-CDA), and the format behind the EU patient summary exchanged through MyHealth@EU. Apple's `export_cda.xml` is also CDA, but it likely holds Apple's own measurements, and vital-sign sections from it would break the self-contained vitals decision. This feature checks a real export before reading it. Sections are identified by LOINC codes, and entries carry real codes (RxNorm, SNOMED, LOINC, CVX), so the FHIR that comes out is clean.

| CDA section | LOINC | FHIR target |
|---|---|---|
| Immunizations | 11369-6 | Immunization |
| Allergies | 48765-2 | AllergyIntolerance |
| Problems | 11450-4 | Condition |
| Medications | 10160-0 | MedicationStatement |
| Results | 30954-2 | Observation (laboratory) |
| Procedures | 47519-4 | Procedure |
| Encounters | 46240-8 | Encounter |
| Vital signs | 8716-3 | Observation (vital-signs) |
| Social history | 29762-2 | Observation (social-history) |

The open-source converters (Amida cda2r4, SRDC cda2fhir, Microsoft FHIR Converter) sit on MDHT, HAPI, or .NET and are not usable on Android. An in-house subset mapper on the SDK's `XmlPullParser`, one section at a time, is realistic. The HL7 "C-CDA on FHIR" mapping tables are the reference. Estimate: one to two weeks for the sections above.

### 2c. Estonian health portal export

The Estonian portal (terviseportaal.ee, formerly digilugu.ee) lets a person download their documents as XML plus a PDF rendering. The national health information system publishes its document standards as HL7 CDA R2 profiles, so the XML is expected to be CDA under Estonian profiles: epicrises, referrals, immunisation notices, and lab reports with their own section codes and code systems. That needs confirming against a real export before scoping. If it is CDA, this feature is the 2b mapper plus an Estonian profile table and Estonian code systems. If it is a portal-specific schema, it is a parser of its own. The PDF is the rendered document and is only a fallback through 2d.

First step when picked up: collect one sample of each document type from a real export, anonymise it, and add it to the fixture corpus. The analyte dictionary has no Estonian names yet. Estonian documents carry the national identity code, which stays hidden like other identifiers.

### 2d. PDF with a text layer

The common case in Spain and most of Europe: lab reports, vaccination histories, and discharge summaries from regional portals come as PDFs with selectable text.

Text extraction options:

| Option | Covers | Notes |
|---|---|---|
| Platform `PdfRenderer.Page.getTextContents()` | Android 15 and newer | No dependency. Nothing below API 35. |
| Jetpack `androidx.pdf` 1.0.0-beta01 (2026-08-26) | minSdk 28, SDK extension 13 and newer | A viewer, text search, and page text content, to verify. Its OCR artifact is ML Kit through Play services. |
| PdfBox-Android 2.0.27.0 (Apache 2.0) | minSdk 26, every device | `PDFTextStripper` gives text with positions. Pure Java. Last release January 2023, so it is maintained by nobody. |

Provisional, settled in the 2d proposal. The whole medical feature needs Android 14, so PdfBox would only ever serve Android 14, and the Jetpack PDF library appears to expose page text already. The 2d proposal verifies that and picks. Every option sits behind one `PdfTextExtractor` interface.

Text is only half of it. The second half is **structure recovery**, and it is the real work:

- A lab report is a table: analyte, value, unit, reference range, flag. A line parser with locale-aware numbers (Spanish decimal commas) recovers rows. A bundled dictionary of the hundred or so common analytes in Spanish and English maps names to LOINC and UCUM: glucose 2345-7, HbA1c 4548-4, total cholesterol 2093-3, HDL 2085-9, LDL 13457-7, triglycerides 2571-8, creatinine 2160-0, TSH 3016-3, haemoglobin 718-7, and so on. Rows that match no entry still import, with `code.text` only. Health Connect classifies an `Observation` by its `laboratory` category, so a LOINC code is not required. The dictionary ships LOINC and UCUM codes, which need their licence notices in the app's licences screen.
- A vaccination history is rows of date, vaccine name, lot, and centre. The same approach yields `Immunization` records, with an optional CVX or ATC lookup. ATC needs a check of the WHO terms before it ships.
- The report date and the issuing laboratory come from the header, by pattern, and the user confirms them once in the review step.

Lab reports and vaccination lists are two parsers, and two features.

### 2e. Scanned PDF or photo

No text layer, so OCR first, then the same structure recovery as 2d, with lower confidence and a mandatory review.

| Engine | Licence and distribution | Notes |
|---|---|---|
| Tesseract4Android 4.9.0 | Apache 2.0, F-Droid compatible | Trained data is 5 to 15 MB per language. Import it as a file, the way map packs are imported, or bundle Spanish and English. Good on printed reports, poor on handwriting. |
| ML Kit Text Recognition v2, bundled model | Proprietary Google library, on device, no Play services for inference | Better accuracy on Latin scripts. Excluded from the F-Droid build, so it needs a Play-only flavor. The app has no Google dependency today, so this is a policy decision, not a technical one. |

Recommendation: Tesseract in every build. Decide on an ML Kit flavor only if Tesseract proves too weak on real reports. OCR of a multi-page scan can take tens of seconds and runs as user-started foreground work with progress, like the Apple Health path.

### 2f. Apple DSTU2 to R4

Some providers still hand Apple Health FHIR DSTU2. Those records fail platform validation in phase 1. A field-level mapping for the handful of resource types Apple exports (`Immunization.date` to `occurrenceDateTime`, `AllergyIntolerance.substance` to `code`, `MedicationOrder` to `MedicationRequest`, and so on) is a small, testable feature.

### 2g. On-device language models

Turning free text into FHIR with a model is possible on device in 2026, and it is the least trustworthy path:

- **ML Kit GenAI Prompt API** with Gemini Nano supports structured JSON output against a schema. It runs through AICore on Pixel 9 and later, Galaxy S25, and a short list of other phones. Proprietary, Play-only.
- **LiteRT-LM** with Gemma weights runs on more devices. Weights are 0.8 to 1.5 GB and would have to be imported from a file, since the app cannot download. Slow on mid-range phones. The runtime is Apache 2.0, the weights are under the Gemma licence and never bundled.

Both hallucinate numbers and units. If this is ever tried, the model output is a suggestion shown beside the source text and never written without a keep. An experiment after 2a to 2e, and probably never in the F-Droid build.

### Not convertible

Images and PDFs as such, DICOM, and clinical notes. The platform has no place for them. OpenVitals does not keep files that yield no records. [Saved documents](#saved-documents) only covers files that went through an import.

### Shared phase 2 pieces

| Piece | Location | Notes |
|---|---|---|
| Review table | `features/imports/medical/` | One row per proposed record: snippet, editable fields, keep switch. CSV import's column mapping is the nearest relative. Registered in `components-map.md` when created. |
| `PdfTextExtractor`, `OcrEngine` | `core/` | Interfaces with platform, Jetpack PDF or PdfBox, and Tesseract implementations. |
| Analyte dictionary | `app/src/main/assets/medical/analytes.json` | Name variants, LOINC, UCUM unit, typical range hints. Loaded once. |
| Fixture corpus | `app/src/test/resources/medical/` | Anonymised or synthetic documents per parser with expected records. Parser accuracy is measured against it, and a regression fails the build. |

Phase 2 also adds to the privacy page: files are read on device, nothing is uploaded, and extracted records are reviewed before they are written. And to the disclaimer: extracted records can contain recognition errors.

## Policy, Privacy, And Docs

### Google Play

Since 5 March 2025, health records access is a separate justification inside the Play Console Health Connect declaration. Only apps with a justified use case are accepted. The justification must name each data type and its user-facing benefit. Reproductive health data is under heightened scrutiny, so the pregnancy category needs its own sentence. The app already ships Health Connect fitness permissions on Play, so a declaration exists to extend. Its current state should be checked before filing. The approved-use-case list frames medical records under clinical care apps, so the wording matters. The Data safety form needs the same review for the new data types.

Draft justification: OpenVitals lets the user view, import, and export their own medical records held in Health Connect, on device, with no server and no internet permission. Each read permission unlocks one visible category in the records browser and in the FHIR export. Write is used only for user-started import, manual entry, and phone-to-phone sync the user starts. Pregnancy records are shown and exported like every other category and are never interpreted.

All thirteen permissions go in one declaration, per the decision above. It is filed when the nightly blocks. Step 1.0 adds the permissions to the manifest on main, and the next nightly upload to Play open testing stops until the Health Connect declaration covers them. That block is the signal to file the declaration. Nightly uploads resume once Play approves it, and production waits for the same approval. A refusal of any category holds both. The F-Droid and Codeberg builds are unaffected. What happens after a refusal is decided if it happens. The app has build types but no product flavors, so a Play build without the refused permissions would need a new flavor dimension.

### Privacy and permission docs

- `PRIVACY.md` and `docs/app/privacy.md`: a new "Medical Records" section. The existing "Health Records" heading in the privacy page means Health Connect fitness records and will become confusing. Rename it to "Health Connect Records".
- `docs/app/permissions.md`: a new "Medical Records Permissions" group with the 13 strings and the point-of-use rule.
- `docs/app/health-connect.md`: a "Medical Records" block under read and write coverage, plus the availability note.
- `PermissionsRationaleActivity` strings: one paragraph on medical records.
- Health disclaimer: unchanged in substance. Add one sentence that records are shown as received and OpenVitals does not interpret them.
- Saved documents, step 1.4: a source file is kept only when the user chooses to, in the app's private storage, never backed up, and deleted with the app. Opening one hands it to another app's viewer. The README line saying OpenVitals does not store health records locally changes at the same time.
- The medical import report includes record content. The privacy page says to review it before sharing.
- Phone-to-phone sync: the privacy page and `docs/features/device-sync.md` say medical records travel with it.
- The docs site repo keeps its own copies of the privacy, permissions, and Health Connect pages, and they already differ from this repo's. Both sets get the updates.

### After implementation

`docs/features/medical-records.md` with the standard header, a `feature-map.md` row, the `Features.md` inventory, the landing page feature cards, and the changelog on release. New strings go in `values/strings.xml`, following the repo's translation rule. AGENTS.md gets the amended write rule.

### Design system

Reuse `DashboardPillWidget` for the tile, `SettingsListItem`, `DetailRow`, `Card`, `OpenVitalsSurface`, `StepBar`, and the shared permission shell. Gaps to register in `components-map.md` in the same change that creates them: a general empty state, since only `ChartEmptyState` exists, and a row style for a declined category if `SettingsListItem` cannot express it cleanly.

## Phases

### Phase 1: FHIR direct

| Step | Scope | Size |
|---|---|---|
| 1.0 Spike | The medical reader, writer, and fake client, feature gate, all thirteen manifest permissions on main, one diagnostics-only screen that lists a category. Confirms the API works on the Pixel and measures page and batch cost. The nightly Play upload blocks from here until the declaration is approved, and that block is when the declaration is filed. | 2 to 3 days |
| 1.1 View, import, and export | One release. Dashboard tile with the first-tap permission request, records home with all twelve categories, category lists, detail with raw JSON. Import wizard on the `MedicalImportSource` seam with id assignment, source grouping, and the patient check. FHIR files and Apple Health clinical records by provider. FHIR bundle export that keeps sources, and share. Delete own records and sources. | 3 to 4 weeks |
| 1.2 Manual entry | Forms for vaccines, allergies, medications, and conditions, with the owner's Patient record. Its own feature. | 1 week |
| 1.3 PDF report section | Medical records in the health report. | 3 to 4 days |
| 1.4 Saved documents | Keep switch in the FHIR wizard and the Apple clinical records import, app-private storage with a Room index, the saved documents list, open, save, and share. | 1 week |
| 1.5 Phone-to-phone sync | Medical records category in the sync wizard, the export bundle as a new message type, the receiving pipeline, and the report line. | 1 week |

### Phase 2: document parsers, one feature each

Suggested order, most deterministic first. Each gets its own proposal page before work starts.

| Feature | Depends on |
|---|---|
| 2a SMART Health Cards from image and PDF | ZXing. |
| 2b CDA mapper | Nothing new. Covers C-CDA. Apple's `export_cda.xml` only after a real export is checked. |
| 2c Estonian portal export | A sample export. Probably 2b plus Estonian profiles. |
| 2f Apple DSTU2 to R4 | Nothing new. |
| 2d PDF text: lab reports, then vaccination lists | `PdfTextExtractor`, the analyte dictionary, the review table. |
| 2e OCR for scans and photos | 2d, Tesseract, the language data decision. |
| 2g On-device model | Experiment only, after the rest. |

Later, outside both phases: a camera QR scanner.

## Open Questions

1. **Apple `export_cda.xml`** (feature 2b). What it holds is unverified. Check a real export before the CDA mapper reads it.
2. **Play refusal** (step 1.0 onward). Decided if it happens. A store flavor dimension is the likely path.
3. **PDF text library** (feature 2d). Settled in the 2d proposal.

Everything else is recorded in [Decisions Taken](#decisions-taken).

## Risks

- **Experimental API.** Every call is opt-in and may change. Keeping all usage in one package bounds the churn.
- **No changelog API.** Views re-read on open. Fine at hundreds of records, unknown at tens of thousands.
- **Validation grows.** Rules the platform announces as coming will reject files that pass today. The pre-flight must stay in step, and the report must always say why a record was rejected.
- **Play review.** One declaration covers all thirteen permissions. The nightly stays blocked until Play approves it, and a refusal of any category holds production. See the policy section.
- **Patient check false alarms.** Providers spell names differently, with or without accents or second surnames. The check compares loosely and always lets the user confirm.
- **Feature availability.** Users on Android 13, and on Android 14 without the module update, never see the tile. The docs must say so.
- **Phase 2 recognition errors.** Parsed and OCR'd values can be wrong. The review step is mandatory, the note on each record says where it came from, and the disclaimer says so.
- **Full-detail reports.** The import report carries record content, so a user who shares it for help shares their records. The result step warns before sharing.
- **Phase 2 library upkeep.** If PdfBox-Android is chosen, it has had no release since 2023. It sits behind an interface so it can be replaced.

## References

- Medical Records overview: https://developer.android.com/health-and-fitness/health-connect/medical-records
- Data format, supported resources, validation: https://developer.android.com/health-and-fitness/health-connect/medical-records/data-format
- Write medical data: https://developer.android.com/health-and-fitness/health-connect/medical-records/write-data
- Read medical data: https://developer.android.com/health-and-fitness/health-connect/medical-records/read-data
- Rate limiting: https://developer.android.com/health-and-fitness/health-connect/rate-limiting
- Jetpack release notes (PHR added in 1.1.0-beta02): https://developer.android.com/jetpack/androidx/releases/health-connect
- `HealthConnectClient`, `MedicalResource`, `HealthConnectFeatures` sources: https://github.com/androidx/androidx/tree/androidx-main/health/connect/connect-client
- Play policy update, 5 March 2025: https://support.google.com/googleplay/android-developer/answer/15931464
- Play health permissions guidance: https://support.google.com/googleplay/android-developer/answer/12991134
- FHIR R4: https://hl7.org/fhir/R4/ and FHIR R4B: https://hl7.org/fhir/R4B/
- SMART Health Cards framework: https://spec.smarthealth.cards/
- C-CDA on FHIR mappings: https://build.fhir.org/ig/HL7/ccda-on-fhir/
- Estonian Health Portal: https://www.terviseportaal.ee/en/terviseportaalist
- Android `PdfRenderer.Page` (API 35 text contents): https://developer.android.com/reference/android/graphics/pdf/PdfRenderer.Page
- Jetpack `androidx.pdf` releases: https://developer.android.com/jetpack/androidx/releases/pdf
- PdfBox-Android: https://github.com/TomRoush/PdfBox-Android
- Tesseract4Android: https://github.com/adaptech-cz/Tesseract4Android
- ZXing core: https://github.com/zxing/zxing
- ML Kit GenAI Prompt API: https://developers.google.com/ml-kit/genai/prompt/android
- LiteRT-LM: https://github.com/google-ai-edge/LiteRT-LM
