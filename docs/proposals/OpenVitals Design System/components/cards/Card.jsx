import React from 'react';

/**
 * OpenVitalsCard — the base surface for every dashboard/detail card.
 * Flat (0 elevation), 16px (medium) radius, sits on a surface-container tone.
 * Depth comes from tone steps, not shadow. Mirrors OpenVitalsCard in
 * ui/components/DetailCards.kt with its style variants.
 */
export function Card({
  children,
  variant = 'neutral',
  accentColor,
  radius = 'md',
  onClick,
  padding,
  style,
  ...rest
}) {
  const radii = {
    xs: 'var(--ov-radius-xs)', sm: 'var(--ov-radius-sm)', md: 'var(--ov-radius-md)',
    lg: 'var(--ov-radius-lg)', xl: 'var(--ov-radius-xl)',
  };

  let background;
  switch (variant) {
    case 'metric': background = 'var(--ov-surface-container-highest)'; break;
    case 'accent':
      background = accentColor
        ? `color-mix(in srgb, ${accentColor} 9%, var(--ov-surface-container))`
        : 'var(--ov-surface-container)';
      break;
    case 'error': background = 'var(--ov-error-container)'; break;
    default: background = 'var(--ov-surface-container)';
  }

  const interactive = typeof onClick === 'function';

  return (
    <div
      role={interactive ? 'button' : undefined}
      tabIndex={interactive ? 0 : undefined}
      onClick={onClick}
      style={{
        background,
        borderRadius: radii[radius] || radii.md,
        padding: padding != null ? padding : undefined,
        color: variant === 'error' ? 'var(--ov-on-error-container)' : 'var(--ov-on-surface)',
        cursor: interactive ? 'pointer' : 'default',
        transition: 'filter 120ms ease',
        boxSizing: 'border-box',
        ...style,
      }}
      onMouseEnter={interactive ? (e) => { e.currentTarget.style.filter = 'brightness(0.98)'; } : undefined}
      onMouseLeave={interactive ? (e) => { e.currentTarget.style.filter = 'none'; } : undefined}
      {...rest}
    >
      {children}
    </div>
  );
}
