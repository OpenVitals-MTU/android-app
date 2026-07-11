import { CSSProperties } from 'react';

export interface SummaryRingCardProps {
  title: string;
  value: string | number;
  subtitle?: string;
  /** 0..1 — fraction of the 280° arc filled. */
  progress?: number;
  /** Accent color for the ring fill — usually a --ov-metric-* token. */
  accentColor?: string;
  /** Ring diameter in px. Default 168. */
  size?: number;
  onClick?: () => void;
  style?: CSSProperties;
}

/**
 * Hero summary stat with an open (bottom-gap) progress ring.
 *
 * @startingPoint section="Cards" subtitle="Hero stat with progress ring" viewport="200x200"
 */
export function SummaryRingCard(props: SummaryRingCardProps): JSX.Element;
