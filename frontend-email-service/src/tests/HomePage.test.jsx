import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, waitFor } from '@testing-library/react';
import React from 'react';
import { BrowserRouter } from 'react-router-dom';
import { AppProvider } from '../AppContext';
import HomePage from '../pages/HomePage';
import { server } from './server';
import { http, HttpResponse } from 'msw';

const originalLocation = window.location;
const originalHistory = window.history;

const renderHomePage = () =>
  render(
    <BrowserRouter>
      <AppProvider>
        <HomePage />
      </AppProvider>
    </BrowserRouter>
  );

describe('HomePage', () => {
  beforeEach(() => {
    localStorage.clear();
    localStorage.setItem('sharedEmailToFullyView', JSON.stringify({ emailID: 1, sender: 'a@b.com', subject: 'Test' }));

    server.use(
      http.get('*/inbox', () => HttpResponse.json({ content: [], totalElements: 0, totalPages: 0 })),
      http.get('*/profile-picture', () => new HttpResponse(new Uint8Array(0), { status: 200, headers: { 'Content-Type': 'image/jpeg' } }))
    );
  });

  afterEach(() => {
    Object.defineProperty(window, 'location', {
      configurable: true,
      writable: true,
      value: originalLocation,
    });
    Object.defineProperty(window, 'history', {
      configurable: true,
      writable: true,
      value: originalHistory,
    });
  });

  it('[M-09] exchanges the Discord ?code= ticket for tokens and stores them', async () => {
    server.use(
      http.post('*/auth/exchange', async ({ request }) => {
        const body = await request.json();
        if (body.code === 'valid-ticket') {
          return HttpResponse.json({
            accessToken: 'discord-jwt',
            refreshToken: 'discord-refresh',
            email: 'user@seamail.com',
          });
        }
        return HttpResponse.json({ message: 'Unauthorized' }, { status: 401 });
      })
    );

    Object.defineProperty(window, 'location', {
      configurable: true,
      writable: true,
      value: new URL('http://localhost:8080/home?code=valid-ticket'),
    });
    Object.defineProperty(window, 'history', {
      configurable: true,
      writable: true,
      value: { ...originalHistory, replaceState: vi.fn() },
    });

    renderHomePage();

    await waitFor(() => {
      expect(localStorage.getItem('authToken')).toBe('discord-jwt');
      expect(localStorage.getItem('refreshToken')).toBe('discord-refresh');
      expect(localStorage.getItem('sharedUserEmail')).toBe('user@seamail.com');
    });
  });

  it('[M-09] does NOT leave tokens in the URL after exchange', async () => {
    const replaceStateMock = vi.fn();
    server.use(
      http.post('*/auth/exchange', () =>
        HttpResponse.json({
          accessToken: 'discord-jwt',
          refreshToken: 'discord-refresh',
          email: 'user@seamail.com',
        })
      )
    );

    Object.defineProperty(window, 'location', {
      configurable: true,
      writable: true,
      value: new URL('http://localhost:8080/home?code=valid-ticket'),
    });
    Object.defineProperty(window, 'history', {
      configurable: true,
      writable: true,
      value: { ...originalHistory, replaceState: replaceStateMock },
    });
    document.title = 'Test';

    renderHomePage();

    await waitFor(() => {
      expect(replaceStateMock).toHaveBeenCalledWith({}, document.title, '/home');
    });
  });

  it('[M-09] does NOT store tokens when the ticket is invalid or expired', async () => {
    server.use(
      http.post('*/auth/exchange', () =>
        HttpResponse.json({ message: 'Unauthorized' }, { status: 401 })
      )
    );

    Object.defineProperty(window, 'location', {
      configurable: true,
      writable: true,
      value: new URL('http://localhost:8080/home?code=expired-ticket'),
    });

    renderHomePage();

    await waitFor(() => {
      expect(localStorage.getItem('authToken')).toBeNull();
      expect(localStorage.getItem('refreshToken')).toBeNull();
      expect(localStorage.getItem('sharedUserEmail')).toBeNull();
    });
  });

  it('[M-09] renders empty fragment when no code is in the URL and no auth exists', () => {
    Object.defineProperty(window, 'location', {
      configurable: true,
      writable: true,
      value: new URL('http://localhost:8080/home'),
    });

    const { container } = renderHomePage();

    expect(container.querySelector('[data-rht-toaster]')).toBeInTheDocument();
    expect(container.querySelector('.bg-gray-700')).toBeNull();
  });
});
