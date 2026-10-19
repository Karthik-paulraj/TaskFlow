import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import React from 'react';
import { StatusBadge } from '../components/StatusBadge';
import { PriorityBadge } from '../components/PriorityBadge';

describe('StatusBadge', () => {
  it('renders QUEUED status correctly', () => {
    render(<StatusBadge status="QUEUED" />);
    expect(screen.getByText('QUEUED')).toBeInTheDocument();
  });

  it('renders RUNNING status correctly', () => {
    render(<StatusBadge status="RUNNING" />);
    expect(screen.getByText('RUNNING')).toBeInTheDocument();
  });

  it('renders SUCCEEDED status correctly', () => {
    render(<StatusBadge status="SUCCEEDED" />);
    expect(screen.getByText('SUCCEEDED')).toBeInTheDocument();
  });

  it('renders DEAD_LETTER status correctly', () => {
    render(<StatusBadge status="DEAD_LETTER" />);
    expect(screen.getByText('DEAD_LETTER')).toBeInTheDocument();
  });
});

describe('PriorityBadge', () => {
  it('renders P5 priority badge', () => {
    render(<PriorityBadge priority={5} />);
    expect(screen.getByText('P5')).toBeInTheDocument();
  });

  it('renders P10 priority badge', () => {
    render(<PriorityBadge priority={10} />);
    expect(screen.getByText('P10')).toBeInTheDocument();
  });
});
