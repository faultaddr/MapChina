import { describe, expect, it } from 'vitest';
import packageJson from '../../../package.json';

describe('standalone deployment', () => {
  it('prepares static assets and starts the generated standalone server', () => {
    expect(packageJson.scripts).toMatchObject({
      postbuild: 'node scripts/prepare-standalone.mjs',
      start: 'node .next/standalone/server.js',
    });
  });
});
