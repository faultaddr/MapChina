# Digital Shanhe Website Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the MapChina website into the approved bilingual “Digital Shanhe” product-and-content experience, verify every route and responsive state, and leave a production server running locally on `127.0.0.1:3100`.

**Architecture:** Keep the existing Next.js 16 App Router application. Add a typed content-composition layer between public API functions and view components so live items can be normalized, de-duplicated, and supplemented with clearly labeled locale-specific editorial items. Rebuild the UI from small shared layout, media, content-card, and beta-conversion components; keep data fetching in Server Components and isolate only interactive navigation, theme switching, motion, and image-error handling as Client Components.

**Tech Stack:** Next.js 16.2.9, React 19.2.4, TypeScript 5, Tailwind CSS 4, next-intl 4.13, Framer Motion 12, React Three Fiber 9, Vitest 4, React Testing Library 16.

## Global Constraints

- Do not add search, complex filters, infinite scrolling, CMS, web accounts, posting, comments, or new backend endpoints.
- Do not display App Store or Google Play buttons; the app status is “Coming soon / 即将上线”.
- Every beta CTA uses a localized `mailto:cuferpan@gmail.com` URL with prefilled subject and body.
- Live API data wins; locale-specific editorial data fills shortages and is explicitly labeled without invented metrics.
- Chinese routes and content use `zh`; English routes and content use `en`; internal links retain the active locale.
- Mobile defaults to a static SVG map and must not create horizontal overflow at 390 px.
- Respect `prefers-reduced-motion` and WCAG AA contrast; all interactive elements have visible focus styles.
- Do not add a heavyweight dependency or a new icon library.
- Preserve unrelated worktree changes and stage only files belonging to each task.
- Read the relevant Next.js 16 docs in `website/node_modules/next/dist/docs/01-app/` before changing images, fonts, metadata, Server/Client Component boundaries, or deployment code.
- After UI/runtime changes, satisfy root `AGENTS.md`: build/install Android, launch with adb, interact, and capture before/after screenshots.

---

## File Structure

### Create

- `website/src/lib/content.ts` — normalizes API records and composes live and editorial data.
- `website/src/lib/curated-content.ts` — strongly typed zh/en editorial attractions and stories.
- `website/src/lib/site-config.ts` — beta email configuration and localized mailto builder.
- `website/src/lib/__tests__/content.test.ts` — merge, normalization, and fallback tests.
- `website/src/lib/__tests__/site-config.test.ts` — beta mailto tests.
- `website/src/lib/seeded-random.ts` — deterministic random source for the particle map.
- `website/src/lib/__tests__/seeded-random.test.ts` — deterministic sequence coverage.
- `website/src/components/content/AttractionCard.tsx` — one attraction preview contract.
- `website/src/components/content/StoryCard.tsx` — one community preview contract.
- `website/src/components/content/__tests__/AttractionCard.test.tsx` — live/editorial attraction rendering.
- `website/src/components/content/__tests__/StoryCard.test.tsx` — live/editorial story rendering.
- `website/src/components/shared/PageHero.tsx` — reusable interior-page hero.
- `website/src/components/shared/SectionHeading.tsx` — reusable section heading and optional link.
- `website/src/components/shared/MediaFallback.tsx` — semantic regional/section media fallback.
- `website/src/components/shared/EditorialBadge.tsx` — explicit editorial-source label.
- `website/src/components/shared/BetaCTA.tsx` — centralized beta state and email conversion block.
- `website/src/components/shared/PathIcon.tsx` — dependency-free SVG icons for product paths.
- `website/src/components/home/ProductPaths.tsx` — Footprint, Discover, Shanhe, Profile product story.
- `website/src/components/home/LiveShanhe.tsx` — balanced attraction/community content section.
- `website/src/components/shared/__tests__/BetaCTA.test.tsx` — CTA content/link coverage.
- `website/src/components/layout/__tests__/MobileMenu.test.tsx` — mobile menu state coverage.
- `website/public/themes/*.png` — copied existing app theme assets used by the real gallery.

### Modify

- `website/package.json` — add `test` and `test:watch` scripts.
- `website/vitest.config.ts` — configure setup and include patterns.
- `website/src/types/index.ts` — add normalized view-model contracts.
- `website/src/lib/api.ts` and `website/src/lib/__tests__/api.test.ts` — validate response shapes and preserve timeout behavior.
- `website/src/i18n/zh.json`, `website/src/i18n/en.json` — complete all page and shared copy.
- `website/src/app/globals.css` and root/locale layouts — visual tokens, focus, metadata, locale-aware shell.
- Layout components — locale-safe links, contrast, mobile drawer, footer.
- Home components and page — approved Digital Shanhe narrative.
- Attraction/community list and detail routes — shared cards, local fallback details, metadata, JSON-LD.
- Download/about routes — beta and product-story pages.
- `website/src/components/three/ChinaMapScene.tsx` — deterministic particle generation.
- `website/src/app/sitemap.ts`, `website/src/app/robots.ts` — current route metadata.

