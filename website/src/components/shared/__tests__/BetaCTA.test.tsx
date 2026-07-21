import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { BetaCTA } from '../BetaCTA';

describe('BetaCTA', () => {
  it('renders the Chinese coming-soon state and beta email', () => {
    render(<BetaCTA locale="zh" variant="panel" />);

    expect(screen.getByText('即将上线')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: '参与内测' })).toHaveAttribute(
      'href',
      expect.stringContaining('mailto:cuferpan@gmail.com'),
    );
  });

  it('renders localized English copy', () => {
    render(<BetaCTA locale="en" variant="compact" />);

    expect(screen.getByText('Coming soon')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Join the beta' })).toBeInTheDocument();
  });
});
