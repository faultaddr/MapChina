import type { SiteLocale } from '@/types';
import { getProductPaths } from '@/lib/home-content';
import { PathIcon } from '@/components/shared/PathIcon';
import { SectionHeading } from '@/components/shared/SectionHeading';

const headings = {
  zh: { eyebrow: '一段旅程，四条路径', title: '从抵达，到沉淀成自己的山河', description: 'MapChina 不只保存去过哪里，也让发现、记录和成长彼此相连。' },
  en: { eyebrow: 'One journey, four paths', title: 'From each arrival to a landscape of your own', description: 'MapChina connects discovery, memory, and progress—not just a list of places.' },
} satisfies Record<SiteLocale, { eyebrow: string; title: string; description: string }>;

export function ProductPaths({ locale }: { locale: SiteLocale }) {
  const text = headings[locale];
  return (
    <section className="paper-noise bg-paper py-24 md:py-32">
      <div className="mx-auto max-w-6xl px-6">
        <SectionHeading {...text} />
        <div className="grid gap-4 md:grid-cols-2">
          {getProductPaths(locale).map((path) => (
            <article key={path.key} className="group relative overflow-hidden rounded-[2rem] border border-border/70 bg-white/75 p-7 transition hover:-translate-y-1 hover:border-primary/30 hover:bg-white md:p-9">
              <span className="absolute right-6 top-4 font-heading text-6xl font-bold text-primary/[0.045] md:text-8xl">{path.index}</span>
              <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-dark text-primary-light shadow-lg shadow-dark/10"><PathIcon name={path.key} /></div>
              <p className="mt-7 text-xs font-semibold uppercase tracking-[0.2em] text-primary">{path.kicker}</p>
              <h3 className="mt-3 font-heading text-3xl font-bold text-ink">{path.title}</h3>
              <p className="mt-4 max-w-lg leading-7 text-ink-secondary">{path.description}</p>
            </article>
          ))}
        </div>
      </div>
    </section>
  );
}
