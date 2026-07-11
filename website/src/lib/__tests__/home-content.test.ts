import { describe, expect, it } from 'vitest';
import { getProductPaths, themePreviews } from '../home-content';

describe('home content', () => {
  it('uses the four current product paths in both locales', () => {
    expect(getProductPaths('zh').map((item) => item.key)).toEqual(['footprint', 'discover', 'shanhe', 'profile']);
    expect(getProductPaths('en')).toHaveLength(4);
    expect(getProductPaths('zh').map((item) => item.title)).toEqual(['足迹', '发现', '山河', '我的']);
  });

  it('uses real app theme assets instead of flat color placeholders', () => {
    expect(themePreviews).toHaveLength(6);
    expect(themePreviews.filter((theme) => theme.image !== null).map((theme) => theme.image)).toEqual([
      '/themes/ink-wash.png',
      '/themes/vintage-map.png',
      '/themes/rice-paper.png',
      '/themes/starry-night.png',
      '/themes/mountain-mist.png',
    ]);
  });
});
