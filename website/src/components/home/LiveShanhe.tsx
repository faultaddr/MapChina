import type { SiteLocale } from '@/types';
import { fetchAttractions, fetchCommunityFeed } from '@/lib/api';
import { composeAttractions, composeStories } from '@/lib/content';
import { localizeHref } from '@/lib/site-config';
import { AttractionCard } from '@/components/content/AttractionCard';
import { StoryCard } from '@/components/content/StoryCard';
import { SectionHeading } from '@/components/shared/SectionHeading';

const copy = {
  zh: { eyebrow: '此刻，山河正在发生', title: '从值得抵达的地方，读到真实旅程', description: '一边发现下一站，一边看看旅行者如何把它写进自己的地图。', action: '浏览全部' },
  en: { eyebrow: 'Across the landscape right now', title: 'Find a place worth reaching—and the stories it inspired', description: 'Discover the next destination while seeing how other travelers made it part of their map.', action: 'Explore all' },
} satisfies Record<SiteLocale, { eyebrow: string; title: string; description: string; action: string }>;

export async function LiveShanhe({ locale }: { locale: SiteLocale }) {
  const [attractionResponse, storyResponse] = await Promise.all([
    fetchAttractions(1, 4),
    fetchCommunityFeed(1, 3),
  ]);
  const attractions = composeAttractions(attractionResponse?.data, locale, 4);
  const stories = composeStories(storyResponse?.data, locale, 3);
  const text = copy[locale];

  return (
    <section className="bg-[#EEE9DE] py-24 md:py-32">
      <div className="mx-auto max-w-6xl px-6">
        <SectionHeading {...text} action={{ href: localizeHref(locale, '/community'), label: text.action }} />
        <div className="grid gap-5 lg:grid-cols-[1.2fr_.8fr]">
          <AttractionCard item={attractions[0]} locale={locale} featured />
          <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-1">
            {stories.slice(0, 2).map((story) => <StoryCard key={story.id} item={story} locale={locale} />)}
          </div>
        </div>
        <div className="mt-5 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {attractions.slice(1, 4).map((attraction) => <AttractionCard key={attraction.id} item={attraction} locale={locale} />)}
        </div>
      </div>
    </section>
  );
}
