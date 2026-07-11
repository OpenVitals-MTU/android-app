import React from 'react';

/**
 * Select — Material 3 outlined dropdown. A styled native <select> with a
 * trailing chevron. Used for units, language, theme mode, favorite activity.
 */
export function Select({
  value, onChange, options = [], label, disabled = false, style, ...rest
}) {
  return (
    <label style={{ display: 'block', ...style }}>
      {label ? (
        <span style={{ display: 'block', marginBottom: 6,
          font: 'var(--ov-weight-medium) var(--ov-label-md-size)/var(--ov-label-md-line) var(--ov-font-sans)',
          color: 'var(--ov-on-surface-variant)' }}>{label}</span>
      ) : null}
      <div style={{ position: 'relative', opacity: disabled ? 0.5 : 1 }}>
        <select value={value} disabled={disabled}
          onChange={(e) => onChange && onChange(e.target.value)}
          style={{
            width: '100%', height: 52, appearance: 'none', WebkitAppearance: 'none',
            border: '1px solid var(--ov-outline-variant)', borderRadius: 'var(--ov-radius-sm)',
            padding: '0 44px 0 14px', background: 'transparent',
            font: 'var(--ov-weight-regular) var(--ov-body-lg-size)/1 var(--ov-font-sans)',
            color: 'var(--ov-on-surface)', cursor: disabled ? 'not-allowed' : 'pointer',
          }} {...rest}>
          {options.map((opt) => {
            const val = typeof opt === 'string' ? opt : opt.value;
            const lbl = typeof opt === 'string' ? opt : opt.label;
            return <option key={val} value={val}>{lbl}</option>;
          })}
        </select>
        <span className="material-symbols-outlined" aria-hidden="true"
          style={{ position: 'absolute', right: 14, top: '50%', transform: 'translateY(-50%)',
            fontSize: 24, color: 'var(--ov-on-surface-variant)', pointerEvents: 'none' }}>expand_more</span>
      </div>
    </label>
  );
}
