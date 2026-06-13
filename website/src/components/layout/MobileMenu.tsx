'use client';

import { useEffect } from 'react';
import Link from 'next/link';
import { motion, AnimatePresence } from 'framer-motion';

interface MobileMenuProps {
  open: boolean;
  onClose: () => void;
  links: { href: string; label: string }[];
  downloadLabel: string;
}

export default function MobileMenu({ open, onClose, links, downloadLabel }: MobileMenuProps) {
  useEffect(() => {
    document.body.style.overflow = open ? 'hidden' : '';
    return () => { document.body.style.overflow = ''; };
  }, [open]);

  return (
    <AnimatePresence>
      {open && (
        <>
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-40 bg-black/40 md:hidden"
            onClick={onClose}
          />
          <motion.div
            initial={{ x: '100%' }}
            animate={{ x: 0 }}
            exit={{ x: '100%' }}
            transition={{ type: 'spring', damping: 25, stiffness: 300 }}
            className="fixed right-0 top-0 bottom-0 z-50 w-64 bg-bg p-6 shadow-xl md:hidden"
          >
            <button onClick={onClose} className="mb-8" aria-label="Close menu">
              <svg width="24" height="24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M18 6L6 18M6 6l12 12" />
              </svg>
            </button>
            <div className="flex flex-col gap-6">
              {links.map((link) => (
                <Link key={link.href} href={link.href} onClick={onClose} className="text-lg font-medium text-ink">
                  {link.label}
                </Link>
              ))}
              <Link
                href="/download"
                onClick={onClose}
                className="mt-4 rounded-full bg-primary px-5 py-3 text-center text-sm font-medium text-white"
              >
                {downloadLabel}
              </Link>
            </div>
          </motion.div>
        </>
      )}
    </AnimatePresence>
  );
}
