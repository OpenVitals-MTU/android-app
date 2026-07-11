import React from 'react';

/**
 * SensorStatusCard — the dashboard sensor/battery status row (features/
 * dashboard/DashboardSensorStatusCard.kt). A tappable card with a 40px
 * battery-tinted icon circle, a "Sensors" label + lowest-battery headline, and
 * an "active · connected" supporting line. Accent follows battery: >40 primary,
 * <=40 tertiary, <=20 error, unknown primary.
 */
export function SensorStatusCard({
  batteryPercent,            // number | null
  activeCount = 0,
  connectedCount = 0,
  onClick,
  style,
  ...rest
}) {
  const accent = batteryPercent == null ? 'var(--ov-primary)'
    : batteryPercent <= 20 ? 'var(--ov-error)'
    : batteryPercent <= 40 ? 'var(--ov-tertiary)' : 'var(--ov-primary)';
  const headline = batteryPercent == null ? 'Battery level unavailable' : `Lowest battery ${batteryPercent}%`;
  const supporting = activeCount === 0 ? 'All sensors disabled' : `${activeCount} active · ${connectedCount} connected`;
  const interactive = typeof onClick === 'function';

  return (
    <div role={interactive ? 'button' : undefined} onClick={onClick}
      style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 8,
        background: 'var(--ov-surface-container)', borderRadius: 'var(--ov-radius-md)',
        padding: '12px 14px', cursor: interactive ? 'pointer' : 'default', boxSizing: 'border-box', ...style }} {...rest}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, minWidth: 0, flex: 1 }}>
        <div style={{ width: 40, height: 40, flex: '0 0 auto', borderRadius: 'var(--ov-radius-full)',
          background: `color-mix(in srgb, ${accent} 14%, transparent)`,
          display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <span className="material-symbols-outlined" style={{ fontSize: 21, color: accent }} aria-hidden="true">battery_charging_full</span>
        </div>
        <div style={{ minWidth: 0 }}>
          <div style={{ font: 'var(--ov-weight-semibold) var(--ov-label-md-size)/var(--ov-label-md-line) var(--ov-font-sans)',
            color: 'var(--ov-on-surface-variant)' }}>Sensors</div>
          <div style={{ font: 'var(--ov-weight-semibold) var(--ov-title-md-size)/var(--ov-title-md-line) var(--ov-font-sans)',
            color: 'var(--ov-on-surface)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{headline}</div>
        </div>
      </div>
      <div style={{ font: 'var(--ov-weight-semibold) var(--ov-label-md-size)/var(--ov-label-md-line) var(--ov-font-sans)',
        color: 'var(--ov-on-surface-variant)', flex: '0 0 auto', paddingLeft: 8 }}>{supporting}</div>
    </div>
  );
}
