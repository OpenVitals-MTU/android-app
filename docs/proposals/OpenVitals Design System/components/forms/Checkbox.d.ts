import { CSSProperties } from 'react';

export interface CheckboxProps {
  checked?: boolean;
  onChange?: (checked: boolean) => void;
  disabled?: boolean;
  label?: string;
  style?: CSSProperties;
}

/** Material 3 checkbox with optional label. */
export function Checkbox(props: CheckboxProps): JSX.Element;
