import React from 'react';

/**
 * CrossMetricInsightCard — the correlation insight card (ui/components/
 * CrossMetricInsightCard.kt). A 1px outline-variant bordered card: a trend
 * glyph (up / down / flat) in the accent, a title + relationship line, a bold
 * signed correlation % on the right, then a message and paired-days note.
 */
export function CrossMetricInsightCard({
  title,
  direction = 'flat',        // 'positive' | 'negative' | 'flat'
  correlation = 0,           // percent, e.g. 62 or -41
  relationship,              // e.g. "Positive link"
  message,
  pairedDays,
  accentColor = 'var(--ov-primary)',
  style,
  ...rest
}) {
  const icon = direction === 'positive' ? 'trending_up' : direction === 'negative' ? 'trending_down' : 'trending_flat';
  const rel = relationship || (direction === 'positive' ? 'Positive link' : direction === 'negative' ? 'Negative link' : 'Weak link');
  const pct = Math.round(correlation);
  const signed = pct > 0 ? `+${pct}%` : pct < 0 ? `${pct}%` : '0%';

  return (
    <div style={{
      background: 'var(--ov-surface-container)',
      border: '1px solid color-mix(in srgb, var(--ov-outline-variant) 70%, transparent)',
      borderRadius: 'var(--ov-radius-md)',
      padding: 16,
      ...style,
    }} {...rest}>
      <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: 12 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12, minWidth: 0, flex: 1 }}>
          <span className="material-symbols-outlined" style={{ fontSize: 24, color: accentColor, flex: '0 0 auto' }} aria-hidden="true">{icon}</span>
          <div style={{ minWidth: 0 }}>
            <div style={{ font: 'var(--ov-weight-semibold) var(--ov-title-sm-size)/var(--ov-title-sm-line) var(--ov-font-sans)',
              color: 'var(--ov-on-surface)' }}>{title}</div>
            <div style={{ font: 'var(--ov-weight-regular) var(--ov-body-sm-size)/var(--ov-body-sm-line) var(--ov-font-sans)',
              color: 'var(--ov-on-surface-variant)' }}>{rel}</div>
          </div>
        </div>
        <div style={{ font: 'var(--ov-weight-bold) var(--ov-title-md-size)/var(--ov-title-md-line) var(--ov-font-sans)',
          color: 'var(--ov-on-surface)', fontFeatureSettings: "'tnum'", flex: '0 0 auto' }}>{signed}</div>
      </div>
      {message ? (
        <div style={{ marginTop: 12, font: 'var(--ov-weight-regular) var(--ov-body-md-size)/var(--ov-body-md-line) var(--ov-font-sans)',
          color: 'var(--ov-on-surface-variant)' }}>{message}</div>
      ) : null}
      {pairedDays != null ? (
        <div style={{ marginTop: 8, font: 'var(--ov-weight-medium) var(--ov-label-md-size)/var(--ov-label-md-line) var(--ov-font-sans)',
          color: 'var(--ov-on-surface-variant)' }}>Based on {pairedDays} paired days</div>
      ) : null}
    </div>
  );
}
