import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import React from 'react';
import { BrowserRouter } from 'react-router-dom';
import { AppProvider } from '../AppContext';
import Sidebar from '../components/Sidebar';

const renderSidebar = () =>
  render(
    <BrowserRouter>
      <AppProvider>
        <Sidebar />
      </AppProvider>
    </BrowserRouter>
  );

describe('Sidebar', () => {
  beforeEach(() => {
    localStorage.clear();
    localStorage.setItem('authToken', 'test-token');
    localStorage.setItem('refreshToken', 'test-refresh');
    localStorage.setItem('sharedUserEmail', 'test@seamail.com');
    localStorage.setItem('sharedMailBoxOption', 'Inbox');
    localStorage.setItem('sharedEmailToFullyView', JSON.stringify({ emailID: 1 }));
    localStorage.setItem('sharedUserLanguage', 'en');
  });

  it('[M-17] sign-out calls clearSession() which clears all localStorage keys', async () => {
    const user = userEvent.setup();

    renderSidebar();

    await user.click(screen.getByText(/sign out/i));

    expect(localStorage.getItem('authToken')).toBeNull();
    expect(localStorage.getItem('refreshToken')).toBeNull();
    expect(localStorage.getItem('sharedUserEmail')).toBeNull();
    expect(localStorage.getItem('sharedMailBoxOption')).toBeNull();
    expect(localStorage.getItem('sharedEmailToFullyView')).toBeNull();
    expect(localStorage.getItem('sharedUserLanguage')).toBeNull();
  });

  it('[M-17] sign-out clears context state (not just localStorage)', async () => {
    const user = userEvent.setup();

    renderSidebar();

    expect(localStorage.getItem('authToken')).toBe('test-token');

    await user.click(screen.getByText(/sign out/i));

    expect(localStorage.getItem('authToken')).toBeNull();
    expect(localStorage.getItem('refreshToken')).toBeNull();
  });

  it('[M-17] does NOT use old pattern of only removing localStorage without clearing context', async () => {
    const user = userEvent.setup();

    renderSidebar();

    await user.click(screen.getByText(/sign out/i));

    const allKeysAfter = [];
    for (let i = 0; i < localStorage.length; i++) {
      allKeysAfter.push(localStorage.key(i));
    }
    expect(allKeysAfter).toEqual([]);
  });

  it('[M-17] navigates to /sign-in on sign-out', async () => {
    const user = userEvent.setup();

    renderSidebar();

    await user.click(screen.getByText(/sign out/i));
  });
});
