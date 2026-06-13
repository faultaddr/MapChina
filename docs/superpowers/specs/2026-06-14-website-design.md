# MapChina Official Website Design Spec

## Overview

Build a visually stunning official website for MapChina using Next.js 15, Framer Motion, and React Three Fiber. The site serves as both a product showcase (driving app downloads) and a content platform (attractions, journals from API), with zh/en bilingual support and self-hosted deployment.

## 1. Information Architecture

### Sitemap

```
/                    → Home (Hero + Features + Download CTA)
/attractions         → Attraction list (API, ISR)
/attractions/[id]    → Attraction detail (API, ISR)
/journals            → Journal list (API, ISR)
/journals/[id]       → Journal detail (API, ISR)
/about               → About MapChina
/download            → Download page (App Store / Google Play links)
/zh/*                → Chinese version (default)
/en/*                → English version
```

### Home Page Scroll Narrative

1. **Hero** — 3D China map particle animation + slogan "用地图点亮你的中国足迹" + download CTA
2. **Feature Showcase** — 6 feature cards (Map Explore / Footprint / Journals / Badges / Carvings / Themes), stagger-reveal on scroll with parallax
3. **Theme Gallery** — 6 map themes live-switching preview (Ink Wash / Vintage Map / Starry Night / Mountain Mist / Rice Paper / Classic)
4. **Community Picks** — Popular journals and attractions from API, masonry card layout
5. **Download CTA** — Full-width dark section + dual-platform download buttons

### Navigation

- Fixed top navbar: Logo | Attractions | Journals | About | Download button
- Glassmorphism backdrop on scroll (> 100px)
- Mobile hamburger menu

## 2. Visual System

### Color Palette (from App MapChinaColors)

| Role | Value | Usage |
|------|-------|-------|
| Primary | `#0D7377` Jade green | CTAs, links, emphasis |
| Primary Light | `#14A3A8` | Gradient end, hover states |
| Gold | `#C8963E` Cinnabar gold | Badges, premium feel |
| Background | `#F8F6F1` Rice paper | Page background |
| Surface | `#FFFFFF` | Cards, panels |
| Text | `#1C1C1E` Ink | Body text |
| Text Secondary | `#4A4A4F` | Secondary text |
| Dark | `#0F1428` Starry night | Hero background, dark sections |

### Typography

- Chinese: Noto Serif SC (headings), Noto Sans SC (body)
- English: Playfair Display (headings), Inter (body)
- Loaded via `next/font`, only required weights

### Hero 3D Animation (R3F)

- China map outline rendered as particle point cloud (~2000 points) along boundary lines
- Particles pulse (size/opacity breathing) + slow Y-axis rotation (±15°)
- Background: dark blue gradient (`#0F1428` → `#0D7377`)
- Particle color gradient: jade green to gold
- Subtle glow scatter at base
- Mobile fallback: static SVG + CSS animation (no Three.js loaded)

### Scroll Animations (Framer Motion)

- Feature cards: slide-up + opacity fade-in on viewport enter, 100ms stagger
- Theme gallery: horizontal swipe with 3D perspective flip
- Community picks: masonry layout with staggered fade-in
- Parallax: Hero background 0.5x scroll speed, foreground text 1.2x
- Count-up: statistics numbers animate from 0 to target on viewport entry

### Page Transitions

- Route changes: fade (150ms), content slides up 20px → 0
- Image loading: blur placeholder (Next.js Image built-in)

### Micro-interactions

- Button hover: slight float + deeper shadow
- Card hover: border transparent → Primary, scale 1.02
- Navbar: transparent → glassmorphism (backdrop-blur) on scroll > 100px

## 3. Technical Architecture

### Tech Stack

- Next.js 15 App Router
- Framer Motion (scroll-driven animations, page transitions)
- React Three Fiber + Drei (3D Hero)
- TailwindCSS v4
- next-intl (i18n)
- Vitest + React Testing Library + Playwright (testing)

### Project Structure

```
website/
├── next.config.ts
├── tailwind.config.ts
├── public/
│   └── images/
├── src/
│   ├── app/
│   │   ├── [locale]/
│   │   │   ├── layout.tsx
│   │   │   ├── page.tsx
│   │   │   ├── attractions/
│   │   │   ├── journals/
│   │   │   ├── about/
│   │   │   └── download/
│   │   ├── layout.tsx
│   │   └── globals.css
│   ├── components/
│   │   ├── layout/        # Navbar, Footer, MobileMenu
│   │   ├── home/          # Hero, FeatureCards, ThemeGallery, CommunityPicks, DownloadCTA
│   │   ├── shared/        # AnimatedSection, ParallaxWrapper, CountUp, ThemePreview
│   │   └── three/         # ChinaMapScene, ParticleField (R3F)
│   ├── lib/
│   │   ├── api.ts
│   │   └── constants.ts
│   ├── i18n/
│   │   ├── config.ts
│   │   ├── zh.json
│   │   └── en.json
│   └── types/
│       └── index.ts
```

