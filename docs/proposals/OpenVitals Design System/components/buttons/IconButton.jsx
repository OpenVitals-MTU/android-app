import React from 'react';

/**
 * Icon-only button. Two treatments from source:
 *  - "plain"   → bare IconButton (44px touch target), for top-bar actions.
 *  - "surface" → OpenVitalsIconSurfaceButton: 52px filled circle on a
 *    surface-container tone, used for the date-nav chevrons / calendar.
 */
export function IconButton({
  icon,
  variant = 'plain',
  size,
  disabled = false,
  label,
  onClick,
  style,
  ...rest
}) {
  const isSurface = variant === 'surface';
  const dim = size || (isSurface ? 52 : 44);
  const glyphSize = isSurface ? 24 : 22;

  return (
    <button
      type="button"
      aria-label={label}
      title={label}
      onClick={disabled ? undefined : onClick}
      disabled={disabled}
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        justifyContent: 'center',
        width: dim,
        height: dim,
        borderRadius: 'var(--ov-radius-full)',
        border: 'none',
        background: isSurface ? 'var(--ov-surface-container)' : 'transparent',
        color: isSurface ? 'var(--ov-on-surface)' : 'var(--ov-on-surface-variant)',
        cursor: disabled ? 'not-allowed' : 'pointer',
        opacity: disabled ? 0.38 : 1,
        transition: 'filter 120ms ease, background 120ms ease',
        WebkitTapHighlightColor: 'transparent',
        ...style,
      }}
      onMouseDown={(e) => { if (!disabled) e.currentTarget.style.filter = 'brightness(0.93)'; }}
      onMouseUp={(e) => { e.currentTarget.style.filter = 'none'; }}
      onMouseLeave={(e) => { e.currentTarget.style.filter = 'none'; }}
      {...rest}
    >
      <span
        className="material-symbols-outlined"
        style={{ fontSize: glyphSize, lineHeight: 1, fontVariationSettings: "'FILL' 0, 'wght' 500, 'opsz' 24" }}
        aria-hidden="true"
      >
        {icon}
      </span>
    </button>
  );
}
