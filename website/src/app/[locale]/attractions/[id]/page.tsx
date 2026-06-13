import { fetchAttraction } from '@/lib/api';
import { notFound } from 'next/navigation';
import Image from 'next/image';
import type { Metadata } from 'next';

export const revalidate = 1800;

type Props = { params: Promise<{ id: string }> };

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { id } = await params;
  const res = await fetchAttraction(id);
  if (!res?.data) return { title: 'Not Found' };
  return {
    title: `${res.data.name} — MapChina`,
    description: res.data.description,
    openGraph: { images: res.data.imageUrl ? [res.data.imageUrl] : [] },
  };
}

export default async function AttractionDetailPage({ params }: Props) {
  const { id } = await params;
  const res = await fetchAttraction(id);
  if (!res?.data) notFound();

  const attraction = res.data;

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
      {attraction.imageUrl && (
        <div className="relative mb-8 aspect-video overflow-hidden rounded-2xl">
          <Image src={attraction.imageUrl} alt={attraction.name} fill className="object-cover" />
        </div>
      )}
      <h1 className="mb-4 font-heading text-3xl font-bold text-ink">{attraction.name}</h1>
      <p className="leading-relaxed text-ink-secondary">{attraction.description}</p>
    </main>
  );
}
