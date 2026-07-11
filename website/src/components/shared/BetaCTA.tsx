import type { SiteLocale } from '@/types';
import { buildBetaMailto } from '@/lib/site-config';

type BetaCTAProps = {
  locale: SiteLocale;
  variant?: 'panel' | 'compact';
};

const copy = {
  zh: {
    eyebrow: '即将上线',
    title: '一起把走过的地方，沉淀成自己的山河',
    body: '参与首批体验，告诉我们你最想点亮的旅程。',
    action: '参与内测',
  },
  en: {
    eyebrow: 'Coming soon',
    title: 'Turn every journey into your own digital landscape',
    body: 'Join the first group of explorers and help shape MapChina.',
    action: 'Join the beta',
  },
} satisfies Record<SiteLocale, { eyebrow: string; title: string; body: string; action: string }>;

export function BetaCTA({ locale, variant = 'panel' }: BetaCTAProps) {
  const text = copy[locale];

  return (
    <section
      className={variant === 'compact'
        ? 'rounded-[2rem] border border-white/10 bg-white/5 p-7'
        : 'relative overflow-hidden rounded-[2.5rem] border border-white/10 bg-dark px-7 py-16 text-center text-white md:px-14 md:py-20'}
    >
      <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_top_right,rgba(20,163,168,0.22),transparent_42%),radial-gradient(circle_at_bottom_left,rgba(200,150,62,0.16),transparent_36%)]" />
      <div className="relative mx-auto max-w-3xl">
        <p className="mb-4 text-xs font-semibold uppercase tracking-[0.28em] text-gold">{text.eyebrow}</p>
        <h2 className="font-heading text-3xl font-bold leading-tight md:text-5xl">{text.title}</h2>
        <p className="mx-auto mt-5 max-w-2xl text-base leading-7 text-white/65 md:text-lg">{text.body}</p>
        <a
          href={buildBetaMailto(locale)}
          className="mt-8 inline-flex min-h-12 items-center justify-center rounded-full bg-gold px-7 py-3 font-semibold text-dark transition hover:-translate-y-0.5 hover:bg-[#ddb667] focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-gold"
        >
          {text.action}
        </a>
      </div>
    </section>
  );
}
