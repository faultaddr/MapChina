import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import type { AttractionViewModel } from '@/types';
import { AttractionCard } from '../AttractionCard';

const editorial: AttractionViewModel = {
  id: 'editorial-attraction-beijing-central-axis',
  slug: 'beijing-central-axis',
  source: 'editorial',
  name: '北京中轴线',
  region: '北京',
  level: null,
  description: '沿城市脊梁阅读古都秩序。',
  image: null,
  visitCount: null,
  coordinates: null,
};

describe('AttractionCard', () => {
  it('labels editorial content without inventing visit metrics', () => {
    render(<AttractionCard item={editorial} locale="zh" />);

    expect(screen.getByText('编辑精选')).toBeInTheDocument();
    expect(screen.queryByText(/次访问/)).not.toBeInTheDocument();
    expect(screen.getByRole('link', { name: /北京中轴线/ })).toHaveAttribute('href', '/zh/attractions/beijing-central-axis');
  });

  it('shows a real visit count for live content', () => {
    render(<AttractionCard item={{ ...editorial, id: 'live-1', slug: 'live-1', source: 'live', visitCount: 42 }} locale="en" />);

    expect(screen.getByText('42 visits')).toBeInTheDocument();
    expect(screen.queryByText('Editor’s pick')).not.toBeInTheDocument();
  });
});
