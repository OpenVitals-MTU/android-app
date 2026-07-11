import { CSSProperties } from 'react';

export interface IconButtonProps {
  /** Material Symbols Outlined glyph name, e.g. "chevron_left", "settings". */
  icon: string;
  /** plain = bare 44px target (top bar); surface = 52px filled circle (date nav). */
  variant?: 'plain' | 'surface';
  size?: number;
  disabled?: boolean;
  /** Accessible label / tooltip. */
  label?: string;
  onClick?: () => void;
  style?: CSSProperties;
}

/** Icon-only button — plain (top bar) or filled surface circle (date navigation). */
export function IconButton(props: IconButtonProps): JSX.Element;
