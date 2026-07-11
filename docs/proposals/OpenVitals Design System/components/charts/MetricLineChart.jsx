import React from 'react';

/**
 * MetricLineChart — a smooth accent line over a soft area fill with a faint
 * baseline/gridline, matching the detail-screen trend charts (Body Energy
 * timeline, heart-rate over time). Renders an SVG with an accent stroke, an
 * accent gradient fill, and optional Y grid lines + labels.
 */
export function MetricLineChart({
  data = [],
  accentColor = 'var(--ov-metric-workout)',
  height = 180,
  min,
  max,
  yTicks = [0, 50, 100],
  showArea = true,
  showDots = false,
  style,
  ...rest
}) {
  const W = 320;
  const H = height;
  const padL = 34, padR = 8, padT = 10, padB = 8;
  const vals = data.length ? data : [0];
  const lo = min != null ? min : Math.min(...vals, ...(yTicks.length ? yTicks : []));
  const hi = max != null ? max : Math.max(...vals, ...(yTicks.length ? yTicks : []));
  const span = hi - lo || 1;
  const gid = React.useMemo(() => 'ovlg' + Math.random().toString(36).slice(2, 8), []);

  const x = (i) => padL + (i / Math.max(1, vals.length - 1)) * (W - padL - padR);
  const y = (v) => padT + (1 - (v - lo) / span) * (H - padT - padB);

  const pts = vals.map((v, i) => [x(i), y(v)]);
  // smooth path (catmull-rom → bezier)
  let d = '';
  if (pts.length) {
    d = `M ${pts[0][0]} ${pts[0][1]}`;
    for (let i = 0; i < pts.length - 1; i++) {
      const p0 = pts[i - 1] || pts[i];
      const p1 = pts[i];
      const p2 = pts[i + 1];
      const p3 = pts[i + 2] || p2;
      const c1x = p1[0] + (p2[0] - p0[0]) / 6;
      const c1y = p1[1] + (p2[1] - p0[1]) / 6;
      const c2x = p2[0] - (p3[0] - p1[0]) / 6;
      const c2y = p2[1] - (p3[1] - p1[1]) / 6;
      d += ` C ${c1x} ${c1y}, ${c2x} ${c2y}, ${p2[0]} ${p2[1]}`;
    }
  }
  const area = d ? `${d} L ${x(vals.length - 1)} ${H - padB} L ${x(0)} ${H - padB} Z` : '';

  return (
    <svg viewBox={`0 0 ${W} ${H}`} width="100%" height={H} preserveAspectRatio="none"
      style={{ display: 'block', ...style }} {...rest}>
      <defs>
        <linearGradient id={gid} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor={accentColor} stopOpacity="0.28" />
          <stop offset="100%" stopColor={accentColor} stopOpacity="0" />
        </linearGradient>
      </defs>
      {yTicks.map((t, i) => (
        <g key={i}>
          <line x1={padL} y1={y(t)} x2={W - padR} y2={y(t)}
            stroke="var(--ov-outline-variant)" strokeWidth="1" opacity="0.5" />
          <text x={padL - 6} y={y(t) + 3} textAnchor="end"
            fontFamily="var(--ov-font-sans)" fontSize="10" fontWeight="500"
            fill="var(--ov-on-surface-variant)">{t}</text>
        </g>
      ))}
      {showArea && area ? <path d={area} fill={`url(#${gid})`} /> : null}
      {d ? <path d={d} fill="none" stroke={accentColor} strokeWidth="2.5"
        strokeLinecap="round" strokeLinejoin="round" /> : null}
      {showDots ? pts.map(([px, py], i) => (
        <circle key={i} cx={px} cy={py} r="3" fill={accentColor} />
      )) : null}
    </svg>
  );
}
