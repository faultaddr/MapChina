import { NextIntlClientProvider, hasLocale } from 'next-intl';
import { notFound } from 'next/navigation';
import { routing } from '@/i18n/routing';
import { Navbar } from '@/components/layout/Navbar';
import Footer from '@/components/layout/Footer';
import type { Metadata } from 'next';
import type { SiteLocale } from '@/types';

type Props = {
  children: React.ReactNode;
  params: Promise<{ locale: string }>;
};

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { locale } = await params;
  const isChinese = locale === 'zh';
  return {
    title: isChinese ? 'MapChina — 点亮足迹，看见自己的山河' : 'MapChina — Light up every journey',
    description: isChinese ? '探索下一站，记录每一次抵达，与旅行者分享中国之美。' : 'Explore China, record every arrival, and share the places that shape your journey.',
    alternates: {
      canonical: `/${locale}`,
    },
  };
}

export function generateStaticParams() {
  return routing.locales.map((locale) => ({ locale }));
}

export default async function LocaleLayout({ children, params }: Props) {
  const { locale } = await params;
  if (!hasLocale(routing.locales, locale)) {
    notFound();
  }

  return (
    <NextIntlClientProvider>
      <div lang={locale === 'zh' ? 'zh-CN' : 'en'}>
        <Navbar locale={locale as SiteLocale} />
        <div>{children}</div>
        <Footer locale={locale as SiteLocale} />
      </div>
    </NextIntlClientProvider>
  );
}
