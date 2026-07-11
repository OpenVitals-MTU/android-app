import React from 'react';

/**
 * BottomNavBar — Material 3 navigation bar (OpenVitalsAdaptiveScaffold). Each
 * item is an icon over a label; the selected item gets a secondary-container
 * pill behind its icon and on-secondary-container tint. Controlled via
 * value/onChange over an items array.
 */
export function BottomNavBar({ items = [], value, onChange, style, ...rest }) {
  return (
    <div style={{
      display: 'flex', alignItems: 'stretch',
      height: 'var(--ov-nav-bar-height)',
      background: 'var(--ov-surface-container)',
      ...style,
    }} {...rest}>
      {items.map((it) => {
        const selected = it.value === value;
        return (
          <button key={it.value} type="button" onClick={() => onChange && onChange(it.value)}
            aria-label={it.label} aria-current={selected}
            style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center',
              justifyContent: 'center', gap: 4, border: 'none', background: 'transparent',
              cursor: 'pointer', paddingTop: 12 }}>
            <span style={{ display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
              width: 64, height: 32, borderRadius: 999,
              background: selected ? 'var(--ov-secondary-container)' : 'transparent',
              transition: 'background 120ms ease' }}>
              <span className="material-symbols-outlined"
                style={{ fontSize: 24, color: selected ? 'var(--ov-on-secondary-container)' : 'var(--ov-on-surface-variant)',
                  fontVariationSettings: `'FILL' ${selected ? 1 : 0}, 'wght' 500, 'opsz' 24` }}
                aria-hidden="true">{it.icon}</span>
            </span>
            <span style={{ font: `${selected ? 'var(--ov-weight-semibold)' : 'var(--ov-weight-medium)'} var(--ov-label-md-size)/var(--ov-label-md-line) var(--ov-font-sans)`,
              color: selected ? 'var(--ov-on-surface)' : 'var(--ov-on-surface-variant)' }}>{it.label}</span>
          </button>
        );
      })}
    </div>
  );
}
