**AchievementBadge** — a Fitbit-inspired achievement card. Unlocked badges tint their surface with the accent; locked ones stay neutral with a lock glyph. Category accents: steps green, distance blue, floors amber, elevation.

```jsx
<AchievementBadge icon="directions_walk" name="High Tops" unlocked
  requirement="Walk 20,000 steps in a day" current="19,576" target="20,000"
  progress={0.98} status="Almost there" accentColor="var(--ov-metric-steps)" />
```
