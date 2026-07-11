import { CSSProperties } from 'react';

export interface IconProps {
  /** Material Symbols Outlined glyph name, e.g. "favorite", "directions_run". */
  name: string;
  size?: number;
  color?: string;
  /** Weight axis 100..700 (default 500). */
  weight?: number;
  /** FILL axis 0 or 1 (default 0 = outlined). */
  fill?: number;
  style?: CSSProperties;
}

/** Material Symbols Outlined icon — OpenVitals' icon set. */
export function Icon(props: IconProps): JSX.Element;
