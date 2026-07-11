'use client';

import Image from 'next/image';
import { useState } from 'react';
import type { SiteLocale } from '@/types';
import { themePreviews } from '@/lib/home-content';
import { SectionHeading } from '@/components/shared/SectionHeading';

const copy = {
  zh: {
    eyebrow: '地图也有自己的气候', title: '六种主题，六种看见山河的方式', description: '从水墨留白到星夜深蓝，让地图贴近每一段旅程的心境。',
    names: { classic: '经典', ink_wash: '水墨', vintage_map: '古舆图', rice_paper: '宣纸', starry_night: '星夜', mountain_mist: '山水' },
  },
  en: {
    eyebrow: 'A map can have its own atmosphere', title: 'Six themes, six ways to see the landscape', description: 'From ink-wash restraint to a deep starry night, let the map match the journey.',
    names: { classic: 'Classic', ink_wash: 'Ink Wash', vintage_map: 'Vintage Map', rice_paper: 'Rice Paper', starry_night: 'Starry Night', mountain_mist: 'Mountain Mist' },
  },
} satisfies Record<SiteLocale, { eyebrow: string; title: string; description: string; names: Record<(typeof themePreviews)[number]['key'], string> }>;

export default function ThemeGallery({ locale }: { locale: SiteLocale }) {
  const [active, setActive] = useState(0);
  const theme = themePreviews[active];
  const text = copy[locale];

  return (
    <section className="bg-paper py-24 md:py-32">
      <div className="mx-auto max-w-6xl px-6">
        <SectionHeading eyebrow={text.eyebrow} title={text.title} description={text.description} />
        <div className="grid items-stretch gap-5 lg:grid-cols-[1fr_17rem]">
          <div className={`relative min-h-[28rem] overflow-hidden rounded-[2.25rem] border border-border bg-surface md:min-h-[38rem] ${theme.dark ? 'text-white' : 'text-ink'}`}>
            {theme.image ? (
              <Image key={theme.image} src={theme.image} alt={`${text.names[theme.key]} ${locale === 'zh' ? '地图主题预览' : 'map theme preview'}`} fill sizes="(min-width: 1024px) 70vw, 100vw" className="object-cover" priority={active === 0} />
            ) : (
              <ClassicTheme />
            )}
            <div className="absolute inset-0 bg-gradient-to-t from-black/55 via-transparent to-transparent" />
            <div className="absolute bottom-0 left-0 p-7 text-white md:p-10">
              <p className="text-xs font-semibold uppercase tracking-[0.25em] text-white/55">Map theme · 0{active + 1}</p>
              <h3 className="mt-3 font-heading text-4xl font-bold md:text-5xl">{text.names[theme.key]}</h3>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-1">
            {themePreviews.map((item, index) => (
              <button
                key={item.key}
                type="button"
                aria-pressed={index === active}
                onClick={() => setActive(index)}
                className={`flex min-w-0 items-center gap-3 rounded-2xl border px-4 py-4 text-left transition ${index === active ? 'border-primary bg-primary text-white shadow-lg shadow-primary/15' : 'border-border bg-white/70 text-ink hover:border-primary/40 hover:bg-white'}`}
              >
                <span className="h-8 w-8 shrink-0 rounded-lg border border-black/5" style={{ background: item.image ? `linear-gradient(135deg, ${item.accent}, #f3ead6)` : 'linear-gradient(135deg,#0D7377,#D1B369)' }} />
                <span className="min-w-0 truncate text-sm font-semibold">{text.names[item.key]}</span>
              </button>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
}

function ClassicTheme() {
  return (
    <div className="absolute inset-0 bg-gradient-to-br from-[#DDEDEA] via-[#F6F1E7] to-[#E5D3A8]">
      <div className="absolute inset-0 opacity-35 [background-image:linear-gradient(rgba(13,115,119,.14)_1px,transparent_1px),linear-gradient(90deg,rgba(13,115,119,.14)_1px,transparent_1px)] [background-size:42px_42px]" />
      <svg viewBox="0 0 600 480" className="absolute inset-0 h-full w-full p-12" aria-hidden="true">
        <path d="M73 206c35-75 106-95 150-149 54 15 105 3 139 46 47-9 96 21 124 65 29 46 18 99-34 118-25 55-88 53-137 78-55 28-116 40-162 2-49 9-94-24-91-73-46-18-42-66 11-87Z" fill="rgba(13,115,119,.18)" stroke="#0D7377" strokeWidth="3" />
        {[ [190,170], [274,126], [376,188], [420,258], [320,308], [213,285] ].map(([cx, cy]) => <g key={`${cx}-${cy}`}><circle cx={cx} cy={cy} r="6" fill="#C8963E"/><circle cx={cx} cy={cy} r="16" fill="none" stroke="#C8963E" opacity=".35"/></g>)}
      </svg>
    </div>
  );
}