### Remove

- `website/src/components/home/FeatureCards.tsx` — replaced by `ProductPaths`.
- `website/src/components/home/CommunityPicks.tsx` — replaced by `LiveShanhe`.
- Empty app-store hrefs and obsolete emoji UI from all touched files.

---

### Task 1: Test Harness, Site Configuration, and Content Contracts

**Files:**
- Modify: `website/package.json`
- Modify: `website/vitest.config.ts`
- Modify: `website/src/types/index.ts`
- Create: `website/src/lib/site-config.ts`
- Create: `website/src/lib/__tests__/site-config.test.ts`

**Interfaces:**
- Produces: `SiteLocale`, `ContentSource`, `AttractionViewModel`, `StoryViewModel`, `buildBetaMailto(locale)`.
- Consumes: existing `Attraction` and `CommunityPost` API types.

- [ ] **Step 1: Add failing mailto and type-contract tests**

```ts
import { describe, expect, it } from 'vitest';
import { buildBetaMailto } from '../site-config';

describe('buildBetaMailto', () => {
  it('builds the Chinese beta request', () => {
    const url = new URL(buildBetaMailto('zh'));
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
```

- [ ] **Step 2: Add test scripts and run the focused test to prove failure**

```json
"scripts": {
  "dev": "next dev",
  "build": "next build",
  "start": "next start",
  "lint": "eslint",
  "test": "vitest run",
  "test:watch": "vitest"
}
```

Run: `cd website && npm test -- src/lib/__tests__/site-config.test.ts`

Expected: FAIL because `../site-config` does not exist.

- [ ] **Step 3: Add exact shared contracts and site configuration**

```ts
export type SiteLocale = 'zh' | 'en';
export type ContentSource = 'live' | 'editorial';

export interface AttractionViewModel {
  id: string;
  slug: string;
  source: ContentSource;
  name: string;
  region: string;
  level: string | null;
  description: string;
  image: string | null;
  visitCount: number | null;
  coordinates: { latitude: number; longitude: number } | null;
}

export interface StoryViewModel {
  id: string;
  slug: string;
  source: ContentSource;
  author: string;
  title: string;
  excerpt: string;
  content: string;
  coverImage: string | null;
  region: string | null;
  likeCount: number | null;
  commentCount: number | null;
  createdAt: number | null;
}
```

```ts
import type { SiteLocale } from '@/types';

export const BETA_EMAIL = 'cuferpan@gmail.com';

const betaCopy = {
  zh: { subject: '申请参与 MapChina 内测', body: '你好，\n\n我想参与 MapChina 内测。\n\n我的常用设备：\n我最期待的功能：\n' },
  en: { subject: 'MapChina beta access request', body: 'Hello,\n\nI would like to join the MapChina beta.\n\nMy primary device:\nThe feature I am most interested in:\n' },
} satisfies Record<SiteLocale, { subject: string; body: string }>;

export function buildBetaMailto(locale: SiteLocale): string {
  const copy = betaCopy[locale];
  return `mailto:${BETA_EMAIL}?subject=${encodeURIComponent(copy.subject)}&body=${encodeURIComponent(copy.body)}`;
}
```

- [ ] **Step 4: Run focused tests and type check through the production build compiler**

Run: `cd website && npm test -- src/lib/__tests__/site-config.test.ts`

Expected: 2 tests PASS.

- [ ] **Step 5: Commit the foundation**

```bash
git add website/package.json website/vitest.config.ts website/src/types/index.ts website/src/lib/site-config.ts website/src/lib/__tests__/site-config.test.ts
git commit -m "feat(web): add site content contracts and beta config"
```

### Task 2: Editorial Content and Live-First Composition

**Files:**
- Create: `website/src/lib/curated-content.ts`
- Create: `website/src/lib/content.ts`
- Create: `website/src/lib/__tests__/content.test.ts`
- Modify: `website/src/lib/api.ts`
- Modify: `website/src/lib/__tests__/api.test.ts`

**Interfaces:**
- Consumes: `Attraction`, `CommunityPost`, `SiteLocale`, `AttractionViewModel`, `StoryViewModel`.
- Produces: `getEditorialAttractions(locale)`, `getEditorialStories(locale)`, `composeAttractions(live, locale, minimum)`, `composeStories(live, locale, minimum)`, `findEditorialAttraction(slug, locale)`, `findEditorialStory(slug, locale)`.

- [ ] **Step 1: Write failing composition tests**

