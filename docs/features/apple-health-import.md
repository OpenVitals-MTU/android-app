# Apple Health Import

OpenVitals can import supported records from Apple Health exports and write them into Health Connect.

## Input Files

The import starts from Settings and accepts supported Apple Health `export.xml` or `export.zip` files. Large exports run as user-started background work so the import can continue after leaving the Settings screen.

## Import Flow

The app scans the export, converts supported record types, requests required Health Connect write permissions, and writes accepted records into Health Connect. Large exports are processed with targeted lookups and time-window chunking to reduce memory pressure. Progress and result counts are shown while the import runs.

Result summaries can include parsed, imported, duplicate, unsupported, skipped, and failed counts.

## Supported Areas

Supported imports cover activity, heart, body, hydration, nutrition, sleep, mindfulness, vitals, and cycle records where Health Connect has compatible record types and write permissions are granted.

Unsupported or incompatible records are skipped with diagnostics rather than forcing partial data into the wrong Health Connect type.

## Data Ownership

Imported records are written to Health Connect. OpenVitals does not upload the export to an OpenVitals server and does not provide a bulk rollback after records are written.
