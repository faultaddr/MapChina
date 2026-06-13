import type { ApiResponse, Attraction, Journal } from '@/types';

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080';
const TIMEOUT_MS = 8000;

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

export async function fetchAttractions(
  page = 1,
  limit = 20,
  sort = 'popular',
): Promise<ApiResponse<Attraction[]> | null> {
  try {
    const res = await fetchWithTimeout(
      `${API_URL}/api/attractions?page=${page}&limit=${limit}&sort=${sort}`,
      3600,
    );
    if (!res.ok) return null;
    return await res.json();
  } catch {
    return null;
  }
}

export async function fetchAttraction(
  id: string,
): Promise<ApiResponse<Attraction> | null> {
  try {
    const res = await fetchWithTimeout(`${API_URL}/api/attractions/${id}`, 1800);
    if (!res.ok) return null;
    return await res.json();
  } catch {
    return null;
  }
}

export async function fetchJournals(
  page = 1,
  limit = 20,
  sort = 'popular',
): Promise<ApiResponse<Journal[]> | null> {
  try {
    const res = await fetchWithTimeout(
      `${API_URL}/api/journals?page=${page}&limit=${limit}&sort=${sort}`,
      3600,
    );
    if (!res.ok) return null;
    return await res.json();
  } catch {
    return null;
  }
}

export async function fetchJournal(
  id: string,
): Promise<ApiResponse<Journal> | null> {
  try {
    const res = await fetchWithTimeout(`${API_URL}/api/journals/${id}`, 1800);
    if (!res.ok) return null;
    return await res.json();
  } catch {
    return null;
  }
}
