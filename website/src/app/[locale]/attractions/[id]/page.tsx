import type { Metadata } from 'next';
import { notFound } from 'next/navigation';
import { getTranslations } from 'next-intl/server';
import type { SiteLocale } from '@/types';
import { fetchAttraction, fetchAttractions } from '@/lib/api';
import { composeAttractions, findEditorialAttraction, resolveAttractionDetail } from '@/lib/content';
import { AttractionCard } from '@/components/content/AttractionCard';
import { EditorialBadge } from '@/components/shared/EditorialBadge';
import { MediaFallback } from '@/components/shared/MediaFallback';
import { PageHero } from '@/components/shared/PageHero';
import { SectionHeading } from '@/components/shared/SectionHeading';

export const revalidate = 1800;

type Props = { params: Promise<{ locale: string; id: string }> };

async function getDetail(id: string, locale: SiteLocale) {
  const editorial = findEditorialAttraction(id, locale);
  if (editorial) return editorial;
  return resolveAttractionDetail(id, await fetchAttraction(id), locale);
}

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { id, locale } = await params;
  const item = await getDetail(id, locale as SiteLocale);
  if (!item) return { title: locale === 'zh' ? '未找到景点 — MapChina' : 'Attraction not found — MapChina' };
  return { title: `${item.name} — MapChina`, description: item.description };
}

export default async function AttractionDetailPage({ params }: Props) {
  const { id, locale } = await params;
  const siteLocale = locale as SiteLocale;
  const t = await getTranslations({ locale, namespace: 'attractions' });
  const [item, relatedResponse] = await Promise.all([getDetail(id, siteLocale), fetchAttractions(1, 6)]);
  if (!item) notFound();

  const related = composeAttractions(relatedResponse?.data, siteLocale, 6).filter((candidate) => candidate.slug !== item.slug).slice(0, 3);
  const jsonLd = {
    '@context': 'https://schema.org',
    '@type': 'TouristAttraction',
    name: item.name,
    description: item.description,
    ...(item.coordinates ? { geo: { '@type': 'GeoCoordinates', latitude: item.coordinates.latitude, longitude: item.coordinates.longitude } } : {}),
  };

  return (
    <main>
      <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }} />
      <PageHero eyebrow={item.region} title={item.name} description={item.description}>
        <div className="flex flex-wrap items-center gap-3 text-sm text-white/65">
          {item.source === 'editorial' && <EditorialBadge locale={siteLocale} />}
          {item.level && <span className="rounded-full border border-white/15 px-3 py-1.5">{item.level}</span>}
          {item.visitCount !== null && <span>{t('visit_count', { count: item.visitCount })}</span>}
        </div>
      </PageHero>

      <section className="paper-noise bg-paper py-20 md:py-28">
        <div className="mx-auto grid max-w-6xl gap-10 px-6 lg:grid-cols-[1.25fr_.75fr]">
          <MediaFallback label={`${item.name} · ${item.region}`} tone={item.source === 'editorial' ? 'jade' : 'night'} className="aspect-[16/10] rounded-[2rem]" />
          <div className="flex flex-col justify-center rounded-[2rem] border border-border bg-white/70 p-7 md:p-10">
            <p className="text-xs font-semibold uppercase tracking-[0.24em] text-primary">{t('detail_eyebrow')}</p>
            <h2 className="mt-4 font-heading text-3xl font-bold">{t('detail_title')}</h2>
            <p className="mt-5 whitespace-pre-line leading-8 text-ink-secondary">{item.description}</p>
            <dl className="mt-8 grid grid-cols-2 gap-5 border-t border-border pt-6 text-sm">
              <div><dt className="text-ink-secondary">{t('region_label')}</dt><dd className="mt-1 font-semibold">{item.region}</dd></div>
              <div><dt className="text-ink-secondary">{t('source_label')}</dt><dd className="mt-1 font-semibold">{item.source === 'live' ? t('source_live') : t('source_editorial')}</dd></div>
            </dl>
          </div>
        </div>
      </section>

      <section className="bg-[#EEE9DE] py-20 md:py-28">
        <div className="mx-auto max-w-6xl px-6">
          <SectionHeading eyebrow={t('related_eyebrow')} title={t('related_title')} />
          <div className="grid gap-5 md:grid-cols-3">{related.map((candidate) => <AttractionCard key={candidate.id} item={candidate} locale={siteLocale} />)}</div>
        </div>
      </section>
    </main>
  );
}
