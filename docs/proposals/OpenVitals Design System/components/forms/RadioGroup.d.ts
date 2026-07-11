import { CSSProperties } from 'react';

export type RadioOption = string | { value: string; label: string };

export interface RadioGroupProps {
  options: RadioOption[];
  value?: string;
  onChange?: (value: string) => void;
  disabled?: boolean;
  style?: CSSProperties;
}

/** Material 3 radio list, controlled via value/onChange. */
export function RadioGroup(props: RadioGroupProps): JSX.Element;
