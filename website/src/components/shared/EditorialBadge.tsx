import type { SiteLocale } from '@/types';

export function EditorialBadge({ locale }: { locale: SiteLocale }) {
  return (
    <span className="inline-flex rounded-full border border-gold/30 bg-gold/10 px-2.5 py-1 text-[0.68rem] font-semibold uppercase tracking-[0.16em] text-[#8B641F]">
      {locale === 'zh' ? '编辑精选' : 'Editor’s pick'}
    </span>
  );
}
