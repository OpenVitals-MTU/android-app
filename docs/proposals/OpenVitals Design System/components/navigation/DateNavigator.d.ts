import { CSSProperties } from 'react';

export interface DateNavigatorProps {
  title?: string;
  subtitle?: string;
  /** Enables the "next" chevron. */
  canGoForward?: boolean;
  onPrevious?: () => void;
  onNext?: () => void;
  onOpenCalendar?: () => void;
  style?: CSSProperties;
}

/**
 * Detail-screen date header: title/subtitle + prev/next/calendar circle buttons.
 *
 * @startingPoint section="Navigation" subtitle="Date header with prev/next/calendar" viewport="700x110"
 */
export function DateNavigator(props: DateNavigatorProps): JSX.Element;
