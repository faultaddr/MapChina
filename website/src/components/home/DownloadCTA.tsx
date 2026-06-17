'use client';

import { useTranslations } from 'next-intl';
import { AnimatedSection } from '../shared/AnimatedSection';

export default function DownloadCTA() {
  const t = useTranslations('download');

  return (
    <section className="relative overflow-hidden bg-dark py-24">
      <div className="absolute inset-0 bg-gradient-to-br from-primary/20 to-transparent" />
      <div className="relative mx-auto max-w-3xl px-6 text-center">
        <AnimatedSection>
          <h2 className="mb-4 font-heading text-3xl font-bold text-white md:text-5xl">
            {t('title')}
          </h2>
          <p className="mb-10 text-lg text-white/60">{t('subtitle')}</p>
          <div className="flex flex-col items-center justify-center gap-4 sm:flex-row">
            <a
              href="#"
              className="inline-flex items-center gap-3 rounded-2xl bg-white px-8 py-4 font-medium text-dark transition-all hover:-translate-y-1 hover:shadow-xl"
            >
              <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
                <path d="M18.71 19.5c-.83 1.24-1.71 2.45-3.05 2.47-1.34.03-1.77-.79-3.29-.79-1.53 0-2 .77-3.27.82-1.31.05-2.3-1.32-3.14-2.53C4.25 17 2.94 12.45 4.7 9.39c.87-1.52 2.43-2.48 4.12-2.51 1.28-.02 2.5.87 3.29.87.78 0 2.26-1.07 3.8-.91.65.03 2.47.26 3.64 1.98-.09.06-2.17 1.28-2.15 3.81.03 3.02 2.65 4.03 2.68 4.04-.03.07-.42 1.44-1.38 2.83M13 3.5c.73-.83 1.94-1.46 2.94-1.5.13 1.17-.34 2.35-1.04 3.19-.69.85-1.83 1.51-2.95 1.42-.15-1.15.41-2.35 1.05-3.11z" />
              </svg>
              {t('app_store')}
            </a>
            <a
              href="#"
              className="inline-flex items-center gap-3 rounded-2xl bg-white px-8 py-4 font-medium text-dark transition-all hover:-translate-y-1 hover:shadow-xl"
            >
              <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
                <path d="M3 20.5v-17c0-.59.34-1.11.84-1.35L13.69 12l-9.85 9.85c-.5-.24-.84-.76-.84-1.35zm13.81-5.38L6.05 21.34l8.49-8.49 2.27 2.27zm.91-.91L19.59 12l-1.87-2.21-2.27 2.27 2.27 2.15zM6.05 2.66l10.76 6.22-2.27 2.27-8.49-8.49z" />
              </svg>
              {t('google_play')}
            </a>
          </div>
        </AnimatedSection>
      </div>
    </section>
  );
}
