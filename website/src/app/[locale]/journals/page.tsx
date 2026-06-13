import { fetchJournals } from '@/lib/api';
import Image from 'next/image';
import Link from 'next/link';
import type { Metadata } from 'next';

export const revalidate = 3600;

export async function generateMetadata(): Promise<Metadata> {
  return {
    title: '游记 — MapChina',
    description: '阅读旅行者的中国游记',
  };
}

export default async function JournalsPage() {
  const res = await fetchJournals();
  const journals = res?.data ?? [];

  return (
    <main className="mx-auto max-w-6xl px-6 py-24 pt-28">
      <h1 className="mb-12 font-heading text-3xl font-bold text-ink">游记</h1>
      {journals.length === 0 ? (
        <p className="text-ink-secondary">暂无数据</p>
      ) : (
        <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {journals.map((item) => (
            <Link key={item.id} href={`/journals/${item.id}`}>
              <div className="group overflow-hidden rounded-2xl bg-surface transition-all hover:shadow-lg hover:scale-[1.02]">
                {item.images[0] && (
                  <div className="relative aspect-[4/3] bg-border">
                    <Image src={item.images[0]} alt={item.title} fill className="object-cover" />
                  </div>
                )}
                <div className="p-4">
                  <h2 className="font-heading font-semibold text-ink">{item.title}</h2>
                  <p className="mt-1 text-sm text-ink-secondary">{item.author.nickname}</p>
                </div>
              </div>
            </Link>
          ))}
        </div>
      )}
    </main>
  );
}
