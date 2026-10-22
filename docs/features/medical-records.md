# Medical Records

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `features/medical`, `features/imports/medical`, `domain/medical`, `domain/usecase/ImportMedicalRecordsUseCase.kt`, `domain/usecase/ExportMedicalRecordsUseCase.kt`, `domain/usecase/SaveManualMedicalRecordUseCase.kt`, `healthconnect/MedicalRecords*`, `data/repository/MedicalRecordsRepositoryImpl.kt`, `data/repository/MedicalDocumentsRepositoryImpl.kt`, `data/local/medical`.
> **Navigation:** `Screen.MedicalRecords`, `Screen.MedicalRecordCategory`, `Screen.MedicalRecordDetail`, `Screen.MedicalSources`, `Screen.MedicalDocuments`, `Screen.MedicalRecordEntry`, `Screen.SettingsMedicalImport`; widget `MEDICAL_RECORDS`.
> **Related:** [Feature map](feature-map.md), [Apple Health import](apple-health-import.md), [Permissions](../app/permissions.md), [Health Connect](../app/health-connect.md), [Health report export](health-report-export.md), [Privacy](../app/privacy.md), [Proposal](../proposals/medical-records.md), [What is left](../proposals/medical-records-left.md).

Medical records shows, imports, adds and exports the medical records Health Connect holds: vaccines, allergies, lab results and more. Health Connect stores them in the FHIR format, the standard health systems exchange records in. Everything happens on the phone.

Records are shown as they were received. OpenVitals does not interpret them.

## Where it is available

Health Connect offers medical records on Android 14 and newer, once its module has been updated to support them. On other phones the tile, the import card and the screens do not appear.

## Opening it

A **Medical records** tile on the dashboard opens the records home. The tile is static: a glyph, the title and "Tap to browse". It reads nothing.

The first time the records home opens, a notice marked **Beta** says the area is still under development, that bugs may arise, and asks the user to use it with care and review the data they import. It shows once: closing it, with its button or Back, stores that it was seen. A tap beside it does not close it. Someone who used the area before the notice existed sees it once, on their next visit.

