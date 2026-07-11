**IconButton** — icon-only button; use `variant="plain"` for top-app-bar actions and `variant="surface"` for the circular filled controls in date navigation (chevrons, calendar).

```jsx
<IconButton icon="settings" variant="plain" label="Settings" />
<IconButton icon="chevron_left" variant="surface" label="Previous day" />
```

`plain` is a 44px bare target tinted `on-surface-variant`; `surface` is a 52px filled circle on `surface-container`. Pass a Material Symbols Outlined glyph name to `icon` and always set `label`.
