import { describe, expect, it } from 'vitest';
import { getHeroCopy, getProductPaths, themePreviews } from '../home-content';

describe('home content', () => {
  it('keeps the Chinese hero title to two compact display lines', () => {
    expect(getHeroCopy('zh').title).toBe('点亮足迹，\n看见我的山河');
    expect(getHeroCopy('zh').title.split('\n')).toHaveLength(2);
  });

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