```ts
it('keeps live attractions first, drops broken records, and fills shortages', () => {
  const validAttraction: Attraction = {
    id: 'live-1', name: '故宫', regionId: 'beijing', level: '5A', latitude: 39.916,
    longitude: 116.397, description: '沿中轴线走进紫禁城。', visitCount: 42,
  };
  const live = [validAttraction, { ...validAttraction, id: '', name: '' }];
  const result = composeAttractions(live, 'zh', 4);
  expect(result).toHaveLength(4);
  expect(result[0]).toMatchObject({ id: 'live-1', source: 'live', visitCount: 42 });
  expect(result.slice(1).every((item) => item.source === 'editorial')).toBe(true);
  expect(new Set(result.map((item) => item.id)).size).toBe(4);
});

it('does not invent editorial story metrics', () => {
  const result = composeStories([], 'en', 3);
  expect(result).toHaveLength(3);
  expect(result.every((item) => item.likeCount === null && item.commentCount === null)).toBe(true);
});
```

- [ ] **Step 2: Run the composition test to prove failure**

Run: `cd website && npm test -- src/lib/__tests__/content.test.ts`

Expected: FAIL because content modules do not exist.

- [ ] **Step 3: Add complete locale-specific editorial fixtures**

```ts
const attractionSeeds = {
  zh: [
    { slug: 'beijing-central-axis', name: '北京中轴线', region: '北京', description: '从钟鼓楼到永定门，沿城市脊梁阅读古都秩序。', image: null },
    { slug: 'hangzhou-west-lake', name: '西湖环线', region: '浙江·杭州', description: '在山水与城市之间小驻，感受十景之外的日常。', image: null },
    { slug: 'dunhuang-mogao', name: '敦煌莫高窟', region: '甘肃·敦煌', description: '循着丝路抵达壁画与洞窟交织的千年现场。', image: null },
    { slug: 'yunnan-yuanyang', name: '元阳梯田', region: '云南·红河', description: '在云海与水田的季节变化中，看见山地生活。', image: null },
  ],
  en: [
    { slug: 'beijing-central-axis', name: 'Beijing Central Axis', region: 'Beijing', description: 'Walk the historic spine of the capital from the Drum Tower to Yongdingmen.', image: null },
    { slug: 'hangzhou-west-lake', name: 'West Lake Loop', region: 'Hangzhou, Zhejiang', description: 'Pause between landscape and city life along the shores of West Lake.', image: null },
    { slug: 'dunhuang-mogao', name: 'Mogao Caves', region: 'Dunhuang, Gansu', description: 'Follow the Silk Road into a millennium of murals and cave art.', image: null },
    { slug: 'yunnan-yuanyang', name: 'Yuanyang Rice Terraces', region: 'Honghe, Yunnan', description: 'See mountain life reflected in seasonal clouds and flooded terraces.', image: null },
  ],
} as const;
```

Use these four bilingual stories; map every editorial record to null metrics and stable `editorial-*` IDs:

```ts
const storySeeds = {
  zh: [
    { slug: 'autumn-central-axis', author: 'MapChina 编辑部', title: '沿着中轴线，走进北京的秋天', excerpt: '从晨钟到城门，把古都的一天交给脚步。', content: '清晨从钟鼓楼出发，屋脊和树影把街巷切成明暗两半。一路向南，城市的尺度在步行中慢慢显现。\n\n抵达永定门时，天色已经变暖。这条路不是景点清单，而是一条可以反复走进的城市脊梁。', coverImage: null, region: '北京' },
    { slug: 'west-lake-rain', author: '青禾', title: '雨后的西湖，适合慢一点', excerpt: '避开人潮，在湿润的石阶与桂香之间小驻。', content: '雨停后，湖面把远山压成很淡的一层灰。沿着杨公堤慢慢走，不必追赶十景，也能遇见属于自己的片刻。', coverImage: null, region: '浙江·杭州' },
    { slug: 'dunhuang-night', author: '远山来信', title: '敦煌入夜之后', excerpt: '白日看洞窟，夜里听风从沙丘上经过。', content: '傍晚离开洞窟，壁画里的颜色仍留在眼前。沙漠降温很快，风把白天的喧闹一点点带走。', coverImage: null, region: '甘肃·敦煌' },
    { slug: 'yuanyang-clouds', author: '阿野', title: '云落在元阳梯田', excerpt: '水面收下天光，也收下山地生活的节奏。', content: '日出前的梯田没有鲜艳颜色，只有云和水面交换着微弱的光。等村庄醒来，山坡也跟着有了声音。', coverImage: null, region: '云南·红河' },
  ],
  en: [
    { slug: 'autumn-central-axis', author: 'MapChina Editors', title: 'Autumn Along Beijing’s Central Axis', excerpt: 'Walk from morning bells to the southern gate and let the city set the pace.', content: 'Start at the Drum and Bell Towers while the lanes are still divided by roof shadows. Walking south reveals the scale of the capital one block at a time.\n\nBy Yongdingmen, the light has warmed. This is less a checklist than a historic spine worth returning to.', coverImage: null, region: 'Beijing' },
    { slug: 'west-lake-rain', author: 'Qinghe', title: 'West Lake After the Rain', excerpt: 'Slow down between wet stone paths, osmanthus, and distant hills.', content: 'After the rain, the lake presses the mountains into a pale grey line. Walk the Yanggong Causeway without chasing landmarks and the quieter lake will find you.', coverImage: null, region: 'Hangzhou, Zhejiang' },
    { slug: 'dunhuang-night', author: 'Letters from Afar', title: 'When Night Reaches Dunhuang', excerpt: 'See the caves by day, then hear the wind crossing the dunes.', content: 'The colors of the murals remain after leaving the caves. The desert cools quickly, and the evening wind carries away the noise of the day.', coverImage: null, region: 'Dunhuang, Gansu' },
    { slug: 'yuanyang-clouds', author: 'Aye', title: 'Clouds Over Yuanyang', excerpt: 'Terraced water holds the sky and the rhythm of mountain life.', content: 'Before sunrise the terraces have little color, only clouds and water exchanging faint light. When the villages wake, the slopes begin to make sound.', coverImage: null, region: 'Honghe, Yunnan' },
  ],
} as const;
```

