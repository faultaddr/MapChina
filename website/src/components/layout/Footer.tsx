import { useTranslations } from 'next-intl';
import Link from 'next/link';

export default function Footer() {
  const t = useTranslations('footer');

  return (
    <footer className="border-t border-border bg-bg py-8">
      <div className="mx-auto flex max-w-6xl items-center justify-between px-6">
        <p className="text-sm text-ink-secondary">{t('copyright')}</p>
        <div className="flex gap-6">
          <Link href="/about" className="text-sm text-ink-secondary transition-colors hover:text-primary">
            关于
          </Link>
        </div>
      </div>
    </footer>
  );
}
