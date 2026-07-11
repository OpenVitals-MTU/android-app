**Card** — the flat tonal surface behind every OpenVitals card. Depth comes from surface-container tone, not shadow. 16px radius by default.

```jsx
<Card padding={16}>…</Card>
<Card variant="accent" accentColor="var(--ov-metric-heart)" padding={16}>…</Card>
```

Variants: `neutral` (surfaceContainer), `metric` (surfaceContainerHighest), `accent` (accent tinted ~9%), `error`. Set `onClick` to make it tappable.
