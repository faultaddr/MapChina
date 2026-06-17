'use client';

import { useTranslations } from 'next-intl';
import { motion } from 'framer-motion';
import { AnimatedSection } from '../shared/AnimatedSection';

const FEATURE_KEYS = ['map', 'footprint', 'journal', 'badge', 'carving', 'theme'] as const;

const ICONS: Record<string, string> = {
  map: '🗺️',
  footprint: '👣',
  journal: '📖',
  badge: '🏅',
  carving: '🖌️',
  theme: '🎨',
};

const containerVariants = {
  hidden: {},
  visible: { transition: { staggerChildren: 0.1 } },
};

const itemVariants = {
  hidden: { opacity: 0, y: 30 },
  visible: { opacity: 1, y: 0, transition: { duration: 0.5, ease: 'easeOut' as const } },
};

export default function FeatureCards() {
  const t = useTranslations('features');

  return (
    <section className="bg-bg py-24">
      <div className="mx-auto max-w-6xl px-6">
        <AnimatedSection>
          <h2 className="mb-16 text-center font-heading text-3xl font-bold text-ink md:text-4xl">
            {t('title')}
          </h2>
        </AnimatedSection>

        <motion.div
          variants={containerVariants}
          initial="hidden"
          whileInView="visible"
          viewport={{ once: true, amount: 0.2 }}
          className="grid gap-8 md:grid-cols-2 lg:grid-cols-3"
        >
          {FEATURE_KEYS.map((key) => (
            <motion.div
              key={key}
              variants={itemVariants}
              className="group rounded-2xl border border-border bg-surface p-8 transition-all hover:border-primary hover:shadow-lg hover:scale-[1.02]"
            >
              <div className="mb-4 text-4xl">{ICONS[key]}</div>
              <h3 className="mb-2 font-heading text-xl font-semibold text-ink">
                {t(`${key}.title`)}
              </h3>
              <p className="text-ink-secondary">{t(`${key}.desc`)}</p>
            </motion.div>
          ))}
        </motion.div>
      </div>
    </section>
  );
}
