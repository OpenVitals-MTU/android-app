import { ReactNode, CSSProperties } from 'react';

export interface ReadinessBannerProps {
  /** Material Symbols Outlined leading glyph. Default "self_improvement". */
  icon?: string;
  title?: string;
  /** Confidence / status line under the title. */
  confidence?: string;
  score?: string | number;
  scoreLabel?: string;
  /** Accent headline (e.g. "Train, but keep it controlled"). */
  headline?: string;
  body?: string;
  /** Accent color; defaults to heart-pink. */
  accentColor?: string;
  /** Sub-tiles rendered below the body (e.g. Body Energy / Training Readiness). */
  children?: ReactNode;
  style?: CSSProperties;
}

/**
 * Hero readiness card for the Daily Readiness screen.
 *
 * @startingPoint section="Cards" subtitle="Daily Readiness hero banner" viewport="700x360"
 */
export function ReadinessBanner(props: ReadinessBannerProps): JSX.Element;
