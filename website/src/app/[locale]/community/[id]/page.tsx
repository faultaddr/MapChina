import { fetchCommunityPost } from '@/lib/api';
import { notFound } from 'next/navigation';
import Image from 'next/image';
import type { Metadata } from 'next';

export const revalidate = 1800;

type Props = { params: Promise<{ id: string }> };

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { id } = await params;
  const post = await fetchCommunityPost(id);
  if (!post) return { title: 'Not Found' };
  return {
    title: `${post.title} — MapChina`,
    description: post.content.slice(0, 160),
    openGraph: { images: post.coverImage ? [post.coverImage] : [] },
  };
}

export default async function CommunityPostPage({ params }: Props) {
  const { id } = await params;
  const post = await fetchCommunityPost(id);
  if (!post) notFound();

  const jsonLd = {
    '@context': 'https://schema.org',
    '@type': 'Article',
    headline: post.title,
    author: { '@type': 'Person', name: post.nickname },
    datePublished: new Date(post.createdAt).toISOString(),
    image: post.coverImage,
  };

  return (
    <main className="mx-auto max-w-3xl px-6 py-24 pt-28">
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }}
      />
      <h1 className="mb-4 font-heading text-3xl font-bold text-ink">{post.title}</h1>
      <p className="mb-8 text-sm text-ink-secondary">
        {post.nickname} · {new Date(post.createdAt).toLocaleDateString()}
      </p>
      {post.coverImage && (
        <div className="relative mb-8 aspect-video overflow-hidden rounded-2xl">
          <Image src={post.coverImage} alt={post.title} fill className="object-cover" />
        </div>
      )}
      <div className="leading-relaxed text-ink-secondary whitespace-pre-wrap">{post.content}</div>
      <div className="mt-8 flex gap-4 text-sm text-ink-secondary">
        <span>{post.likeCount} 赞</span>
        <span>{post.commentCount} 评论</span>
      </div>
    </main>
  );
}
