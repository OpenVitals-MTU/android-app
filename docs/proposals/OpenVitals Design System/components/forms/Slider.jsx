import React from 'react';

/**
 * Slider — Material 3 continuous slider (accent filled track + thumb). Used for
 * calibration/goal inputs (hydration goal, sleep range, caffeine amount).
 * Controlled via value/onChange; renders the current value if valueLabel given.
 */
export function Slider({
  value = 0, min = 0, max = 100, step = 1, onChange, disabled = false,
  accentColor = 'var(--ov-primary)', valueLabel, style, ...rest
}) {
  const frac = Math.max(0, Math.min(1, (value - min) / (max - min || 1)));
  return (
    <div style={{ ...style }} {...rest}>
      {valueLabel != null ? (
        <div style={{ textAlign: 'right', marginBottom: 6,
          font: 'var(--ov-weight-semibold) var(--ov-title-md-size)/1 var(--ov-font-sans)',
          color: 'var(--ov-on-surface)' }}>{valueLabel}</div>
      ) : null}
      <div style={{ position: 'relative', height: 20, display: 'flex', alignItems: 'center' }}>
        <div style={{ position: 'absolute', left: 0, right: 0, height: 4, borderRadius: 999,
          background: 'var(--ov-surface-container-highest)' }} />
        <div style={{ position: 'absolute', left: 0, width: `${frac * 100}%`, height: 4,
          borderRadius: 999, background: accentColor }} />
        <div style={{ position: 'absolute', left: `calc(${frac * 100}% - 9px)`,
          width: 18, height: 18, borderRadius: 999, background: accentColor,
          boxShadow: '0 1px 3px rgba(0,0,0,.25)' }} />
        <input type="range" value={value} min={min} max={max} step={step} disabled={disabled}
          onChange={(e) => onChange && onChange(Number(e.target.value))}
          style={{ position: 'absolute', left: 0, right: 0, width: '100%', margin: 0, height: 20,
            opacity: 0, cursor: disabled ? 'not-allowed' : 'pointer' }} />
      </div>
    </div>
  );
}