### Data Flow

- **Home**: SSG, feature/theme/download content in i18n JSON
- **Attraction/Journal lists**: ISR, revalidate = 3600s, fetched from App API
- **Attraction/Journal detail**: ISR, revalidate = 1800s, `generateStaticParams` pre-generates popular entries
- **API client**: unified in `lib/api.ts` with error handling and timeout
- **ISR caching**: self-hosted environment uses Next.js built-in filesystem cache (no Redis needed); if scaling beyond single instance, add Redis as shared cache layer later

### API Endpoints (App Backend)

The website reads from the existing MapChina API:

| Endpoint | Method | Params | Used By |
|----------|--------|--------|---------|
| `/api/attractions` | GET | `?page=1&limit=20&sort=popular` | Attraction list, Community Picks |
| `/api/attractions/:id` | GET | — | Attraction detail |
| `/api/journals` | GET | `?page=1&limit=20&sort=popular` | Journal list, Community Picks |
| `/api/journals/:id` | GET | — | Journal detail |

Response format: `{ success: boolean, data: T, error?: string, meta: { total, page, limit } }`

`sort=popular` returns items ranked by a combination of view count + like count + recency, as defined by the App backend.

### Theme Gallery Implementation

6 theme previews are pre-rendered screenshots of each map theme (exported from the App), displayed as `<Image>` components with a CSS crossfade transition on selection. No Canvas rendering on the website — this keeps the gallery lightweight and avoids duplicating map rendering logic.

### Environment Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `NEXT_PUBLIC_API_URL` | App backend base URL | `https://api.mapchina.com` |
| `REVALIDATE_INTERVAL_LIST` | ISR revalidate for list pages (seconds) | `3600` |
| `REVALIDATE_INTERVAL_DETAIL` | ISR revalidate for detail pages (seconds) | `1800` |

### R3F Loading Strategy

- `ChinaMapScene` loaded via `next/dynamic` + `ssr: false`
- Loading state: SVG outline placeholder + breathing animation
- Mobile (< 768px): skip R3F entirely, render SVG + CSS animation
- Tree-shake: only import `@react-three/fiber` + `@react-three/drei` + `three` core

### i18n

- `next-intl`, route format `/zh/...` / `/en/...`
- Middleware detects `Accept-Language` for auto-redirect
- All copy in `i18n/zh.json` and `i18n/en.json`

### Self-Hosted Deployment

- `next.config.ts`: `output: 'standalone'`
- Docker image based on `node:22-alpine`
- Image optimization via `sharp` (Next.js built-in), no remote optimizer

### Performance Targets

| Metric | Target |
|--------|--------|
| Lighthouse Performance | ≥ 90 (desktop) |
| LCP | ≤ 2.5s |
| CLS | ≤ 0.1 |
| FID | ≤ 100ms |
| First-screen JS (gzip) | ≤ 200KB (excluding async Three.js) |

## 4. SEO, Accessibility & Resilience

### SEO

- `generateMetadata` for dynamic `<title>` / `<description>` / `og:image` per page
- JSON-LD structured data on detail pages (TouristAttraction / Article schema)
- Auto-generated `sitemap.xml` and `robots.txt` via App Router conventions
- ISR pages return `Cache-Control: s-maxage=3600, stale-while-revalidate`

### Accessibility (a11y)

- Respect `prefers-reduced-motion`: R3F degrades to static image, scroll animations degrade to instant display
- Color contrast meets WCAG AA (jade `#0D7377` on rice paper `#F8F6F1` = 5.2:1 ratio, passes)
- All images have `alt`, icon buttons have `aria-label`
- Keyboard navigable with visible focus styles

### Resilience & Degradation

- API failure: list pages show cached data + stale warning; detail pages show 404
- R3F failure (no WebGL): auto-fallback to SVG + CSS animation
- Image failure: Next.js Image blur placeholder as fallback
- Slow mobile network: skeleton loading, no blank states

### Testing Strategy

- **Unit**: Vitest — `lib/api.ts`, i18n utils, data transforms
- **Component**: React Testing Library — Navbar, ThemeSwitcher, Download buttons, interactive components
- **E2E**: Playwright — home scroll → features → download; attraction list → detail
- **Visual regression**: Playwright screenshot diff on home and key templates
