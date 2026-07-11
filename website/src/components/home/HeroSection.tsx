'use client';

import dynamic from 'next/dynamic';
import { motion, useReducedMotion } from 'framer-motion';
import { useSyncExternalStore } from 'react';
import type { SiteLocale } from '@/types';
import { buildBetaMailto, localizeHref } from '@/lib/site-config';
import { getHeroCopy } from '@/lib/home-content';

const ChinaMapScene = dynamic(() => import('../three/ChinaMapScene'), {
  ssr: false,
  loading: () => <div className="h-full w-full animate-pulse rounded-full bg-primary/10" />,
});

function subscribeDesktop(callback: () => void) {
  const query = window.matchMedia('(min-width: 768px)');
  query.addEventListener('change', callback);
  return () => query.removeEventListener('change', callback);
}

function getDesktopSnapshot() {
  return window.matchMedia('(min-width: 768px)').matches;
}

function getServerSnapshot() {
  return false;
}

export default function HeroSection({ locale }: { locale: SiteLocale }) {
  const text = getHeroCopy(locale);
  const isDesktop = useSyncExternalStore(subscribeDesktop, getDesktopSnapshot, getServerSnapshot);
  const reduceMotion = useReducedMotion();

  return (
    <section className="relative min-h-[780px] overflow-hidden bg-dark text-white md:min-h-screen">
      <div className="map-grid absolute inset-0" />
      <div className="absolute left-[-12rem] top-1/3 h-[34rem] w-[34rem] rounded-full bg-primary/18 blur-[100px]" />
      <div className="absolute right-[-10rem] top-[-8rem] h-[32rem] w-[32rem] rounded-full bg-gold/10 blur-[100px]" />

      <div className="relative mx-auto grid min-h-[780px] max-w-6xl items-center gap-10 px-6 pb-20 pt-32 md:min-h-screen md:grid-cols-[.95fr_1.05fr] md:pb-16 md:pt-28">
        <div className="relative z-10">
          <motion.p initial={reduceMotion ? false : { opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.6 }} className="text-xs font-semibold uppercase tracking-[0.24em] text-primary-light">
            {text.eyebrow}
          </motion.p>
          <motion.h1 initial={reduceMotion ? false : { opacity: 0, y: 24 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.7, delay: 0.08 }} className="mt-6 whitespace-pre-line font-heading text-5xl font-bold leading-[1.08] tracking-[-0.035em] sm:text-6xl lg:text-7xl">
            {text.title}
          </motion.h1>
          <motion.p initial={reduceMotion ? false : { opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.7, delay: 0.16 }} className="mt-7 max-w-xl text-base leading-8 text-white/65 md:text-lg">
            {text.subtitle}
          </motion.p>
          <motion.div initial={reduceMotion ? false : { opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.7, delay: 0.24 }} className="mt-9 flex flex-wrap gap-3">
            <a href={buildBetaMailto(locale)} className="inline-flex min-h-12 items-center rounded-full bg-gold px-7 py-3 font-semibold text-dark transition hover:-translate-y-0.5 hover:bg-[#ddb667]">{text.beta}</a>
            <a href={localizeHref(locale, '/attractions')} className="inline-flex min-h-12 items-center rounded-full border border-white/20 bg-white/5 px-7 py-3 font-semibold text-white transition hover:-translate-y-0.5 hover:bg-white/10">{text.explore}</a>
          </motion.div>
          <div className="mt-10 flex flex-wrap gap-2.5">
            {text.chips.map((chip) => <span key={chip} className="rounded-full border border-white/10 bg-white/[0.035] px-3.5 py-2 text-xs text-white/55">{chip}</span>)}
          </div>
        </div>

        <div className="relative mx-auto aspect-square w-full max-w-[38rem]" role="img" aria-label={text.mapLabel}>
          <div className="absolute inset-[8%] rounded-full border border-white/5 bg-[radial-gradient(circle,rgba(20,163,168,0.12),transparent_62%)]" />
          {isDesktop ? <ChinaMapScene /> : <MobileMapFallback />}
          <div className="absolute bottom-[12%] right-[10%] rounded-2xl border border-white/10 bg-[#101C2F]/80 px-4 py-3 backdrop-blur-md">
            <p className="text-[0.65rem] uppercase tracking-[0.2em] text-white/40">MapChina</p>
            <p className="mt-1 font-heading text-sm text-gold">{locale === 'zh' ? '山河正在展开' : 'Your landscape unfolds'}</p>
          </div>
        </div>
      </div>
    </section>
  );
}

function MobileMapFallback() {
  return (
    <svg viewBox="0 0 420 360" className="absolute inset-0 h-full w-full" aria-hidden="true">
      <defs>
        <linearGradient id="mobile-map-gradient" x1="0" y1="0" x2="1" y2="1">
          <stop stopColor="#21B0AA" />
          <stop offset="1" stopColor="#D2A64F" />
        </linearGradient>
        <filter id="mobile-map-glow"><feGaussianBlur stdDeviation="6" /></filter>
      </defs>
      <path d="M64 162c23-46 65-58 95-91 38 9 68 2 91 29 32-4 62 12 84 38 24 29 24 66-10 82-13 36-54 38-87 53-37 17-79 25-109 3-32 4-64-17-64-49-31-14-30-47 0-65Z" fill="none" stroke="url(#mobile-map-gradient)" strokeWidth="3" opacity=".85" />
      <path d="M64 162c23-46 65-58 95-91 38 9 68 2 91 29 32-4 62 12 84 38 24 29 24 66-10 82-13 36-54 38-87 53-37 17-79 25-109 3-32 4-64-17-64-49-31-14-30-47 0-65Z" fill="none" stroke="#20A7A2" strokeWidth="14" opacity=".12" filter="url(#mobile-map-glow)" />
      {[ [144,132], [195,105], [260,142], [301,188], [232,213], [175,246], [111,211] ].map(([cx, cy], index) => <g key={`${cx}-${cy}`}><circle cx={cx} cy={cy} r="4" fill={index % 2 ? '#D2A64F' : '#21B0AA'} /><circle cx={cx} cy={cy} r="11" fill="none" stroke={index % 2 ? '#D2A64F' : '#21B0AA'} opacity=".22" /></g>)}
    </svg>
  );
}
