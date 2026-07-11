**Slider** — Material 3 continuous slider for goals/calibration. Pass `valueLabel` for a readout above the track.

```jsx
<Slider value={goal} min={0.5} max={4} step={0.25} onChange={setGoal}
  valueLabel={`${goal.toFixed(2)} L`} accentColor="var(--ov-metric-hydration)" />
```
