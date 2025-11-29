import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { AuditorDashboard } from '../../../components/AuditorDashboard';
import { AuditQueue, AuditStatus, AuditPriority } from '../../../types';

// Mock fetch
global.fetch = vi.fn();

const mockQueueData: AuditQueue[] = [
  {
    queueId: '1',
    returnId: 'ret-1',
    priority: AuditPriority.HIGH,
    status: AuditStatus.PENDING,
    submissionDate: '2024-11-29T08:00:00Z',
    riskScore: 75,
    flaggedIssuesCount: 3,
    daysInQueue: 2,
    taxpayerName: 'Test Business LLC',
    returnType: 'BUSINESS',
    taxYear: '2024',
    taxDue: 15000
  },
  {
    queueId: '2',
    returnId: 'ret-2',
    priority: AuditPriority.MEDIUM,
    status: AuditStatus.IN_REVIEW,
    submissionDate: '2024-11-28T08:00:00Z',
    assignedAuditorId: 'aud-1',
    riskScore: 35,
    flaggedIssuesCount: 1,
    daysInQueue: 3,
    taxpayerName: 'John Doe',
    returnType: 'INDIVIDUAL',
    taxYear: '2024',
    taxDue: 2500
  }
];

describe('AuditorDashboard', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render dashboard with title', async () => {
    (global.fetch as any).mockResolvedValueOnce({
      ok: true,
      json: async () => ({
        content: [],
        totalElements: 0
      })
    }).mockResolvedValueOnce({
      ok: true,
      json: async () => ({
        pending: 0,
        highPriority: 0
      })
    });

    render(<AuditorDashboard userId="test-user" onReviewReturn={vi.fn()} />);
    
    await waitFor(() => {
      expect(screen.getByText('Auditor Dashboard')).toBeInTheDocument();
    });
  });

  it('should display queue items', async () => {
    (global.fetch as any).mockResolvedValueOnce({
      ok: true,
      json: async () => ({
        content: mockQueueData,
        totalElements: 2
      })
    }).mockResolvedValueOnce({
      ok: true,
      json: async () => ({
        pending: 5,
        highPriority: 2
      })
    });

    render(<AuditorDashboard userId="test-user" onReviewReturn={vi.fn()} />);
    
    await waitFor(() => {
      expect(screen.getByText('Test Business LLC')).toBeInTheDocument();
      expect(screen.getByText('John Doe')).toBeInTheDocument();
    });
  });

  it('should show high priority badge for high risk items', async () => {
    (global.fetch as any).mockResolvedValueOnce({
      ok: true,
      json: async () => ({
        content: mockQueueData,
        totalElements: 2
      })
    }).mockResolvedValueOnce({
      ok: true,
      json: async () => ({
        pending: 5,
        highPriority: 2
      })
    });

    render(<AuditorDashboard userId="test-user" onReviewReturn={vi.fn()} />);
    
    await waitFor(() => {
      const highPriorityBadges = screen.getAllByText('HIGH');
      expect(highPriorityBadges.length).toBeGreaterThan(0);
    });
  });

  it('should show loading state initially', () => {
    (global.fetch as any).mockImplementation(() => new Promise(() => {}));

    render(<AuditorDashboard userId="test-user" onReviewReturn={vi.fn()} />);
    
    // Loading spinner should be visible
    const spinner = document.querySelector('.animate-spin');
    expect(spinner).toBeInTheDocument();
  });
});
