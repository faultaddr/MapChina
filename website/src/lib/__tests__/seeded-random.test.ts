import { describe, expect, it } from 'vitest';
import { seededRandom } from '../seeded-random';

describe('seededRandom', () => {
  it('returns the same sequence for the same seed', () => {
    const first = seededRandom(2026);
    const second = seededRandom(2026);

    expect([first(), first(), first(), first()]).toEqual([second(), second(), second(), second()]);
  });

  it('keeps every value inside the random unit interval', () => {
    const random = seededRandom(7);
    const values = Array.from({ length: 40 }, () => random());

    expect(values.every((value) => value >= 0 && value < 1)).toBe(true);
    expect(new Set(values).size).toBeGreaterThan(30);
  });
});
