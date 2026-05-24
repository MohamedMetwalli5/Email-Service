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
      http.get('*/inbox', () => HttpResponse.json([])),
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

  it('[M-09] stores refreshToken from Discord redirect query params', async () => {
    Object.defineProperty(window, 'location', {
      configurable: true,
      writable: true,
      value: new URL('http://localhost:8080/home?token=discord-jwt&refreshToken=discord-refresh&email=user@seamail.com'),
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

  it('[M-09] does NOT crash when refreshToken is absent from query params', async () => {
    Object.defineProperty(window, 'location', {
      configurable: true,
      writable: true,
      value: new URL('http://localhost:8080/home?token=discord-jwt&email=user@seamail.com'),
    });
    Object.defineProperty(window, 'history', {
      configurable: true,
      writable: true,
      value: { ...originalHistory, replaceState: vi.fn() },
    });

    renderHomePage();

    await waitFor(() => {
      expect(localStorage.getItem('authToken')).toBe('discord-jwt');
      expect(localStorage.getItem('sharedUserEmail')).toBe('user@seamail.com');
      expect(localStorage.getItem('refreshToken')).toBeNull();
    });
  });

  it('[M-09] strips query params from browser URL using replaceState', async () => {
    const replaceStateMock = vi.fn();
    Object.defineProperty(window, 'location', {
      configurable: true,
      writable: true,
      value: new URL('http://localhost:8080/home?token=jwt&refreshToken=ref&email=user@seamail.com'),
    });
    Object.defineProperty(window, 'history', {
      configurable: true,
      writable: true,
      value: { ...originalHistory, replaceState: replaceStateMock },
    });
    document.title = 'Test';

    renderHomePage();

    await waitFor(() => {
      expect(replaceStateMock).toHaveBeenCalledWith(
        {},
        document.title,
        '/home'
      );
    });
  });

  it('[M-09] renders empty fragment when no auth token is available (no query params)', () => {
    Object.defineProperty(window, 'location', {
      configurable: true,
      writable: true,
      value: new URL('http://localhost:8080/home'),
    });

    const { container } = renderHomePage();

    // Toaster div is present from AppProvider, but no HomePage content
    expect(container.querySelector('[data-rht-toaster]')).toBeInTheDocument();
    expect(container.querySelector('.bg-gray-700')).toBeNull();
  });
});
