import React from 'react';

/**
 * Icon — thin wrapper over Material Symbols Outlined (the app's icon set,
 * androidx.compose.material.icons). Use everywhere a glyph is needed.
 */
export function Icon({ name, size = 24, color = 'currentColor', weight = 500, fill = 0, style, ...rest }) {
  return (
    <span
      className="material-symbols-outlined"
      aria-hidden="true"
      style={{
        fontSize: size,
        lineHeight: 1,
        color,
        fontVariationSettings: `'FILL' ${fill}, 'wght' ${weight}, 'opsz' ${Math.min(48, Math.max(20, size))}`,
        ...style,
      }}
      {...rest}
    >
      {name}
    </span>
  );
}
