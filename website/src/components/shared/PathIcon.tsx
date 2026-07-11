export type PathIconName = 'footprint' | 'discover' | 'shanhe' | 'profile';

export function PathIcon({ name }: { name: PathIconName }) {
  const paths: Record<PathIconName, React.ReactNode> = {
    footprint: <><path d="M7 4c2 1 3 3 3 5S8 13 6 13 3 11 3 9s2-4 4-5Z" /><path d="M16 10c3 1 5 3 5 6s-3 5-5 5-5-2-5-5 2-5 5-6Z" /></>,
    discover: <><circle cx="12" cy="12" r="8" /><path d="m15.5 8.5-2.2 4.8-4.8 2.2 2.2-4.8 4.8-2.2Z" /></>,
    shanhe: <><path d="m3 19 5-9 4 6 3-5 6 8H3Z" /><path d="M5 19c3-2 5-2 8 0s5 2 7 0" /></>,
    profile: <><circle cx="12" cy="8" r="4" /><path d="M4 21c1-5 4-7 8-7s7 2 8 7" /></>,
  };

  return <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{paths[name]}</svg>;
}
