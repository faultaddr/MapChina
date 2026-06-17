import { fetchAttraction } from '@/lib/api';
import { notFound } from 'next/navigation';
import type { Metadata } from 'next';

export const revalidate = 1800;

type Props = { params: Promise<{ id: string }> };

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { id } = await params;
  const attraction = await fetchAttraction(id);
  if (!attraction) return { title: 'Not Found' };
  return {
    title: `${attraction.name} — MapChina`,
    description: attraction.description ?? undefined,
  };
}

export default async function AttractionDetailPage({ params }: Props) {
  const { id } = await params;
  const attraction = await fetchAttraction(id);
  if (!attraction) notFound();

  const jsonLd = {
    '@context': 'https://schema.org',
    '@type': 'TouristAttraction',
    name: attraction.name,
    description: attraction.description,
    geo: {
      '@type': 'GeoCoordinates',
      latitude: attraction.latitude,
      longitude: attraction.longitude,
    },
  };

  return (
    <main className="mx-auto max-w-4xl px-6 py-24 pt-28">
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }}
      />
      <h1 className="mb-4 font-heading text-3xl font-bold text-ink">{attraction.name}</h1>
      <div className="mb-6 flex gap-4 text-sm text-ink-secondary">
        <span>{attraction.level}</span>
        <span>{attraction.visitCount} 次访问</span>
      </div>
      {attraction.description && (
        <p className="leading-relaxed text-ink-secondary">{attraction.description}</p>
      )}
    </main>
  );
}
