**DataConfidenceCard** — the bordered "Data confidence" card on detail screens. Border and level word are tinted by `level` (high = accent, medium = green, low = red).

```jsx
<DataConfidenceCard level="medium" coverage="5 of 7 days tracked (71%)"
  samples="9 records" source="Source: Fitbit"
  valueKind="Estimated values" warnings={["Some buckets have sparse Health Connect data"]}
  accentColor="var(--ov-metric-workout)" />
```
