import type { Metadata } from 'next';
import { getTranslations } from 'next-intl/server';
import type { SiteLocale } from '@/types';
import { fetchAttractions } from '@/lib/api';
import { composeAttractions } from '@/lib/content';
import { AttractionCard } from '@/components/content/AttractionCard';
import { PageHero } from '@/components/shared/PageHero';
import { SectionHeading } from '@/components/shared/SectionHeading';

export const revalidate = 3600;

type Props = { params: Promise<{ locale: string }> };

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { locale } = await params;
  const t = await getTranslations({ locale, namespace: 'attractions' });
  return { title: t('meta_title'), description: t('meta_description') };
}

export default async function AttractionsPage({ params }: Props) {
  const { locale } = await params;
  const siteLocale = locale as SiteLocale;
  const t = await getTranslations({ locale, namespace: 'attractions' });
  const response = await fetchAttractions(1, 12);
  const attractions = composeAttractions(response?.data, siteLocale, 9);

  return (
    <main>
      <PageHero eyebrow={t('eyebrow')} title={t('title')} description={t('description')} />
      <section className="paper-noise bg-paper py-20 md:py-28">
        <div className="mx-auto max-w-6xl px-6">
          <SectionHeading eyebrow={t('featured_eyebrow')} title={t('featured_title')} description={t('featured_description')} />
          <AttractionCard item={attractions[0]} locale={siteLocale} featured />

          <div className="mt-20">
            <SectionHeading eyebrow={t('all_eyebrow')} title={t('all_title')} />
            <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
              {attractions.slice(1).map((item) => <AttractionCard key={item.id} item={item} locale={siteLocale} />)}
            </div>
          </div>
        </div>
      </section>
    </main>
  );
}
