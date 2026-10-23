# FHIR dataset samples

Files from published synthetic datasets, read by `FhirDatasetSamplesTest`. Every file is copied byte for byte; none is a real person's record.

| Folder | Source | Licence | What was taken |
|---|---|---|---|
| `synthea/` | Synthea, the synthetic patient generator, R4 transaction bundle output kept in the repository's ignored `fhir/` folder | Synthea's output is free of cost, privacy and security restrictions | The smallest patient, `Letha284_Haag279_b9a32653-9fde-401f-bb32-9932e680c456.json`, renamed `transaction-bundle.json` |
| `smart-bulk/` | [smart-on-fhir/sample-bulk-fhir-datasets](https://github.com/smart-on-fhir/sample-bulk-fhir-datasets), branch `10-patients`, commit `54fa822` | CC0 | Every line of every type file that names patient `32576ada-5829-2fd7-63ee-2253bdd5da55`, in file order, as one NDJSON file |
| `hl7au/` | [hl7au/au-fhir-test-data](https://github.com/hl7au/au-fhir-test-data), commit `4aee98e`, folder `au-fhir-test-data-set` | CC0 | From `au-core`, every `-missing` and `-suppressed` case, a few records Health Connect cannot classify, and a contained medication. From `au-patient-summary`, three summaries |

To run the importer over the whole datasets, see `FhirCorpusCheck`.
