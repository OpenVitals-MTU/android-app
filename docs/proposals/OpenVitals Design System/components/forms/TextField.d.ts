import { CSSProperties, HTMLInputTypeAttribute } from 'react';

export interface TextFieldProps {
  value?: string;
  onChange?: (value: string) => void;
  label?: string;
  placeholder?: string;
  type?: HTMLInputTypeAttribute;
  disabled?: boolean;
  /** Material Symbols Outlined leading glyph (e.g. "search"). */
  leadingIcon?: string;
  /** Trailing unit/suffix text (e.g. "ml"). */
  suffix?: string;
  style?: CSSProperties;
}

/** Material 3 outlined text field. */
export function TextField(props: TextFieldProps): JSX.Element;
