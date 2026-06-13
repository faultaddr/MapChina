import { fetchAttractions, fetchJournals } from '@/lib/api';
import { AnimatedSection } from '../shared/AnimatedSection';
import Image from 'next/image';
import Link from 'next/link';

export default async function CommunityPicks() {
  const [attractionsRes, journalsRes] = await Promise.all([
    fetchAttractions(1, 4),
    fetchJournals(1, 4),
  ]);

  const attractions = attractionsRes?.data ?? [];
  const journals = journalsRes?.data ?? [];

  if (attractions.length === 0 && journals.length === 0) return null;

  return (
    <section className="bg-surface py-24">
      <div className="mx-auto max-w-6xl px-6">
        <AnimatedSection>
          <h2 className="mb-16 text-center font-heading text-3xl font-bold text-ink md:text-4xl">
            社区精选
          </h2>
        </AnimatedSection>

        <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-4">
          {attractions.map((item) => (
            <Link key={item.id} href={`/attractions/${item.id}`}>
              <div className="group overflow-hidden rounded-2xl bg-bg transition-all hover:shadow-lg hover:scale-[1.02]">
                <div className="relative aspect-[4/3] bg-border">
                  {item.imageUrl && (
                    <Image src={item.imageUrl} alt={item.name} fill className="object-cover" />
                  )}
                </div>
                <div className="p-4">
                  <h3 className="font-heading font-semibold text-ink">{item.name}</h3>
                </div>
              </div>
            </Link>
          ))}
        </div>
      </div>
    </section>
  );
}
