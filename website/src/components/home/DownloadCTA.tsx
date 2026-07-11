import type { SiteLocale } from '@/types';
import { BetaCTA } from '@/components/shared/BetaCTA';

export default function DownloadCTA({ locale }: { locale: SiteLocale }) {
  return (
    <section className="bg-[#EEE9DE] py-20 md:py-28">
      <div className="mx-auto max-w-6xl px-6"><BetaCTA locale={locale} /></div>
    </section>
  );
}
