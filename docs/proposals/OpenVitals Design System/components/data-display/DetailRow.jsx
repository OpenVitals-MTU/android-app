import React from 'react';

/**
 * DetailRow — a label/value row (DetailRow in DetailCards.kt). Label on the
 * left in on-surface-variant, value right-aligned. The building block of the
 * activity-detail "Metrics" list.
 */
export function DetailRow({ label, value, style, ...rest }) {
  return (
    <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between',
      gap: 16, padding: '2px 0', ...style }} {...rest}>
      <span style={{
        flex: '0 1 42%',
        font: 'var(--ov-weight-regular) var(--ov-body-md-size)/var(--ov-body-md-line) var(--ov-font-sans)',
        color: 'var(--ov-on-surface-variant)',
      }}>{label}</span>
      <span style={{
        flex: '1 1 58%', textAlign: 'right',
        font: 'var(--ov-weight-regular) var(--ov-body-md-size)/var(--ov-body-md-line) var(--ov-font-sans)',
        color: 'var(--ov-on-surface)',
      }}>{value}</span>
    </div>
  );
}
