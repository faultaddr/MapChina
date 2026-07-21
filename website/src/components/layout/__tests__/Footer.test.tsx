import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import Footer from '../Footer';

vi.mock('next-intl', () => ({
  useTranslations: () => (key: string) => key,
}));

vi.mock('next/navigation', () => ({
  usePathname: () => '/zh/attractions/beijing-central-axis',
}));

describe('Footer', () => {
  it('preserves the deep path when switching locales', () => {
    render(<Footer locale="zh" />);

    expect(screen.getByRole('link', { name: 'English' })).toHaveAttribute(
      'href',
      '/en/attractions/beijing-central-axis',
    );
    expect(screen.getByRole('link', { name: '中文' })).toHaveAttribute(
      'href',
      '/zh/attractions/beijing-central-axis',
    );
  });
});
