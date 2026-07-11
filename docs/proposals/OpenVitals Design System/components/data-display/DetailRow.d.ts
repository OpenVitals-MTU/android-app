import { ReactNode, CSSProperties } from 'react';

export interface DetailRowProps {
  label: string;
  value: ReactNode;
  style?: CSSProperties;
}

/** Label/value row — the building block of detail-screen metric lists. */
export function DetailRow(props: DetailRowProps): JSX.Element;
