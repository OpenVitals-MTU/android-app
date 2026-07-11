import { CSSProperties } from 'react';

export interface DataConfidenceCardProps {
  level?: 'high' | 'medium' | 'low';
  /** e.g. "7 of 7 days tracked (100%)". */
  coverage?: string;
  /** e.g. "14 records". */
  samples?: string;
  /** e.g. "Source: Fitbit". */
  source?: string;
  /** e.g. "Measured Health Connect records". */
  valueKind?: string;
  /** Up to 3 shown, rendered as "- warning" notes. */
  warnings?: string[];
  accentColor?: string;
  style?: CSSProperties;
}

/**
 * Bordered "Data confidence" card, tinted by confidence level.
 *
 * @startingPoint section="Insights" subtitle="Data-confidence card" viewport="700x220"
 */
export function DataConfidenceCard(props: DataConfidenceCardProps): JSX.Element;
