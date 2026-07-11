**MetricCard** — a larger metric surface for detail screens: icon + title header, a big value with unit, optional subtitle and a data-source chip.

```jsx
<MetricCard title="Body Energy" value="83" icon="battery_charging_full"
  accentColor="var(--ov-metric-workout)" subtitle="Estimated by OpenVitals" />
```

Use for standalone metrics on detail screens. For dense dashboard grids prefer `MetricStatCard`.
