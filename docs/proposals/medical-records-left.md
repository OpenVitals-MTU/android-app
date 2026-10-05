# Medical Records: What Is Left

> **Status:** Open work list, 2026-09-30. Nothing on this page is done yet.
> **Audience:** Contributors who pick this work up later.
> **Context:** Phase 1 and phase 2 features 2a, 2b, 2c and 2f are on main (pull request #339, 2026-09-30). The rest waits, because the material to test it was not at hand.
> **Related:** [Proposal](medical-records.md), [Feature page](../features/medical-records.md), [Permissions](../app/permissions.md), [Privacy](../app/privacy.md).

This page lists what is left of the medical records work. Each entry says what is missing, what it takes, and where to start. Remove an entry when it is done.

- [Built, not checked on real data](#built-not-checked-on-real-data)
- [Small fixes](#small-fixes)
- [Release steps](#release-steps)
- [Not built](#not-built): features 2d, 2e and 2g
- [Open questions](#open-questions)
- [Rules for picking this up](#rules-for-picking-this-up)

## Built, not checked on real data

These parts are on main and pass their tests. The tests use invented data, so each part still needs one check with the real thing.

| Part | Checked so far | Still to check | What it takes |
|---|---|---|---|
| Camera scanner (2a) | On the test phone: the scanner opens, the camera starts, and Cancel stops it. Unit tests read a code from a camera frame. | A scan of a card. A scan of a card split over several codes. | Any SMART Health Card QR code, on paper or on a screen. The specification publishes [examples](https://spec.smarthealth.cards/examples/). `example-00-g-qr-code-0.svg` is a card in one code. `example-02-g-qr-code-0.svg`, `-1.svg` and `-2.svg` are one card in three codes. |
| A card from a real issuer (2a) | Invented cards, as a photo and as a PDF, on the test phone. | A card an issuer gave out. | A vaccination or lab card with a SMART Health Card QR code. The examples above are the next best thing. |
| Apple DSTU2 records (2f) | Invented records, in unit tests only. No converted record has reached Health Connect. | That Health Connect accepts each converted record. | First, an invented Apple export with DSTU2 records, imported on a phone. Then a real Apple Health export from a provider that still sends DSTU2: in `export.xml` its `ClinicalRecord` entries say `fhirVersion="1.0.2"`. The import report gives Health Connect's reason for each record it refuses. |
| Apple `export_cda.xml` (2b) | Nothing. The Apple import reads the clinical records and leaves this file alone. | What the file holds, and whether any of it is a medical record and not Apple's own measurements. | A real Apple Health export. Print its structure with every value masked first, as was done for the Estonian export. `CcdaMapper` already gives no vital signs for a document written by a device or an app. |
| CDA from a real portal (2b) | One invented C-CDA on the test phone: 16 of 16 records accepted. | A document from a real portal, and an EU patient summary. | A "download my record" XML file. HL7 publishes [sample C-CDA documents](https://github.com/HL7/C-CDA-Examples), which are a step closer than the invented one. |
| Other Estonian document types (2c) | The owner's export: 177 documents, 1,415 records, none refused. | Types that export did not hold, such as a hospital stay. | An export with those types. Sections are read by their own codes, whatever the document type. A section the mapper does not know stays in the PDF. Run the local check below before the phone. |
| Sync of medical records (1.5) | Unit tests with two sessions. One phone only. | A real transfer between two phones. | Two phones with Health Connect medical records: Android 14 with the module update, or newer. |
| Another app's records | Nothing on a phone. | The button on a record's source row that opens Health Connect, where the record can be deleted. | A second app that writes medical records to Health Connect. |

### The local check for an Estonian export

`EstonianPortalRealExportCheck` runs the importer over an unzipped export on the developer's computer. It prints counts only: no names, codes, values or dates. CI skips it.

```
env OPENVITALS_EE_EXPORT=<folder> ./gradlew :app:testCiUnitTest --tests '*EstonianPortalRealExportCheck*'
```

It also prints an id fingerprint. Keep it before a change to the CDA code and compare it after. See [the rules](#rules-for-picking-this-up).

## Small fixes

- **Kept Estonian PDFs show the portal's file name.** That name holds the national ID code, and Saved documents lists it. Name the kept file by the document's title and date instead. The name is set in `CdaImportSource`, where it builds each `MedicalSourceDocument`.
- **The PDF viewer shows a UUID as the title.** `MedicalDocumentActions.kt` hands the stored file to the viewer under its storage name. Hand it over under its display name.
- **The medical screenshot goldens are out of date.** The import button now reads "Import records", the pick step has the scan button, and outlined buttons draw a border since pull request #340. Re-record them on the test phone.

## Release steps

Do these when the feature ships, not before.

- **Google Play declaration.** The thirteen medical permissions are now in the manifest on main. The nightly upload to Play stops until the Health Connect declaration covers them. The proposal holds the [draft justification](medical-records.md#google-play). Review the Data safety form in the same sitting, for the medical data types and the camera.
- **Changelog and landing page.** A changelog entry and the landing page feature cards.
- **If Play refuses a category.** Not decided. A store flavor without the refused permissions is the likely path.

## Not built

Three features are left, and the pieces they share. None has code yet. Each gets its own proposal page before work starts. The proposal describes them in full; this section says what each one waits for.

### Shared pieces, needed first

| Piece | Where it goes | What it is |
|---|---|---|
| Review table | `features/imports/medical/` | One row per proposed record: the source snippet, fields the user can correct, and a keep switch. Every record read from a PDF or a scan is a proposal until the user keeps it. |
| `PdfTextExtractor`, `OcrEngine` | `core/` | Interfaces, so the library behind each can change. |
| Analyte dictionary | `app/src/main/assets/medical/analytes.json` | Lab test names in each language, with their LOINC code and unit. LOINC and UCUM need their licence notices in the app. |
| Fixture corpus | `app/src/test/resources/medical/` | Invented documents with the records expected from each. A parser that gets worse fails the build. |

### 2d. PDF with a text layer

Lab reports first, then vaccination lists. See [2d in the proposal](medical-records.md#2d-pdf-with-a-text-layer).

- **Waits for:** real PDFs to build against. The layout of a lab report differs by portal and by laboratory, so a parser built without samples would be a guess.
- **Decide first:** the text library. The proposal lists three: the platform's page text (Android 15 and newer), Jetpack `androidx.pdf`, and PdfBox-Android. Check whether `androidx.pdf` gives page text with positions.
- **Material at hand:** the owner's Estonian export has 177 PDFs, and all 177 have a text layer (counted on 2026-09-30). Each has an XML file with the same content, which the app already imports. So a text extractor and a lab-row parser can be scored against known answers, on the developer's computer, with counts only. It is one portal's layout, in Estonian, so it proves the pipeline and not the dictionary for other languages.
- **First steps:** the 2d proposal page, the `PdfTextExtractor` interface with one implementation, then the review table.
- **To test:** invented PDFs in the fixture corpus, the local scoring above, and lab reports from at least two laboratories on a phone.

### 2e. Scanned PDF or photo

OCR first, then the same parsing as 2d. See [2e in the proposal](medical-records.md#2e-scanned-pdf-or-photo).

- **Waits for:** 2d, and scans or photos of printed reports.
- **Decide first:** how Tesseract's language data arrives. It is 5 to 15 MB per language: bundled, or imported as a file like a map pack. An ML Kit build for Play only is a policy decision, taken only if Tesseract is too weak on real reports.
- **To test:** the same reports as 2d, printed and photographed, in good and poor light. Count wrong numbers and units, not only missed rows.

### 2g. On-device language model

An experiment, after 2d and 2e. See [2g in the proposal](medical-records.md#2g-on-device-language-models).

- **Waits for:** the review table, and a phone that runs the model: one with AICore for Gemini Nano, or a Gemma weights file of 0.8 to 1.5 GB imported by hand.
- **The risk:** a model invents numbers and units. Its output is a suggestion beside the source text, never written without a keep.
- **To test:** the 2d and 2e corpus, scored the same way.

### Decided against, for now

These are not open. The proposal records why.

- **Card signatures are not checked.** That needs the issuer's keys from the internet, and the app has no internet permission. Each record from a card says so.
- **SMART Health Links (`shlink:/`)** fetch from the network.
- **The EU Digital COVID Certificate (`HC1:`)** is another format.

## Open questions

1. **Apple `export_cda.xml`.** What it holds. See the table above.
2. **Play refusal.** What ships if Play refuses a category.
3. **PDF text library.** Settled in the 2d proposal.
4. **Tesseract language data.** Bundled or imported. Settled in the 2e proposal.

## Rules for picking this up

- **Real documents never enter the repository.** Not as fixtures, not in a test, not in a commit message. Read a real file's structure with its values masked, and write an invented fixture in the same shape.
- **Estonian record ids must not change.** People have already imported their export, and a new id would duplicate every record on the next import. Run the local check before and after any change to `domain/medical/cda/` and compare the id fingerprint.
- **A parsed record is a proposal.** Nothing read from a PDF, a scan or a model is written until the user has seen it beside its source and kept it.
- **No new network use.** The app has no internet permission, and medical records do not change that.
- **One feature, one proposal page.** Then the feature page, the privacy and permission pages, and the strings in every language.
