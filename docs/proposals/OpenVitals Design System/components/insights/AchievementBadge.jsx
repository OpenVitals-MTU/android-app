import React from 'react';

/**
 * AchievementBadge — a Fitbit-inspired achievement card (features/achievements/
 * AchievementsContent.kt · AchievementBadgeCard). A 48px accent icon badge
 * (accent at 20% when unlocked, 10% when locked), name + lock/check glyph,
 * requirement text, an 8px progress bar, and a current/target + status row.
 * Unlocked cards tint their whole surface with the accent at 12%.
 */
export function AchievementBadge({
  icon = 'directions_walk',
  name,
  requirement,
  current,
  target,
  progress = 0,
  unlocked = false,
  status,                 // e.g. "Achieved 2 Jul" / "Locked"
  accentColor = 'var(--ov-metric-steps)',
  style,
  ...rest
}) {
  const frac = Math.max(0, Math.min(1, progress));
  return (
    <div style={{
      background: unlocked ? `color-mix(in srgb, ${accentColor} 12%, var(--ov-surface-container))` : 'var(--ov-surface-container)',
      borderRadius: 'var(--ov-radius-md)',
      padding: 16,
      ...style,
    }} {...rest}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
        <div style={{ width: 48, height: 48, flex: '0 0 auto', borderRadius: 'var(--ov-radius-full)',
          background: `color-mix(in srgb, ${accentColor} ${unlocked ? 20 : 10}%, transparent)`,
          display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <span className="material-symbols-outlined" style={{ fontSize: 26, color: accentColor }} aria-hidden="true">{icon}</span>
        </div>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <span style={{ flex: 1, font: 'var(--ov-weight-semibold) var(--ov-title-md-size)/var(--ov-title-md-line) var(--ov-font-sans)',
              color: 'var(--ov-on-surface)' }}>{name}</span>
            <span className="material-symbols-outlined"
              style={{ fontSize: 20, color: unlocked ? accentColor : 'var(--ov-on-surface-variant)' }}
              aria-hidden="true">{unlocked ? 'check_circle' : 'lock'}</span>
          </div>
          {requirement ? (
            <div style={{ font: 'var(--ov-weight-regular) var(--ov-body-md-size)/var(--ov-body-md-line) var(--ov-font-sans)',
              color: 'var(--ov-on-surface-variant)' }}>{requirement}</div>
          ) : null}
        </div>
      </div>

      <div style={{ marginTop: 12, height: 8, borderRadius: 999, background: 'var(--ov-surface-container-highest)', overflow: 'hidden' }}>
        <div style={{ height: '100%', width: `${frac * 100}%`, background: accentColor }} />
      </div>

      <div style={{ marginTop: 8, display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 8 }}>
        {current != null && target != null ? (
          <span style={{ font: 'var(--ov-weight-regular) var(--ov-body-sm-size)/var(--ov-body-sm-line) var(--ov-font-sans)',
            color: 'var(--ov-on-surface-variant)' }}>{current} / {target}</span>
        ) : <span />}
        {status ? (
          <span style={{ font: 'var(--ov-weight-medium) var(--ov-label-md-size)/var(--ov-label-md-line) var(--ov-font-sans)',
            color: unlocked ? accentColor : 'var(--ov-on-surface-variant)' }}>{status}</span>
        ) : null}
      </div>
    </div>
  );
}
