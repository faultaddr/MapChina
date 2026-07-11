import Link from 'next/link';

type SectionHeadingProps = {
  eyebrow: string;
  title: string;
  description?: string;
  action?: { href: string; label: string };
  align?: 'left' | 'center';
};

export function SectionHeading({ eyebrow, title, description, action, align = 'left' }: SectionHeadingProps) {
  return (
    <div className={`mb-10 flex gap-6 ${align === 'center' ? 'flex-col items-center text-center' : 'items-end justify-between'}`}>
      <div className="max-w-3xl">
        <p className="text-xs font-semibold uppercase tracking-[0.28em] text-primary">{eyebrow}</p>
        <h2 className="mt-4 font-heading text-3xl font-bold leading-tight text-ink md:text-5xl">{title}</h2>
        {description && <p className="mt-4 max-w-2xl leading-7 text-ink-secondary">{description}</p>}
      </div>
      {action && (
        <Link className="hidden shrink-0 font-semibold text-primary underline-offset-4 hover:underline md:block" href={action.href}>
          {action.label} →
        </Link>
      )}
    </div>
  );
}
