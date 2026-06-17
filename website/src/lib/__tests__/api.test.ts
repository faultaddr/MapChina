import { describe, it, expect, vi, beforeEach } from 'vitest';
import { fetchAttractions, fetchAttraction, fetchRegions, fetchRegion, fetchCommunityFeed, fetchCommunityPost } from '../api';

beforeEach(() => {
  vi.restoreAllMocks();
});

describe('fetchAttractions', () => {
  it('fetches attractions with default params', async () => {
    const mockResponse = {
      data: [{ id: '1', name: '故宫', regionId: 'r1', level: '5A', latitude: 39.9, longitude: 116.4, description: null, visitCount: 100 }],
      total: 1,
      page: 1,
      limit: 20,
    };
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
      ok: true,
      json: () => Promise.resolve(mockResponse),
    } as Response);

    const result = await fetchAttractions();
    expect(result!.data).toHaveLength(1);
    expect(result!.data[0].name).toBe('故宫');
    expect(result!.total).toBe(1);
  });

  it('passes sort and regionId params', async () => {
    const mockResponse = { data: [], total: 0, page: 1, limit: 20 };
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
      ok: true,
      json: () => Promise.resolve(mockResponse),
    } as Response);

    await fetchAttractions(1, 10, 'popular', 'r1');
    const calledUrl = fetchSpy.mock.calls[0][0] as string;
    expect(calledUrl).toContain('sort=popular');
    expect(calledUrl).toContain('regionId=r1');
  });

  it('returns null on API error', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
      ok: false,
      status: 500,
    } as Response);

    const result = await fetchAttractions();
    expect(result).toBeNull();
  });

  it('returns null on network error', async () => {
    vi.spyOn(globalThis, 'fetch').mockRejectedValueOnce(new Error('Network error'));

    const result = await fetchAttractions();
    expect(result).toBeNull();
  });
});

describe('fetchAttraction', () => {
  it('fetches single attraction by id', async () => {
    const mockAttraction = { id: '1', name: '故宫', regionId: 'r1', level: '5A', latitude: 39.9, longitude: 116.4, description: 'test', visitCount: 100 };
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
      ok: true,
      json: () => Promise.resolve(mockAttraction),
    } as Response);

    const result = await fetchAttraction('1');
    expect(result!.id).toBe('1');
    expect(result!.name).toBe('故宫');
  });

  it('returns null on 404', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
      ok: false,
      status: 404,
    } as Response);

    const result = await fetchAttraction('nonexistent');
    expect(result).toBeNull();
  });
});

describe('fetchRegions', () => {
  it('fetches regions with level and parentId filters', async () => {
    const mockResponse = {
      data: [{ id: 'r1', name: '北京', level: 'province', parentId: null }],
      total: 1,
      page: 1,
      limit: 50,
    };
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
      ok: true,
      json: () => Promise.resolve(mockResponse),
    } as Response);

    const result = await fetchRegions(1, 50, 'province', 'p1');
    const calledUrl = fetchSpy.mock.calls[0][0] as string;
    expect(calledUrl).toContain('level=province');
    expect(calledUrl).toContain('parentId=p1');
    expect(result!.data).toHaveLength(1);
  });
});

describe('fetchCommunityFeed', () => {
  it('fetches community posts', async () => {
    const mockResponse = {
      data: [{ id: 'c1', nickname: '旅行者', avatarUrl: null, title: '北京之旅', content: '内容', coverImage: null, regionId: null, attractionId: null, likeCount: 10, commentCount: 3, createdAt: 1700000000000 }],
      total: 1,
      page: 1,
      limit: 20,
    };
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
      ok: true,
      json: () => Promise.resolve(mockResponse),
    } as Response);

    const result = await fetchCommunityFeed();
    expect(result!.data).toHaveLength(1);
    expect(result!.data[0].title).toBe('北京之旅');
  });
});

describe('fetchCommunityPost', () => {
  it('returns null on 404', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
      ok: false,
      status: 404,
    } as Response);

    const result = await fetchCommunityPost('nonexistent');
    expect(result).toBeNull();
  });
});
