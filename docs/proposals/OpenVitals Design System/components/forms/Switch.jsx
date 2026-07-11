import React from 'react';

/**
 * Switch — Material 3 toggle. Off: outline track + outline thumb. On: primary
 * track + on-primary thumb that grows and slides right. Used for settings
 * toggles (reminders, keep-screen-on, high-contrast mode…).
 */
export function Switch({ checked = false, onChange, disabled = false, label, style, ...rest }) {
  const toggle = () => { if (!disabled && onChange) onChange(!checked); };
  const control = (
    <button type="button" role="switch" aria-checked={checked} aria-label={label}
      onClick={toggle} disabled={disabled}
      style={{
        width: 52, height: 32, borderRadius: 999, flex: '0 0 auto', position: 'relative',
        border: checked ? '2px solid transparent' : '2px solid var(--ov-outline)',
        background: checked ? 'var(--ov-primary)' : 'var(--ov-surface-container-highest)',
        cursor: disabled ? 'not-allowed' : 'pointer', opacity: disabled ? 0.38 : 1,
        transition: 'background 140ms ease', boxSizing: 'border-box', padding: 0,
      }} {...rest}>
      <span style={{
        position: 'absolute', top: '50%', left: checked ? 26 : 6, transform: 'translate(-0%, -50%)',
        width: checked ? 22 : 14, height: checked ? 22 : 14, borderRadius: 999,
        background: checked ? 'var(--ov-on-primary)' : 'var(--ov-outline)',
        transition: 'left 140ms ease, width 140ms ease, height 140ms ease',
      }} />
    </button>
  );
  if (!label) return control;
  return (
    <label style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 16,
      cursor: disabled ? 'not-allowed' : 'pointer', ...style }}>
      <span style={{ font: 'var(--ov-weight-regular) var(--ov-body-lg-size)/var(--ov-body-lg-line) var(--ov-font-sans)',
        color: 'var(--ov-on-surface)' }}>{label}</span>
      {control}
    </label>
  );
}
