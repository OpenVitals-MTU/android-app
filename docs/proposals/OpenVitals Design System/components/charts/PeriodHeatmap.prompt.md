**PeriodHeatmap** — the month calendar heatmap for period detail (Month range). Cells shade by value from a faint accent to full; zero-days stay grey. Pass `values` (one per day) and `startWeekday` (0 = Monday).

```jsx
<PeriodHeatmap title="Steps" summary="June · 402,180 total"
  values={[6200,8100,/* …30 days… */14024]} startWeekday={6}
  accentColor="var(--ov-metric-steps)" />
```
