**CrossMetricInsightCard** — the correlation insight card (e.g. "Sleep ↔ Readiness"). Trend glyph + relationship on the left, a bold signed correlation % on the right, then a plain-language message and paired-days note.

```jsx
<CrossMetricInsightCard title="Sleep and readiness" direction="positive"
  correlation={62} message="On nights you sleep longer, next-day readiness tends to be higher."
  pairedDays={21} accentColor="var(--ov-metric-sleep)" />
```
