import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import MobileMenu from '../MobileMenu';

describe('MobileMenu', () => {
  it('renders locale-safe links and the beta action', () => {
    render(
      <MobileMenu
        open
        onClose={vi.fn()}
        links={[{ href: '/zh/attractions', label: '景点' }]}
        betaLabel="参与内测"
        betaHref="mailto:test@example.com"
        closeLabel="关闭菜单"
      />,
    );

    expect(screen.getByRole('link', { name: '景点' })).toHaveAttribute('href', '/zh/attractions');
    expect(screen.getByRole('link', { name: '参与内测' })).toHaveAttribute('href', 'mailto:test@example.com');
    expect(screen.getByRole('button', { name: '关闭菜单' })).toBeInTheDocument();
  });

  it('closes after a navigation link is activated', () => {
    const onClose = vi.fn();
    render(
      <MobileMenu
        open
        onClose={onClose}
        links={[{ href: '/en/community', label: 'Community' }]}
        betaLabel="Join the beta"
        betaHref="mailto:test@example.com"
        closeLabel="Close menu"
      />,
    );

    const link = screen.getByRole('link', { name: 'Community' });
    link.addEventListener('click', (event) => event.preventDefault());
    fireEvent.click(link);
    expect(onClose).toHaveBeenCalledOnce();
  });
});
