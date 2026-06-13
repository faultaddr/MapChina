import HeroSection from '@/components/home/HeroSection';
import FeatureCards from '@/components/home/FeatureCards';
import ThemeGallery from '@/components/home/ThemeGallery';
import CommunityPicks from '@/components/home/CommunityPicks';
import DownloadCTA from '@/components/home/DownloadCTA';

export default function HomePage() {
  return (
    <main>
      <HeroSection />
      <FeatureCards />
      <ThemeGallery />
      <CommunityPicks />
      <DownloadCTA />
    </main>
  );
}
