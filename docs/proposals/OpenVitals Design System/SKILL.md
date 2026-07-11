---
name: openvitals-design
description: Use this skill to generate well-branded interfaces and assets for OpenVitals (a privacy-first Health Connect dashboard, activity tracker, and manual-entry app for Android), either for production or throwaway prototypes/mocks/etc. Contains essential design guidelines, colors, type, fonts, assets, and UI kit components for prototyping.
user-invocable: true
---

Read the README.md file within this skill, and explore the other available files.

If creating visual artifacts (slides, mocks, throwaway prototypes, etc), copy assets out and create static HTML files for the user to view. If working on production code, you can copy assets and read the rules here to become an expert in designing with this brand.

If the user invokes this skill without any other guidance, ask them what they want to build or design, ask some questions, and act as an expert designer who outputs HTML artifacts _or_ production code, depending on the need.

Key things to know about OpenVitals:
- Material 3, restrained health-dashboard style: neutral surfaces, one accent per metric, flat cards (depth by surface tone, not shadow), 16px card corners.
- Roboto type (Material default); Material Symbols Outlined icons; no emoji.
- Two palettes: canonical blue/teal static scheme (`:root`) and a warm Material You sample (`[data-theme="warm"]`) matching the reference screenshots. Metric accents are fixed.
- Copy is calm and factual, sentence case, addresses "you", numbers-first, honest about data confidence.
- Link `styles.css` for tokens. Components live under `components/` (namespace `OpenVitalsDesignSystem_626946`); full screens under `ui_kits/openvitals-app/`.
