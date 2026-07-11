import React from 'react';

/**
 * RadioGroup — Material 3 radio list. Each option is a circle (outline ring
 * when unselected, primary ring + filled dot when selected) with a label.
 * Controlled via value/onChange over an options array.
 */
export function RadioGroup({ options = [], value, onChange, disabled = false, style, ...rest }) {
  return (
    <div role="radiogroup" style={{ display: 'flex', flexDirection: 'column', gap: 4, ...style }} {...rest}>
      {options.map((opt) => {
        const val = typeof opt === 'string' ? opt : opt.value;
        const label = typeof opt === 'string' ? opt : opt.label;
        const selected = val === value;
        return (
          <label key={val} role="radio" aria-checked={selected}
            onClick={() => { if (!disabled && onChange) onChange(val); }}
            style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '8px 0',
              cursor: disabled ? 'not-allowed' : 'pointer', opacity: disabled ? 0.38 : 1 }}>
            <span aria-hidden="true" style={{
              width: 20, height: 20, flex: '0 0 auto', borderRadius: 999, boxSizing: 'border-box',
              display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
              border: `2px solid ${selected ? 'var(--ov-primary)' : 'var(--ov-outline)'}`,
              transition: 'border-color 120ms ease',
            }}>
              {selected ? <span style={{ width: 10, height: 10, borderRadius: 999, background: 'var(--ov-primary)' }} /> : null}
            </span>
            <span style={{ font: 'var(--ov-weight-regular) var(--ov-body-lg-size)/var(--ov-body-lg-line) var(--ov-font-sans)',
              color: 'var(--ov-on-surface)' }}>{label}</span>
          </label>
        );
      })}
    </div>
  );
}
