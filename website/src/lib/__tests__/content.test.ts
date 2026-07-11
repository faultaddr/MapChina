import { describe, expect, it } from 'vitest';
import type { Attraction, CommunityPost } from '@/types';
import {
  composeAttractions,
  composeStories,
  findEditorialAttraction,
  findEditorialStory,
  resolveAttractionDetail,
  resolveStoryDetail,
} from '../content';

const validAttraction: Attraction = {
  id: 'live-1',
  name: '故宫',
  regionId: '北京',
  level: '5A',
  latitude: 39.916,
  longitude: 116.397,
  description: '沿中轴线走进紫禁城。',
  visitCount: 42,
};

const validStory: CommunityPost = {
  id: 'story-1',
  nickname: '旅行者',
  avatarUrl: null,
  title: '北京的一天',
  content: '从清晨走到黄昏。',
  coverImage: null,
  regionId: '北京',
  attractionId: null,
  likeCount: 12,
  commentCount: 3,
  createdAt: 1_700_000_000_000,
};

describe('composeAttractions', () => {
  it('keeps live items first, drops broken records, and fills shortages', () => {
    const live = [validAttraction, { ...validAttraction, id: '', name: '' }];

    const result = composeAttractions(live, 'zh', 4);

    expect(result).toHaveLength(4);
    expect(result[0]).toMatchObject({ id: 'live-1', source: 'live', visitCount: 42 });
    expect(result.slice(1).every((item) => item.source === 'editorial')).toBe(true);
    expect(new Set(result.map((item) => item.id)).size).toBe(4);
  });

  it('uses localized editorial details without invented metrics', () => {
    const result = composeAttractions([], 'en', 2);

    expect(result[0].name).toBe('Beijing Central Axis');
    expect(result.every((item) => item.visitCount === null)).toBe(true);
    expect(findEditorialAttraction('beijing-central-axis', 'zh')?.name).toBe('北京中轴线');
  });
});

describe('composeStories', () => {
  it('keeps live metrics and fills with editorial stories without metrics', () => {
    const result = composeStories([validStory], 'zh', 4);

    expect(result).toHaveLength(4);
    expect(result[0]).toMatchObject({ source: 'live', likeCount: 12, commentCount: 3 });
    expect(result.slice(1).every((item) => item.likeCount === null && item.commentCount === null)).toBe(true);
  });

  it('finds localized editorial story details', () => {
    expect(findEditorialStory('west-lake-rain', 'en')?.title).toBe('West Lake After the Rain');
    expect(findEditorialStory('missing-story', 'zh')).toBeNull();
  });
});

describe('detail resolution', () => {
  it('resolves editorial attraction slugs before remote data', () => {
    expect(resolveAttractionDetail('beijing-central-axis', null, 'en')).toMatchObject({
      source: 'editorial',
      name: 'Beijing Central Axis',
    });
    expect(resolveAttractionDetail('live-1', validAttraction, 'zh')).toMatchObject({
      source: 'live',
      name: '故宫',
    });
    expect(resolveAttractionDetail('missing', null, 'zh')).toBeNull();
  });

  it('resolves editorial and live story details', () => {
    expect(resolveStoryDetail('west-lake-rain', null, 'zh')).toMatchObject({
      source: 'editorial',
      title: '雨后的西湖，适合慢一点',
    });
    expect(resolveStoryDetail('story-1', validStory, 'en')).toMatchObject({
      source: 'live',
      title: '北京的一天',
    });
    expect(resolveStoryDetail('missing', null, 'en')).toBeNull();
  });
});
