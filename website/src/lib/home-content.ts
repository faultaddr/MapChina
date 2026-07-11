import type { SiteLocale } from '@/types';

export type ProductPathKey = 'footprint' | 'discover' | 'shanhe' | 'profile';

export type ProductPath = {
  key: ProductPathKey;
  index: string;
  title: string;
  kicker: string;
  description: string;
};

const productPaths: Record<SiteLocale, ProductPath[]> = {
  zh: [
    { key: 'footprint', index: '01', title: '足迹', kicker: '确认每一次抵达', description: '从省市到景点，用深游、小驻、途经记录真实到访，让地图逐步亮起来。' },
    { key: 'discover', index: '02', title: '发现', kicker: '找到值得去的下一站', description: '从待确认足迹、主题路线和旅行灵感中，发现下一块想点亮的版图。' },
    { key: 'shanhe', index: '03', title: '山河', kicker: '看见旅程的沉淀', description: '勋章、图鉴、碑刻和统计，把一次次行走汇成属于你的山河账本。' },
    { key: 'profile', index: '04', title: '我的', kicker: '让记录安全地留下', description: '管理账号、同步、主题与足迹偏好，把旅行记忆掌握在自己手里。' },
  ],
  en: [
    { key: 'footprint', index: '01', title: 'Footprint', kicker: 'Confirm every arrival', description: 'Mark provinces, cities, and places as explored, stayed, or passed through, and watch your map come alive.' },
    { key: 'discover', index: '02', title: 'Discover', kicker: 'Find the next place worth going', description: 'Turn suggested footprints, themed routes, and travel inspiration into the next area on your map.' },
    { key: 'shanhe', index: '03', title: 'Shanhe', kicker: 'See what every journey adds up to', description: 'Badges, collections, carvings, and statistics become a personal ledger of the landscapes you know.' },
    { key: 'profile', index: '04', title: 'Profile', kicker: 'Keep your record safely yours', description: 'Manage sync, themes, and footprint preferences while staying in control of your travel history.' },
  ],
};

export const themePreviews = [
  { key: 'classic', image: null, accent: '#0D7377', dark: false },
  { key: 'ink_wash', image: '/themes/ink-wash.png', accent: '#596565', dark: false },
  { key: 'vintage_map', image: '/themes/vintage-map.png', accent: '#7B552C', dark: false },
  { key: 'rice_paper', image: '/themes/rice-paper.png', accent: '#9B7C42', dark: false },
  { key: 'starry_night', image: '/themes/starry-night.png', accent: '#E0B85D', dark: true },
  { key: 'mountain_mist', image: '/themes/mountain-mist.png', accent: '#425D6A', dark: false },
] as const;

export function getProductPaths(locale: SiteLocale): ProductPath[] {
  return productPaths[locale];
}
