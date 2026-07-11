import { CSSProperties } from 'react';

export interface MetricCardProps {
  title: string;
  value: string | number;
  unit?: string;
  /** Material Symbols Outlined glyph name. */
  icon: string;
  accentColor?: string;
  subtitle?: string;
  /** Data-source label; renders a small source chip. */
  source?: string;
  onClick?: () => void;
  style?: CSSProperties;
}

/** Larger detail metric surface: icon+title header, big value, optional source chip. */
export function MetricCard(props: MetricCardProps): JSX.Element;
