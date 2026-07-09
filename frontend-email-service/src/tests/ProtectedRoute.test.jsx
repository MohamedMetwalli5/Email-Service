import { describe, it, expect, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import React from 'react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { AppProvider } from '../AppContext';
import ProtectedRoute from '../components/ProtectedRoute';

const ProtectedContent = () => <div data-testid="protected-content">Protected</div>;
const SignInPage = () => <div data-testid="sign-in-page">Sign In</div>;

const renderProtectedRoute = (initialPath) =>
  render(
    <MemoryRouter initialEntries={[initialPath]}>
      <AppProvider>
        <Routes>
          <Route path="/sign-in" element={<SignInPage />} />
          <Route path="/home" element={
            <ProtectedRoute>
              <ProtectedContent />
            </ProtectedRoute>
          } />
        </Routes>
      </AppProvider>
    </MemoryRouter>
  );

describe('ProtectedRoute', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('[M-08] redirects to /sign-in when no authToken is in context', () => {
    renderProtectedRoute('/home');
    expect(screen.getByTestId('sign-in-page')).toBeInTheDocument();
    expect(screen.queryByTestId('protected-content')).toBeNull();
  });

  it('[M-08] renders protected content when authToken is in context', () => {
    localStorage.setItem('authToken', 'test-token');
    renderProtectedRoute('/home');
    expect(screen.getByTestId('protected-content')).toBeInTheDocument();
    expect(screen.queryByTestId('sign-in-page')).toBeNull();
  });

  it('[M-08] allows /home with a Discord ticket code so the exchange can complete', () => {
    renderProtectedRoute('/home?code=discord-ticket');
    expect(screen.getByTestId('protected-content')).toBeInTheDocument();
    expect(screen.queryByTestId('sign-in-page')).toBeNull();
  });
});
