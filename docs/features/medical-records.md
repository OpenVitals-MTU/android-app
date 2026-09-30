# Medical Records

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `features/medical`, `features/imports/medical`, `domain/medical`, `domain/usecase/ImportMedicalRecordsUseCase.kt`, `domain/usecase/ExportMedicalRecordsUseCase.kt`, `domain/usecase/SaveManualMedicalRecordUseCase.kt`, `healthconnect/MedicalRecords*`, `data/repository/MedicalRecordsRepositoryImpl.kt`, `data/repository/MedicalDocumentsRepositoryImpl.kt`, `data/local/medical`.
> **Navigation:** `Screen.MedicalRecords`, `Screen.MedicalRecordCategory`, `Screen.MedicalRecordDetail`, `Screen.MedicalSources`, `Screen.MedicalDocuments`, `Screen.MedicalRecordEntry`, `Screen.SettingsMedicalImport`; widget `MEDICAL_RECORDS`.
> **Related:** [Feature map](feature-map.md), [Apple Health import](apple-health-import.md), [Permissions](../app/permissions.md), [Health Connect](../app/health-connect.md), [Health report export](health-report-export.md), [Privacy](../app/privacy.md), [Proposal](../proposals/medical-records.md).

Medical records shows, imports, adds and exports the medical records Health Connect holds: vaccines, allergies, lab results and more. Health Connect stores them in the FHIR format, the standard health systems exchange records in. Everything happens on the phone.

Records are shown as they were received. OpenVitals does not interpret them.

## Where it is available

Health Connect offers medical records on Android 14 and newer, once its module has been updated to support them. On other phones the tile, the import card and the screens do not appear.

## Opening it

A **Medical records** tile on the dashboard opens the records home. The tile is static: a glyph, the title and "Tap to browse". It reads nothing.

The first time the area opens, OpenVitals asks for all thirteen medical permissions in one Health Connect request: one read permission per category, and write. The request holds only medical permissions, so Health Connect shows its own medical screen, where any of them can be turned off. OpenVitals does not ask again by itself.

## The records home

One row per category, in two blocks:

- **Care:** vaccines, allergies, conditions, medications, lab results, procedures, visits and vital signs.
- **Sensitive:** pregnancy, social history, personal details and practitioner details.

Each row shows how many records the category holds. A category whose read permission is off still lists the records OpenVitals added itself, and says that records from other apps need access.

When permissions are missing, a callout at the top asks for them. Health Connect stops asking for a permission after the user refuses it twice, and closes any request that includes it. The callout leaves those permissions out. When nothing is left to ask, it opens Health Connect's settings instead, where access can still be turned on.

The home also has **Import FHIR file**, **Export all**, a **+** to add a record by hand, and an **Added by OpenVitals** row when OpenVitals has added records.

## A category

A category lists its records newest first, by each type's own date. Each row shows the title, the date, the source and the status, such as "Completed" or "Entered in error". Every record shows, whatever its status.

Places, organisations and drug definitions appear inside the records that name them, not as rows of their own.

## A record

A record shows its main fields, its source, and its raw FHIR, which can be selected and copied. Codes are shown as words, dates in the phone's format, and a code with no words by its code system's short name.

A lab result or vital sign shows its value, its unit and its reference range as text. A High or Low flag appears only when the lab set it. OpenVitals computes nothing from the values.

A reference to another record shows that record's name when it can be read. When its category is declined, the row says that access is needed.

A record OpenVitals added can be deleted, after a confirmation. A record another app added cannot. Its source row offers a button that opens Health Connect's data screens, where it can be deleted.

## Importing

The import wizard is reached from Settings, Import & export, and from the records home. It reads a FHIR file: one resource, a Bundle, or NDJSON with one resource per line. It also reads a CDA document (XML), an Apple Health export zip, taking the clinical records from it, the zip the Estonian health portal hands out, and SMART Health Cards. Files over 8 MB are refused, except card photos and PDFs, up to 32 MB.

