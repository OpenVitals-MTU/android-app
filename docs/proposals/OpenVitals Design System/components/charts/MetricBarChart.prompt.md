**MetricBarChart** — rounded accent bars for period summaries (weekly steps, daily calories). Pass `labels` for an X axis and `highlightIndex` to emphasize one bar.

```jsx
<MetricBarChart data={[6200,8100,7400,9000,12300,5100,14024]}
  labels={['M','T','W','T','F','S','S']} highlightIndex={6}
  accentColor="var(--ov-metric-steps)" />
```
