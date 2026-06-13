import { useTranslations } from 'next-intl';

export default function HomePage() {
  const t = useTranslations('hero');

  return (
    <main className="flex min-h-screen items-center justify-center">
      <h1 className="font-heading text-4xl font-bold text-ink">{t('slogan')}</h1>
    </main>
  );
}
