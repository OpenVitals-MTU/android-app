import { CSSProperties } from 'react';

export interface SwitchProps {
  checked?: boolean;
  onChange?: (checked: boolean) => void;
  disabled?: boolean;
  /** Optional label; when set, renders a full-width row (label + switch). */
  label?: string;
  style?: CSSProperties;
}

/**
 * Material 3 toggle switch for settings.
 *
 * @startingPoint section="Forms" subtitle="Toggle switch" viewport="360x48"
 */
export function Switch(props: SwitchProps): JSX.Element;
