import type { SiteLocale } from '@/types';

export const BETA_EMAIL = 'cuferpan@gmail.com';

const betaCopy = {
  zh: {
    subject: '申请参与 MapChina 内测',
    body: '你好，\n\n我想参与 MapChina 内测。\n\n我的常用设备：\n我最期待的功能：\n',
  },
  en: {
    subject: 'MapChina beta access request',
    body: 'Hello,\n\nI would like to join the MapChina beta.\n\nMy primary device:\nThe feature I am most interested in:\n',
  },
} satisfies Record<SiteLocale, { subject: string; body: string }>;

export function buildBetaMailto(locale: SiteLocale): string {
  const copy = betaCopy[locale];
  return `mailto:${BETA_EMAIL}?subject=${encodeURIComponent(copy.subject)}&body=${encodeURIComponent(copy.body)}`;
}
