import React from 'react';

/**
 * SectionHeader — a lightweight list/section label (titleSmall,
 * on-surface-variant) as in ui/components/SectionHeader.kt. Optional trailing
 * affordance (e.g. a chevron "see all" link).
 */
export function SectionHeader({ text, trailing, onTrailingClick, style, ...rest }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      padding: '8px 0', ...style }} {...rest}>
      <span style={{
        font: 'var(--ov-weight-medium) var(--ov-title-sm-size)/var(--ov-title-sm-line) var(--ov-font-sans)',
        color: 'var(--ov-on-surface-variant)',
      }}>{text}</span>
      {trailing ? (
        <button type="button" onClick={onTrailingClick} aria-label={typeof trailing === 'string' ? trailing : 'More'}
          style={{ border: 'none', background: 'transparent', cursor: 'pointer', color: 'var(--ov-on-surface-variant)',
            display: 'inline-flex', alignItems: 'center' }}>
          {typeof trailing === 'string'
            ? <span className="material-symbols-outlined" style={{ fontSize: 22 }} aria-hidden="true">{trailing}</span>
            : trailing}
        </button>
      ) : null}
    </div>
  );
}
