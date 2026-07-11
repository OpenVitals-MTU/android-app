import React from 'react';

/**
 * SettingsListItem — a tappable settings row on a card surface: leading glyph,
 * title + supporting text, trailing chevron. Matches the Settings screen rows
 * (Display, Activities, Sensors & devices…). Each row is its own card.
 */
export function SettingsListItem({ icon, title, supportingText, onClick, trailing = 'chevron_right', style, ...rest }) {
  return (
    <div
      role="button"
      onClick={onClick}
      style={{
        display: 'flex',
        alignItems: 'center',
        gap: 16,
        background: 'var(--ov-surface-container)',
        borderRadius: 'var(--ov-radius-md)',
        padding: '16px 16px',
        cursor: onClick ? 'pointer' : 'default',
        boxSizing: 'border-box',
        transition: 'filter 120ms ease',
        ...style,
      }}
      onMouseEnter={(e) => { if (onClick) e.currentTarget.style.filter = 'brightness(0.98)'; }}
      onMouseLeave={(e) => { e.currentTarget.style.filter = 'none'; }}
      {...rest}
    >
      {icon ? (
        <span className="material-symbols-outlined"
          style={{ fontSize: 24, color: 'var(--ov-on-surface)', flex: '0 0 auto',
            fontVariationSettings: "'FILL' 0, 'wght' 500, 'opsz' 24" }}
          aria-hidden="true">{icon}</span>
      ) : null}
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{
          font: 'var(--ov-weight-semibold) var(--ov-title-md-size)/var(--ov-title-md-line) var(--ov-font-sans)',
          color: 'var(--ov-on-surface)',
        }}>{title}</div>
        {supportingText ? (
          <div style={{
            font: 'var(--ov-weight-regular) var(--ov-body-md-size)/var(--ov-body-md-line) var(--ov-font-sans)',
            color: 'var(--ov-on-surface-variant)', marginTop: 2,
          }}>{supportingText}</div>
        ) : null}
      </div>
      {trailing ? (
        <span className="material-symbols-outlined"
          style={{ fontSize: 22, color: 'var(--ov-on-surface-variant)', flex: '0 0 auto' }}
          aria-hidden="true">{trailing}</span>
      ) : null}
    </div>
  );
}
