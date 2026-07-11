import { CSSProperties } from 'react';

export interface SliderProps {
  value?: number;
  min?: number;
  max?: number;
  step?: number;
  onChange?: (value: number) => void;
  disabled?: boolean;
  accentColor?: string;
  /** Optional value readout shown above the track. */
  valueLabel?: string;
  style?: CSSProperties;
}

/**
 * Material 3 continuous slider for goals/calibration.
 *
 * @startingPoint section="Forms" subtitle="Continuous slider" viewport="360x64"
 */
export function Slider(props: SliderProps): JSX.Element;
