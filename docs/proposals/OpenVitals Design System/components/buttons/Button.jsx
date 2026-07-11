import React from 'react';

/**
 * OpenVitals button — Material 3 button family (Filled / Tonal / Outlined / Text).
 * Mirrors OpenVitalsButton in ui/components/DetailCards.kt. Filled = primary
 * action, Tonal = secondary action (e.g. dashboard "Log"), Outlined/Text = low
 * emphasis. Pill-shaped, 40px min height, label + optional leading icon.
 */
export function Button({
  children,
  variant = 'filled',
  size = 'medium',
  icon,
  iconPosition = 'leading',
  disabled = false,
  fullWidth = false,
  onClick,
  style,
  ...rest
}) {
  const heights = { small: 36, medium: 40, large: 48 };
  const height = heights[size] || 40;

  const palettes = {
    filled: {
      background: 'var(--ov-primary)',
      color: 'var(--ov-on-primary)',
      border: '1px solid transparent',
    },
    tonal: {
      background: 'var(--ov-secondary-container)',
      color: 'var(--ov-on-secondary-container)',
      border: '1px solid transparent',
    },
    outlined: {
      background: 'transparent',
      color: 'var(--ov-primary)',
      border: '1px solid var(--ov-outline-variant)',
    },
    text: {
      background: 'transparent',
      color: 'var(--ov-primary)',
      border: '1px solid transparent',
    },
  };
  const palette = palettes[variant] || palettes.filled;

  const glyph = icon ? (
    <span
      className="material-symbols-outlined"
      style={{ fontSize: 18, lineHeight: 1, fontVariationSettings: "'FILL' 0, 'wght' 500, 'opsz' 20" }}
      aria-hidden="true"
    >
      {icon}
    </span>
  ) : null;

  return (
    <button
      type="button"
      onClick={disabled ? undefined : onClick}
      disabled={disabled}
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 8,
        height,
        width: fullWidth ? '100%' : 'auto',
        padding: `0 ${size === 'small' ? 16 : 24}px`,
        borderRadius: 'var(--ov-radius-full)',
        fontFamily: 'var(--ov-font-sans)',
        fontSize: 'var(--ov-label-lg-size)',
        fontWeight: 'var(--ov-weight-medium)',
        letterSpacing: 'var(--ov-label-lg-tracking)',
        cursor: disabled ? 'not-allowed' : 'pointer',
        opacity: disabled ? 0.38 : 1,
        transition: 'filter 120ms ease, opacity 120ms ease',
        WebkitTapHighlightColor: 'transparent',
        ...palette,
        ...style,
      }}
      onMouseDown={(e) => { if (!disabled) e.currentTarget.style.filter = 'brightness(0.94)'; }}
      onMouseUp={(e) => { e.currentTarget.style.filter = 'none'; }}
      onMouseLeave={(e) => { e.currentTarget.style.filter = 'none'; }}
      {...rest}
    >
      {glyph && iconPosition === 'leading' && glyph}
      <span>{children}</span>
      {glyph && iconPosition === 'trailing' && glyph}
    </button>
  );
}
