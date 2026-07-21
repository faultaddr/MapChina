import type { Metadata } from 'next';
import { getTranslations } from 'next-intl/server';
import type { SiteLocale } from '@/types';
import { fetchCommunityFeed } from '@/lib/api';
import { composeStories } from '@/lib/content';
import { StoryCard } from '@/components/content/StoryCard';
import { PageHero } from '@/components/shared/PageHero';
import { SectionHeading } from '@/components/shared/SectionHeading';

export const revalidate = 3600;

type Props = { params: Promise<{ locale: string }> };

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { locale } = await params;
  const t = await getTranslations({ locale, namespace: 'community_page' });
  return { title: t('meta_title'), description: t('meta_description') };
}

export default async function CommunityPage({ params }: Props) {
  const { locale } = await params;
  const siteLocale = locale as SiteLocale;
  const t = await getTranslations({ locale, namespace: 'community_page' });
  const response = await fetchCommunityFeed(1, 12);
  const stories = composeStories(response?.data, siteLocale, 8);

  return (
    <main>
      <PageHero eyebrow={t('eyebrow')} title={t('title')} description={t('description')} />
      <section className="paper-noise bg-paper py-20 md:py-28">
        <div className="mx-auto max-w-6xl px-6">
          <SectionHeading eyebrow={t('featured_eyebrow')} title={t('featured_title')} description={t('featured_description')} />
          <StoryCard item={stories[0]} locale={siteLocale} featured />
          <div className="mt-20">
            <SectionHeading eyebrow={t('all_eyebrow')} title={t('all_title')} />
            <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
              {stories.slice(1).map((story) => <StoryCard key={story.id} item={story} locale={siteLocale} />)}
            </div>
          </div>
        </div>
      </section>
    </main>
  );
}
