import { CSSProperties } from 'react';

export type SelectOption = string | { value: string; label: string };

export interface SelectProps {
  value?: string;
  onChange?: (value: string) => void;
  options: SelectOption[];
  label?: string;
  disabled?: boolean;
  style?: CSSProperties;
}

/** Material 3 outlined dropdown (units, language, theme, favorite activity). */
export function Select(props: SelectProps): JSX.Element;
