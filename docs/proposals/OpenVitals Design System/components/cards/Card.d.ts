import { ReactNode, CSSProperties } from 'react';

export interface CardProps {
  children: ReactNode;
  /** neutral = surfaceContainer; metric = surfaceContainerHighest; accent = tinted; error. */
  variant?: 'neutral' | 'metric' | 'accent' | 'error';
  /** Accent color (used by variant="accent"), any CSS color or token var. */
  accentColor?: string;
  radius?: 'xs' | 'sm' | 'md' | 'lg' | 'xl';
  padding?: number | string;
  onClick?: () => void;
  style?: CSSProperties;
}

/**
 * Flat Material 3 card — the base surface for every OpenVitals card.
 *
 * @startingPoint section="Cards" subtitle="Flat tonal card surface" viewport="700x180"
 */
export function Card(props: CardProps): JSX.Element;
