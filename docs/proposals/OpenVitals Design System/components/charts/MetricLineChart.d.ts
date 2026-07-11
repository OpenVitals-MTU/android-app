import { CSSProperties } from 'react';

export interface MetricLineChartProps {
  /** Y-values in order. */
  data: number[];
  /** Accent color — usually a --ov-metric-* token. */
  accentColor?: string;
  height?: number;
  min?: number;
  max?: number;
  /** Y gridline values + labels. Default [0,50,100]. */
  yTicks?: number[];
  showArea?: boolean;
  showDots?: boolean;
  style?: CSSProperties;
}

/**
 * Smooth accent trend line with soft area fill and Y gridlines.
 *
 * @startingPoint section="Charts" subtitle="Trend line with area fill" viewport="700x220"
 */
export function MetricLineChart(props: MetricLineChartProps): JSX.Element;
