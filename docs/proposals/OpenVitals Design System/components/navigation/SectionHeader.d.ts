import { ReactNode, CSSProperties } from 'react';

export interface SectionHeaderProps {
  text: string;
  /** A Material Symbols glyph name (e.g. "chevron_right") or a ReactNode. */
  trailing?: string | ReactNode;
  onTrailingClick?: () => void;
  style?: CSSProperties;
}

/** Lightweight list/section label, optionally with a trailing "see all" affordance. */
export function SectionHeader(props: SectionHeaderProps): JSX.Element;
