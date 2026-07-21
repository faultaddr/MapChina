import Image from 'next/image';
import Link from 'next/link';
import type { SiteLocale, StoryViewModel } from '@/types';
import { EditorialBadge } from '@/components/shared/EditorialBadge';
import { MediaFallback } from '@/components/shared/MediaFallback';
import { localizeHref } from '@/lib/site-config';

type StoryCardProps = {
  item: StoryViewModel;
  locale: SiteLocale;
  featured?: boolean;
};

export function StoryCard({ item, locale, featured = false }: StoryCardProps) {
  const likes = item.likeCount === null ? null : locale === 'zh' ? `${item.likeCount} 赞` : `${item.likeCount} likes`;
  const comments = item.commentCount === null ? null : locale === 'zh' ? `${item.commentCount} 评论` : `${item.commentCount} comments`;

  return (
    <Link
      href={localizeHref(locale, `/community/${item.slug}`)}
      className={`group block overflow-hidden rounded-[1.7rem] border border-border/75 bg-surface shadow-[0_18px_55px_rgba(25,45,42,0.06)] transition hover:-translate-y-1 hover:border-primary/35 hover:shadow-[0_22px_60px_rgba(25,45,42,0.12)] ${featured ? 'md:grid md:grid-cols-[1.15fr_.85fr]' : ''}`}
    >
      <div className={`relative overflow-hidden ${featured ? 'min-h-72 md:min-h-[25rem]' : 'aspect-[4/3]'}`}>
        {item.coverImage ? (
          <Image src={item.coverImage} alt={item.title} fill sizes={featured ? '(min-width: 768px) 55vw, 100vw' : '(min-width: 1024px) 30vw, 50vw'} className="object-cover transition duration-700 group-hover:scale-105" />
        ) : (
          <MediaFallback label={item.region ?? item.title} tone={item.source === 'editorial' ? 'gold' : 'jade'} className="h-full min-h-inherit" />
        )}
      </div>
      <div className={`flex flex-col ${featured ? 'justify-center p-7 md:p-10' : 'p-5'}`}>
        <div className="flex flex-wrap items-center gap-2 text-xs text-ink-secondary">
          <span>{item.author}</span>
          {item.region && <><span aria-hidden="true">·</span><span>{item.region}</span></>}
          {item.source === 'editorial' && <EditorialBadge locale={locale} />}
        </div>
        <h3 className={`mt-4 font-heading font-bold leading-tight text-ink group-hover:text-primary ${featured ? 'text-3xl md:text-4xl' : 'text-xl'}`}>{item.title}</h3>
        <p className={`mt-3 text-sm leading-6 text-ink-secondary ${featured ? 'md:text-base md:leading-7' : 'line-clamp-2'}`}>{item.excerpt}</p>
        <div className="mt-5 flex min-h-5 items-center gap-4 text-xs text-ink-secondary">
          {likes && <span>{likes}</span>}
          {comments && <span>{comments}</span>}
          {!likes && !comments && <span>{locale === 'zh' ? '读这段旅程' : 'Read this journey'}</span>}
        </div>
      </div>
    </Link>
  );
}
