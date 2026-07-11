import { CSSProperties } from 'react';

export interface CrossMetricInsightCardProps {
  title: string;
  direction?: 'positive' | 'negative' | 'flat';
  /** Correlation percent, e.g. 62 or -41. */
  correlation?: number;
  /** Relationship line; defaults from direction. */
  relationship?: string;
  message?: string;
  pairedDays?: number;
  accentColor?: string;
  style?: CSSProperties;
}

/**
 * Correlation insight card with trend glyph and signed correlation %.
 *
 * @startingPoint section="Insights" subtitle="Cross-metric correlation card" viewport="700x180"
 */
export function CrossMetricInsightCard(props: CrossMetricInsightCardProps): JSX.Element;
