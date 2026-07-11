import React from 'react';

/**
 * TopBar — the app's Material 3 top app bar. Two forms:
 *  - Home (large title flush-left, e.g. "OpenVitals") with trailing action icons.
 *  - Detail (back chevron + title, e.g. "Daily Readiness") with optional actions.
 * Transparent over the app background; no divider.
 */
export function TopBar({
  title,
  onBack,
  actions = [],
  large = false,
  style,
  ...rest
}) {
  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        gap: 8,
        minHeight: 'var(--ov-top-bar-height)',
        padding: '8px 8px 8px 16px',
        background: 'transparent',
        ...style,
      }}
      {...rest}
    >
      {onBack ? (
        <button type="button" aria-label="Back" onClick={onBack}
          style={{ width: 44, height: 44, borderRadius: 'var(--ov-radius-full)', border: 'none',
            background: 'transparent', color: 'var(--ov-on-surface)', cursor: 'pointer',
            display: 'inline-flex', alignItems: 'center', justifyContent: 'center', marginRight: 4 }}>
          <span className="material-symbols-outlined" style={{ fontSize: 24 }} aria-hidden="true">arrow_back</span>
        </button>
      ) : null}
      <div style={{
        flex: 1, minWidth: 0,
        font: large
          ? 'var(--ov-weight-bold) var(--ov-headline-lg-size)/var(--ov-headline-lg-line) var(--ov-font-sans)'
          : 'var(--ov-weight-semibold) var(--ov-title-lg-size)/var(--ov-title-lg-line) var(--ov-font-sans)',
        color: 'var(--ov-on-surface)',
        whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis',
      }}>{title}</div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 2 }}>
        {actions.map((a, i) => (
          <button key={i} type="button" aria-label={a.label} title={a.label} onClick={a.onClick}
            style={{ width: 44, height: 44, borderRadius: 'var(--ov-radius-full)', border: 'none',
              background: 'transparent', color: 'var(--ov-on-surface)', cursor: 'pointer',
              display: 'inline-flex', alignItems: 'center', justifyContent: 'center' }}>
            <span className="material-symbols-outlined"
              style={{ fontSize: 24, fontVariationSettings: "'FILL' 0, 'wght' 500, 'opsz' 24" }}
              aria-hidden="true">{a.icon}</span>
          </button>
        ))}
      </div>
    </div>
  );
}
