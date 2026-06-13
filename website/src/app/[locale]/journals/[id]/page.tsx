import { fetchJournal } from '@/lib/api';
import { notFound } from 'next/navigation';
import Image from 'next/image';
import type { Metadata } from 'next';

export const revalidate = 1800;

type Props = { params: Promise<{ id: string }> };

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { id } = await params;
  const res = await fetchJournal(id);
  if (!res?.data) return { title: 'Not Found' };
  return {
    title: `${res.data.title} — MapChina`,
    description: res.data.content.slice(0, 160),
    openGraph: { images: res.data.images.slice(0, 1) },
  };
}

export default async function JournalDetailPage({ params }: Props) {
  const { id } = await params;
  const res = await fetchJournal(id);
  if (!res?.data) notFound();

  const journal = res.data;

  const jsonLd = {
    '@context': 'https://schema.org',
    '@type': 'Article',
    headline: journal.title,
    author: { '@type': 'Person', name: journal.author.nickname },
    datePublished: journal.createdAt,
    image: journal.images[0],
  };

  return (
    <main className="mx-auto max-w-3xl px-6 py-24 pt-28">
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }}
      />
      <h1 className="mb-4 font-heading text-3xl font-bold text-ink">{journal.title}</h1>
      <p className="mb-8 text-sm text-ink-secondary">
        {journal.author.nickname} · {new Date(journal.createdAt).toLocaleDateString()}
      </p>
      {journal.images[0] && (
        <div className="relative mb-8 aspect-video overflow-hidden rounded-2xl">
          <Image src={journal.images[0]} alt={journal.title} fill className="object-cover" />
        </div>
      )}
      <div className="leading-relaxed text-ink-secondary whitespace-pre-wrap">{journal.content}</div>
    </main>
  );
}
