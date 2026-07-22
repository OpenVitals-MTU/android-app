# Reference

This folder contains supporting material that informs product and design decisions but is not, by itself, OpenVitals implementation policy.

- [Design reference](design/README.md): OpenVitals UI notes and external Health Connect/Open Health Stack design material.
- [Garmin FIT files](garmin-fit-files.md): FIT file types (incl. Garmin-proprietary wellness types), verified against a real Connect export, and how each maps to Health Connect — reference for extending the FIT importer beyond activities.
- [Garmin settings service](garmin-settings-service.md): the protobuf settings protocol over GFDI, the settings tree as measured on a vívoactive 5, and the transport traps met reading it — reference for [Garmin watch sync](../features/garmin-watch-sync.md).

For implementation rules, use the [Engineering](../engineering/README.md) docs.
