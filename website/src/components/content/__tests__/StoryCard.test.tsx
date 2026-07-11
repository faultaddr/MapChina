import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import type { StoryViewModel } from '@/types';
import { StoryCard } from '../StoryCard';

const editorial: StoryViewModel = {
  id: 'editorial-story-west-lake-rain',
  slug: 'west-lake-rain',
  source: 'editorial',
  author: '青禾',
  title: '雨后的西湖，适合慢一点',
  excerpt: '避开人潮，在湿润的石阶与桂香之间小驻。',
  content: '雨停后，湖面把远山压成很淡的一层灰。',
  coverImage: null,
  region: '浙江 · 杭州',
  likeCount: null,
  commentCount: null,
  createdAt: null,
};

describe('StoryCard', () => {
  it('labels editorial stories without invented engagement', () => {
    render(<StoryCard item={editorial} locale="zh" />);

    expect(screen.getByText('编辑精选')).toBeInTheDocument();
    expect(screen.queryByText(/赞/)).not.toBeInTheDocument();
    expect(screen.getByRole('link', { name: /雨后的西湖/ })).toHaveAttribute('href', '/zh/community/west-lake-rain');
  });

  it('shows real engagement for live stories', () => {
    render(<StoryCard item={{ ...editorial, id: 'live-story', slug: 'live-story', source: 'live', likeCount: 8, commentCount: 2 }} locale="en" />);

    expect(screen.getByText('8 likes')).toBeInTheDocument();
    expect(screen.getByText('2 comments')).toBeInTheDocument();
  });
});