- [ ] **Step 4: Implement pure composition functions**

```ts
export function composeAttractions(live: Attraction[] | null | undefined, locale: SiteLocale, minimum = 6): AttractionViewModel[] {
  const normalized = (live ?? []).map(normalizeAttraction).filter((item): item is AttractionViewModel => item !== null);
  return fillUnique(normalized, getEditorialAttractions(locale), minimum);
}

export function composeStories(live: CommunityPost[] | null | undefined, locale: SiteLocale, minimum = 4): StoryViewModel[] {
  const normalized = (live ?? []).map(normalizeStory).filter((item): item is StoryViewModel => item !== null);
  return fillUnique(normalized, getEditorialStories(locale), minimum);
}

function fillUnique<T extends { id: string }>(live: T[], editorial: T[], minimum: number): T[] {
  const result: T[] = [];
  const ids = new Set<string>();
  for (const item of [...live, ...editorial]) {
    if (!ids.has(item.id)) { ids.add(item.id); result.push(item); }
    if (result.length >= minimum) break;
  }
  return result;
}
```

- [ ] **Step 5: Harden API parsing without changing public signatures**

Require array payloads for paginated functions and object payloads for details; return `null` for malformed JSON. Extend `api.test.ts` with a malformed-payload case and remove the unused `fetchRegion` import warning.

```ts
const payload: unknown = await res.json();
if (!isPaginatedResponse<Attraction>(payload)) return null;
return payload;
```

- [ ] **Step 6: Run content and API tests**

Run: `cd website && npm test -- src/lib/__tests__/content.test.ts src/lib/__tests__/api.test.ts`

Expected: all content and API tests PASS.

- [ ] **Step 7: Commit content composition**

```bash
git add website/src/lib/curated-content.ts website/src/lib/content.ts website/src/lib/api.ts website/src/lib/__tests__/content.test.ts website/src/lib/__tests__/api.test.ts
git commit -m "feat(web): add live-first editorial content composition"
```

### Task 3: Shared Visual Primitives and Locale-Safe Site Shell

**Files:**
- Modify: `website/src/app/globals.css`
- Modify: `website/src/app/layout.tsx`
- Modify: `website/src/app/[locale]/layout.tsx`
- Modify: `website/src/components/layout/Navbar.tsx`
- Modify: `website/src/components/layout/MobileMenu.tsx`
- Modify: `website/src/components/layout/Footer.tsx`
- Create: `website/src/components/shared/PageHero.tsx`
- Create: `website/src/components/shared/SectionHeading.tsx`
- Create: `website/src/components/shared/MediaFallback.tsx`
- Create: `website/src/components/shared/EditorialBadge.tsx`
- Create: `website/src/components/shared/BetaCTA.tsx`
- Create: `website/src/components/shared/PathIcon.tsx`
- Create: `website/src/components/shared/__tests__/BetaCTA.test.tsx`
- Create: `website/src/components/layout/__tests__/MobileMenu.test.tsx`
- Modify: both locale JSON files

**Interfaces:**
- Consumes: `buildBetaMailto(locale)`, active locale from next-intl.
- Produces: `PageHero`, `SectionHeading`, `MediaFallback`, `EditorialBadge`, `BetaCTA`, `PathIcon`, and locale-safe global shell.

- [ ] **Step 1: Read required Next.js 16 docs**

Run:

```bash
sed -n '1,240p' website/node_modules/next/dist/docs/01-app/01-getting-started/05-server-and-client-components.md
sed -n '1,220p' website/node_modules/next/dist/docs/01-app/01-getting-started/11-css.md
sed -n '1,220p' website/node_modules/next/dist/docs/01-app/01-getting-started/13-fonts.md
```

