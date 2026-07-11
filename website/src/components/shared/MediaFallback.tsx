type MediaFallbackProps = {
  label: string;
  tone?: 'jade' | 'gold' | 'night';
  className?: string;
};

const toneClasses = {
  jade: 'from-[#123C3E] via-[#1D6B68] to-[#B9B17F]',
  gold: 'from-[#5A382A] via-[#A46D3D] to-[#E3C77C]',
  night: 'from-[#0B1324] via-[#1C3652] to-[#337D7B]',
};

export function MediaFallback({ label, tone = 'jade', className = '' }: MediaFallbackProps) {
  return (
    <div className={`relative overflow-hidden bg-gradient-to-br ${toneClasses[tone]} ${className}`} role="img" aria-label={label}>
      <svg className="absolute inset-0 h-full w-full opacity-50" viewBox="0 0 480 320" fill="none" aria-hidden="true">
        <path d="M-20 258C55 180 86 229 150 160c43-47 91-18 125-70 37-57 98-7 139-69 28-42 74-20 103-55" stroke="rgba(255,255,255,.45)" strokeWidth="1.5" />
        <path d="M-34 292c95-88 143-18 207-91 58-67 104-4 163-70 51-57 91-19 157-86" stroke="rgba(255,255,255,.2)" strokeWidth="1" />
        <circle cx="305" cy="122" r="5" fill="#E0B85D" />
        <circle cx="305" cy="122" r="16" stroke="#E0B85D" strokeOpacity=".35" />
      </svg>
      <div className="absolute inset-x-0 bottom-0 bg-gradient-to-t from-black/55 to-transparent px-5 pb-5 pt-16 text-sm font-semibold text-white/90">{label}</div>
    </div>
  );
}
