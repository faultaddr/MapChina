'use client';

import dynamic from 'next/dynamic';
import { motion } from 'framer-motion';
import { useTranslations } from 'next-intl';
import { useEffect, useState } from 'react';
import { BREAKPOINT_MOBILE } from '@/lib/constants';

const ChinaMapScene = dynamic(() => import('../three/ChinaMapScene'), {
  ssr: false,
  loading: () => (
    <div className="flex h-full items-center justify-center">
      <div className="h-16 w-16 animate-pulse rounded-full bg-primary/20" />
    </div>
  ),
});

export default function HeroSection() {
  const t = useTranslations('hero');
  const [isMobile, setIsMobile] = useState(false);

  useEffect(() => {
    setIsMobile(window.innerWidth < BREAKPOINT_MOBILE);
    const onResize = () => setIsMobile(window.innerWidth < BREAKPOINT_MOBILE);
    window.addEventListener('resize', onResize);
    return () => window.removeEventListener('resize', onResize);
  }, []);

  return (
    <section className="relative flex min-h-screen items-center justify-center overflow-hidden bg-dark">
      <div className="absolute inset-0 bg-gradient-to-b from-dark via-dark to-primary/30" />

      <div className="absolute inset-0 opacity-70">
        {isMobile ? <MobileMapFallback /> : <ChinaMapScene />}
      </div>

      <div className="relative z-10 px-6 text-center">
        <motion.h1
          initial={{ opacity: 0, y: 30 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8, delay: 0.3 }}
          className="font-heading text-4xl font-bold text-white md:text-6xl lg:text-7xl"
        >
          {t('slogan')}
        </motion.h1>
        <motion.p
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8, delay: 0.6 }}
          className="mx-auto mt-6 max-w-xl text-lg text-white/70"
        >
          {t('subtitle')}
        </motion.p>
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8, delay: 0.9 }}
          className="mt-10"
        >
          <a
            href="/download"
            className="inline-block rounded-full bg-primary px-8 py-4 text-lg font-medium text-white transition-all hover:-translate-y-1 hover:shadow-xl hover:shadow-primary/25"
          >
            {t('download')}
          </a>
        </motion.div>
      </div>
    </section>
  );
}

function MobileMapFallback() {
  return (
    <svg viewBox="0 0 200 160" className="h-full w-full opacity-30">
      <defs>
        <linearGradient id="heroGrad" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stopColor="#0D7377" />
          <stop offset="100%" stopColor="#C8963E" />
        </linearGradient>
      </defs>
      <path
        d="M40,60 L45,55 L55,50 L65,45 L80,40 L95,38 L110,42 L120,48 L130,55 L135,65 L130,75 L120,80 L105,85 L90,88 L75,85 L60,78 L50,70 Z"
        fill="none"
        stroke="url(#heroGrad)"
        strokeWidth="1"
        className="animate-pulse"
      />
    </svg>
  );
}
