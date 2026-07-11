import React from 'react';

/**
 * DateNavigator — the detail-screen date header (DayNavigator in source):
 * a big day title + subtitle on the left, three circular surface buttons on
 * the right (previous / next / calendar). Next is disabled when canGoForward
 * is false. Matches the header on Dashboard / Daily Readiness / Body Energy.
 */
export function DateNavigator({
  title = 'Today',
  subtitle,
  canGoForward = false,
  onPrevious,
  onNext,
  onOpenCalendar,
  style,
  ...rest
}) {
  const CircleBtn = ({ icon, label, onClick, disabled }) => (
    <button type="button" aria-label={label} onClick={disabled ? undefined : onClick} disabled={disabled}
      style={{ width: 52, height: 52, borderRadius: 'var(--ov-radius-full)', border: 'none',
        background: 'var(--ov-surface-container)',
        color: disabled ? 'color-mix(in srgb, var(--ov-on-surface) 38%, transparent)' : 'var(--ov-on-surface)',
        opacity: disabled ? 0.55 : 1, cursor: disabled ? 'not-allowed' : 'pointer',
        display: 'inline-flex', alignItems: 'center', justifyContent: 'center' }}>
      <span className="material-symbols-outlined" style={{ fontSize: 24 }} aria-hidden="true">{icon}</span>
    </button>
  );

  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 12, ...style }} {...rest}>
      <div style={{ flex: 1, minWidth: 0, cursor: onOpenCalendar ? 'pointer' : 'default' }}
        onClick={onOpenCalendar}>
        <div style={{
          font: 'var(--ov-weight-semibold) var(--ov-title-lg-size)/var(--ov-title-lg-line) var(--ov-font-sans)',
          color: 'var(--ov-on-surface)',
        }}>{title}</div>
        {subtitle ? (
          <div style={{
            font: 'var(--ov-weight-regular) var(--ov-body-sm-size)/var(--ov-body-sm-line) var(--ov-font-sans)',
            color: 'var(--ov-on-surface-variant)',
          }}>{subtitle}</div>
        ) : null}
      </div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
        <CircleBtn icon="chevron_left" label="Previous" onClick={onPrevious} />
        <CircleBtn icon="chevron_right" label="Next" onClick={onNext} disabled={!canGoForward} />
        <CircleBtn icon="calendar_month" label="Open calendar" onClick={onOpenCalendar} />
      </div>
    </div>
  );
}
