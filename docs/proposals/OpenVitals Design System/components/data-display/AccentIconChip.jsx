import React from 'react';

/**
 * AccentIconChip — a small round accent-tinted icon badge (AccentIconChip in
 * DetailCards.kt). Circular, accent at 14% alpha, colored glyph. Used as the
 * leading marker in insight rows, readiness banner, drink rows, etc.
 */
export function AccentIconChip({ icon, color = 'var(--ov-primary)', size = 40, iconSize, style, ...rest }) {
  const glyph = iconSize || Math.round(size * 0.5);
  return (
    <div
      style={{
        width: size,
        height: size,
        borderRadius: 'var(--ov-radius-full)',
        background: `color-mix(in srgb, ${color} 14%, transparent)`,
        display: 'inline-flex',
        alignItems: 'center',
        justifyContent: 'center',
        flex: '0 0 auto',
        ...style,
      }}
      {...rest}
    >
      <span className="material-symbols-outlined"
        style={{ fontSize: glyph, color, fontVariationSettings: "'FILL' 0, 'wght' 500, 'opsz' 24" }}
        aria-hidden="true">{icon}</span>
    </div>
  );
}
