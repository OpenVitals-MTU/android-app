import React from 'react';

/**
 * Checkbox — Material 3 checkbox. Unchecked: 2px outline square. Checked:
 * primary fill with an on-primary check glyph. Optional trailing label.
 */
export function Checkbox({ checked = false, onChange, disabled = false, label, style, ...rest }) {
  const toggle = () => { if (!disabled && onChange) onChange(!checked); };
  const box = (
    <span aria-hidden="true" style={{
      width: 20, height: 20, flex: '0 0 auto', borderRadius: 4, boxSizing: 'border-box',
      display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
      border: checked ? '2px solid var(--ov-primary)' : '2px solid var(--ov-outline)',
      background: checked ? 'var(--ov-primary)' : 'transparent',
      transition: 'background 120ms ease, border-color 120ms ease',
    }}>
      {checked ? <span className="material-symbols-outlined"
        style={{ fontSize: 16, color: 'var(--ov-on-primary)' }}>check</span> : null}
    </span>
  );
  return (
    <label role="checkbox" aria-checked={checked} onClick={toggle}
      style={{ display: 'inline-flex', alignItems: 'center', gap: 12,
        cursor: disabled ? 'not-allowed' : 'pointer', opacity: disabled ? 0.38 : 1, ...style }} {...rest}>
      {box}
      {label ? <span style={{ font: 'var(--ov-weight-regular) var(--ov-body-lg-size)/var(--ov-body-lg-line) var(--ov-font-sans)',
        color: 'var(--ov-on-surface)' }}>{label}</span> : null}
    </label>
  );
}
