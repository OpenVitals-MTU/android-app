import { CSSProperties } from 'react';

export interface AchievementBadgeProps {
  /** Material Symbols Outlined glyph (directions_walk, straighten, stairs, workspace_premium). */
  icon?: string;
  name: string;
  /** Requirement line, e.g. "Walk 10,000 steps in a day". */
  requirement?: string;
  current?: string;
  target?: string;
  /** 0..1 progress toward the badge. */
  progress?: number;
  unlocked?: boolean;
  /** Status label, e.g. "Achieved 2 Jul" or "Locked". */
  status?: string;
  /** Category accent — steps green, distance blue, floors amber, elevation. */
  accentColor?: string;
  style?: CSSProperties;
}

/**
 * Fitbit-inspired achievement badge card with progress.
 *
 * @startingPoint section="Insights" subtitle="Achievement badge card" viewport="700x160"
 */
export function AchievementBadge(props: AchievementBadgeProps): JSX.Element;
