'use client';

import { useTranslations } from 'next-intl';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import type { SiteLocale } from '@/types';
import { BETA_EMAIL, buildBetaMailto, localizeHref } from '@/lib/site-config';

export default function Footer({ locale }: { locale: SiteLocale }) {
  const t = useTranslations('footer');
  const pathname = usePathname();
  const pathWithoutLocale = pathname.replace(/^\/(?:zh|en)(?=\/|$)/, '') || '/';

  return (
    <footer className="border-t border-white/10 bg-dark py-14 text-white">
      <div className="mx-auto grid max-w-6xl gap-10 px-6 md:grid-cols-[1.2fr_.8fr_.8fr]">
        <div>
          <Link href={localizeHref(locale, '/')} className="font-heading text-2xl font-bold">MapChina</Link>
          <p className="mt-4 max-w-sm leading-7 text-white/55">{t('tagline')}</p>
        </div>
        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.24em] text-primary-light">{t('explore')}</p>
          <div className="mt-4 flex flex-col gap-3 text-sm text-white/70">
            <Link href={localizeHref(locale, '/attractions')} className="hover:text-white">{t('attractions')}</Link>
            <Link href={localizeHref(locale, '/community')} className="hover:text-white">{t('community')}</Link>
            <Link href={localizeHref(locale, '/about')} className="hover:text-white">{t('about')}</Link>
          </div>
        </div>
        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.24em] text-gold">{t('beta')}</p>
          <a href={buildBetaMailto(locale)} className="mt-4 block text-sm text-white/70 hover:text-white">{BETA_EMAIL}</a>
          <div className="mt-5 flex gap-4 text-sm">
            <Link href={localizeHref('zh', pathWithoutLocale)} className={locale === 'zh' ? 'text-white' : 'text-white/45'}>中文</Link>
            <Link href={localizeHref('en', pathWithoutLocale)} className={locale === 'en' ? 'text-white' : 'text-white/45'}>English</Link>
          </div>
        </div>
      </div>
      <div className="mx-auto mt-12 max-w-6xl border-t border-white/10 px-6 pt-6 text-xs text-white/35">{t('copyright')}</div>
    </footer>
  );
}
