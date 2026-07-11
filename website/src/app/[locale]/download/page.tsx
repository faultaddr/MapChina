import type { Metadata } from 'next';
import { getTranslations } from 'next-intl/server';
import type { SiteLocale } from '@/types';
import { BetaCTA } from '@/components/shared/BetaCTA';
import { PageHero } from '@/components/shared/PageHero';
import { PathIcon } from '@/components/shared/PathIcon';

type Props = { params: Promise<{ locale: string }> };

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { locale } = await params;
  const t = await getTranslations({ locale, namespace: 'download_page' });
  return { title: t('meta_title'), description: t('meta_description') };
}

export default async function DownloadPage({ params }: Props) {
  const { locale } = await params;
  const siteLocale = locale as SiteLocale;
  const t = await getTranslations({ locale, namespace: 'download_page' });
  const features = [
    { icon: 'footprint' as const, title: t('feature_footprint_title'), body: t('feature_footprint_body') },
    { icon: 'discover' as const, title: t('feature_discover_title'), body: t('feature_discover_body') },
    { icon: 'shanhe' as const, title: t('feature_shanhe_title'), body: t('feature_shanhe_body') },
  ];

  return (
    <main>
      <PageHero eyebrow={t('eyebrow')} title={t('title')} description={t('description')}>
        <div className="flex flex-wrap gap-2 text-xs text-white/65">
          <span className="rounded-full border border-white/15 px-3 py-1.5">iOS · {t('planned')}</span>
          <span className="rounded-full border border-white/15 px-3 py-1.5">Android · {t('planned')}</span>
        </div>
      </PageHero>
      <section className="paper-noise bg-paper py-20 md:py-28">
        <div className="mx-auto max-w-6xl px-6">
          <div className="grid gap-5 md:grid-cols-3">
            {features.map((feature) => <article key={feature.title} className="rounded-[2rem] border border-border bg-white/75 p-7 md:p-9"><div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-dark text-primary-light"><PathIcon name={feature.icon} /></div><h2 className="mt-6 font-heading text-2xl font-bold">{feature.title}</h2><p className="mt-3 leading-7 text-ink-secondary">{feature.body}</p></article>)}
          </div>
          <div className="mt-16"><BetaCTA locale={siteLocale} /></div>
        </div>
      </section>
    </main>
  );
}
