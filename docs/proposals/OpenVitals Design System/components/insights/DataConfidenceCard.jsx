import React from 'react';

/**
 * DataConfidenceCard — the bordered "Data confidence" card (ui/components/
 * DataConfidenceCard.kt). A 1px border tinted by confidence level (high =
 * accent, medium = tertiary/green, low = error), a Verified icon + title +
 * bold level word, then coverage / samples / source / value-kind lines and
 * up to three "- warning" notes.
 */
export function DataConfidenceCard({
  level = 'high',            // 'high' | 'medium' | 'low'
  coverage,                  // e.g. "7 of 7 days tracked (100%)"
  samples,                   // e.g. "14 records"
  source,                    // e.g. "Source: Fitbit"
  valueKind,                 // e.g. "Measured Health Connect records"
  warnings = [],
  accentColor = 'var(--ov-primary)',
  style,
  ...rest
}) {
  const levelColor = level === 'high' ? accentColor
    : level === 'medium' ? 'var(--ov-tertiary)' : 'var(--ov-error)';
  const levelText = level.charAt(0).toUpperCase() + level.slice(1) + ' confidence';
  const lines = [coverage, samples, source, valueKind].filter(Boolean);

  return (
    <div style={{
      background: 'var(--ov-surface-container)',
      border: `1px solid color-mix(in srgb, ${levelColor} 25%, transparent)`,
      borderRadius: 'var(--ov-radius-md)',
      padding: 16,
      ...style,
    }} {...rest}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
        <span className="material-symbols-outlined" style={{ fontSize: 22, color: levelColor }} aria-hidden="true">verified</span>
        <div>
          <div style={{ font: 'var(--ov-weight-semibold) var(--ov-title-sm-size)/var(--ov-title-sm-line) var(--ov-font-sans)',
            color: 'var(--ov-on-surface)' }}>Data confidence</div>
          <div style={{ font: 'var(--ov-weight-bold) var(--ov-label-lg-size)/var(--ov-label-lg-line) var(--ov-font-sans)',
            letterSpacing: 'var(--ov-label-lg-tracking)', color: levelColor }}>{levelText}</div>
        </div>
      </div>
      <div style={{ marginTop: 12, display: 'flex', flexDirection: 'column', gap: 0 }}>
        {lines.map((l, i) => (
          <div key={i} style={{ font: 'var(--ov-weight-regular) var(--ov-body-md-size)/var(--ov-body-md-line) var(--ov-font-sans)',
            color: 'var(--ov-on-surface-variant)' }}>{l}</div>
        ))}
        {warnings.slice(0, 3).map((w, i) => (
          <div key={i} style={{ marginTop: 6, font: 'var(--ov-weight-regular) var(--ov-body-sm-size)/var(--ov-body-sm-line) var(--ov-font-sans)',
            color: 'var(--ov-on-surface-variant)' }}>- {w}</div>
        ))}
      </div>
    </div>
  );
}
