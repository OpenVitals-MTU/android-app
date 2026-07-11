import { CSSProperties } from 'react';

export interface MetricStatCardProps {
  title: string;
  value: string | number;
  unit?: string;
  /** Material Symbols Outlined glyph name, e.g. "straighten", "local_fire_department". */
  icon: string;
  /** Accent color — usually a --ov-metric-* token. Tints the icon + progress. */
  accentColor?: string;
  subtitle?: string;
  /** 0..1 — shows a thin accent underline pinned to the bottom edge. */
  progress?: number;
  onClick?: () => void;
  style?: CSSProperties;
}

/**
 * Compact dashboard stat tile with accent icon chip and optional progress underline.
 *
 * @startingPoint section="Cards" subtitle="Compact metric stat tile" viewport="360x88"
 */
export function MetricStatCard(props: MetricStatCardProps): JSX.Element;
