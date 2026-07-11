import { CSSProperties } from 'react';

export interface TimeRangeSelectorProps {
  /** Segment labels. Default: Day / Week / Month / Year. */
  options?: string[];
  value?: string;
  onChange?: (value: string) => void;
  style?: CSSProperties;
}

/**
 * Day/Week/Month/Year segmented pill control.
 *
 * @startingPoint section="Navigation" subtitle="Day/Week/Month/Year segmented control" viewport="700x64"
 */
export function TimeRangeSelector(props: TimeRangeSelectorProps): JSX.Element;
