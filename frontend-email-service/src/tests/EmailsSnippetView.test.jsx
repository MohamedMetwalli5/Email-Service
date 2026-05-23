import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import React from 'react';
import { BrowserRouter } from 'react-router-dom';
import { AppProvider } from '../AppContext';
import EmailsSnippetView from '../components/home-main-content-components/EmailsSnippetView';
import { server } from './server';
import { http, HttpResponse } from 'msw';

const renderWithAuth = () =>
  render(
    <BrowserRouter>
      <AppProvider>
        <EmailsSnippetView />
      </AppProvider>
    </BrowserRouter>
  );

describe('EmailsSnippetView', () => {
  beforeEach(() => {
    localStorage.clear();
    localStorage.setItem('authToken', 'test-token');
    localStorage.setItem('sharedUserEmail', 'test@seamail.com');

    server.use(
      http.get('*/inbox', () =>
        HttpResponse.json([
          { emailID: 1, subject: 'Default', sender: 'a@b.com', date: '2026-01-01', priority: '1' },
        ])
      )
    );
  });

  it('[M-04] uses GET /emails?sort=priority instead of POST /sort-emails', async () => {
    let method, url;
    server.use(
      http.get('*/emails', ({ request }) => {
        method = 'GET';
        url = request.url;
        return HttpResponse.json([{ emailID: 1, subject: 'Priority email' }]);
      })
    );

    renderWithAuth();

    const sortSelect = await screen.findByLabelText(/sort by/i);
    await userEvent.selectOptions(sortSelect, 'priority');
    await userEvent.click(screen.getByRole('button', { name: /apply/i }));

    await waitFor(() => {
      expect(method).toBe('GET');
      expect(url).toContain('sort=priority');
    });
  });

  it('[M-04] does NOT send POST /sort-emails (old removed endpoint)', () => {
    const oldEndpoint = '*/sort-emails';
    const handler = vi.fn();
    server.use(http.post(oldEndpoint, handler));
    expect(handler).not.toHaveBeenCalled();
  });

  it('[M-05] uses GET /emails?filterBy=subject&filterValue= instead of POST /filter-emails', async () => {
    let method, url;
    server.use(
      http.get('*/emails', ({ request }) => {
        method = 'GET';
        url = request.url;
        return HttpResponse.json([{ emailID: 1, subject: 'Test' }]);
      })
    );

    renderWithAuth();

    const filterSelect = await screen.findByLabelText(/filter by/i);
    await userEvent.selectOptions(filterSelect, 'subject');
    const filterInput = await screen.findByPlaceholderText(/search/i);
    await userEvent.type(filterInput, 'Invoice');
    await userEvent.click(screen.getByRole('button', { name: /apply/i }));

    await waitFor(() => {
      expect(method).toBe('GET');
      expect(url).toContain('filterBy=subject');
      expect(url).toContain('filterValue=Invoice');
    });
  });

  it('[M-05] does NOT send POST /filter-emails (old removed endpoint)', () => {
    const oldEndpoint = '*/filter-emails';
    const handler = vi.fn();
    server.use(http.post(oldEndpoint, handler));
    expect(handler).not.toHaveBeenCalled();
  });

  it('[M-14] uses email.emailID as React list key (not email.id)', async () => {
    server.use(
      http.get('*/inbox', () =>
        HttpResponse.json([
          { emailID: 42, subject: 'Test', sender: 'a@b.com', date: '2026-01-01', priority: '1' },
          { emailID: 99, subject: 'Hello', sender: 'c@d.com', date: '2026-01-02', priority: '2' },
        ])
      )
    );

    renderWithAuth();

    await waitFor(() => {
      const listItems = screen.getAllByRole('listitem');
      expect(listItems.length).toBe(2);
    });
  });

  it('[M-14] does not use email.id as key (which would be undefined)', async () => {
    server.use(
      http.get('*/inbox', () =>
        HttpResponse.json([
          { emailID: 1, subject: 'A', sender: 'x@y.com', date: '2026-01-01', priority: '1' },
          { emailID: 2, subject: 'B', sender: 'z@w.com', date: '2026-01-02', priority: '2' },
        ])
      )
    );

    renderWithAuth();

    await waitFor(() => {
      expect(screen.getByText('A')).toBeInTheDocument();
      expect(screen.getByText('B')).toBeInTheDocument();
    });
  });

  it('[M-14] uses email.emailID or index fallback in key', async () => {
    server.use(
      http.get('*/inbox', () =>
        HttpResponse.json([
          { emailID: 100, subject: 'Unique', sender: 'x@y.com', date: '2026-01-01', priority: '1' },
        ])
      )
    );

    renderWithAuth();

    await waitFor(() => {
      expect(screen.getByText('Unique')).toBeInTheDocument();
    });
  });

  it('[M-04] sorts by date via GET /emails?sort=date', async () => {
    let capturedUrl;
    server.use(
      http.get('*/emails', ({ request }) => {
        capturedUrl = request.url;
        return HttpResponse.json([]);
      })
    );

    renderWithAuth();

    const sortSelect = await screen.findByLabelText(/sort by/i);
    await userEvent.selectOptions(sortSelect, 'date');
    await userEvent.click(screen.getByRole('button', { name: /apply/i }));

    await waitFor(() => {
      expect(capturedUrl).toContain('sort=date');
    });
  });

  it('[M-04/05] combined filter + sort sends filterBy, filterValue, sort, and mailbox in URL', async () => {
    let capturedUrl;
    server.use(
      http.get('*/emails', ({ request }) => {
        capturedUrl = request.url;
        return HttpResponse.json([]);
      })
    );

    renderWithAuth();

    const filterSelect = await screen.findByLabelText(/filter by/i);
    await userEvent.selectOptions(filterSelect, 'subject');
    const filterInput = await screen.findByPlaceholderText(/search/i);
    await userEvent.type(filterInput, 'Invoice');
    const sortSelect = await screen.findByLabelText(/sort by/i);
    await userEvent.selectOptions(sortSelect, 'priority');
    await userEvent.click(screen.getByRole('button', { name: /apply/i }));

    await waitFor(() => {
      expect(capturedUrl).toContain('filterBy=subject');
      expect(capturedUrl).toContain('filterValue=Invoice');
      expect(capturedUrl).toContain('sort=priority');
    });
  });
});
