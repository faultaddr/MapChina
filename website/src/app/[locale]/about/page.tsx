import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: '关于 — MapChina',
  description: '了解 MapChina，用地图点亮你的中国足迹',
};

export default function AboutPage() {
  return (
    <main className="mx-auto max-w-3xl px-6 py-24 pt-28">
      <h1 className="mb-8 font-heading text-3xl font-bold text-ink">关于 MapChina</h1>
      <div className="space-y-6 text-ink-secondary leading-relaxed">
        <p>
          MapChina 是一款以地图为核心的中国旅行记录应用。我们相信，每一次旅行都值得被铭记，每一寸国土都值得被探索。
        </p>
        <p>
          从碧玉绿的地图到水墨风的渲染，从足迹标记到石壁碑刻，MapChina 将传统美学与现代技术融为一体，
          让你用最自然的方式记录和分享你的中国之旅。
        </p>
        <p>
          「用地图点亮你的中国足迹」— 这不只是一句口号，更是我们对每一位旅行者的承诺。
        </p>
      </div>
    </main>
  );
}
