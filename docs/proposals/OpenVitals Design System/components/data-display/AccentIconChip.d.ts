import { CSSProperties } from 'react';

export interface AccentIconChipProps {
  /** Material Symbols Outlined glyph name. */
  icon: string;
  /** Accent color — usually a --ov-metric-* token. */
  color?: string;
  size?: number;
  iconSize?: number;
  style?: CSSProperties;
}

/** Small round accent-tinted icon badge (14% accent fill, colored glyph). */
export function AccentIconChip(props: AccentIconChipProps): JSX.Element;
