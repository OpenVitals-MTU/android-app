import { CSSProperties } from 'react';

export interface SettingsListItemProps {
  /** Material Symbols Outlined leading glyph. */
  icon?: string;
  title: string;
  supportingText?: string;
  /** Trailing glyph name; default "chevron_right". */
  trailing?: string;
  onClick?: () => void;
  style?: CSSProperties;
}

/**
 * Settings row card: leading glyph, title + supporting text, trailing chevron.
 *
 * @startingPoint section="Lists" subtitle="Settings row with icon and chevron" viewport="700x88"
 */
export function SettingsListItem(props: SettingsListItemProps): JSX.Element;
