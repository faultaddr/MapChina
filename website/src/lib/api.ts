import type { PaginatedResponse, Attraction, Region, CommunityPost } from '@/types';

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080';
const TIMEOUT_MS = 8000;

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

function isPaginatedPayload<T>(value: unknown): value is PaginatedResponse<T> {
  return isRecord(value) && Array.isArray(value.data);
}

function hasStringFields(value: unknown, fields: string[]): value is Record<string, unknown> {
  return isRecord(value) && fields.every((field) => typeof value[field] === 'string');
}

async function fetchWithTimeout(url: string, revalidate?: number): Promise<Response> {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), TIMEOUT_MS);
  try {
    const res = await fetch(url, {
      signal: controller.signal,
      ...(revalidate ? { next: { revalidate } } : {}),
    });
    return res;
  } finally {
    clearTimeout(timer);
  }
}

// ---- Attractions (public) ----

export async function fetchAttractions(
  page = 1,
  limit = 20,
  sort = 'popular',
  regionId?: string,
): Promise<PaginatedResponse<Attraction> | null> {
  try {
    const params = new URLSearchParams({ page: String(page), limit: String(limit), sort });
    if (regionId) params.set('regionId', regionId);
    const res = await fetchWithTimeout(`${API_URL}/public/attractions?${params}`, 3600);
    if (!res.ok) return null;
    const payload: unknown = await res.json();
    return isPaginatedPayload<Attraction>(payload) ? payload : null;
  } catch {
    return null;
  }
}

export async function fetchAttraction(id: string): Promise<Attraction | null> {
  try {
    const res = await fetchWithTimeout(`${API_URL}/public/attractions/${id}`, 1800);
    if (!res.ok) return null;
    const payload: unknown = await res.json();
    return hasStringFields(payload, ['id', 'name']) ? payload as unknown as Attraction : null;
  } catch {
    return null;
  }
}

// ---- Regions (public) ----

export async function fetchRegions(
  page = 1,
  limit = 50,
  level?: string,
  parentId?: string,
): Promise<PaginatedResponse<Region> | null> {
  try {
    const params = new URLSearchParams({ page: String(page), limit: String(limit) });
    if (level) params.set('level', level);
    if (parentId) params.set('parentId', parentId);
    const res = await fetchWithTimeout(`${API_URL}/public/regions?${params}`, 3600);
    if (!res.ok) return null;
    const payload: unknown = await res.json();
    return isPaginatedPayload<Region>(payload) ? payload : null;
  } catch {
    return null;
  }
}

export async function fetchRegion(id: string): Promise<Region | null> {
  try {
    const res = await fetchWithTimeout(`${API_URL}/public/regions/${id}`, 1800);
    if (!res.ok) return null;
    const payload: unknown = await res.json();
    return hasStringFields(payload, ['id', 'name']) ? payload as unknown as Region : null;
  } catch {
    return null;
  }
}

// ---- Community (public) ----

export async function fetchCommunityFeed(
  page = 1,
  limit = 20,
): Promise<PaginatedResponse<CommunityPost> | null> {
  try {
    const res = await fetchWithTimeout(
      `${API_URL}/public/community/feed?page=${page}&limit=${limit}`,
      3600,
    );
    if (!res.ok) return null;
    const payload: unknown = await res.json();
    return isPaginatedPayload<CommunityPost>(payload) ? payload : null;
  } catch {
    return null;
  }
}

export async function fetchCommunityPost(id: string): Promise<CommunityPost | null> {
  try {
    const res = await fetchWithTimeout(`${API_URL}/public/community/posts/${id}`, 1800);
    if (!res.ok) return null;
    const payload: unknown = await res.json();
    return hasStringFields(payload, ['id', 'title', 'content']) ? payload as unknown as CommunityPost : null;
  } catch {
    return null;
  }
}
