import React from 'react';

/**
 * DashboardSummaryCard — the large hero stat with an open progress ring
 * (Steps, Weekly cardio). From features/dashboard/components/DashboardSummaryCard.kt:
 * a 280°-sweep arc starting at 130° (open at the bottom), round caps; the fill
 * is the accent at ~65% alpha over an outline-variant track. Title / value /
 * subtitle are stacked in the centre.
 */
function polar(cx, cy, r, deg) {
  const rad = (deg * Math.PI) / 180;
  return { x: cx + r * Math.cos(rad), y: cy + r * Math.sin(rad) };
}
function arcPath(cx, cy, r, startDeg, sweepDeg) {
  const end = startDeg + sweepDeg;
  const s = polar(cx, cy, r, startDeg);
  const e = polar(cx, cy, r, end);
  const largeArc = sweepDeg > 180 ? 1 : 0;
  return `M ${s.x} ${s.y} A ${r} ${r} 0 ${largeArc} 1 ${e.x} ${e.y}`;
}

export function SummaryRingCard({
  title,
  value,
  subtitle,
  progress = 0,
  accentColor = 'var(--ov-metric-steps)',
  size = 168,
  onClick,
  style,
  ...rest
}) {
  const START = 130;
  const SWEEP = 280;
  const stroke = Math.max(5, Math.min(10, size * 0.09));
  const r = (size - stroke) / 2 - 2;
  const cx = size / 2;
  const cy = size / 2;
  const frac = Math.max(0, Math.min(1, progress));
  const interactive = typeof onClick === 'function';

  return (
    <div
      role={interactive ? 'button' : undefined}
      onClick={onClick}
      style={{
        background: 'var(--ov-surface-container)',
        borderRadius: 'var(--ov-radius-md)',
        padding: 6,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        cursor: interactive ? 'pointer' : 'default',
        boxSizing: 'border-box',
        ...style,
      }}
      {...rest}
    >
      <div style={{ position: 'relative', width: size, height: size }}>
        <svg width={size} height={size} style={{ display: 'block' }}>
          <path d={arcPath(cx, cy, r, START, SWEEP)} fill="none"
            stroke="var(--ov-outline-variant)" strokeWidth={stroke} strokeLinecap="round" />
          <path d={arcPath(cx, cy, r, START, SWEEP * frac)} fill="none"
            stroke={accentColor} strokeWidth={stroke} strokeLinecap="round"
            style={{ opacity: 0.72 }} />
        </svg>
        <div style={{
          position: 'absolute', inset: 0, display: 'flex', flexDirection: 'column',
          alignItems: 'center', justifyContent: 'center', textAlign: 'center', padding: stroke + 6,
        }}>
          <div style={{
            font: 'var(--ov-weight-medium) var(--ov-label-sm-size)/var(--ov-label-sm-line) var(--ov-font-sans)',
            color: 'var(--ov-on-surface-variant)',
          }}>{title}</div>
          <div style={{
            font: 'var(--ov-weight-bold) var(--ov-headline-sm-size)/var(--ov-headline-sm-line) var(--ov-font-sans)',
            color: 'var(--ov-on-surface)', fontFeatureSettings: "'tnum'",
          }}>{value}</div>
          {subtitle ? (
            <div style={{
              font: 'var(--ov-weight-medium) var(--ov-label-sm-size)/var(--ov-label-sm-line) var(--ov-font-sans)',
              color: 'var(--ov-on-surface-variant)',
            }}>{subtitle}</div>
          ) : null}
        </div>
      </div>
    </div>
  );
}
