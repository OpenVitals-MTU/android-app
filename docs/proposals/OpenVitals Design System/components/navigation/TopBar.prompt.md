**TopBar** — the Material 3 top app bar. Use `large` with no `onBack` for the home screen ("OpenVitals" + trailing action icons); use a plain title with `onBack` for detail screens ("Daily Readiness").

```jsx
<TopBar large title="OpenVitals" actions={[
  { icon: 'self_improvement', label: 'Mindfulness' },
  { icon: 'settings', label: 'Settings' },
]} />
<TopBar title="Daily Readiness" onBack={() => {}} />
```
