import type { Metadata } from 'next';
import Image from 'next/image';
import { notFound } from 'next/navigation';
import { getTranslations } from 'next-intl/server';
import type { SiteLocale } from '@/types';
import { fetchCommunityFeed, fetchCommunityPost } from '@/lib/api';
import { composeStories, findEditorialStory, resolveStoryDetail } from '@/lib/content';
import { StoryCard } from '@/components/content/StoryCard';
import { EditorialBadge } from '@/components/shared/EditorialBadge';
import { MediaFallback } from '@/components/shared/MediaFallback';
import { PageHero } from '@/components/shared/PageHero';
import { SectionHeading } from '@/components/shared/SectionHeading';

export const revalidate = 1800;

type Props = { params: Promise<{ locale: string; id: string }> };

async function getDetail(id: string, locale: SiteLocale) {
  const editorial = findEditorialStory(id, locale);
  if (editorial) return editorial;
  return resolveStoryDetail(id, await fetchCommunityPost(id), locale);
}

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { id, locale } = await params;
  const story = await getDetail(id, locale as SiteLocale);
  if (!story) return { title: locale === 'zh' ? '未找到故事 — MapChina' : 'Story not found — MapChina' };
  return { title: `${story.title} — MapChina`, description: story.excerpt, openGraph: { images: story.coverImage ? [story.coverImage] : [] } };
}

export default async function CommunityPostPage({ params }: Props) {
  const { id, locale } = await params;
  const siteLocale = locale as SiteLocale;
  const t = await getTranslations({ locale, namespace: 'community_page' });
  const [story, feedResponse] = await Promise.all([getDetail(id, siteLocale), fetchCommunityFeed(1, 5)]);
  if (!story) notFound();

  const related = composeStories(feedResponse?.data, siteLocale, 5).filter((candidate) => candidate.slug !== story.slug).slice(0, 3);
  const date = story.createdAt === null ? null : new Intl.DateTimeFormat(locale === 'zh' ? 'zh-CN' : 'en-US', { dateStyle: 'long' }).format(new Date(story.createdAt));
  const jsonLd = {
    '@context': 'https://schema.org',
    '@type': 'Article',
    headline: story.title,
    description: story.excerpt,
    author: { '@type': 'Person', name: story.author },
    ...(story.createdAt ? { datePublished: new Date(story.createdAt).toISOString() } : {}),
    ...(story.coverImage ? { image: story.coverImage } : {}),
  };

  return (
    <main>
      <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }} />
      <PageHero eyebrow={story.region ?? t('story_eyebrow')} title={story.title} description={story.excerpt}>
        <div className="flex flex-wrap items-center gap-3 text-sm text-white/65">
          <span>{story.author}</span>
          {date && <><span aria-hidden="true">·</span><span>{date}</span></>}
          {story.source === 'editorial' && <EditorialBadge locale={siteLocale} />}
        </div>
      </PageHero>

      <article className="paper-noise bg-paper py-16 md:py-24">
        <div className="mx-auto max-w-4xl px-6">
          <div className="relative mb-12 aspect-[16/9] overflow-hidden rounded-[2rem]">
            {story.coverImage ? <Image src={story.coverImage} alt={story.title} fill sizes="(min-width: 1024px) 896px, 100vw" className="object-cover" priority /> : <MediaFallback label={story.region ?? story.title} tone="gold" className="h-full" />}
          </div>
          <div className="mx-auto max-w-3xl">
            <p className="whitespace-pre-line font-heading text-xl leading-9 text-ink md:text-2xl md:leading-10">{story.content}</p>
            {(story.likeCount !== null || story.commentCount !== null) && (
              <div className="mt-12 flex gap-5 border-t border-border pt-6 text-sm text-ink-secondary">
                {story.likeCount !== null && <span>{t('like_count', { count: story.likeCount })}</span>}
                {story.commentCount !== null && <span>{t('comment_count', { count: story.commentCount })}</span>}
              </div>
            )}
          </div>
        </div>
      </article>

      <section className="bg-[#EEE9DE] py-20 md:py-28">
        <div className="mx-auto max-w-6xl px-6">
          <SectionHeading eyebrow={t('related_eyebrow')} title={t('related_title')} />
          <div className="grid gap-5 md:grid-cols-3">{related.map((candidate) => <StoryCard key={candidate.id} item={candidate} locale={siteLocale} />)}</div>
        </div>
      </section>
    </main>
  );
}