Expected: confirm Client Components are limited to state/event/browser APIs and global CSS remains imported by the root layout.

- [ ] **Step 2: Write failing BetaCTA and mobile-menu tests**

```tsx
render(<BetaCTA locale="zh" variant="panel" />);
expect(screen.getByRole('link', { name: '参与内测' })).toHaveAttribute('href', expect.stringContaining('mailto:cuferpan@gmail.com'));
expect(screen.getByText('即将上线')).toBeInTheDocument();
```

```tsx
render(<MobileMenu open onClose={onClose} links={[{ href: '/zh/attractions', label: '景点' }]} betaLabel="参与内测" betaHref="mailto:test@example.com" />);
expect(screen.getByRole('link', { name: '景点' })).toHaveAttribute('href', '/zh/attractions');
expect(screen.getByRole('button', { name: '关闭菜单' })).toBeInTheDocument();
```

- [ ] **Step 3: Run focused component tests to prove failure**

Run: `cd website && npm test -- src/components/shared/__tests__/BetaCTA.test.tsx src/components/layout/__tests__/MobileMenu.test.tsx`

Expected: FAIL because the new components and props do not exist.

- [ ] **Step 4: Implement the visual tokens and shared primitives**

Define CSS variables for `--night`, `--night-soft`, `--jade`, `--jade-light`, `--gold`, `--paper`, `--surface`, `--ink`, `--muted`, and `--line`; add reusable `.focus-ring`, `.section-shell`, `.eyebrow`, `.display-title`, `.glass-nav`, and reduced-motion rules. Implement semantic SVG icons in `PathIcon` and map-gradient variants in `MediaFallback`.

```tsx
export function BetaCTA({ locale, variant = 'panel' }: { locale: SiteLocale; variant?: 'panel' | 'compact' }) {
  const copy = locale === 'zh'
    ? { eyebrow: '即将上线', title: '一起把走过的地方，沉淀成自己的山河', body: '参与首批体验，告诉我们你最想点亮的旅程。', action: '参与内测' }
    : { eyebrow: 'Coming soon', title: 'Turn every journey into your own digital landscape', body: 'Join the first group of explorers and help shape MapChina.', action: 'Join the beta' };
  return <section className={`beta-cta beta-cta--${variant}`}><p className="eyebrow">{copy.eyebrow}</p><h2>{copy.title}</h2><p>{copy.body}</p><a className="button button--gold focus-ring" href={buildBetaMailto(locale)}>{copy.action}</a></section>;
}
```

- [ ] **Step 5: Make the site shell locale-safe and accessible**

Build every internal href with `/${locale}`; pass the same stable hrefs to desktop and mobile navigation. Add localized menu labels, visible focus classes, `aria-current` based on `usePathname`, and a footer with beta email and language switch links.

- [ ] **Step 6: Run component tests and lint the touched shell**

Run: `cd website && npm test -- src/components/shared/__tests__/BetaCTA.test.tsx src/components/layout/__tests__/MobileMenu.test.tsx && npx eslint src/components/layout src/components/shared src/app/layout.tsx 'src/app/[locale]/layout.tsx'`

Expected: component tests PASS; ESLint reports no TypeScript/React errors.

- [ ] **Step 7: Commit the design shell**

```bash
git add website/src/app/globals.css website/src/app/layout.tsx 'website/src/app/[locale]/layout.tsx' website/src/components/layout website/src/components/shared website/src/i18n/zh.json website/src/i18n/en.json
git commit -m "feat(web): build digital shanhe site shell"
```

### Task 4: Digital Shanhe Home Page

**Files:**
- Modify: `website/src/app/[locale]/page.tsx`
- Modify: `website/src/components/home/HeroSection.tsx`
- Create: `website/src/components/home/ProductPaths.tsx`
- Create: `website/src/components/home/LiveShanhe.tsx`
- Modify: `website/src/components/home/ThemeGallery.tsx`
- Modify: `website/src/components/home/DownloadCTA.tsx`
- Modify: `website/src/components/three/ChinaMapScene.tsx`
- Create: `website/src/lib/seeded-random.ts`
- Create: `website/src/lib/__tests__/seeded-random.test.ts`
- Remove: `website/src/components/home/FeatureCards.tsx`
- Remove: `website/src/components/home/CommunityPicks.tsx`
- Create: `website/public/themes/*.png`

**Interfaces:**
- Consumes: `composeAttractions`, `composeStories`, shared content cards, `BetaCTA`, locale translations.
- Produces: approved home order: hero, live content, four product paths, theme assets, beta CTA.

- [ ] **Step 1: Copy real app theme assets**

Run:

