**BottomNavBar** — the Material 3 bottom navigation bar. Selected item gets a secondary-container pill behind a filled icon. Controlled via `value`/`onChange`.

```jsx
<BottomNavBar value={tab} onChange={setTab} items={[
  { value: 'summary', label: 'Summary', icon: 'dashboard' },
  { value: 'activity', label: 'Activity', icon: 'directions_run' },
  { value: 'sleep', label: 'Sleep', icon: 'bedtime' },
  { value: 'body', label: 'Body', icon: 'monitor_weight' },
]} />
```
