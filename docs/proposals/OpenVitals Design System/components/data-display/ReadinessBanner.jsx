import React from 'react';

/**
 * ReadinessBanner — the hero card on the Daily Readiness screen. An accent
 * (heart-pink) tinted card: leading chip + title + confidence line, a large
 * score at the top-right, an accent headline, and a body explanation. Children
 * render below the body (e.g. the Body Energy / Training Readiness sub-tiles).
 */
export function ReadinessBanner({
  icon = 'self_improvement',
  title = 'Daily Readiness',
  confidence,
  score,
  scoreLabel = 'Readiness',
  headline,
  body,
  accentColor = 'var(--ov-metric-heart)',
  children,
  style,
  ...rest
}) {
  return (
    <div style={{
      background: `color-mix(in srgb, ${accentColor} 9%, var(--ov-surface-container))`,
      borderRadius: 'var(--ov-radius-md)',
      padding: 20,
      ...style,
    }} {...rest}>
      <div style={{ display: 'flex', alignItems: 'flex-start', gap: 12 }}>
        <div style={{ width: 48, height: 48, borderRadius: 'var(--ov-radius-full)', flex: '0 0 auto',
          background: `color-mix(in srgb, ${accentColor} 16%, transparent)`,
          display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <span className="material-symbols-outlined" style={{ fontSize: 26, color: accentColor }} aria-hidden="true">{icon}</span>
        </div>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ font: 'var(--ov-weight-semibold) var(--ov-title-md-size)/var(--ov-title-md-line) var(--ov-font-sans)',
            color: 'var(--ov-on-surface)' }}>{title}</div>
          {confidence ? (
            <div style={{ font: 'var(--ov-weight-regular) var(--ov-body-sm-size)/var(--ov-body-sm-line) var(--ov-font-sans)',
              color: 'var(--ov-on-surface-variant)' }}>{confidence}</div>
          ) : null}
        </div>
        {score != null ? (
          <div style={{ textAlign: 'right', flex: '0 0 auto' }}>
            <div style={{ font: 'var(--ov-weight-medium) var(--ov-label-sm-size)/var(--ov-label-sm-line) var(--ov-font-sans)',
              color: 'var(--ov-on-surface-variant)' }}>{scoreLabel}</div>
            <div style={{ font: 'var(--ov-weight-bold) var(--ov-headline-md-size)/var(--ov-headline-md-line) var(--ov-font-sans)',
              color: accentColor, fontFeatureSettings: "'tnum'" }}>{score}</div>
          </div>
        ) : null}
      </div>
      {headline ? (
        <div style={{ marginTop: 14,
          font: 'var(--ov-weight-bold) var(--ov-headline-sm-size)/var(--ov-headline-sm-line) var(--ov-font-sans)',
          color: accentColor }}>{headline}</div>
      ) : null}
      {body ? (
        <div style={{ marginTop: 8,
          font: 'var(--ov-weight-regular) var(--ov-body-lg-size)/var(--ov-body-lg-line) var(--ov-font-sans)',
          color: 'var(--ov-on-surface)' }}>{body}</div>
      ) : null}
      {children ? <div style={{ marginTop: 16 }}>{children}</div> : null}
    </div>
  );
}
