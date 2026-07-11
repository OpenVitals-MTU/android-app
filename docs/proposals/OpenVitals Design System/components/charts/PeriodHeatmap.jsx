import React from 'react';

/**
 * PeriodHeatmap — the month calendar heatmap (ui/charts/PeriodHeatmap.kt ·
 * PeriodMonthHeatmap). A weekday header, a Mon-start grid of square cells
 * shaded by value (accent alpha 0.25→1.0), empty cells before the 1st, day
 * numbers inside, and a Less→More legend. Zero-value days use a faint
 * surface tone.
 */
export function PeriodHeatmap({
  title,
  summary,
  values = [],              // one number per day of the month (index 0 = day 1)
  startWeekday = 0,         // 0 = Monday … 6 = Sunday (leading empty cells)
  accentColor = 'var(--ov-metric-steps)',
  style,
  ...rest
}) {
  const weekdays = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
  const positives = values.filter((v) => v > 0);
  const minPos = positives.length ? Math.min(...positives) : 0;
  const maxVal = Math.max(1, ...values);

  const cellColor = (v) => {
    if (v == null) return 'transparent';
    if (v <= 0) return 'color-mix(in srgb, var(--ov-surface-container-highest) 65%, transparent)';
    const frac = maxVal <= minPos ? 1 : Math.max(0, Math.min(1, (v - minPos) / (maxVal - minPos)));
    return `color-mix(in srgb, ${accentColor} ${Math.round((0.25 + 0.75 * frac) * 100)}%, transparent)`;
  };

  const cells = [];
  for (let i = 0; i < startWeekday; i++) cells.push({ day: null, value: null });
  values.forEach((v, i) => cells.push({ day: i + 1, value: v }));
  while (cells.length % 7 !== 0) cells.push({ day: null, value: null });
  const rows = [];
  for (let i = 0; i < cells.length; i += 7) rows.push(cells.slice(i, i + 7));

  const legend = [0, 1, 2, 3, 4].map((i) => (maxVal <= minPos ? maxVal : minPos + (maxVal - minPos) * i / 4));

  return (
    <div style={{ background: 'var(--ov-surface-container)', borderRadius: 'var(--ov-radius-md)', padding: 16, ...style }} {...rest}>
      {title ? <div style={{ font: 'var(--ov-weight-medium) var(--ov-title-sm-size)/var(--ov-title-sm-line) var(--ov-font-sans)', color: 'var(--ov-on-surface)' }}>{title}</div> : null}
      {summary ? <div style={{ marginTop: 4, font: 'var(--ov-weight-regular) var(--ov-body-sm-size)/var(--ov-body-sm-line) var(--ov-font-sans)', color: 'var(--ov-on-surface-variant)' }}>{summary}</div> : null}

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)', gap: 8, marginTop: 16 }}>
        {weekdays.map((w) => (
          <div key={w} style={{ textAlign: 'center', font: 'var(--ov-weight-medium) var(--ov-label-sm-size)/var(--ov-label-sm-line) var(--ov-font-sans)', color: 'var(--ov-on-surface-variant)' }}>{w}</div>
        ))}
        {rows.flat().map((c, i) => (
          <div key={i} style={{ aspectRatio: '1 / 1', borderRadius: 'var(--ov-radius-sm)',
            background: cellColor(c.value), display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            {c.day != null ? (
              <span style={{ font: 'var(--ov-weight-medium) var(--ov-label-sm-size)/1 var(--ov-font-sans)', color: 'var(--ov-on-surface)' }}>{c.day}</span>
            ) : null}
          </div>
        ))}
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginTop: 12 }}>
        <span style={{ font: 'var(--ov-weight-medium) var(--ov-label-sm-size)/1 var(--ov-font-sans)', color: 'var(--ov-on-surface-variant)' }}>Less</span>
        <div style={{ flex: 1 }} />
        {legend.map((v, i) => (
          <span key={i} style={{ width: 12, height: 12, borderRadius: 999, background: cellColor(Math.max(v, 0.0001)) }} />
        ))}
        <div style={{ flex: 1 }} />
        <span style={{ font: 'var(--ov-weight-medium) var(--ov-label-sm-size)/1 var(--ov-font-sans)', color: 'var(--ov-on-surface-variant)' }}>More</span>
      </div>
    </div>
  );
}
