import { fetchCommunityFeed } from '@/lib/api';
import Image from 'next/image';
import Link from 'next/link';
import type { Metadata } from 'next';

export const revalidate = 3600;

export async function generateMetadata(): Promise<Metadata> {
  return {
    title: '社区 — MapChina',
    description: '发现旅行者的精彩分享',
  };
}

export default async function CommunityPage() {
  const res = await fetchCommunityFeed();
  const posts = res?.data ?? [];

  return (
    <main className="mx-auto max-w-6xl px-6 py-24 pt-28">
      <h1 className="mb-12 font-heading text-3xl font-bold text-ink">社区</h1>
      {posts.length === 0 ? (
        <p className="text-ink-secondary">暂无数据</p>
      ) : (
        <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {posts.map((post) => (
            <Link key={post.id} href={`/community/${post.id}`}>
              <div className="group overflow-hidden rounded-2xl bg-surface transition-all hover:shadow-lg hover:scale-[1.02]">
                {post.coverImage && (
                  <div className="relative aspect-[4/3] bg-border">
                    <Image src={post.coverImage} alt={post.title} fill className="object-cover" />
                  </div>
                )}
                <div className="p-4">
                  <h2 className="font-heading font-semibold text-ink">{post.title}</h2>
                  <div className="mt-2 flex items-center gap-2 text-sm text-ink-secondary">
                    <span>{post.nickname}</span>
                    <span>·</span>
                    <span>{post.likeCount} 赞</span>
                  </div>
                </div>
              </div>
            </Link>
          ))}
        </div>
      )}
    </main>
  );
}
