import Image from 'next/image';
import Link from 'next/link';
import type { AttractionViewModel, SiteLocale } from '@/types';
import { EditorialBadge } from '@/components/shared/EditorialBadge';
import { MediaFallback } from '@/components/shared/MediaFallback';
import { localizeHref } from '@/lib/site-config';

type AttractionCardProps = {
  item: AttractionViewModel;
  locale: SiteLocale;
  featured?: boolean;
};

export function AttractionCard({ item, locale, featured = false }: AttractionCardProps) {
  const visitLabel = item.visitCount === null
    ? null
    : locale === 'zh' ? `${item.visitCount} 次访问` : `${item.visitCount} visits`;

  return (
    <Link
      href={localizeHref(locale, `/attractions/${item.slug}`)}
      className={`group block overflow-hidden rounded-[1.7rem] border border-border/75 bg-surface shadow-[0_18px_55px_rgba(25,45,42,0.06)] transition hover:-translate-y-1 hover:border-primary/35 hover:shadow-[0_22px_60px_rgba(25,45,42,0.12)] ${featured ? 'md:grid md:grid-cols-[1.15fr_.85fr]' : ''}`}
    >
      <div className={`relative overflow-hidden ${featured ? 'min-h-64 md:min-h-80' : 'aspect-[4/3]'}`}>
        {item.image ? (
          <Image src={item.image} alt={item.name} fill sizes={featured ? '(min-width: 768px) 55vw, 100vw' : '(min-width: 1024px) 30vw, 50vw'} className="object-cover transition duration-700 group-hover:scale-105" />
        ) : (
          <MediaFallback label={`${item.name} · ${item.region}`} tone={item.source === 'editorial' ? 'jade' : 'night'} className="h-full min-h-inherit" />
        )}
      </div>
      <div className={`flex flex-col ${featured ? 'justify-center p-7 md:p-10' : 'p-5'}`}>
        <div className="flex flex-wrap items-center gap-2 text-xs text-ink-secondary">
          <span>{item.region}</span>
          {item.level && <span className="rounded-full bg-primary/8 px-2 py-1 font-semibold text-primary">{item.level}</span>}
          {item.source === 'editorial' && <EditorialBadge locale={locale} />}
        </div>
        <h3 className={`mt-4 font-heading font-bold leading-tight text-ink group-hover:text-primary ${featured ? 'text-3xl md:text-4xl' : 'text-xl'}`}>{item.name}</h3>
        <p className={`mt-3 text-sm leading-6 text-ink-secondary ${featured ? 'md:text-base md:leading-7' : 'line-clamp-2'}`}>{item.description}</p>
        <div className="mt-5 flex items-center justify-between text-xs text-ink-secondary">
          {visitLabel ? <span>{visitLabel}</span> : <span>{locale === 'zh' ? '去地图里看看' : 'Explore on the map'}</span>}
          <span className="font-semibold text-primary" aria-hidden="true">↗</span>
        </div>
      </div>
    </Link>
  );
}
