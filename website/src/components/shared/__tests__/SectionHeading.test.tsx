import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { SectionHeading } from '../SectionHeading';

describe('SectionHeading', () => {
  it('keeps its action visible below the heading on mobile', () => {
    render(
      <SectionHeading
        eyebrow="Across the map"
        title="Explore"
        action={{ href: '/en/community', label: 'Explore all' }}
      />,
    );

    const action = screen.getByRole('link', { name: 'Explore all →' });
    expect(action.className).not.toContain('hidden');
    expect(action.parentElement?.className).toContain('flex-col');
  });
});
