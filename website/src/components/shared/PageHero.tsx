import type { ReactNode } from 'react';

type PageHeroProps = {
  eyebrow: string;
  title: string;
  description: string;
  children?: ReactNode;
};

export function PageHero({ eyebrow, title, description, children }: PageHeroProps) {
  return (
    <section className="relative overflow-hidden bg-dark pb-20 pt-32 text-white md:pb-24 md:pt-40">
      <div className="map-grid absolute inset-0" />
      <div className="absolute -left-20 top-1/3 h-72 w-72 rounded-full bg-primary/25 blur-3xl" />
      <div className="absolute -right-16 top-12 h-64 w-64 rounded-full bg-gold/15 blur-3xl" />
      <div className="relative mx-auto max-w-6xl px-6">
        <p className="text-xs font-semibold uppercase tracking-[0.3em] text-primary-light">{eyebrow}</p>
        <h1 className="mt-5 max-w-4xl font-heading text-4xl font-bold leading-[1.15] md:text-6xl">{title}</h1>
        <p className="mt-6 max-w-2xl text-base leading-8 text-white/65 md:text-lg">{description}</p>
        {children && <div className="mt-8">{children}</div>}
      </div>
    </section>
  );
}
