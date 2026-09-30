# Medical Records Through Health Connect

> **Status:** Proposal draft, 2026-09-25. Revised on 2026-09-26 after a review pass, and on 2026-09-29 after a check against the code and the spike on the test phone. Step 1.0 (the spike) and slices 1.1a (the FHIR core), 1.1b (the screens and the tile), 1.1c (the import wizard), 1.1d (Apple Health clinical records), 1.1e (export and delete) and 1.1f (docs, the re-ask, screenshot tests, and removing the spike screen) are built. Step 1.1 is complete, and so are steps 1.2 (manual entry), 1.3 (the PDF report section), 1.4 (saved documents) and 1.5 (phone-to-phone sync). Phase 1 is built. Of phase 2, features 2a (SMART Health Cards, with a camera scanner), 2b (CDA documents), 2c (the Estonian health portal export) and 2f (Apple DSTU2 records) are built too. 2d, 2e and 2g are left for later. All of it reached main on 2026-09-30 (pull request #339). [What is left](medical-records-left.md) lists the open work.
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

Recorded 2026-09-25, 2026-09-26, and 2026-09-29:

| Question | Decision |
|---|---|
| Sensitive categories (pregnancy, social history, personal details, practitioner details) | All twelve categories ship in phase 1. One Play declaration covers them all. |
| FHIR vital signs on the existing vitals screens | No. Medical records stay self-contained. OpenVitals never writes Health Connect vitals as FHIR or the reverse. |
| Entry point | A dashboard tile called Medical Records, like Steps or Distance. Imports stay under Settings, Data Importers, and are also reachable from the records home. |
| Document parsers | Phase 2. Phase 1 imports FHIR directly and nothing else. Each parser (health cards, CDA, Estonian export, PDF text, OCR) is a feature of its own. |
| Estonian export | Built on 2026-09-30, ahead of 2a and 2b, from the owner's own export. The portal zip holds one Estonian CDA XML per document and its PDF. The XML is mapped; each PDF is kept as the original. A referral gives only its diagnoses, as provisional Conditions (Health Connect has no ServiceRequest); the national ID code is left out. |
| Manual entry codes | Free text only. An optional code field, nothing bundled, no licence work. |
| Tile content | Static: glyph, title, and "Tap to browse". No reads, no summary, no staleness. |
| SMART Health Card signatures (phase 2a) | Not verified. Every imported card is labelled "signature not verified". No issuer-key snapshot. |
| Keeping the source document | The user chooses per import. The switch starts off every time and nothing is remembered. FHIR files and parsed documents can be kept, files with no records cannot. A saved copy lives in the app's private storage with a Room index, and the wizard says it takes space on the phone. Ships in phase 1 as step 1.4. |
| Apple clinical records | One data source per provider, from the `ClinicalRecord` entries in export.xml. DSTU2 records are converted to R4 (feature 2f); a type with no mapping is skipped before any write. |
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
| FHIR version | Detected from R4B hints, else R4. A matched source's version wins. A version the file states, as Apple's export does, must match the source's: otherwise the group gets its own source. |
| Import report | Full detail, like the Apple importer's, with a review warning before sharing. |
| Play refusal | Decided if it happens. |
| Phone-to-phone sync | Medical records sync in full, as step 1.5. |
| Privacy policy | Updated in step 1.0, describing all of phase 1 at once, so it is live when the declaration is filed. Users see one re-prompt. |

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
| `readMedicalResources(ReadMedicalResourcesInitialRequest)`, then `ReadMedicalResourcesPageRequest` | Paged read by category and a set of source ids, which has no default. The response carries `nextPageToken` and `remainingCount`. The default page size is 1000. |
| `readMedicalResources(List<MedicalResourceId>)` | Read by id. |
| `deleteMedicalResources(ids)`, `deleteMedicalResources(DeleteMedicalResourcesRequest)` | Delete own records. |
| `deleteMedicalDataSourceWithData(id)` | Delete a source and everything in it. |

Validation on write, as documented: valid JSON, a supported version and type, an `id` that matches the FHIR id rule, uniqueness per (source, type), no `contained`, top-level fields that exist in the spec with the right JSON types, and at most one choice field (`effectiveDateTime` or `effectivePeriod`, never both). The docs call deeper checks "coming", but the test phone already has them on. Its Health Connect flags include `phr_fhir_primitive_type_validation`, `phr_fhir_complex_type_validation`, `phr_fhir_extension_validation`, `phr_xhtml_validation`, and `phr_fhir_validation_disallow_empty_objects_arrays`. So primitives, complex types, extensions, and XHTML narratives are checked, and empty objects and arrays are refused.

Missing from the platform today:

- No changelog call for medical records in the Jetpack client. The platform has a `phr_change_logs` flag, but `connect-client` 1.2.0-alpha06 exposes nothing for it. Every view is a fresh paged read.
- No count call as such, but `remainingCount` on a page gives one: a read of page size 1 returns the category's total. No documented sort order, so sorting is the client's job.
- No published numbers for rate limits. The general Health Connect quotas apply, stricter in the background.
- The API is experimental and may change.

### Availability

The feature needs Android 14 or newer with Health Connect module SDK extension 16 or newer. Android 13 and older never get it. The check is `getFeatureStatus`, which the app already uses for mindfulness, history, background reads, skin temperature, and planned exercise in `healthconnect/HealthConnectPermissionService.kt`.

The test phone, a Pixel 6 Pro on GrapheneOS with Android 17, has SDK extension 22 and `personal_health_record=true`. The spike ran there. See [Spike findings](#spike-findings).

## Where OpenVitals Stands

| Fact | Consequence |
|---|---|
| `connect-client` 1.2.0-alpha06 in `gradle/libs.versions.toml`, compileSdk 37, targetSdk 36, minSdk 26 | The medical classes are already in the AAR. No dependency change for phase 1. |
| No medical permission in `AndroidManifest.xml` | 13 new `<uses-permission>` lines and a Play Console declaration. |
| On Android 14 and newer, `loadGrantedPermissions()` checks only the permissions in `managedPermissions` | The 13 permissions join `managedPermissions`, gated on the feature like `plannedExercisePermissions`, or a grant is never seen. They stay out of `allPermissions`, the phase sets, and the onboarding sets, so `PERMISSION_SET_VERSION` stays at 4 and nobody is re-prompted. |
| The Jetpack medical classes (`FhirVersion`, `MedicalDataSource`, `MedicalResource`, and the requests) check the platform feature and build platform objects in their constructors. `connect-testing` 1.0.0-alpha04 has no medical calls | No JVM test can create a Jetpack medical object. The reader and writer depend on a small `MedicalRecordsClient` interface in app types. `HealthConnectMedicalRecordsClient` is the one class that touches the Jetpack classes, and the device run is its test. Unit tests use a fake of the interface. |
| No Health Connect writer class exists. Writes live in the readers or in `AppleHealthImportRepository.insertImportedRecords`. `HealthConnectManager.kt` is a facade with one wrapper per call, at 752 of its 800-line limit | `MedicalRecordsWriter` is a new pattern. The manager builds the reader and the writer and exposes them as two properties, not one wrapper per call. |
| `PreferencesRepository.kt` is at 1445 of its 1450-line ceiling | The one stored medical value lives in a small `MedicalRecordsPreferences` of its own. |
| kotlinx-serialization-json is used only through `JsonElement`, no plugin, no `@Serializable` | FHIR handling can stay on the tree API. No FHIR SDK. |
| The Apple Health importer already opens the export zip in `features/imports/applehealth/AppleHealthImportParser.kt` and skips unknown entries. Analyze runs in `DataImportViewModel` and reads only export.xml. The import pass runs in `AppleHealthImportWorker`, a WorkManager foreground worker. Apple's export.xml lists each clinical record as a `ClinicalRecord` element with `type`, `identifier`, `sourceName`, `sourceURL`, `fhirVersion`, `receivedDate`, and `resourceFilePath` | The provider and FHIR version of every record are known before its JSON is read. What `export_cda.xml` holds is unverified. It likely holds Apple's own measurements, and importing those as FHIR would break the self-contained vitals decision, so feature 2b checks a real export first. |
| The PDF report is built on the platform `PdfDocument` in `features/reports/pdf/`, with per-section "missing" notes | A medical records section fits the existing model. The app writes PDFs but has no PDF reader, which matters in phase 2. |
| Each feature wires `OpenDocument`, `CreateDocument`, and the app `FileProvider` itself. `core/export/ExportStaging.kt` only stages a file in a cache folder and prunes that folder's files older than a day | Import and export follow the route and cycle journal exports. |
| Dashboard tiles are `DashboardWidgetId` entries rendered through `DashboardPillWidget`, which wraps `MetricStatCard`. The watch tile is the only one with no metric. New ids are appended to saved layouts by `dashboardWidgetIdsWithNewOnesAppended` | A Medical Records tile renders as a pill with no value. Three details decide whether existing users see it, listed under [Entry point](#entry-point-a-dashboard-tile). |
| Shared shell: `WithHealthConnectFeatureScreen`, `HealthConnectFeature`, `rememberHealthConnectPermissionLauncher`. It always renders the screen and shows a callout above it for missing permissions. Only `DATA_IMPORT` blocks. Policy: asking happens at point of use, never on the dashboard | The tile never prompts by itself. Its first tap opens the records home, which asks once for every medical permission. The shell needs no change. |
| No Google dependency for functionality. Distributed on Google Play and on F-Droid and Codeberg | A phase 2 parser that needs a proprietary library is a separate decision. `CAMERA` was added on 2026-09-30, by the owner's decision, for the card scanner only. Play's health records policy applies to the Play build. |
| No `INTERNET` permission. Health Connect is the source of truth. No local mirror of records | Import and export are file based. No FHIR server client. No Room copy of records. |

## Spike Findings

Measured on 2026-09-29 on the test phone, through the diagnostics-only spike screen, since removed, with the 12-record sample bundle in `app/src/test/resources/fhir/`.

| Question | Answer |
|---|---|
| Feature status | `FEATURE_PERSONAL_HEALTH_RECORD` is available. |
| Permission screen | A request with only the 13 medical permissions opens Health Connect's own medical records screen, with "Allow all". |
| Grant seen by the app | Yes, once the permissions are in `managedPermissions`. |
| No permission at all | Every read throws `SecurityException` naming the missing permission. Listing sources throws "Caller doesn't have permission to read or write medical data". |
| Read declined, write granted | A category read returns this app's own records and does not throw. The declined-category design holds. |
| Empty source filter | Returns this app's records. No other app on the phone has medical data, so the cross-app case is untested. |
| Count | A page of size 1 plus `remainingCount` gives the total. `remainingCount` counts the records after the page. |
| Page size | At most 5000: the client throws `IllegalArgumentException` above it. 100 records take 147 ms, 1000 take 607 ms, 1617 in one page take 874 ms. |
| Whole category | 1617 records in two pages of 1000 take about 965 ms. |
| Batch size | 1, 10, 100, 500 and 1000 records all write, in 24, 68, 233, 484 and 816 ms. No limit found at 1000. |
| Health Connect browser | Shows the imported records under the source name. Deleting the source empties it. |
| Rejection shape | `IllegalArgumentException` wrapping `HealthConnectException`, with a reason that names the rule and the field, such as "Found empty array in field: reasonCode". |
| Refused | Empty arrays and objects, a bad `dateTime`, a number where a string belongs, an unknown field, an extension without `url`, `<script>` in a narrative, `contained`, two choice fields, an invalid id, an unsupported type, and an Observation with neither a category nor a classifiable code. |
| Accepted | A valid narrative, a narrative whose `div` lacks the XHTML namespace, and an extension with a `url`. |
| LOINC without a category | Body weight goes to vital signs, pregnancy status to pregnancy, smoking status to social history. |

What this changes for step 1.1:

- Category lists page at 1000. The records home counts each category with one page of size 1.
- Imports write batches of 100. A rejection is common, so the per-record retry is the normal path.
- The import report quotes the platform's reason. It is specific enough to show as it is.
- Pre-flight stays small. The platform's own checks are strict and well worded.

## Phase 1: Proposed User Experience

### Entry point: a dashboard tile

A **Medical Records** tile, `DashboardWidgetId.MEDICAL_RECORDS`, placed after the Cycle tile in `DefaultDashboardWidgetIds`. Existing users get it appended to their layout, as the watch tile was. Three details make that true:

- `WidgetIdsKnownBeforeTracking` in `DashboardWidgetId.kt` is every entry minus WATCH. It must also subtract `MEDICAL_RECORDS`. Otherwise users who upgrade from before id tracking never get the tile. `DashboardWidgetOrderMigrationTest` gets a case.
- `showsNoDataMessage()` in `DashboardDisplayState.kt` treats a tile with no value as empty, and empty tiles sort last by default. The tile needs its own branch, as CYCLE and WATCH have.
- `homeMetricWidgetCatalog` lists every id unless filtered. The medical tile stays out of the home-screen widget picker.

The tile is static. It is a `DashboardPillWidget` with a medical records glyph, the title, and a new message, "Tap to browse". No value, no subtitle, no reads. The platform has no count call, and a summary would need daily reads and a staleness story, so the tile carries none. It also stays out of the metric-group coalescer, which is where a wedged group has stuck tiles on Loading before.

The tile still needs an entry in `display.widgets`, because `dashboardWidgetSpecs` skips any id without one. The entry carries no value.

The tile never asks for permissions by itself. Its first tap is the point of use: it opens the records home, which asks for every medical permission at once. See [Permissions](#permissions).

Gating. `toDashboardMetricOrNull()` returns null, so provider support never hides the tile. Feature status does. When `FEATURE_PERSONAL_HEALTH_RECORD` is unavailable, `buildWidget` in the presentation mapper returns null for the tile, as it does for WATCH with no watch paired. The mapper skips a null before its unsupported check, so the same check also keeps the tile out of edit mode's add tray. Users on Android 13, and on Android 14 without the module update, never see the tile.

### Permissions

The first time the medical area opens, OpenVitals makes one Health Connect request with all thirteen medical permissions: the twelve read permissions and write. The first tap of the tile is the usual trigger. Opening medical import from Settings or choosing clinical records in the Apple Health importer triggers the same request if it has not happened yet. Phone-to-phone sync never asks: it offers medical records only when write access is already held.

- The request holds only medical permissions. It never mixes them with fitness permissions, so Health Connect shows its own medical permission screen. The user can turn off any permission there.
- It goes through the shared shell. A `HealthConnectFeature.MEDICAL_RECORDS` entry returns all thirteen. The shell renders a screen with partial access anyway. The records home shows its own callout above the content instead of the shell's, so it can leave out what Health Connect no longer asks for. No row asks on its own.
- A small `MedicalRecordsPreferences` stores that the first request has happened.
- The category and record screens use a quiet shell entry, `HealthConnectFeature.MEDICAL_RECORDS_BROWSE`, that requires nothing, so the prompt card shows on the records home only.
- A category the user denied twice is fixed, and Health Connect then closes any request that includes it at once, even when other categories could still be asked for. Seen on the test phone on 2026-09-29. Settled in slice 1.1f: the records home and the import wizard ask themselves, not through the shell's callout. After the first request, Android's rationale check is false for a permission refused twice, so the re-ask leaves it out. When nothing is left to ask, the button opens Health Connect's settings, which can still grant it.
- After the first request, OpenVitals does not ask again by itself. A category the user turned off still lists the records OpenVitals itself imported, which write access lets it read, with a line saying records from other apps need access. One screen-level callout re-asks for what is missing and can still be asked for, or opens Health Connect's settings.
- Write is granted up front, so the import wizard does not stop mid-way. If the user declined write, the wizard asks for it before the analyze step, because matching existing data sources needs it.

### Records home

One row per category, in two blocks: **Care** (vaccines, allergies, conditions, medications, lab results, procedures, visits, vital signs) and **Sensitive** (pregnancy, social history, personal details, practitioner details). All twelve ship in phase 1. The split is presentation only. A category with permission reads one page of size 1 and shows the count from `remainingCount`, such as "12 records", or "No records". A declined category follows the rule in [Permissions](#permissions).

Two actions at the top: Import and Export all.

### Category list

One `SettingsListItem`-style row per record: leading category glyph, title (vaccine name, allergen, test name), supporting text (date, source display name, status). The screen pages through the category at 1000 records a page, sorts locally, and shows progress while paging. On the test phone a page of 1000 takes about 0.6 s. Personal record volumes are tens to low hundreds, so loading a category fully is acceptable. The spike should confirm this.

- **Rows are events.** Immunizations, allergies, conditions, medication requests and statements, observations, procedures, and encounters are rows. Locations, organisations, and bare drug definitions show inside the records that point at them, and become rows only when nothing points at them. Personal details and Practitioner details list their Patient and practitioner records, since those categories hold nothing else.
- **References.** A reference shows the referenced record's name when it is readable, else the reference's own `display` text, else "needs access to <category>".
- **Sorting.** Newest first, by each type's own date: `occurrence[x]` for immunizations, onset or `recordedDate` for allergies and conditions, `authoredOn` or `effective[x]` for medications, `effective[x]` for observations, `performed[x]` for procedures, `period.start` for encounters. A partial date such as "2020" sorts as its first day and shows as written. Records without a date sort last, by name.
- **Status.** Every record shows, each with its status label, including entered-in-error, refuted, inactive, and resolved. The label is a word, not only a colour.

### Record detail

`DetailRow`s in a `Card`: the curated fields for the resource type (see FHIR handling), a **Source** row with the data source display name and FHIR base URI, and an expandable **Raw FHIR** section with the pretty-printed JSON as selectable text. Actions: share this record as a FHIR JSON file; delete when OpenVitals wrote it, otherwise a note that another app owns it with a link to Health Connect settings.

Lab results and vital signs show the value, the unit, and the reference range as text. A High or Low flag appears only when the lab set it in `interpretation`. OpenVitals computes nothing, which keeps the promise that it does not interpret records. No trend charts. Vital signs stay in this area and never join the vitals detail screens.

### Import (the "upload")

Lives under Settings, Import & export, next to CSV and Apple Health, and is also reachable from the records home. It is a stepped screen built on `StepBar`, like CSV import. Phase 1 accepts FHIR only.

1. **Choose a file.** `OpenDocument` for `application/json`, `application/fhir+json`, `application/x-ndjson`, `text/plain`, and `application/octet-stream`. Accepted shapes: a single FHIR resource, a FHIR `Bundle` of any type (entries are unwrapped), or NDJSON with one resource per line. Resources without ids, bundles that link entries by `urn:uuid` URLs, and resources with contained resources are all accepted. See [Ids and references](#ids-and-references). Anything else is refused with one line that names the phase 2 parsers as not available yet. If write access is missing, the wizard asks for it here.
2. **Analyze.** Parse, assign ids, rewrite references, and group the records by source. Show counts per category and the FHIR version, detected from R4B hints such as `meta.profile` URLs and otherwise R4. When a group matches an existing source, that source's version wins. Resources of types Health Connect does not support, such as Composition, DiagnosticReport, and DocumentReference, show as skipped with a count per type. Records that will be rejected are listed with the reason, such as an `Observation` without a category the platform can classify. The step names the data source each group goes to, runs the [patient check](#patient-model), and marks groups already in Health Connect from another app. Nothing is written yet.
3. **Confirm.** Confirm or edit the display name of each new data source.
4. **Import.** Upsert in batches of 100 with progress. A batch of 100 takes about 0.25 s on the test phone, and a failed one costs 100 single writes of about 25 ms. A batch is transactional, so one bad record fails its batch. The importer then retries that batch one record at a time to isolate the bad one, and keeps going. The platform checks more than pre-flight can, so this retry is the normal path for rejections, not a rare one.
5. **Result.** Written, updated, skipped, and rejected counts, grouped by reason, and a copy or save report, like CSV import. "Updated" comes from reading each batch's ids before the write, because upsert does not say. The report includes record content, like the Apple importer's, and the result step says to review it before sharing.

Phase 2 parsers plug in behind step 1 and add a review table to step 2. Steps 3 to 5 do not change. The `MedicalImportScreen` is built with that seam from the start: a `MedicalImportSource` that yields proposed resources, with the FHIR file source as its only phase 1 implementation. Built in slice 1.1c: `MedicalImportPlanner` matches groups to sources, `ImportMedicalRecordsUseCase` writes them, and `MedicalImportReport` writes the report. A file that names no origin is matched by the name the user types, so re-importing it under the same name updates the same source.

**Apple Health export.** The clinical records in an Apple Health export go through the same wizard as a FHIR file, with the grouping taken from Apple's own index. Built in slice 1.1d, which changed the plan below: the records do not ride along with the Apple import's background worker. That worker cannot show the review or the patient check, and medical permissions must never be asked for together with the fitness permissions the Apple card asks for.

- The Apple import's analyze step counts the `ClinicalRecord` elements in export.xml. When there are any, a card under the Apple card says how many and opens the medical import on the same export. The Apple export stays readable, since the Apple card holds a lasting read grant.
- The medical import also accepts an Apple export zip picked directly. `PickedFileImportSource` sends a zip to `AppleClinicalRecordsImportSource` and anything else to the FHIR file source.
- One pass over the zip reads the index from export.xml, through the Apple importer's own parser, and the `clinical-records/` files, in whatever order the zip holds them. The files share the 8 MB budget of a FHIR file.
- One data source per provider. `fhirBaseUri` is the provider's `sourceURL` and the display name is its `sourceName`. A provider with no URL gets a base from its name. Two hospitals never share a source, so their ids cannot collide.
- `fhirVersion` decides per record. 4.0.1 and 4.3.0 continue. 1.0.2, which is DSTU2, is converted to R4 first (feature 2f). Any other version is left out before analysis, with its reason.
- A provider with records in both R4 and R4B gets one source per version, because a data source has one version. The second one gets the version as a name suffix. A re-import matches each source by base URI and version. Health Connect accepts two sources with one base URI (checked on the Pixel).
- An index entry with no file, or a file with no index entry, is left out and reported. The review lists every record left out before import, by reason, and so does the report.

### Export (the "download")

- **FHIR file.** From the records home, "Export all" builds a FHIR `Bundle` of type `collection` with every record the app can read and saves it through `CreateDocument` as `openvitals-medical-records-<date>.json`, or shares it through the FileProvider. From a category or a record, the same for that subset. The bundle keeps where each record came from, so a re-import restores the same sources and ids:
  - Each entry's `fullUrl` is the data source's `fhirBaseUri` followed by `<type>/<id>`, which is what FHIR defines `fullUrl` to be. Records from different sources can share an id without clashing.
  - Each entry carries an OpenVitals extension with the data source's display name, the package that wrote it, and its FHIR version. The importer keys a group by base URI and that version, so an origin in R4 and R4B comes back as two sources.
  - The bundle's `meta.tag` marks it as an OpenVitals export, with the app version.
  - Resources are written exactly as Health Connect returns them. Ids and references inside them are not changed.
  - A shared export is staged in a new `medical_exports` cache path in `file_paths.xml`. Each export deletes the previous file in that folder (`stageSoleExport`), instead of the day `stageExport` keeps. No sweep at app start.
  - Built in slice 1.1e as one dialog on the records home ("Export all"), a category, and a record. It names the record count, warns that the file holds medical records, and lists the categories that hold only OpenVitals' own records or none, for lack of access. The file then goes to Share or to Save.
- **PDF report section**, later in phase 1. The health report builder gets a "Medical records" section: vaccines, allergies, medications with their recorded status, conditions, and lab results in the date range, with the existing honesty rule when a category has no read permission. `ReportPdfWriter.kt` is at 970 of its 1000-line ceiling, so the section's builder goes in its own file, as `ReportPdfCycleItems.kt` does.
  - Built in step 1.3. Allergies, conditions and medications are standing facts, so every record shows with its status; vaccines and lab results show when dated in the range. The section is a report option of its own, not a `ReportMetric`, since medical records have no dashboard metric. It asks for no permission, because the builder's request would mix medical and fitness permissions, and names what access kept out. With no metric chosen the fitness history limit does not apply, so a medical-only report keeps its range.

### Delete

Only records OpenVitals wrote. Per record from the detail screen. Per data source from an "Added by OpenVitals" screen, reached from the records home, with a confirmation that names the record count. Re-import never deletes. It upserts. A record another app wrote shows a button that opens Health Connect's data screens, where it can be deleted. Built in slice 1.1e.

### Manual entry

Its own feature, after the import work. Simple forms for the four categories a person can reasonably type: a vaccine, an allergy, a medication, a condition. OpenVitals authors the FHIR resource with a UUID id, `meta.source` set to the app, the name as free text in `code.text`, and a reference to the owner's Patient record. An optional code field exists for users who have one, but nothing is bundled and no vocabulary ships. [Patient model](#patient-model) covers the patient and the other required fields. This is direct FHIR, not parsing, so it stays in phase 1, but it can move if phase 1 needs to be smaller.

Built in step 1.2. Each form also takes a note, and a vaccine its lot number, an allergy its reaction and criticality, and a medication its dosage and start date. A record typed in can be edited from its detail screen: the form replaces only the fields it shows and keeps the rest. The first entry asks for the owner's name, with an optional birth date, only when no Patient record can be read.

### Saved documents

Step 1.4. The user decides, per import, whether OpenVitals keeps the file the records came from. The switch starts off on every import, and nothing is remembered.

**Which files.** FHIR files from the import wizard, and every phase 2 document that produces records. A kept FHIR file holds what Health Connect drops on import, such as DiagnosticReport, DocumentReference, and Composition resources. For an Apple Health export, the switch keeps only the clinical records files, packed as one zip, not the whole export. A file that yields no records cannot be kept. OpenVitals is not a general document store. The one exception, asked for by the owner on 2026-09-30: the PDFs of the declarations of intent and authorisations in an Estonian portal export, which belong to the person's record but have no FHIR type.

**Where the choice is made.** The Confirm step gets a switch, "Keep a copy of this document on the phone". Under it: the file's size, the total space saved documents already use, and one line: "Saved documents take space on this phone. They stay in the app's private storage, are not backed up, and are deleted when the app is uninstalled." The install size of the app does not change, only its data does.

**Storage.** Files go under the app's private files directory, `files/medical_documents/<uuid>.<ext>`, never the cache, which `ExportStaging` prunes. A Room table `medical_documents` indexes them: id, original file name, MIME type, size, SHA-256, import time, data source id, and source display name. A second table, `medical_document_records`, links a document to the `MedicalResourceId`s imported from it. The same file twice, by hash, keeps one copy. This is a bump to the next Room version, with its migration, the committed schema file, and the architecture doc row, per the playbook. It qualifies under the playbook rule because a document file is data Health Connect cannot hold. `file_paths.xml` gains a `files-path` for the folder.

**Where documents are seen.** The records home gets a "Saved documents" row: a list with name, date, size, and the number of records that came from each file, plus the total space used and "Delete all". A document opens in whatever viewer the phone has for its type through the `FileProvider` (`ACTION_VIEW`), can be saved elsewhere through `CreateDocument`, or shared. A record whose document was kept gets an "Open original document" action on its detail screen. Deleting a document does not delete its records, and the confirmation says so. Deleting records does not delete the document. A link to a record that no longer exists in Health Connect is dropped when the list loads.

**Privacy.** The file can hold more than the records extracted from it: names, identifiers, free text. The privacy page says so. Opening a document hands it to another app's viewer, which the privacy page also says. Device file-based encryption covers the private directory. No extra encryption layer in the first version.

Saved documents do not travel with phone-to-phone sync. Only records do.

Built in step 1.4, at Room version 15. The whole app already sets `allowBackup="false"`, so no backup rule was needed. The copy is held in memory from the analyze step until the import ends, so the picked file is read once; the Apple zip is packed from the clinical files the source already read. A JSON file with no JSON viewer on the phone is offered as plain text.

### Phone-to-phone sync

Step 1.5. Medical records travel with [Sync with another phone](../features/device-sync.md).

- **Category.** The sync wizard's category picker gains "Medical records". It appears when this phone has the medical feature, holds write access, and can read at least one medical category.
- **Range.** Medical records ignore the "how far back" choice and always sync in full, because a vaccination history cut at one year is not useful. The picker says so under the category.
- **Payload.** Each record travels as the entry the FHIR export writes, with its source kept in `fullUrl` and the OpenVitals extension. It is a new record type, `MedicalRecord`, in the existing batches, handled by `MedicalRecordsSyncStore` in a file of its own, because `SyncRecordCodec.kt` is at 1100 of its 1150-line ceiling. The phones negotiate the types both support, so an older build does not see it.
- **Receiving.** The receiver runs the import pipeline without the wizard: source grouping, matching by base URI, the rule that skips sources another app owns, and pre-flight. A record lands in the receiver's own source for its origin, so a re-sync upserts.
- **Identity.** A record's key is its source's base URI, its FHIR version, its type and id, and a hash of its content. An unchanged record counts as already present. When the phones hold two versions of one record, only the one with the later `meta.lastUpdated` replaces the other, and a version with no such time never does. Otherwise both phones would swap versions in one sync. Manual entries stamp `meta.lastUpdated` on each save.
- **Patient check.** Patient records go first, in one batch of their own. The receiver compares them with its own Patient records. A mismatch, or incoming records that name more than one person, holds back every medical record from that session, and the report says why. The user can still move them with an export and a checked import.
- **Report.** The sync report gains a medical records line: added, already here, skipped, and not added.

## Phase 1: Design

### Packages and classes

Health Connect access stays in `healthconnect/`, as the AGENTS.md reader rule requires. `HealthConnectLayeringTest` also stops those classes from importing repositories.

| Piece | Location | Notes |
|---|---|---|
| `MedicalRecordsHealthReader` and `MedicalRecordsWriter` | `healthconnect/` | Created inside `HealthConnectManager`, and exposed as two properties because the manager is near its size limit. They call Health Connect through `MedicalRecordsClient` and check the feature first. Reads use `HealthConnectReaderSupport.withLoggingOrThrow`, so a missing permission reaches `toScreenError()` instead of a silent empty list. Writes pass the manager's sync gate. The AGENTS.md write rule and the writer notes in `docs/engineering/architecture.md` are amended in the same change: medical writes go through `MedicalRecordsWriter`, because FHIR records have no `clientRecordId`. |
| `MedicalRecordsClient` and `HealthConnectMedicalRecordsClient` | `healthconnect/` | The medical calls in app types, and the real client over `HealthConnectClient`. With `MedicalCategoryMapping`, the only code with `@OptIn(ExperimentalPersonalHealthRecordApi::class)`. The mapping also names the permission strings and the category and FHIR type ids. |
| `FakeMedicalRecordsClient` | `app/src/test/.../healthconnect/` | `MedicalRecordsClient` on in-memory maps: the documented write checks, all-or-nothing batches, page tokens with `remainingCount`, own-only delete, and denied categories. It does not model the phone's deeper checks. |
| `MedicalRecordsRepository` | `data/repository/contract/` and `data/repository/MedicalRecordsRepositoryImpl.kt` | Paged reads by category, own-source listing, upsert batches, delete. Bound in `di/RepositoryModule.kt`. |
| FHIR parsing and summaries | `domain/medical/` | Built in slice 1.1a. `FhirFileParser` (file to entries: one resource, a Bundle, or NDJSON), `FhirSourceGrouper` (splits by index origin, `fullUrl` base, or `meta.source`), `FhirVersionDetector`, `FhirIdAssigner` (ids, contained resources, reference rewriting), `FhirPreflight` (the local checks), `FhirImportAnalyzer` (the analyze step, in that order), `PatientCheck`, `FhirSummaries` (per-type display fields and sort dates), `FhirBundleWriter` for export. Pure Kotlin on `JsonElement`, unit-tested on fixtures. |
| Import source seam | `domain/medical/MedicalImportSource.kt` | Interface: a file in, proposed resources with optional source snippets out. `FhirFileImportSource` is the phase 1 implementation. Phase 2 parsers implement it. |
| Category model | `domain/model/MedicalCategory.kt` | Enum with title, glyph, and block (care or sensitive), free of Health Connect types. The mapping to `MEDICAL_RESOURCE_TYPE_*` and permission strings lives in `healthconnect/`, because the playbook keeps Health Connect permissions below the repository layer. |
| Sync | `features/devicesync/` | A medical records category, a message type that carries the export bundle, and the receiving side of the import pipeline. |
| Tile | `features/dashboard/` | `DashboardWidgetId.MEDICAL_RECORDS`, a branch in `dashboardWidgetSpecs` next to the cycle branch, a title and meta entry, and a feature-available flag passed into `DashboardPresentationMapper.build`, so `buildWidget` can return null. The exhaustive `when`s in `metricTitleRes`, `DashboardData.toSnapshot`, `homeMetricTitleRes`, and `dashboardTileDestination` get a branch each, plus the three details in [Entry point](#entry-point-a-dashboard-tile). No display model of its own. |
| Screens | `features/medical/` and `features/imports/medical/` | `MedicalRecordsScreen` (home), `MedicalCategoryScreen`, `MedicalRecordDetailScreen`, `MedicalImportScreen`. ViewModels are `@HiltViewModel`. State holds display models, never raw resources. |
| Navigation | `navigation/Screen.kt`, `AppNavigationMetricRoutes.kt`, `AppNavigationSettingsRoutes.kt` | `Screen.MedicalRecords`, `Screen.MedicalCategory`, `Screen.MedicalRecordDetail`, `Screen.SettingsMedicalImport`. The tile maps to `Screen.MedicalRecords` where the other widget ids map to their routes. Add each to `Screen.all`. |
| Permissions | `healthconnect/HealthConnectFeature.kt`, `HealthConnectPermissionService.kt`, the manifest | One `HealthConnectFeature.MEDICAL_RECORDS` entry that returns all thirteen permissions, plus its branch in the shell's copy. A `medicalRecordsPermissions` set, empty when the feature is unavailable, added to `managedPermissions` only. An `isMedicalRecordsAvailable()` check next to the existing feature checks. |
| Errors | `core/presentation/ScreenError.kt` | A new `ScreenError.FeatureUnavailable` for the feature gate's `UnsupportedOperationException`. `HealthConnectUnavailable` exists, but its text speaks of Health Connect as a whole. |

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
- **The patient check.** The analyze step compares each Patient in the file with the Patient records already in Health Connect, by name and birth date. Names compare without case, accents, or a missing second given or family name. A missing name or birth date counts as unknown, not as a difference. A match passes silently, and so does a first import into an empty store or a file with no Patient. A mismatch, or a file that names more than one person, stops the import and shows both identities side by side. The user can confirm "These records are mine" or cancel. Without access to personal details, the check cannot compare, so it shows the file's patient and asks for the same confirmation. OpenVitals does not hold records for other people, so a child's vaccination card is cancelled, not filed under the owner. The check counts people, not Patient records: an OpenVitals export with several sources holds one Patient per source, all for the same person.
- **Manual entry.** The manual source gets one Patient with the id `self`, created with the first manual entry. Its name and birth date come from an existing Patient record when one is readable. Otherwise the first manual entry asks for them once. Every manual record references `Patient/self` and fills the other fields FHIR requires or expects: `status` and the occurrence date on an immunization, `clinicalStatus` on an allergy and a condition, `status` on a medication statement. Each save stamps `meta.lastUpdated`, so phone-to-phone sync can tell the newer edit.
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

Pre-flight runs after id assignment and mirrors the platform's enforced rules, so rejections are explained before the write, not after: the JSON parses, `resourceType` is supported, `id` matches `[A-Za-z0-9\-\.]{1,64}`, there is no `contained`, an `Observation` carries one of the three category codes or a LOINC code, and there is no empty object or array. After id assignment, the id and contained checks are a safety net. Pre-flight knows the three category codes the platform names: `laboratory`, `vital-signs`, and `social-history`. The platform's LOINC lists for pregnancy, social history, and vital signs are not all published, so an Observation that passes pre-flight can still be rejected on write.

Pre-flight does not try to mirror the deeper checks the platform already runs: primitives, complex types, extensions, XHTML narratives, and empty objects or arrays. A record that passes pre-flight can fail any of them. The report then gives the platform's reason. The id assigner and contained-resource lifting must never leave an empty object or array behind, such as `contained: []`.

### No local copy of records

Health Connect stays the source of truth, as everywhere else in the app. No Room table holds records, and no record data goes in preferences. The only stored value is the note in `MedicalRecordsPreferences` that the first permission request has happened. The tile is static, so there is no summary to keep. Saved documents, step 1.4, are files with an index, not records.

### Errors, limits, threading

- Throwables go through `toScreenError()`. `SecurityException` already maps to `PermissionDenied`, and the shell renders the grant affordance. The `UnsupportedOperationException` from the feature gate maps to the new `ScreenError.FeatureUnavailable`, which renders as "not available on this device".
- Reads happen in the foreground only, through `HealthConnectReaderSupport.withLoggingOrThrow`, so the existing rate-limit guard and logging apply and failures are not swallowed.
- Import is user-started work. A file import runs in the ViewModel scope. The Apple Health import pass already runs in a WorkManager foreground worker and inherits that.
- NDJSON is read line by line. A single JSON file is parsed as one tree, so the wizard refuses files over 8 MB with a message that says so. The parsed tree takes about 14 bytes of heap per byte of file, so 8 MB costs about 115 MB, within the app's large heap. That is about 11,000 lab results.
- Nothing on the main thread. The ratchet tests still apply.

### Tests

- Permissions: the medical set is in `managedPermissions` only when the feature is available, and never in the phase, onboarding, requestable, or all sets. `PERMISSION_SET_VERSION` stays 4. The mindfulness leak test in `HealthConnectPermissionServiceTest` is the model.
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

Built on 2026-09-30, in `domain/medical/shc/`.

- **Where a card comes from.** A photo or screenshot of the QR code, a PDF that shows it (the first five pages are rendered with `PdfRenderer`), a `.smart-health-card` file (a JSON list of JWS under `verifiableCredential`), or text with the `shc:/` code. The wizard tells them apart by their first bytes; text that is not a card goes on to the FHIR reader.
- **Reading the code.** ZXing core 3.5.4 (Apache 2.0) reads every QR code in the image, with the hybrid binarizer first and the global one second. Photos are scaled down and PDF pages rendered to about 2,400 pixels on the long side, as one brightness byte per pixel. At 1,600 the phone's PDF renderer left a dense code unreadable (checked on the Pixel). Decoding the code is `shc:/` digit pairs to a JWS, Base64url, raw DEFLATE, then `vc.credentialSubject.fhirBundle`. A card split over several codes (`shc:/<part>/<total>/`) is joined; one with a part missing is listed in the review.
- **Records.** Each issuer (`iss`) becomes a source. The card's `resource:N` references are rewritten like `urn:uuid` ones, and ids come from the content, so the same card imported twice updates.
- **Not verified.** Signature verification needs the issuer's keys from the network, so it is not done. Every record carries an OpenVitals `meta.tag` (`smart-health-card-unverified`); the review shows a notice and the record's detail screen says the signature was not checked. No issuer-key snapshot ships.
- **Camera scanner.** Added on 2026-09-30, by the owner's decision. "Scan a QR code" on the pick step asks for `CAMERA`, then shows a CameraX preview (1.6.2) and reads each frame's brightness plane with the same ZXing reader. `CardScan` collects the parts of a split card. The scanned text goes through the wizard like a picked file. Frames are never stored.
- **Not covered.** SMART Health Links (`shlink:/`, which fetch from the network), and the EU Digital COVID Certificate (`HC1:`).

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

The open-source converters (Amida cda2r4, SRDC cda2fhir, Microsoft FHIR Converter) sit on MDHT, HAPI, or .NET and are not usable on Android, so the mapper is in-house. The HL7 "C-CDA on FHIR" mapping tables are the reference.

Built on 2026-09-30, in `domain/medical/cda/`, on the reader 2c made (a DOM parse with DTDs and external entities off, not `XmlPullParser`: a document is small and its entries nest).

- **Shared and specific.** `CdaDocumentContext` holds what every CDA document needs: ids, the patient, the author, the clinic, the source, and narrative lookups. A `CdaProfile` says what differs: the Estonian one names registries and Estonian time; the generic one does not. `CcdaMapper` reads the sections in the table above; `EstonianCdaMapper` reads the Estonian ones. `CdaImportSource` takes one XML file or a zip and sends each document to its mapper.
- **Problems and allergies.** A concern act wraps the observations. A closed concern, or an end date, makes the record resolved; otherwise it is active. An allergy's substance is the consumable participant. A negated entry ("no known allergies") makes no record.
- **Names from the narrative.** An entry often points at its text with `<reference value="#id"/>`. The mapper looks the text up, so a problem or a dosage with no code still has its name.
- **Times.** C-CDA times usually carry an offset. One that does not keeps only its date, since FHIR refuses a time with no zone and the document's zone is not known.
- **Visits.** `Encounter.class` is required. It is stated only when the document codes the visit in HL7's ActCode; otherwise it is `UNK`.
- **Sources.** The custodian's id names the source (`openvitals://cda/<root>/<extension>`). With no custodian the user names it, and ids come from a hash of the file.
- **Apple's `export_cda.xml`.** Still not checked against a real export. The guard is general: a document whose author is a device or an app, with no person, gives no vital signs, so an app's own measurements do not become medical records.

### 2c. Estonian health portal export

Built on 2026-09-30, in `domain/medical/cda/`. The Estonian portal (terviseportaal.ee, formerly digilugu.ee) hands out one zip: an `xml/` folder with one HL7 CDA R2 document per record, and a `pdf/` folder with a PDF of the same name that renders it. The XML uses the national health information system's profiles (template root `1.3.6.1.4.1.28284.6.1.1`, extension namespace `urn:hl7-EE-DL-Ext:v1`). Document types are coded in `1.3.6.1.4.1.28284.6.2.1.3.x` and sections in `1.3.6.1.4.1.28284.6.2.2.11.x`.

The structure was read from a real export with a local script that printed element paths and structural codes only, so no personal data left the computer. The test fixtures are invented documents in the same shape. A check skipped in CI (`EstonianPortalRealExportCheck`) runs the importer over a local export and prints counts only.

| CDA | FHIR |
|---|---|
| The patient | `Patient/self` per clinic, with name, sex and birth date. The national ID code, the address and the phone are left out. |
| The author and the clinic | Practitioner and Organization, keyed by their public registry codes. |
| Visit (AMBS) and dental visit (DENTDISE) encounters | Encounter, class AMB, or VR for a phone consultation. |
| Diagnoses (RHK-10, the notifiable disease list, pathology) | Condition, `encounter-diagnosis`, confirmed or provisional ("esialgne"). A referral's diagnoses are all provisional and carry the referral's title as a note. |
| Prescriptions (DRUG, "Retsepti andmed") | MedicationRequest, intent `order`, status `unknown`: the document does not say whether the drug is still taken. |
| Lab results (ANA, and panels within panels) | Observation, `laboratory`, LOINC, with value, reference range and flag. A result can sit on the test or on an observation nested in it. |
| Immunisations (IMM) | Immunization with the ATC code, trade name, lot, dose number and target disease. |
| Dental work, radiology (RG_PROC), studies done (PROC), pathology text | Procedure, with the tooth or body site and the finding as a note. |

- Each clinic becomes its own source (`openvitals://ee-tis/<registry code>`). RHK-10 maps to ICD-10, and LOINC, SNOMED CT and ATC to their FHIR URIs. Every other list becomes `urn:oid:` with its Estonian display.
- A time with no zone gets Estonian time for that moment, since FHIR needs a zone on a time.
- Every no-break space becomes a plain space. Estonian price list names use it, and Health Connect refuses it in a FHIR string such as a code's text (found in the owner's first import on the phone, 2026-09-30).
- Ids come from the document's series id and the entry's place, so a re-import updates. The versions of one document are read oldest first, so the newest wins.
- A referral (63.x) gives only its diagnoses section, as provisional Conditions: Health Connect stores no ServiceRequest, and the answer to the referral is imported with its results. Declarations of intent (18.x) and authorisations (9) are not health records and make none, but their PDFs are kept with the others, with no record linked. Narrative sections stay in the PDF.
- Each PDF is offered for keeping as the original of its own document's records, so a record's "Open original document" opens that visit's PDF. PDF text is not read: the XML holds the same document.
- Measured on the owner's export: 177 documents, 3 skipped, 1,415 records in 15 sources (27 of them provisional diagnoses from 19 referrals), none held back by pre-flight.

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

Some providers still hand Apple Health FHIR DSTU2, which Health Connect refuses.

Built on 2026-09-30, as `domain/medical/Dstu2ToR4.kt`. It is a field mapping for the record types Apple exports, not a full converter:

| DSTU2 | R4 |
|---|---|
| Immunization | `date` → `occurrenceDateTime`; `wasNotGiven` → status `not-done`; `reported` → `primarySource`; `vaccinationProtocol` → `protocolApplied` |
| AllergyIntolerance | `substance` → `code`; one `status` → `clinicalStatus` and `verificationStatus`; `category` and `note` as lists |
| Condition | `patient` → `subject`; coded statuses; `dateRecorded` → `recordedDate`; `notes` → `note` |
| MedicationOrder | → MedicationRequest with intent `order`; `dateWritten` → `authoredOn`; dose and rate into `doseAndRate` |
| MedicationStatement | `patient` → `subject`; `wasNotTaken` → status `not-taken`; reasons as lists |
| Observation | `category` as a list in R4's code system, so Health Connect can sort it; `comments` → `note`; `related` → `hasMember` and `derivedFrom` |
| Procedure | `notPerformed` → status `not-done`; `aborted` → `stopped`; reasons as lists |
| Medication, Patient, Practitioner, Organization | The few fields that moved, for contained resources |

- A field R4 has no place for is dropped. The narrative is dropped too, since it described the old shape, and empty values are pruned.
- Every converted record carries an OpenVitals `meta.tag` (`converted-from-dstu2`), and its detail screen says so.
- A converted record joins the provider's R4 source. A type with no mapping is still left out, with the reason in the review.
- Tested on invented records only: no real DSTU2 export was at hand.

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

- `PRIVACY.md` and `docs/app/privacy.md`: a new "Medical Records" section, written in step 1.0 for all of phase 1, so the policy matches the declaration when it is filed. The `PRIVACY.md` date and `CURRENT_PRIVACY_POLICY_VERSION` change together, as `PrivacyPolicyVersionTest` requires, and every user sees one re-prompt. The existing "Health Records" heading in the privacy page means Health Connect fitness records and will become confusing. Rename it to "Health Connect Records".
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

Reuse `DashboardPillWidget` for the tile, `DetailRow`, `OpenVitalsCard`, `OpenVitalsSurface`, `StepBar`, and the shared permission shell. `SettingsListItem` is a pattern in `components-map.md`, not a composable. The nearest one, `SettingsCategoryCard`, takes a `SettingsSection`. Gaps to register in the design system's `components-map.md` in the same change that creates them:

- a list row with a glyph, a title, supporting text, and an optional note, used for categories, records, and declined categories
- a general empty state, since only `ChartEmptyState` exists

The same edit fixes the map's stale `SectionHeader` entry, which says it was never shared, though `ui/components/SectionHeader.kt` exists.

## Phases

### Phase 1: FHIR direct

| Step | Scope | Size |
|---|---|---|
| 1.0 Spike | The medical reader, writer, and fake client, feature gate, all thirteen manifest permissions on main, one diagnostics-only screen that lists a category. The permissions doc, the amended write rule, and the privacy policy for all of phase 1. Confirms the API works on the Pixel and measures page cost, batch cost, and which records the platform's checks refuse. The nightly Play upload blocks from here until the declaration is approved, and that block is when the declaration is filed. | 2 to 3 days |
| 1.1 View, import, and export | One release. Dashboard tile with the first-tap permission request, records home with all twelve categories, category lists, detail with raw JSON. Import wizard on the `MedicalImportSource` seam with id assignment, source grouping, and the patient check. FHIR files and Apple Health clinical records by provider. FHIR bundle export that keeps sources, and share. Delete own records and sources. Built in slices, each reviewed on its own: 1.1a FHIR core in `domain/medical/`, 1.1b view and tile, 1.1c import wizard, 1.1d Apple clinical records, 1.1e export and delete, 1.1f docs and the on-device pass. | 3 to 4 weeks |
| 1.2 Manual entry | Forms for vaccines, allergies, medications, and conditions, with the owner's Patient record. Its own feature. | 1 week |
| 1.3 PDF report section | Medical records in the health report. | 3 to 4 days |
| 1.4 Saved documents | Keep switch in the FHIR wizard and the Apple clinical records import, app-private storage with a Room index, the saved documents list, open, save, and share. | 1 week |
| 1.5 Phone-to-phone sync | Medical records category in the sync wizard, the export entries as a new record type, the receiving pipeline, and the report line. | 1 week |

### Phase 2: document parsers, one feature each

Suggested order, most deterministic first. Each gets its own proposal page before work starts.

| Feature | Depends on |
|---|---|
| 2a SMART Health Cards from image and PDF | Built (2026-09-30), with ZXing. |
| 2b CDA mapper | Built (2026-09-30). Covers C-CDA. Apple's `export_cda.xml` is still not checked against a real export. |
| 2c Estonian portal export | Built (2026-09-30), on the CDA reader it shares with 2b. |
| 2f Apple DSTU2 to R4 | Built (2026-09-30). |
| 2d PDF text: lab reports, then vaccination lists | `PdfTextExtractor`, the analyte dictionary, the review table. |
| 2e OCR for scans and photos | 2d, Tesseract, the language data decision. |
| 2g On-device model | Experiment only, after the rest. |

The camera QR scanner, first planned for later, was added with 2a on 2026-09-30.

[What is left](medical-records-left.md) says what 2d, 2e and 2g wait for, and which built parts still need a check on real data.

## Open Questions

1. **Apple `export_cda.xml`** (feature 2b). What it holds is unverified. Check a real export before the CDA mapper reads it.
2. **Play refusal** (step 1.0 onward). Decided if it happens. A store flavor dimension is the likely path.
3. **PDF text library** (feature 2d). Settled in the 2d proposal.

Everything else is recorded in [Decisions Taken](#decisions-taken). The open work list is [What is left](medical-records-left.md).

## Risks

- **Experimental API.** Every call is opt-in and may change. Keeping all usage in one package bounds the churn.
- **No changelog API.** Views re-read on open. Fine at hundreds of records, unknown at tens of thousands.
- **Validation grows.** The test phone already runs checks the docs call "coming", and more may follow. Pre-flight covers only the basics, so the report must always carry the platform's reason for a rejection.
- **Size limits.** `HealthConnectManager.kt`, `PreferencesRepository.kt`, `ReportPdfWriter.kt`, `SyncRecordCodec.kt`, and `SettingsCards.kt` are near their `FileSizeRatchetTest` ceilings. New medical code goes in files of its own.
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
