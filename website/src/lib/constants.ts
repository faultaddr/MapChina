export const Colors = {
  primary: '#0D7377',
  primaryLight: '#14A3A8',
  gold: '#C8963E',
  background: '#F8F6F1',
  surface: '#FFFFFF',
  textPrimary: '#1C1C1E',
  textSecondary: '#4A4A4F',
  dark: '#0F1428',
  borderSubtle: '#E8E5DD',
  error: '#DC3545',
  success: '#1A9E5C',
} as const;

export const BREAKPOINT_MOBILE = 768;

export const REVALIDATE_LIST = parseInt(process.env.REVALIDATE_INTERVAL_LIST ?? '3600');
export const REVALIDATE_DETAIL = parseInt(process.env.REVALIDATE_INTERVAL_DETAIL ?? '1800');
