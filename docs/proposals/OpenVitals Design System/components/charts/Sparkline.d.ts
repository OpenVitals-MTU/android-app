import { CSSProperties } from 'react';

export interface SparklineProps {
  data: number[];
  accentColor?: string;
  width?: number;
  height?: number;
  strokeWidth?: number;
  style?: CSSProperties;
}

/** Tiny inline trend line for embedding in cards and rows. */
export function Sparkline(props: SparklineProps): JSX.Element;