```bash
mkdir -p website/public/themes
cp shared/src/commonMain/composeResources/drawable/bg_ink_wash.png website/public/themes/ink-wash.png
cp shared/src/commonMain/composeResources/drawable/bg_vintage_map.png website/public/themes/vintage-map.png
cp shared/src/commonMain/composeResources/drawable/bg_rice_paper.png website/public/themes/rice-paper.png
cp shared/src/commonMain/composeResources/drawable/bg_starry_night.png website/public/themes/starry-night.png
cp shared/src/commonMain/composeResources/drawable/bg_mountain_mist.png website/public/themes/mountain-mist.png
```

Render the classic preview as a CSS map pattern labeled “经典”; do not reuse the splash image or a flat color block.

- [ ] **Step 2: Write the failing deterministic-particle test, then implement the helper**

Extract `seededRandom(seed: number): () => number` and assert the first three values are stable and remain within `[0, 1)`.

```ts
export function seededRandom(seed: number): () => number {
  let value = seed >>> 0;
  return () => { value = (value * 1664525 + 1013904223) >>> 0; return value / 0x100000000; };
}
```

- [ ] **Step 3: Rebuild the hero and product narrative**

`HeroSection` receives `locale`, renders localized CTA links, uses CSS media queries instead of a synchronous setState effect, and only mounts the dynamic scene in a desktop-only wrapper. `ProductPaths` maps exact keys `footprint`, `discover`, `shanhe`, `profile` to `PathIcon` and localized copy without emoji.

- [ ] **Step 4: Implement LiveShanhe as a Server Component**

```tsx
const [attractionsResponse, storiesResponse] = await Promise.all([fetchAttractions(1, 4), fetchCommunityFeed(1, 3)]);
const attractions = composeAttractions(attractionsResponse?.data, locale, 4);
const stories = composeStories(storiesResponse?.data, locale, 3);
return <section className="section-shell"><SectionHeading eyebrow={copy.eyebrow} title={copy.title} /><div className="live-grid"><AttractionCard item={attractions[0]} featured locale={locale} /><div className="story-stack">{stories.slice(0, 2).map((item) => <StoryCard key={item.id} item={item} locale={locale} />)}</div></div></section>;
```

- [ ] **Step 5: Rebuild ThemeGallery with real assets and accessible controls**

Use buttons with `aria-pressed`, fixed-size responsive media, localized labels, Next Image `sizes`, and a wrapping or horizontally contained mobile control grid whose computed width never exceeds its container.

- [ ] **Step 6: Run tests, lint, and a development browser smoke check**

Run: `cd website && npm test && npm run lint`

Expected: tests PASS; no lint errors in home/three components.

- [ ] **Step 7: Commit the home page**

```bash
git add 'website/src/app/[locale]/page.tsx' website/src/components/home website/src/components/three/ChinaMapScene.tsx website/src/lib/seeded-random.ts website/src/lib/__tests__/seeded-random.test.ts website/public/themes
git commit -m "feat(web): rebuild the digital shanhe home page"
```

### Task 5: Attraction Experience

**Files:**
- Create: `website/src/components/content/AttractionCard.tsx`
- Create: `website/src/components/content/__tests__/AttractionCard.test.tsx`
- Modify: `website/src/app/[locale]/attractions/page.tsx`
- Modify: `website/src/app/[locale]/attractions/[id]/page.tsx`
- Modify: locale JSON files

**Interfaces:**
- Consumes: `composeAttractions`, `findEditorialAttraction`, `AttractionViewModel`, `PageHero`, `EditorialBadge`, `MediaFallback`.
- Produces: localized list and remote/editorial detail experiences.

- [ ] **Step 1: Add focused card rendering tests**

Assert live items show a visit count only when non-null; editorial items show `EditorialBadge` and never render a numeric visit label.

```tsx
render(<AttractionCard item={editorialAttraction} locale="en" />);
expect(screen.getByText('Editor’s pick')).toBeInTheDocument();
expect(screen.queryByText(/visits/)).not.toBeInTheDocument();
```

- [ ] **Step 2: Implement AttractionCard**

Use the stable href `/${locale}/attractions/${item.slug}`, Next Image when `image` exists, `MediaFallback` otherwise, and a consistent region/description/metric footer.

- [ ] **Step 3: Rebuild the list page**

Fetch once, compose at least 9 items, render `PageHero`, a featured first item, and a responsive card grid. The page must remain complete with `fetchAttractions()` returning null.

- [ ] **Step 4: Support live and editorial detail routes**

Try `findEditorialAttraction(id, locale)` first for the reserved editorial slugs; otherwise call `fetchAttraction(id)` and normalize it. Build localized metadata and JSON-LD from the resulting view model, render `notFound()` only when neither exists.

- [ ] **Step 5: Run tests, lint, and commit**

Run: `cd website && npm test -- src/components/content/__tests__/AttractionCard.test.tsx && npm run lint`

