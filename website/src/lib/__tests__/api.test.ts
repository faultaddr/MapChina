import { describe, it, expect, vi, beforeEach } from 'vitest';
import { fetchAttractions, fetchAttraction, fetchJournals, fetchJournal } from '../api';

beforeEach(() => {
  vi.restoreAllMocks();
});

describe('fetchAttractions', () => {
  it('fetches attractions with default params', async () => {
    const mockResponse = {
      success: true,
      data: [{ id: '1', name: '故宫' }],
      meta: { total: 1, page: 1, limit: 20 },
    };
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
      ok: true,
      json: () => Promise.resolve(mockResponse),
    } as Response);

    const result = await fetchAttractions();
    expect(result!.data).toHaveLength(1);
    expect(result!.data[0].name).toBe('故宫');
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
    const mockResponse = {
      success: true,
      data: { id: '1', name: '故宫' },
      meta: { total: 1, page: 1, limit: 1 },
    };
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
      ok: true,
      json: () => Promise.resolve(mockResponse),
    } as Response);

    const result = await fetchAttraction('1');
    expect(result!.data.id).toBe('1');
  });
});

describe('fetchJournals', () => {
  it('fetches journals with custom params', async () => {
    const mockResponse = {
      success: true,
      data: [{ id: 'j1', title: '北京之旅' }],
      meta: { total: 1, page: 2, limit: 10 },
    };
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
      ok: true,
      json: () => Promise.resolve(mockResponse),
    } as Response);

    const result = await fetchJournals(2, 10);
    expect(result!.data).toHaveLength(1);
    expect(result!.meta.page).toBe(2);
  });
});

describe('fetchJournal', () => {
  it('returns null on 404', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
      ok: false,
      status: 404,
    } as Response);

    const result = await fetchJournal('nonexistent');
    expect(result).toBeNull();
  });
});
