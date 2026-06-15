import { fetchAttractions, fetchCommunityFeed } from '@/lib/api';
import { AnimatedSection } from '../shared/AnimatedSection';
import Image from 'next/image';
import Link from 'next/link';

export default async function CommunityPicks() {
  const [attractionsRes, postsRes] = await Promise.all([
    fetchAttractions(1, 4),
    fetchCommunityFeed(1, 4),
  ]);

  const attractions = attractionsRes?.data ?? [];
  const posts = postsRes?.data ?? [];

  if (attractions.length === 0 && posts.length === 0) return null;

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
                <div className="relative flex aspect-[4/3] items-center justify-center bg-border">
                  <span className="text-4xl opacity-30">{item.name[0]}</span>
                </div>
                <div className="p-4">
                  <h3 className="font-heading font-semibold text-ink">{item.name}</h3>
                  <p className="mt-1 text-xs text-ink-secondary">{item.visitCount} 次访问</p>
                </div>
              </div>
            </Link>
          ))}
        </div>

        {posts.length > 0 && (
          <>
            <h3 className="mb-6 mt-12 font-heading text-xl font-bold text-ink">社区动态</h3>
            <div className="grid gap-6 md:grid-cols-2">
              {posts.map((post) => (
                <Link key={post.id} href={`/community/${post.id}`}>
                  <div className="group flex gap-4 rounded-2xl bg-bg p-4 transition-all hover:shadow-lg">
                    {post.coverImage && (
                      <div className="relative h-20 w-20 shrink-0 overflow-hidden rounded-xl bg-border">
                        <Image src={post.coverImage} alt={post.title} fill className="object-cover" />
                      </div>
                    )}
                    <div className="min-w-0 flex-1">
                      <h4 className="truncate font-heading font-semibold text-ink">{post.title}</h4>
                      <p className="mt-1 truncate text-sm text-ink-secondary">{post.nickname}</p>
                      <div className="mt-2 flex gap-3 text-xs text-ink-secondary">
                        <span>{post.likeCount} 赞</span>
                        <span>{post.commentCount} 评论</span>
                      </div>
                    </div>
                  </div>
                </Link>
              ))}
            </div>
          </>
        )}
      </div>
    </section>
  );
}