1. **Pick.** Write access is asked for first, because matching the sources already in Health Connect needs it.
2. **Review.** Records are grouped by where they came from. Each group shows its counts per category, the types Health Connect does not store, and records that would be refused. Nothing is written yet.
3. **Confirm.** Each group goes to one Health Connect source. A group from a source OpenVitals made before adds to it, so importing the same file again updates the same records. A new source can be renamed. A switch keeps a copy of the file on the phone; it is off on every import. See [Saved documents](#saved-documents).
4. **Import.** Records are written in batches. A refused record is reported with Health Connect's reason, and the rest are written.
5. **Result.** Counts of added, updated, skipped and refused records, and a report that can be copied or saved. The report includes record content, so the screen says to review it before sharing.

Before the review, the wizard compares the person named in the file with the personal details already in Health Connect. A file about someone else stops until the user confirms the records are theirs.

Records from a source another app already holds start left out, so a re-import on the same phone never duplicates them.

The importer also repairs common problems in real files. It gives records without ids a stable id, lifts contained records out of their parent, and rewrites the references between them. The report lists every repair.

### Apple Health clinical records

When an Apple Health export holds clinical records, the Apple Health card in Settings says how many and opens the same wizard on the same file. Each provider gets its own source, and a provider with records in two FHIR versions gets one source per version. Records in FHIR DSTU2, the version older iPhones used, are converted to the version Health Connect stores, and each says so on its detail screen. A record of a type that cannot be converted is left out, and the review says why.

### CDA documents

Many patient portals hand out the record as one XML file in the CDA format (C-CDA), often under "download my record". The wizard reads it, alone or in a zip.

- Problems, allergies, medications, vaccinations, lab results, vital signs, social history, procedures and visits become records. Other sections, and the written text of every section, stay in the file.
- The organisation that holds the document becomes the source. When the file names none, you name the source.
- An entry that records an absence, such as "no known allergies", makes no record.
- The patient record keeps name, sex and birth date. Identifiers, address and phone are left out.
- A time written without a time zone keeps only its date.
- A document written by an app or a device, not by a person, gives no vital signs: those are the app's own measurements.
- The keep switch keeps the XML file.

### SMART Health Cards

The wizard also reads SMART Health Cards, the QR codes on many vaccination and lab result cards. Scan the QR code with the camera, or pick a photo or screenshot of it, a PDF that shows it, a `.smart-health-card` file, or a text file with the code's `shc:/` text.

- Every card in the file is read, including a card split over several QR codes. A card with a part missing is listed in the review and left out.
- Each issuer gets its own source.
- A card is signed, but checking the signature needs the issuer's keys from the internet, and OpenVitals has no internet access. The review says so, every record from a card is tagged as not verified, and its detail screen says so too.
- **Scan a QR code** opens the camera. OpenVitals asks for the camera only then. It reads each picture on the phone and keeps none. A card split over several codes is scanned one code after the other.
- The keep switch keeps the picked file: the photo, the PDF, or the card file. For a scanned card it keeps the code's text.
- A PDF without a card, and a photo or scan of a paper document, are not read yet.

### Estonian health portal

The zip from terviseportaal.ee holds each document twice: as XML and as a PDF. The wizard reads the XML.

- Visits, diagnoses, prescriptions, lab results, vaccinations, dental work, radiology and pathology become records. Each clinic gets its own source.
- A referral gives only its diagnoses, marked provisional, with the referral's title as a note. The referring doctor had not confirmed them, and they may repeat a later diagnosis. Health Connect has no record for the request itself; the answer to it is imported with its results.
- Declarations of intent and authorisations hold no health record, so none is made, and the review lists them. With the keep switch on, their PDFs are kept anyway, with no record linked.
- The patient record keeps name, sex and birth date. The national ID code, the address and the phone are left out.
- The keep switch keeps each document's PDF with that document's records, so "Open original document" on a record opens its visit's PDF. Text that is not in a record, such as the anamnesis, stays in the PDF.
- Importing the same zip again updates the same records.

## Adding a record by hand

The **+** button on the records home adds a vaccine, an allergy, a medication or a condition. The vaccines, allergies, medications and conditions lists have their own **+** for that kind.

- **Vaccine:** the name, completed or not done, the date given, and a lot number.
- **Allergy:** the substance, active, inactive or resolved, when it began, the reaction, and how serious it is.
- **Medication:** the name, active, completed, stopped or on hold, since when, and the dosage.
- **Condition:** the name, active, in remission, resolved or inactive, and when it began.

Every kind also takes a note and, for a user who has one, a code with its code system, such as a CVX code printed on a vaccine card. Names are free text. Nothing is looked up and no code list ships with the app.

Records typed in go to one source, "Entered in OpenVitals". Each one points at the owner's own Patient record, which the first entry writes. Its name and birth date come from a Patient record OpenVitals can read. When there is none, the first entry asks for a name once, and a birth date if the user wants.

A record typed in here can be edited from its detail screen. Fields the form does not show are kept. Each save records its time in the record, so phone-to-phone sync can tell which edit is newer.

## Exporting

**Export all** on the records home, or the export button on a category or a record, builds one FHIR Bundle of every record OpenVitals can read in that scope. A dialog gives the record count and warns that the file holds medical records. It also names categories that hold only OpenVitals' own records, or none, for lack of access. The file then goes to Share or to Save.

The export keeps each record's source, name and FHIR version, so importing it on another phone restores the same sources and ids. Importing it on the same phone updates the records and adds none.

A shared file is kept in the app's cache until the next export replaces it.

## In the health report

The health report builder in Settings has a **Medical records** option. The section lists every allergy, condition and medication with its recorded status, and the vaccines and lab results dated in the report's range, labs with their value and the flag the lab set. It asks for no permission and names what access kept out. See [Health report export](health-report-export.md).

## Saved documents

When the switch on the Confirm step is on, OpenVitals keeps the imported file once the import has written records. The step shows the file's size and the space saved documents already use. For an Apple Health export, only the clinical records files are kept, packed as one zip. A file that gave no records is not kept, except the declarations and authorisations in an Estonian portal export.

Kept files stay in the app's private storage. They are not backed up, and they are deleted when the app is uninstalled. The same file kept twice is one copy. A file can hold more than the records taken from it, such as names, identifiers and notes.

The **Saved documents** row on the records home lists each file with its date, size and record count, and the space they use. A file can be opened in a viewer app on the phone, saved elsewhere, shared, or deleted. A record that came from a kept file has an **Open original document** action. Deleting a file does not delete its records, and deleting records does not delete the file. Saved documents do not travel with phone-to-phone sync.

## Phone-to-phone sync

[Sync with another phone](device-sync.md) offers a **Medical records** category when this phone may write medical records and read at least one category. The sync wizard never asks for medical permissions: this area does, the first time it opens.

- Medical records always sync in full, whatever range is chosen.
- Each record keeps its source. The receiving phone files it into its own source for the same origin and runs the import checks. A source another app keeps there is left alone.
- An unchanged record counts as already present. When the two phones hold different versions of a record, the one with the later edit time wins. A version with no edit time never replaces another.
- Patient records go first. When they name someone other than the people on the receiving phone, or more than one person, that phone keeps none of the medical records from the sync, and the report says why. Export and import them to move them anyway: the import checks the person with you.
- Saved documents stay on their phone.

## Added by OpenVitals

This screen lists the sources OpenVitals added, each with its record count, FHIR version and address. Deleting a source removes it and all its records from Health Connect, after a confirmation that says how many records go with it. OpenVitals can delete only the records and sources it added.

## Privacy

Records stay in Health Connect. OpenVitals keeps no copy of them, and the only value it stores about them is that its first permission request has happened. A file you choose to keep from an import is the one thing OpenVitals stores itself. A file leaves the phone only when the user saves or shares it. See [Privacy](../app/privacy.md).
