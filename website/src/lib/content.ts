import type {
  Attraction,
  AttractionViewModel,
  CommunityPost,
  SiteLocale,
  StoryViewModel,
} from '@/types';
import { getEditorialAttractions, getEditorialStories } from './curated-content';

export function normalizeAttraction(item: Attraction, locale: SiteLocale): AttractionViewModel | null {
  if (!item.id?.trim() || !item.name?.trim()) return null;
  const hasCoordinates = Number.isFinite(item.latitude) && Number.isFinite(item.longitude);
  return {
    id: item.id,
    slug: item.id,
    source: 'live',
    name: item.name.trim(),
    region: item.regionId?.trim() || (locale === 'zh' ? '中国' : 'China'),
    level: item.level?.trim() || null,
    description: item.description?.trim() || (locale === 'zh' ? '在地图上继续探索这片山河。' : 'Continue exploring this place on the map.'),
    image: null,
    visitCount: Number.isFinite(item.visitCount) && item.visitCount >= 0 ? item.visitCount : null,
    coordinates: hasCoordinates ? { latitude: item.latitude, longitude: item.longitude } : null,
  };
}

export function normalizeStory(item: CommunityPost, locale: SiteLocale): StoryViewModel | null {
  if (!item.id?.trim() || !item.title?.trim() || !item.content?.trim()) return null;
  const content = item.content.trim();
  const excerpt = content.length > 88 ? `${content.slice(0, 88).trimEnd()}…` : content;
  return {
    id: item.id,
    slug: item.id,
    source: 'live',
    author: item.nickname?.trim() || (locale === 'zh' ? '旅行者' : 'Traveler'),
    title: item.title.trim(),
    excerpt,
    content,
    coverImage: item.coverImage,
    region: item.regionId?.trim() || null,
    likeCount: Number.isFinite(item.likeCount) && item.likeCount >= 0 ? item.likeCount : null,
    commentCount: Number.isFinite(item.commentCount) && item.commentCount >= 0 ? item.commentCount : null,
    createdAt: Number.isFinite(item.createdAt) && item.createdAt > 0 ? item.createdAt : null,
  };
}

export function composeAttractions(
  live: Attraction[] | null | undefined,
  locale: SiteLocale,
  minimum = 6,
): AttractionViewModel[] {
  const normalized = (live ?? [])
    .map((item) => normalizeAttraction(item, locale))
    .filter((item): item is AttractionViewModel => item !== null);
  return fillUnique(normalized, getEditorialAttractions(locale), minimum);
}

export function composeStories(
  live: CommunityPost[] | null | undefined,
  locale: SiteLocale,
  minimum = 4,
): StoryViewModel[] {
  const normalized = (live ?? [])
    .map((item) => normalizeStory(item, locale))
    .filter((item): item is StoryViewModel => item !== null);
  return fillUnique(normalized, getEditorialStories(locale), minimum);
}

export function findEditorialAttraction(slug: string, locale: SiteLocale): AttractionViewModel | null {
  return getEditorialAttractions(locale).find((item) => item.slug === slug) ?? null;
}

export function findEditorialStory(slug: string, locale: SiteLocale): StoryViewModel | null {
  return getEditorialStories(locale).find((item) => item.slug === slug) ?? null;
}

function fillUnique<T extends { id: string }>(live: T[], editorial: T[], minimum: number): T[] {
  const result: T[] = [];
  const ids = new Set<string>();
  for (const item of [...live, ...editorial]) {
    if (!ids.has(item.id)) {
      ids.add(item.id);
      result.push(item);
    }
    if (result.length >= minimum) break;
  }
  return result;
}
