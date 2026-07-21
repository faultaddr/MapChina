import HeroSection from '@/components/home/HeroSection';
import ThemeGallery from '@/components/home/ThemeGallery';
import DownloadCTA from '@/components/home/DownloadCTA';
import { ProductPaths } from '@/components/home/ProductPaths';
import { LiveShanhe } from '@/components/home/LiveShanhe';
import type { SiteLocale } from '@/types';

export default async function HomePage({ params }: { params: Promise<{ locale: string }> }) {
  const { locale } = await params;
  const siteLocale = locale as SiteLocale;
  return (
    <main>
      <HeroSection locale={siteLocale} />
      <LiveShanhe locale={siteLocale} />
      <ProductPaths locale={siteLocale} />
      <ThemeGallery locale={siteLocale} />
      <DownloadCTA locale={siteLocale} />
    </main>
  );
}
