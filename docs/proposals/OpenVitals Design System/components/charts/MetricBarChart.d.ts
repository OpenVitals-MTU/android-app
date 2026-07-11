import { CSSProperties } from 'react';

export interface MetricBarChartProps {
  data: number[];
  labels?: string[];
  accentColor?: string;
  height?: number;
  /** Emphasize one bar; others dim. */
  highlightIndex?: number;
  max?: number;
  style?: CSSProperties;
}

/**
 * Rounded accent bars for period summaries.
 *
 * @startingPoint section="Charts" subtitle="Period bar chart" viewport="700x220"
 */
export function MetricBarChart(props: MetricBarChartProps): JSX.Element;
