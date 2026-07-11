import React from 'react';

/**
 * MetricCard — the larger detail metric surface (icon + title header, big value
 * with unit, optional subtitle and source chip). From ui/components/MetricCard.kt.
 * Used on detail screens and as a full-width dashboard tile.
 */
export function MetricCard({
  title,
  value,
  unit,
  icon,
  accentColor = 'var(--ov-primary)',
  subtitle,
  source,
  onClick,
  style,
  ...rest
}) {
  const interactive = typeof onClick === 'function';
  return (
    <div
      role={interactive ? 'button' : undefined}
      onClick={onClick}
      style={{
        background: 'var(--ov-surface-container)',
        borderRadius: 'var(--ov-radius-md)',
        padding: 16,
        cursor: interactive ? 'pointer' : 'default',
        boxSizing: 'border-box',
        ...style,
      }}
      {...rest}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
        <span className="material-symbols-outlined"
          style={{ fontSize: 20, color: accentColor, fontVariationSettings: "'FILL' 0, 'wght' 500, 'opsz' 20" }}
          aria-hidden="true">{icon}</span>
        <span style={{
          font: 'var(--ov-weight-medium) var(--ov-label-md-size)/var(--ov-label-md-line) var(--ov-font-sans)',
          color: 'var(--ov-on-surface-variant)',
        }}>{title}</span>
      </div>
      <div style={{ display: 'flex', alignItems: 'flex-end', gap: 4, marginTop: 12 }}>
        <span style={{
          font: 'var(--ov-weight-bold) var(--ov-headline-sm-size)/var(--ov-headline-sm-line) var(--ov-font-sans)',
          color: 'var(--ov-on-surface)', fontFeatureSettings: "'tnum'",
        }}>{value}</span>
        {unit ? (
          <span style={{
            font: 'var(--ov-weight-regular) var(--ov-body-sm-size)/var(--ov-body-sm-line) var(--ov-font-sans)',
            color: 'var(--ov-on-surface-variant)', paddingBottom: 3,
          }}>{unit}</span>
        ) : null}
      </div>
      {subtitle ? (
        <div style={{
          font: 'var(--ov-weight-regular) var(--ov-body-sm-size)/var(--ov-body-sm-line) var(--ov-font-sans)',
          color: 'var(--ov-on-surface-variant)', marginTop: 4,
        }}>{subtitle}</div>
      ) : null}
      {source ? (
        <div style={{ display: 'inline-flex', alignItems: 'center', gap: 6, marginTop: 8,
          padding: '3px 8px', borderRadius: 'var(--ov-radius-full)',
          background: 'var(--ov-surface-container-highest)' }}>
          <span style={{ width: 12, height: 12, borderRadius: '50%', background: 'var(--ov-outline)' }} />
          <span style={{ font: 'var(--ov-weight-medium) var(--ov-label-sm-size)/var(--ov-label-sm-line) var(--ov-font-sans)',
            color: 'var(--ov-on-surface-variant)' }}>{source}</span>
        </div>
      ) : null}
    </div>
  );
}
