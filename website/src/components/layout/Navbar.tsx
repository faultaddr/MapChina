'use client';

import { useState } from 'react';
import Link from 'next/link';
import { useTranslations } from 'next-intl';
import { motion, useScroll, useTransform } from 'framer-motion';
import MobileMenu from './MobileMenu';

export function Navbar() {
  const t = useTranslations('nav');
  const [mobileOpen, setMobileOpen] = useState(false);
  const { scrollY } = useScroll();
  const bgAlpha = useTransform(scrollY, [0, 100], [0, 0.9]);

  const links = [
    { href: '/attractions', label: t('attractions') },
    { href: '/journals', label: t('journals') },
    { href: '/about', label: t('about') },
  ];

  return (
    <>
      <motion.header
        style={{ backgroundColor: `rgba(248,246,241,${bgAlpha})` }}
        className="fixed top-0 left-0 right-0 z-50 backdrop-blur-md transition-shadow"
      >
        <nav className="mx-auto flex max-w-6xl items-center justify-between px-6 py-3">
          <Link href="/" className="flex items-center gap-2">
            <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-primary text-sm font-bold text-white">
              M
            </div>
            <span className="font-heading text-lg font-bold text-ink">MapChina</span>
          </Link>

          <div className="hidden items-center gap-8 md:flex">
            {links.map((link) => (
              <Link
                key={link.href}
                href={link.href}
                className="text-sm font-medium text-ink-secondary transition-colors hover:text-primary"
              >
                {link.label}
              </Link>
            ))}
            <Link
              href="/download"
              className="rounded-full bg-primary px-5 py-2 text-sm font-medium text-white transition-all hover:-translate-y-0.5 hover:shadow-lg hover:shadow-primary/25"
            >
              {t('download')}
            </Link>
          </div>

          <button
            className="flex items-center justify-center md:hidden"
            onClick={() => setMobileOpen(true)}
            aria-label="Open menu"
          >
            <svg width="24" height="24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M3 12h18M3 6h18M3 18h18" />
            </svg>
          </button>
        </nav>
      </motion.header>

      <MobileMenu open={mobileOpen} onClose={() => setMobileOpen(false)} links={links} downloadLabel={t('download')} />
    </>
  );
}
