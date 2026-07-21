import type { Metadata } from 'next';
import { getTranslations } from 'next-intl/server';
import type { SiteLocale } from '@/types';
import { getProductPaths } from '@/lib/home-content';
import { BetaCTA } from '@/components/shared/BetaCTA';
import { PageHero } from '@/components/shared/PageHero';
import { PathIcon } from '@/components/shared/PathIcon';

type Props = { params: Promise<{ locale: string }> };

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { locale } = await params;
  const t = await getTranslations({ locale, namespace: 'about_page' });
  return { title: t('meta_title'), description: t('meta_description') };
}

export default async function AboutPage({ params }: Props) {
  const { locale } = await params;
  const siteLocale = locale as SiteLocale;
  const t = await getTranslations({ locale, namespace: 'about_page' });
  return (
    <main>
      <PageHero eyebrow={t('eyebrow')} title={t('title')} description={t('description')} />
      <section className="paper-noise bg-paper py-20 md:py-28">
        <div className="mx-auto grid max-w-6xl gap-12 px-6 lg:grid-cols-[.8fr_1.2fr]">
          <div><p className="text-xs font-semibold uppercase tracking-[0.24em] text-primary">{t('belief_eyebrow')}</p><h2 className="mt-4 font-heading text-4xl font-bold leading-tight">{t('belief_title')}</h2></div>
          <div className="space-y-6 text-lg leading-9 text-ink-secondary"><p>{t('belief_body_1')}</p><p>{t('belief_body_2')}</p><a href="https://github.com/faultaddr/MapChina" target="_blank" rel="noreferrer" className="inline-flex font-semibold text-primary hover:underline">{t('github')} ↗</a></div>
        </div>

        <div className="mx-auto mt-20 grid max-w-6xl gap-5 px-6 md:grid-cols-2">
          {getProductPaths(siteLocale).map((path) => <article key={path.key} className="rounded-[2rem] border border-border bg-white/75 p-7"><div className="flex items-center gap-4"><div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-dark text-primary-light"><PathIcon name={path.key} /></div><div><p className="text-xs font-semibold uppercase tracking-[0.18em] text-primary">{path.kicker}</p><h3 className="mt-1 font-heading text-2xl font-bold">{path.title}</h3></div></div><p className="mt-5 leading-7 text-ink-secondary">{path.description}</p></article>)}
        </div>

        <div className="mx-auto mt-20 max-w-6xl px-6"><BetaCTA locale={siteLocale} /></div>
      </section>
    </main>
  );
}
