'use client';

import { useState } from 'react';
import { useTranslations } from 'next-intl';
import { motion, AnimatePresence } from 'framer-motion';
import { AnimatedSection } from '../shared/AnimatedSection';

const THEMES = [
  { key: 'classic', color: '#E8F4F8' },
  { key: 'ink_wash', color: '#F5F0E6' },
  { key: 'vintage_map', color: '#EBE1C8' },
  { key: 'rice_paper', color: '#F8F4EB' },
  { key: 'starry_night', color: '#0F1428' },
  { key: 'mountain_mist', color: '#E6EBF0' },
] as const;

export default function ThemeGallery() {
  const t = useTranslations('themes');
  const [active, setActive] = useState(0);

  return (
    <section className="bg-bg py-24">
      <div className="mx-auto max-w-6xl px-6">
        <AnimatedSection>
          <h2 className="mb-16 text-center font-heading text-3xl font-bold text-ink md:text-4xl">
            {t('title')}
          </h2>
        </AnimatedSection>

        <div className="flex flex-col items-center gap-8 lg:flex-row">
          <div className="relative aspect-[4/3] w-full overflow-hidden rounded-2xl lg:w-2/3">
            <AnimatePresence mode="wait">
              <motion.div
                key={THEMES[active].key}
                initial={{ opacity: 0, scale: 0.95 }}
                animate={{ opacity: 1, scale: 1 }}
                exit={{ opacity: 0, scale: 0.95 }}
                transition={{ duration: 0.3 }}
                className="absolute inset-0 flex items-center justify-center"
                style={{ backgroundColor: THEMES[active].color }}
              >
                <span className="font-heading text-2xl text-ink/30">
                  {t(THEMES[active].key)}
                </span>
              </motion.div>
            </AnimatePresence>
          </div>

          <div className="flex flex-row gap-3 lg:flex-col">
            {THEMES.map((theme, i) => (
              <button
                key={theme.key}
                onClick={() => setActive(i)}
                className={`flex items-center gap-2 rounded-xl px-4 py-3 transition-all ${
                  i === active
                    ? 'bg-primary/10 ring-2 ring-primary'
                    : 'bg-surface hover:bg-primary/5'
                }`}
              >
                <div
                  className="h-6 w-6 rounded-md border border-border"
                  style={{ backgroundColor: theme.color }}
                />
                <span className="text-sm font-medium text-ink">{t(theme.key)}</span>
              </button>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
}
