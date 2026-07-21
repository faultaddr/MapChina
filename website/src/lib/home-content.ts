import type { SiteLocale } from '@/types';

export type ProductPathKey = 'footprint' | 'discover' | 'shanhe' | 'profile';

export type ProductPath = {
  key: ProductPathKey;
  index: string;
  title: string;
  kicker: string;
  description: string;
};

export type HeroCopy = {
  eyebrow: string;
  title: string;
  subtitle: string;
  beta: string;
  explore: string;
  chips: string[];
  mapLabel: string;
};

const heroCopy: Record<SiteLocale, HeroCopy> = {
  zh: {
    eyebrow: 'YOUR DIGITAL SHANHE · 你的数字山河',
    title: '点亮足迹，\n看见我的山河',
    subtitle: '探索下一站，记录每一次抵达，与旅行者分享中国之美。',
    beta: '参与内测',
    explore: '开始探索',
    chips: ['真实足迹', '山河成长', '双端计划'],
    mapLabel: '由足迹点亮的中国地图轮廓',
  },
  en: {
    eyebrow: 'YOUR DIGITAL SHANHE',
    title: 'Light up each footprint.\nSee your own landscape.',
    subtitle: 'Explore China, record every arrival, and share the places that shape your journey.',
    beta: 'Join the beta',
    explore: 'Start exploring',
    chips: ['Real footprints', 'Personal progress', 'iOS + Android planned'],
    mapLabel: 'An outline of China illuminated by travel footprints',
  },
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

export function getHeroCopy(locale: SiteLocale): HeroCopy {
  return heroCopy[locale];
}
