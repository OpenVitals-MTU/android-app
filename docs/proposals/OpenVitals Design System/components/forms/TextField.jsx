import React from 'react';

/**
 * TextField — Material 3 outlined text field. Floating-ish label above a
 * rounded outlined input. Used for search ("Search drinks"), custom amounts,
 * and manual-entry values. Controlled via value/onChange.
 */
export function TextField({
  value = '', onChange, label, placeholder, type = 'text', disabled = false,
  leadingIcon, suffix, style, ...rest
}) {
  return (
    <label style={{ display: 'block', ...style }}>
      {label ? (
        <span style={{ display: 'block', marginBottom: 6,
          font: 'var(--ov-weight-medium) var(--ov-label-md-size)/var(--ov-label-md-line) var(--ov-font-sans)',
          color: 'var(--ov-on-surface-variant)' }}>{label}</span>
      ) : null}
      <div style={{ display: 'flex', alignItems: 'center', gap: 10,
        border: '1px solid var(--ov-outline-variant)', borderRadius: 'var(--ov-radius-sm)',
        padding: '0 14px', height: 52, background: 'transparent',
        opacity: disabled ? 0.5 : 1 }}>
        {leadingIcon ? <span className="material-symbols-outlined"
          style={{ fontSize: 20, color: 'var(--ov-on-surface-variant)' }}>{leadingIcon}</span> : null}
        <input type={type} value={value} placeholder={placeholder} disabled={disabled}
          onChange={(e) => onChange && onChange(e.target.value)}
          style={{ flex: 1, minWidth: 0, border: 'none', outline: 'none', background: 'transparent',
            font: 'var(--ov-weight-regular) var(--ov-body-lg-size)/1.4 var(--ov-font-sans)',
            color: 'var(--ov-on-surface)' }}
          {...rest} />
        {suffix ? <span style={{ font: 'var(--ov-weight-medium) var(--ov-body-md-size)/1 var(--ov-font-sans)',
          color: 'var(--ov-on-surface-variant)' }}>{suffix}</span> : null}
      </div>
    </label>
  );
}
