**SummaryRingCard** — the hero dashboard stat with an open progress ring (Steps, Weekly cardio). The ring is a 280° arc open at the bottom; `progress` (0..1) fills it in the accent color.

```jsx
<SummaryRingCard title="Steps" value="19,576" subtitle="steps of 8,000"
  progress={1} accentColor="var(--ov-metric-steps)" />
```

Two of these sit side-by-side at the top of the dashboard. Keep the value short; use tabular numerals (built in).
