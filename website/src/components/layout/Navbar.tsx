'use client';

import { useState } from 'react';
import Link from 'next/link';
import { useTranslations } from 'next-intl';
import { motion, useMotionValueEvent, useScroll, useTransform } from 'framer-motion';
import { usePathname } from 'next/navigation';
import MobileMenu from './MobileMenu';
import type { SiteLocale } from '@/types';
import { buildBetaMailto, localizeHref } from '@/lib/site-config';

export function Navbar({ locale }: { locale: SiteLocale }) {
  const t = useTranslations('nav');
  const [mobileOpen, setMobileOpen] = useState(false);
  const [scrolled, setScrolled] = useState(false);
  const pathname = usePathname();
  const { scrollY } = useScroll();
  const backgroundColor = useTransform(scrollY, [0, 90], ['rgba(11,19,36,0)', 'rgba(245,241,232,0.94)']);
  useMotionValueEvent(scrollY, 'change', (value) => setScrolled(value > 48));

  const links = [
    { href: localizeHref(locale, '/attractions'), label: t('attractions') },
    { href: localizeHref(locale, '/community'), label: t('community') },
    { href: localizeHref(locale, '/about'), label: t('about') },
  ];
  const betaHref = buildBetaMailto(locale);
  const foreground = scrolled ? 'text-ink' : 'text-white';

  return (
    <>
      <motion.header
        style={{ backgroundColor }}
        className={`fixed inset-x-0 top-0 z-50 backdrop-blur-md transition-shadow ${scrolled ? 'border-b border-border/70 shadow-sm' : ''}`}
      >
        <nav className="mx-auto flex max-w-6xl items-center justify-between px-6 py-4" aria-label={t('main_label')}>
          <Link href={localizeHref(locale, '/')} className={`flex items-center gap-3 rounded-lg ${foreground}`}>
            <div className="flex h-10 w-10 items-center justify-center rounded-xl border border-white/15 bg-primary text-sm font-bold text-white shadow-lg shadow-primary/20">
              山
            </div>
            <span className="font-heading text-lg font-bold tracking-tight">MapChina</span>
          </Link>

          <div className="hidden items-center gap-8 md:flex">
            {links.map((link) => (
              <Link
                key={link.href}
                href={link.href}
                aria-current={pathname === link.href || pathname.startsWith(`${link.href}/`) ? 'page' : undefined}
                className={`rounded-md text-sm font-medium transition-colors hover:text-primary ${foreground}`}
              >
                {link.label}
              </Link>
            ))}
            <a
              href={betaHref}
              className="rounded-full bg-gold px-5 py-2.5 text-sm font-semibold text-dark transition-all hover:-translate-y-0.5 hover:bg-[#ddb667] hover:shadow-lg"
            >
              {t('beta')}
            </a>
          </div>

          <button
            className={`flex items-center justify-center rounded-lg p-2 md:hidden ${foreground}`}
            onClick={() => setMobileOpen(true)}
            aria-label={t('open_menu')}
          >
            <svg width="24" height="24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M3 12h18M3 6h18M3 18h18" />
            </svg>
          </button>
        </nav>
      </motion.header>

      <MobileMenu
        open={mobileOpen}
        onClose={() => setMobileOpen(false)}
        links={links}
        betaLabel={t('beta')}
        betaHref={betaHref}
        closeLabel={t('close_menu')}
      />
    </>
  );
}
