**MetricStatCard** — the compact dashboard stat tile (Distance, Total calories, Elevation…). Accent icon chip on the left, title + value stacked, optional thin accent progress underline.

```jsx
<MetricStatCard title="Distance" value="18.6" unit="km"
  icon="straighten" accentColor="var(--ov-metric-distance)" progress={0.9} />
```

Use a `--ov-metric-*` token for `accentColor`. Pair with `SummaryRingCard` above the fold and grid these two-up below it.
