import { CSSProperties } from 'react';

export interface SensorStatusCardProps {
  /** Lowest sensor battery %, or null when unknown. */
  batteryPercent?: number | null;
  activeCount?: number;
  connectedCount?: number;
  onClick?: () => void;
  style?: CSSProperties;
}

/**
 * Dashboard sensor/battery status row; accent follows battery level.
 *
 * @startingPoint section="Insights" subtitle="Sensor battery status row" viewport="700x72"
 */
export function SensorStatusCard(props: SensorStatusCardProps): JSX.Element;
