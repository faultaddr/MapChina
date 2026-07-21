import { cp, mkdir, rm } from 'node:fs/promises';
import { resolve } from 'node:path';

const root = process.cwd();
const standalone = resolve(root, '.next/standalone');

await rm(resolve(standalone, 'public'), { force: true, recursive: true });
await cp(resolve(root, 'public'), resolve(standalone, 'public'), { recursive: true });

await mkdir(resolve(standalone, '.next'), { recursive: true });
await rm(resolve(standalone, '.next/static'), { force: true, recursive: true });
await cp(resolve(root, '.next/static'), resolve(standalone, '.next/static'), { recursive: true });

console.log('Prepared standalone server with public and Next.js static assets.');
