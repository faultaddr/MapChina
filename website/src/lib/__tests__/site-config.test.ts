import { describe, expect, it } from 'vitest';
import { BETA_EMAIL, buildBetaMailto } from '../site-config';

describe('buildBetaMailto', () => {
  it('builds the Chinese beta request', () => {
    const url = new URL(buildBetaMailto('zh'));

    expect(BETA_EMAIL).toBe('cuferpan@gmail.com');
    expect(url.protocol).toBe('mailto:');
    expect(url.pathname).toBe('cuferpan@gmail.com');
    expect(url.searchParams.get('subject')).toBe('申请参与 MapChina 内测');
    expect(url.searchParams.get('body')).toContain('我想参与 MapChina 内测');
  });

  it('builds the English beta request', () => {
    const url = new URL(buildBetaMailto('en'));

    expect(url.searchParams.get('subject')).toBe('MapChina beta access request');
    expect(url.searchParams.get('body')).toContain('I would like to join the MapChina beta');
  });
});