Once the notice is closed, the first visit asks for all thirteen medical permissions in one Health Connect request: one read permission per category, and write. The request holds only medical permissions, so Health Connect shows its own medical screen, where any of them can be turned off. OpenVitals does not ask again by itself.

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
2. **Review.** Records are grouped by where they came from. Each group shows its counts per category, the types Health Connect does not store, and records that would be refused. Nothing is written yet. An Observation is counted where Health Connect files it, by the platform's own rules: pregnancy by its LOINC code, then social history and vital signs by LOINC code or category, then lab results by the `laboratory` category. One that matches none would be refused, and is held back.
3. **Confirm.** Each group goes to one Health Connect source. A group from a source OpenVitals made before adds to it, so importing the same file again updates the same records. A new source can be renamed. A switch keeps a copy of the file on the phone; it is off on every import. See [Saved documents](#saved-documents).
4. **Import.** Records are written in batches. A refused record is reported with Health Connect's reason, and the rest are written.
5. **Result.** Counts of added, updated, skipped and refused records, and a report that can be copied or saved. The report includes record content, so the screen says to review it before sharing.

Before the review, the wizard compares the person named in the file with the personal details already in Health Connect. A file about someone else stops until the user confirms the records are theirs.

Records from a source another app already holds start left out, so a re-import on the same phone never duplicates them.

The importer also repairs common problems in real files. It gives records without ids a stable id, lifts contained records out of their parent, and rewrites the references between them. The report lists every repair.

Health Connect stores no diagnostic report, only its results, which are Observation records. A report that holds its results inside itself gives each one as a lab result of its own: it takes the report's patient, date and performer where it names none, and the laboratory category when the report comes from a lab. Most reports point at results kept as records of their own, often in files of their own. A file holding only such reports says so, and the files with the results are the ones to import.

### FHIR DSTU2

Health Connect stores FHIR R4 and R4B. Some providers still hand out DSTU2, the 2015 version: older iPhones exported it, and labs such as LabCorp serve it to the apps that fetch records on a patient's behalf, such as CommonHealth. A DSTU2 record is converted to R4 before it is checked: renamed fields, status codes that moved, single values that R4 holds as lists, such as a lab result's category, and the family names of a person, which DSTU2 holds as a list and R4 as one string. A plain FHIR file states no version, so a record is recognised by its shape, by the fields DSTU2 had and R4 does not; a record that reads the same in both versions needs no conversion. A converted record says so on its detail screen.

### Apple Health clinical records

When an Apple Health export holds clinical records, the Apple Health card in Settings says how many and opens the same wizard on the same file. Each provider gets its own source, and a provider with records in two FHIR versions gets one source per version. The export states each record's FHIR version, so a DSTU2 record is converted as described above; one of a type that cannot be converted is left out, and the review says why.

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

Every kind of record Health Connect holds can be typed in. The **+** button on the records home offers them all. The **+** on a category's list adds that category's kind, or offers a choice where a category holds more than one: a visit, a clinic or a place under visits; a medication or a prescription under medications; a doctor or a doctor's role under practitioner details.

- **Vaccine:** the name, completed or not done, the date given, and a lot number.
- **Allergy:** the substance, active, inactive or resolved, when it began, the reaction, and how serious it is.
- **Medication:** the name, active, completed, intended, stopped, on hold or not taken, since when, and the dosage. It is a FHIR MedicationStatement: what the person takes.
- **Prescription:** the medication, active, on hold, completed, stopped, cancelled or draft, the day it was prescribed, how to take it, and who prescribed it. It is a FHIR MedicationRequest with the intent `order`.
- **Condition:** the name, active, recurrence, relapse, in remission, resolved or inactive, and when it began.
- **Visit:** what it was, its status (finished, planned, arrived, triaged, in progress, on leave or cancelled), the setting (outpatient, emergency, inpatient, acute or non-acute inpatient, observation stay, short stay, pre-admission, home visit, virtual or in the field), the date, the reason, the doctor, the clinic or hospital, and the place. It is a FHIR Encounter.
- **Procedure:** the name, completed, in preparation, in progress, on hold, stopped or not done, the date, the reason, who performed it, the clinic or hospital, and the place.
- **Lab result:** the test, final, registered, preliminary, amended, corrected or cancelled, the date the sample was taken, the result and its unit, whether the report flags it high, low, normal or abnormal, the reference range, and the laboratory. A number, also one with a bound such as `<0.5`, is saved as a quantity; anything else, such as "Negative", as text. The record carries the `laboratory` category, which is how Health Connect files it under lab results.
- **Vital sign:** blood pressure (systolic and diastolic, as one panel), heart rate, breathing rate, oxygen saturation, body temperature, weight, height, body mass index, or head circumference, with the day it was measured. Temperature, weight, height and head circumference take either unit, °C or °F, kg or lb, cm or in, and keep the one typed. These are medical records, apart from the vitals OpenVitals reads from Health Connect's own data types.
- **Social history:** smoking status (daily, occasional, former, never, or unknown), alcoholic drinks per day, or anything else as a topic and free text.
- **Pregnancy:** pregnancy status, the estimated due date (by examination, from the last period, or from ovulation), and the counts of births (total, live, full-term, preterm), living children, miscarriages and abortions, and ectopic pregnancies. A due date may lie ahead.
- **Doctor:** a name, a qualification, and a phone number or email address. It is a FHIR Practitioner, under practitioner details.
- **Doctor's role:** what the doctor is, such as a cardiologist, a specialty, the doctor, the clinic, the place, and a phone number or email address. It is a FHIR PractitionerRole.
- **Clinic or organization:** a name, an address, and a phone number or email address. It is a FHIR Organization, which Health Connect files under visits; the visits list shows it inside the visits that name it.
- **Place:** a name, an address, and a phone number or email address. It is a FHIR Location, also under visits.
- **Personal details:** the owner's own record, written with the first entry, is edited from its detail screen: given and family name, birth date, gender, address, a phone number or email address, and the family doctor.

A vital sign, social history or pregnancy record carries the LOINC code of what it measures, which is how Health Connect files it: the platform recognizes pregnancy, social history and vital signs by their codes. Free social history has no code and is filed by its `social-history` category. A coded answer, such as a smoking status, carries its SNOMED CT code and the words the user picked.

A vaccine, a visit, a procedure, a lab result, a vital sign, a social history and a pregnancy record need their date. Only something still ahead can be dated after today: a planned visit, a procedure in preparation, or a medication the person intends to take.

A doctor, clinic, lab or place is a record of its own, and the records that name it point at it. The form offers the ones already entered as the name is typed; a new name is written as a new record in the same save. The reference also carries the name, so it shows where the record cannot be read.

Every kind but a visit, a doctor, a doctor's role, a clinic, a place and the personal details also takes a note: FHIR gives those no note, and Health Connect refuses a field the type does not have. The clinical kinds take, for a user who has one, a code with its code system, such as a CVX code printed on a vaccine card. Names are free text. Nothing is looked up and no code list ships with the app.

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
