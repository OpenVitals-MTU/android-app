import { CSSProperties } from 'react';

export interface PeriodHeatmapProps {
  title?: string;
  summary?: string;
  /** One value per day of the month (index 0 = day 1). */
  values: number[];
  /** Weekday the 1st falls on: 0 = Monday … 6 = Sunday. */
  startWeekday?: number;
  accentColor?: string;
  style?: CSSProperties;
}

/**
 * Month calendar heatmap shaded by value, with weekday header and legend.
 *
 * @startingPoint section="Charts" subtitle="Month calendar heatmap" viewport="700x420"
 */
export function PeriodHeatmap(props: PeriodHeatmapProps): JSX.Element;
