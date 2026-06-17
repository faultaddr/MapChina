import { fetchAttractions } from '@/lib/api';
import Link from 'next/link';
import type { Metadata } from 'next';

export const revalidate = 3600;

export async function generateMetadata(): Promise<Metadata> {
  return {
    title: '景点 — MapChina',
    description: '探索中国各地的热门景点',
  };
}

export default async function AttractionsPage() {
  const res = await fetchAttractions();
  const attractions = res?.data ?? [];

  return (
    <main className="mx-auto max-w-6xl px-6 py-24 pt-28">
      <h1 className="mb-12 font-heading text-3xl font-bold text-ink">景点</h1>
      {attractions.length === 0 ? (
        <p className="text-ink-secondary">暂无数据</p>
      ) : (
        <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {attractions.map((item) => (
            <Link key={item.id} href={`/attractions/${item.id}`}>
              <div className="group overflow-hidden rounded-2xl bg-surface transition-all hover:shadow-lg hover:scale-[1.02]">
                <div className="relative flex aspect-[4/3] items-center justify-center bg-border">
                  <span className="text-5xl opacity-20">{item.name[0]}</span>
                </div>
                <div className="p-4">
                  <h2 className="font-heading font-semibold text-ink">{item.name}</h2>
                  <p className="mt-1 line-clamp-2 text-sm text-ink-secondary">
                    {item.description ?? `${item.visitCount} 次访问`}
                  </p>
                </div>
              </div>
            </Link>
          ))}
        </div>
      )}
    </main>
  );
}
