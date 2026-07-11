import React from 'react';

/**
 * TimeRangeSelector — the Day / Week / Month / Year segmented control from
 * ui/components/TimeRangeSelector.kt. Each segment is a large-radius (24px)
 * pill; the selected one uses primaryContainer, the rest surfaceContainer.
 */
export function TimeRangeSelector({
  options = ['Day', 'Week', 'Month', 'Year'],
  value,
  onChange,
  style,
  ...rest
}) {
  const selected = value ?? options[0];
  return (
    <div style={{ display: 'flex', gap: 4, ...style }} {...rest}>
      {options.map((opt) => {
        const isSel = opt === selected;
        return (
          <button key={opt} type="button" onClick={() => onChange && onChange(opt)}
            style={{
              flex: 1,
              padding: '12px 8px',
              borderRadius: 'var(--ov-radius-lg)',
              border: 'none',
              cursor: 'pointer',
              background: isSel ? 'var(--ov-primary-container)' : 'var(--ov-surface-container)',
              color: isSel ? 'var(--ov-on-primary-container)' : 'var(--ov-on-surface-variant)',
              font: `${isSel ? 'var(--ov-weight-semibold)' : 'var(--ov-weight-medium)'} var(--ov-label-lg-size)/var(--ov-label-lg-line) var(--ov-font-sans)`,
              letterSpacing: 'var(--ov-label-lg-tracking)',
              transition: 'background 120ms ease',
            }}>
            {opt}
          </button>
        );
      })}
    </div>
  );
}
