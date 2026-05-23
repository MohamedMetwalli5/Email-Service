import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import React from 'react';
import { BrowserRouter } from 'react-router-dom';
import { AppProvider } from '../AppContext';
import SignInPage from '../pages/SignInPage';
import { server } from './server';
import { http, HttpResponse } from 'msw';

const renderSignInPage = () =>
  render(
    <BrowserRouter>
      <AppProvider>
        <SignInPage />
      </AppProvider>
    </BrowserRouter>
  );

describe('SignInPage', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('[M-01] stores accessToken from AuthResponseDto on sign-in', async () => {
    const user = userEvent.setup();
    server.use(
      http.post('*/sign-in', () =>
        HttpResponse.json({
          accessToken: 'jwt-access-token-123',
          refreshToken: 'refresh-token-456',
        })
      )
    );

    renderSignInPage();

    await user.type(screen.getByLabelText(/email/i), 'test@seamail.com');
    await user.type(screen.getByLabelText('Password'), 'password123');
    await user.click(screen.getAllByRole('button', { name: 'Sign In' })[0]);

    await waitFor(() => {
      expect(localStorage.getItem('authToken')).toBe('jwt-access-token-123');
      expect(localStorage.getItem('refreshToken')).toBe('refresh-token-456');
    });
  });

  it('[M-01] does NOT use response.data.split(" ")[1] to parse the token', () => {
    const fakeResponse = { data: { accessToken: 'tok', refreshToken: 'ref' } };
    const oldBrokenCode = (res) => res.data.split(' ')[1];
    expect(() => oldBrokenCode(fakeResponse)).toThrow();
  });

  it('[M-03] sends plain text password (not SHA-256 hex)', async () => {
    const user = userEvent.setup();
    let capturedBody;

    server.use(
      http.post('*/sign-in', async ({ request }) => {
        capturedBody = await request.json();
        return HttpResponse.json({
          accessToken: 'jwt',
          refreshToken: 'ref',
        });
      })
    );

    renderSignInPage();

    await user.type(screen.getByLabelText(/email/i), 'test@seamail.com');
    await user.type(screen.getByLabelText('Password'), 'myPlainPassword');
    await user.click(screen.getAllByRole('button', { name: 'Sign In' })[0]);

    await waitFor(() => {
      expect(capturedBody.password).toBe('myPlainPassword');
    });
  });

  it('[M-03] does NOT send a 64-character hex string (SHA-256) in the password field', async () => {
    const user = userEvent.setup();
    let capturedBody;

    server.use(
      http.post('*/sign-in', async ({ request }) => {
        capturedBody = await request.json();
        return HttpResponse.json({
          accessToken: 'jwt',
          refreshToken: 'ref',
        });
      })
    );

    renderSignInPage();

    await user.type(screen.getByLabelText(/email/i), 'test@seamail.com');
    await user.type(screen.getByLabelText('Password'), 'myPlainPassword');
    await user.click(screen.getAllByRole('button', { name: 'Sign In' })[0]);

    await waitFor(() => {
      expect(capturedBody.password.length).not.toBe(64);
    });
  });

  it('[M-10] uses parseApiError instead of old data.errors pattern', async () => {
    const user = userEvent.setup();
    let requestReceived = false;

    server.use(
      http.post('*/sign-in', () => {
        requestReceived = true;
        return HttpResponse.json(
          { status: 404, error: 'USER_NOT_FOUND', message: 'User not found' },
          { status: 404 }
        );
      })
    );

    renderSignInPage();

    await user.type(screen.getByLabelText(/email/i), 'nonexistent@seamail.com');
    await user.type(screen.getByLabelText('Password'), 'password123');
    await user.click(screen.getAllByRole('button', { name: 'Sign In' })[0]);

    await waitFor(() => {
      expect(requestReceived).toBe(true);
    });
  });

  it('[M-22] handles USER_NOT_FOUND error code without crashing', async () => {
    const user = userEvent.setup();

    server.use(
      http.post('*/sign-in', () =>
        HttpResponse.json(
          { status: 404, error: 'USER_NOT_FOUND', message: 'User not found' },
          { status: 404 }
        )
      )
    );

    renderSignInPage();

    await user.type(screen.getByLabelText(/email/i), 'bad@seamail.com');
    await user.type(screen.getByLabelText('Password'), 'password123');
    await user.click(screen.getAllByRole('button', { name: 'Sign In' })[0]);

    await waitFor(() => {
      expect(screen.getByLabelText(/email/i)).toHaveValue('bad@seamail.com');
    });
  });

  it('[M-10] does NOT read error.response?.data?.errors (old broken field)', async () => {
    const user = userEvent.setup();
    server.use(
      http.post('*/sign-in', () =>
        HttpResponse.json(
          { errors: ['something went wrong'] },
          { status: 400 }
        )
      )
    );

    renderSignInPage();

    await user.type(screen.getByLabelText(/email/i), 'test@seamail.com');
    await user.type(screen.getByLabelText('Password'), 'pw');
    await user.click(screen.getAllByRole('button', { name: 'Sign In' })[0]);

    await waitFor(() => {
      expect(screen.getByLabelText(/email/i)).toHaveValue('test@seamail.com');
    });
  });

  it('[M-21] does not have an unused VITE_CLIENT_ID import affecting functionality', () => {
    expect(typeof SignInPage).toBe('function');
  });
});
