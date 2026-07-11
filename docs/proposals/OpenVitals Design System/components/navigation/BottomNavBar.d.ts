import { CSSProperties } from 'react';

export interface BottomNavItem {
  value: string;
  label: string;
  /** Material Symbols Outlined glyph name. */
  icon: string;
}

export interface BottomNavBarProps {
  items: BottomNavItem[];
  value?: string;
  onChange?: (value: string) => void;
  style?: CSSProperties;
}

/**
 * Material 3 bottom navigation bar with a selected pill indicator.
 *
 * @startingPoint section="Navigation" subtitle="Bottom navigation bar" viewport="700x96"
 */
export function BottomNavBar(props: BottomNavBarProps): JSX.Element;
