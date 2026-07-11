import React from 'react';

/**
 * MetricBarChart — rounded accent bars with optional X labels, for period
 * summaries (weekly steps, daily calories). Bars use the accent at ~0.85 alpha;
 * a highlighted index renders at full accent. Matches PeriodChart bar styling.
 */
export function MetricBarChart({
  data = [],
  labels = [],
  accentColor = 'var(--ov-metric-steps)',
  height = 160,
  highlightIndex,
  max,
  style,
  ...rest
}) {
  const vals = data.length ? data : [0];
  const hi = max != null ? max : Math.max(...vals, 1);
  const gap = 6;

  return (
    <div style={{ ...style }} {...rest}>
      <div style={{ display: 'flex', alignItems: 'flex-end', gap, height }}>
        {vals.map((v, i) => {
          const h = Math.max(2, (v / hi) * (height - 4));
          const active = highlightIndex == null ? true : i === highlightIndex;
          return (
            <div key={i} title={String(v)} style={{
              flex: 1, height: h, borderRadius: 'var(--ov-radius-xs)',
              background: accentColor, opacity: active ? 0.9 : 0.35,
              transition: 'opacity 120ms ease',
            }} />
          );
        })}
      </div>
      {labels.length ? (
        <div style={{ display: 'flex', gap, marginTop: 6 }}>
          {labels.map((l, i) => (
            <div key={i} style={{ flex: 1, textAlign: 'center',
              font: 'var(--ov-weight-medium) var(--ov-label-sm-size)/var(--ov-label-sm-line) var(--ov-font-sans)',
              color: 'var(--ov-on-surface-variant)' }}>{l}</div>
          ))}
        </div>
      ) : null}
    </div>
  );
}
