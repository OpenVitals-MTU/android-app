import { ReactNode, CSSProperties } from 'react';

export interface ButtonProps {
  /** Button label. */
  children: ReactNode;
  /** Emphasis level. filled = primary action, tonal = secondary. */
  variant?: 'filled' | 'tonal' | 'outlined' | 'text';
  size?: 'small' | 'medium' | 'large';
  /** Material Symbols Outlined glyph name, e.g. "add", "directions_run". */
  icon?: string;
  iconPosition?: 'leading' | 'trailing';
  disabled?: boolean;
  fullWidth?: boolean;
  onClick?: () => void;
  style?: CSSProperties;
}

/**
 * Material 3 button family for OpenVitals (Filled / Tonal / Outlined / Text).
 *
 * @startingPoint section="Buttons" subtitle="Filled, tonal, outlined & text buttons" viewport="700x200"
 */
export function Button(props: ButtonProps): JSX.Element;
