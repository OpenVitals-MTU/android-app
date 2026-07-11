import { CSSProperties } from 'react';

export interface TopBarAction {
  /** Material Symbols Outlined glyph name. */
  icon: string;
  label: string;
  onClick?: () => void;
}

export interface TopBarProps {
  title: string;
  /** Show a back chevron; called on tap. Omit for the home bar. */
  onBack?: () => void;
  /** Trailing action icons. */
  actions?: TopBarAction[];
  /** Large home-style title (headlineLarge) vs detail title (titleLarge). */
  large?: boolean;
  style?: CSSProperties;
}

/** Material 3 top app bar — home (large title) or detail (back + title). */
export function TopBar(props: TopBarProps): JSX.Element;
