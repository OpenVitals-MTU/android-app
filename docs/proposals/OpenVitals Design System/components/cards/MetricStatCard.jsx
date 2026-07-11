import React from 'react';

/**
 * MetricStatCard — the small dashboard stat tile (Distance, Calories, Sleep…).
 * Layout from features/dashboard/components/MetricStatCard.kt:
 *  - 28px accent-tinted icon circle (16px glyph)
 *  - title (labelMedium, on-surface-variant) + value (titleMedium, semibold)
 *  - optional 3px accent progress underline pinned to the bottom edge.
 */
export function MetricStatCard({
  title,
  value,
  unit,
  icon,
  accentColor = 'var(--ov-primary)',
  subtitle,
  progress,
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
        position: 'relative',
        overflow: 'hidden',
        background: 'var(--ov-surface-container)',
        borderRadius: 'var(--ov-radius-md)',
        padding: '10px 12px',
        display: 'flex',
        alignItems: 'center',
        gap: 10,
        cursor: interactive ? 'pointer' : 'default',
        boxSizing: 'border-box',
        ...style,
      }}
      {...rest}
    >
      <div
        style={{
          flex: '0 0 auto',
          width: 28,
          height: 28,
          borderRadius: 'var(--ov-radius-full)',
          background: `color-mix(in srgb, ${accentColor} 16%, var(--ov-surface-container-highest))`,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
        }}
      >
        <span className="material-symbols-outlined"
          style={{ fontSize: 16, color: accentColor, fontVariationSettings: "'FILL' 0, 'wght' 500, 'opsz' 20" }}
          aria-hidden="true">{icon}</span>
      </div>
      <div style={{ minWidth: 0, flex: 1 }}>
        <div style={{
          font: 'var(--ov-weight-medium) var(--ov-label-md-size)/var(--ov-label-md-line) var(--ov-font-sans)',
          color: 'var(--ov-on-surface-variant)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis',
        }}>{title}</div>
        <div style={{
          font: 'var(--ov-weight-semibold) var(--ov-title-md-size)/var(--ov-title-md-line) var(--ov-font-sans)',
          color: 'var(--ov-on-surface)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis',
        }}>
          {value}{unit ? <span style={{ fontWeight: 'var(--ov-weight-medium)' }}> {unit}</span> : null}
        </div>
        {subtitle ? (
          <div style={{
            font: 'var(--ov-weight-medium) var(--ov-label-sm-size)/var(--ov-label-sm-line) var(--ov-font-sans)',
            color: 'var(--ov-on-surface-variant)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis',
          }}>{subtitle}</div>
        ) : null}
      </div>
      {progress != null ? (
        <div style={{ position: 'absolute', left: 0, bottom: 0, height: 3, borderRadius: 'var(--ov-radius-xs)',
          width: `${Math.max(0, Math.min(1, progress)) * 100}%`,
          background: `color-mix(in srgb, ${accentColor} 55%, transparent)` }} />
      ) : null}
    </div>
  );
}
