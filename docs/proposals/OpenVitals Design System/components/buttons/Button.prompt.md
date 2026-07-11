**Button** — the Material 3 action button used across OpenVitals; use `filled` for the single primary action on a screen, `tonal` for a secondary action beside it, and `outlined`/`text` for low-emphasis actions.

```jsx
<Button variant="filled" icon="directions_run">Start</Button>
<Button variant="tonal" icon="add">Log</Button>
<Button variant="outlined">Manage data sources</Button>
<Button variant="text">Cancel</Button>
```

Variants: `filled` (primary, brand color), `tonal` (secondary-container), `outlined`, `text`. Sizes: `small` (36) · `medium` (40) · `large` (48, used for dashboard quick actions). Pass `icon` (Material Symbols Outlined name) for a leading glyph; `fullWidth` to fill its container.
