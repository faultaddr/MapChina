export interface PaginatedResponse<T> {
  data: T[];
  total: number;
  page: number;
  limit: number;
}

export interface Attraction {
  id: string;
  name: string;
  regionId: string;
  level: string;
  latitude: number;
  longitude: number;
  description: string | null;
  visitCount: number;
}

export interface Region {
  id: string;
  name: string;
  level: string;
  parentId: string | null;
}

export interface CommunityPost {
  id: string;
  nickname: string;
  avatarUrl: string | null;
  title: string;
  content: string;
  coverImage: string | null;
  regionId: string | null;
  attractionId: string | null;
  likeCount: number;
  commentCount: number;
  createdAt: number;
}

export type SiteLocale = 'zh' | 'en';

export type ContentSource = 'live' | 'editorial';

export interface AttractionViewModel {
  id: string;
  slug: string;
  source: ContentSource;
  name: string;
  region: string;
  level: string | null;
  description: string;
  image: string | null;
  visitCount: number | null;
  coordinates: { latitude: number; longitude: number } | null;
}

export interface StoryViewModel {
  id: string;
  slug: string;
  source: ContentSource;
  author: string;
  title: string;
  excerpt: string;
  content: string;
  coverImage: string | null;
  region: string | null;
  likeCount: number | null;
  commentCount: number | null;
  createdAt: number | null;
}