```bash
git add website/src/components/content/AttractionCard.tsx website/src/components/content/__tests__/AttractionCard.test.tsx 'website/src/app/[locale]/attractions' website/src/i18n/zh.json website/src/i18n/en.json
git commit -m "feat(web): rebuild attraction browsing and detail pages"
```

### Task 6: Community Story Experience

**Files:**
- Create: `website/src/components/content/StoryCard.tsx`
- Create: `website/src/components/content/__tests__/StoryCard.test.tsx`
- Modify: `website/src/app/[locale]/community/page.tsx`
- Modify: `website/src/app/[locale]/community/[id]/page.tsx`
- Modify: locale JSON files

**Interfaces:**
- Consumes: `composeStories`, `findEditorialStory`, `StoryViewModel`, `PageHero`, `EditorialBadge`, `MediaFallback`.
- Produces: localized feed and readable remote/editorial story pages.

- [ ] **Step 1: Add focused story-card tests**

```tsx
render(<StoryCard item={editorialStory} locale="zh" />);
expect(screen.getByText('编辑精选')).toBeInTheDocument();
expect(screen.queryByText(/赞/)).not.toBeInTheDocument();
```

- [ ] **Step 2: Implement StoryCard**

Use stable locale hrefs, a 4:3 cover or `MediaFallback`, visible author/excerpt, and metrics only when the source is live and values are non-null.

- [ ] **Step 3: Rebuild the feed page**

Fetch once, compose at least 8 items, use a featured-story layout followed by a responsive grid, and preserve a useful page when the API is unavailable.

- [ ] **Step 4: Rebuild story details**

Resolve editorial slugs locally before calling the API; produce localized metadata and Article JSON-LD; format timestamps with the active locale; preserve whitespace in story bodies without rendering raw HTML.

- [ ] **Step 5: Run tests, lint, and commit**

Run: `cd website && npm test -- src/components/content/__tests__/StoryCard.test.tsx && npm run lint`

```bash
git add website/src/components/content/StoryCard.tsx website/src/components/content/__tests__/StoryCard.test.tsx 'website/src/app/[locale]/community' website/src/i18n/zh.json website/src/i18n/en.json
git commit -m "feat(web): rebuild community story experiences"
```

### Task 7: Download, About, Metadata, and Bilingual Completeness

**Files:**
- Modify: `website/src/app/[locale]/download/page.tsx`
- Modify: `website/src/app/[locale]/about/page.tsx`
- Modify: `website/src/app/[locale]/layout.tsx`
- Modify: `website/src/app/sitemap.ts`
- Modify: `website/src/app/robots.ts`
- Modify: locale JSON files

**Interfaces:**
- Consumes: `BetaCTA`, `ProductPathCard` visual language, active locale.
- Produces: localized beta landing page, product story page, route metadata, sitemap and robots.

- [ ] **Step 1: Read metadata and deployment docs**

Run:

```bash
sed -n '1,260p' website/node_modules/next/dist/docs/01-app/01-getting-started/14-metadata-and-og-images.md
sed -n '1,240p' website/node_modules/next/dist/docs/01-app/01-getting-started/17-deploying.md
```

- [ ] **Step 2: Rebuild download as beta landing**

Render one H1, platform chips that say iOS/Android support is planned, a “Coming soon” status, product benefits, and `BetaCTA`. Assert there are no `href="#"`, `App Store`, or `Google Play` strings in the route.

- [ ] **Step 3: Rebuild about page**

Explain the map-first travel record, the four product paths, privacy-friendly positioning, and link to `https://github.com/faultaddr/MapChina` with external-link security attributes.

- [ ] **Step 4: Localize metadata and update sitemap**

Generate title/description from locale, keep `/zh` and `/en` URLs for all public pages, and retain `https://mapchina.com/sitemap.xml` in robots.

- [ ] **Step 5: Run stale-copy scans, tests, build, and commit**

Run:

```bash
cd website
if rg -n "href=\"#\"|App Store|Google Play|🗺️|👣|📖|🏅|🖌️|🎨" src; then exit 1; fi
npm test
npm run lint
npm run build
```

Expected: stale-copy scan returns no matches; tests, lint, and build PASS.

```bash
git add website/src/app website/src/i18n/zh.json website/src/i18n/en.json
git commit -m "feat(web): complete beta, about, and bilingual metadata"
```

### Task 8: Browser, Accessibility, and Responsive Verification

**Files:**
- Modify only files implicated by failures.
- Evidence: `/tmp/mapchina-web-before-*.png`, `/tmp/mapchina-web-after-*.png`.

**Interfaces:**
- Consumes: completed website.
- Produces: verified desktop/mobile routes and recorded evidence.

- [ ] **Step 1: Run the full automated baseline**

Run: `cd website && npm test && npm run lint && npm run build`

Expected: all commands exit 0.

- [ ] **Step 2: Start a temporary development server on 3100**

Run: `cd website && npm run dev -- --hostname 127.0.0.1 --port 3100`

Expected: Next.js reports ready at `http://127.0.0.1:3100`.

- [ ] **Step 3: Verify desktop routes in the in-app browser**

Inspect `/zh`, `/en`, attractions list/detail, community list/detail, download, and about. Confirm one H1, locale-preserving internal hrefs, non-empty content, no console errors, and the beta mailto recipient/subject/body.

- [ ] **Step 4: Verify 390x844 mobile routes**

Set the browser viewport to 390x844 and confirm `document.documentElement.scrollWidth === window.innerWidth` on every page, open/close the drawer, activate every theme, and verify static hero fallback.

- [ ] **Step 5: Verify keyboard and reduced motion**

Tab through navigation, theme controls, content cards, and beta CTA; confirm visible focus. Emulate or inspect reduced-motion behavior and ensure content remains visible without waiting for viewport animation.

- [ ] **Step 6: Save after screenshots and fix failures with focused tests**

Save desktop and mobile screenshots for home, attractions, community, and download to `/tmp/mapchina-web-after-*.png`. For each failure, add a focused regression test before the fix, rerun it, then rerun the full automated baseline.

- [ ] **Step 7: Commit verification-driven fixes**

```bash
git add website
git commit -m "fix(web): close responsive and accessibility gaps"
```

### Task 9: Android Project Regression

**Files:**
- Do not modify App files unless a website change demonstrably affected them.
- Evidence: `/tmp/mapchina-web-regression-before.png`, `/tmp/mapchina-web-regression-after.png`.

**Interfaces:**
- Consumes: repository after website changes.
- Produces: required AGENTS.md device evidence.

- [ ] **Step 1: Confirm an adb target and capture the pre-install screen**

Run:

```bash
adb devices
adb shell screencap -p /sdcard/mapchina-web-regression-before.png
adb pull /sdcard/mapchina-web-regression-before.png /tmp/mapchina-web-regression-before.png
```

Expected: at least one `device` target and a readable PNG.

- [ ] **Step 2: Build and install Android debug app**

Run:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew installDebug
```

Expected: `BUILD SUCCESSFUL` and APK installed.

- [ ] **Step 3: Launch and interact**

Run:

```bash
adb shell monkey -p com.mapchina.android -c android.intent.category.LAUNCHER 1
adb shell input tap 180 740
adb shell input swipe 200 650 200 250 350
adb shell uiautomator dump /sdcard/mapchina-web-regression.xml
adb pull /sdcard/mapchina-web-regression.xml /tmp/mapchina-web-regression.xml
```

Expected: App launches, remains responsive, and UI dump contains MapChina navigation content.

- [ ] **Step 4: Capture post-install evidence**

Run:

```bash
adb shell screencap -p /sdcard/mapchina-web-regression-after.png
adb pull /sdcard/mapchina-web-regression-after.png /tmp/mapchina-web-regression-after.png
```

Expected: readable screenshot showing the launched app.

### Task 10: Production Local Deployment and Completion Audit

**Files:**
- No source changes expected; modify only if production-only failures reveal a bug.

**Interfaces:**
- Consumes: verified build.
- Produces: running production website at `127.0.0.1:3100` and a requirement-by-requirement completion record.

- [ ] **Step 1: Stop temporary dev and companion services**

Stop the Next dev process and the brainstorming companion. Confirm port 3100 is free before production start; do not stop the unrelated service on port 3000.

- [ ] **Step 2: Build from a clean website state**

Run: `cd website && npm test && npm run lint && npm run build`

Expected: all commands exit 0 and `.next/BUILD_ID` exists.

- [ ] **Step 3: Start the production server**

Run: `cd website && npm run start -- --hostname 127.0.0.1 --port 3100`

Expected: Next.js production server remains running and reports ready.

- [ ] **Step 4: Verify production URLs and content**

Run:

```bash
curl -fsS http://127.0.0.1:3100/zh >/tmp/mapchina-zh.html
curl -fsS http://127.0.0.1:3100/en >/tmp/mapchina-en.html
rg -n "点亮足迹|参与内测" /tmp/mapchina-zh.html
rg -n "Light up|Join the beta" /tmp/mapchina-en.html
```

Expected: both curls exit 0 and both locale-specific copy scans match.

- [ ] **Step 5: Inspect final repository state**

Run:

```bash
git diff --check HEAD
git status --short
git log --oneline -8
```

Expected: no whitespace errors; only unrelated pre-existing user files remain unstaged; website commits are visible.

- [ ] **Step 6: Audit every specification success criterion**

Record evidence for: visual unity, equal content/beta priority, live/editorial resilience, removal of dead links and fake metrics, bilingual/SEO/accessibility checks, automated test/build passes, browser screenshots, Android device screenshots, and running production URL. Continue fixing if any evidence is missing or indirect.
