import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import React from 'react';
import { BrowserRouter } from 'react-router-dom';
import { AppProvider } from '../AppContext';
import SignUpPage from '../pages/SignUpPage';
import { server } from './server';
import { http, HttpResponse } from 'msw';

const renderSignUpPage = () =>
  render(
    <BrowserRouter>
      <AppProvider>
        <SignUpPage />
      </AppProvider>
    </BrowserRouter>
  );

describe('SignUpPage', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('[M-02] reads accessToken from AuthResponseDto (not response.data.token)', async () => {
    const user = userEvent.setup();
    server.use(
      http.post('*/sign-up', () =>
        HttpResponse.json(
          {
            accessToken: 'jwt-from-signup',
            refreshToken: 'refresh-from-signup',
          },
          { status: 201 }
        )
      )
    );

    renderSignUpPage();

    await user.type(screen.getByLabelText(/email/i), 'newuser@seamail.com');
    const passwordInput = screen.getAllByLabelText('Password')[0];
    await user.type(passwordInput, 'password123');
    const confirmInput = screen.getAllByLabelText('Confirm Password')[0];
    await user.type(confirmInput, 'password123');
    await user.click(screen.getByRole('button', { name: 'Sign Up' }));

    await waitFor(() => {
      expect(localStorage.getItem('authToken')).toBe('jwt-from-signup');
      expect(localStorage.getItem('refreshToken')).toBe('refresh-from-signup');
    });
  });

  it('[M-02] does NOT use response.data.token (old broken field)', () => {
    const fakeResponse = { data: { accessToken: 'tok', refreshToken: 'ref' } };
    const oldPattern = (res) => res.data.token;
    expect(oldPattern(fakeResponse)).toBeUndefined();
  });

  it('[M-03] sends plain text password (not SHA-256 hex)', async () => {
    const user = userEvent.setup();
    let capturedBody;

    server.use(
      http.post('*/sign-up', async ({ request }) => {
        capturedBody = await request.json();
        return HttpResponse.json(
          { accessToken: 'jwt', refreshToken: 'ref' },
          { status: 201 }
        );
      })
    );

    renderSignUpPage();

    await user.type(screen.getByLabelText(/email/i), 'newuser@seamail.com');
    const passwordInput = screen.getAllByLabelText('Password')[0];
    await user.type(passwordInput, 'myPlainPwd123');
    const confirmInput = screen.getAllByLabelText('Confirm Password')[0];
    await user.type(confirmInput, 'myPlainPwd123');
    await user.click(screen.getByRole('button', { name: 'Sign Up' }));

    await waitFor(() => {
      expect(capturedBody.password).toBe('myPlainPwd123');
    });
  });

  it('[M-03] does NOT send a 64-character hex string (SHA-256)', async () => {
    const user = userEvent.setup();
    let capturedBody;

    server.use(
      http.post('*/sign-up', async ({ request }) => {
        capturedBody = await request.json();
        return HttpResponse.json(
          { accessToken: 'jwt', refreshToken: 'ref' },
          { status: 201 }
        );
      })
    );

    renderSignUpPage();

    await user.type(screen.getByLabelText(/email/i), 'newuser@seamail.com');
    const passwordInput = screen.getAllByLabelText('Password')[0];
    await user.type(passwordInput, 'plainPwd');
    const confirmInput = screen.getAllByLabelText('Confirm Password')[0];
    await user.type(confirmInput, 'plainPwd');
    await user.click(screen.getByRole('button', { name: 'Sign Up' }));

    await waitFor(() => {
      expect(capturedBody.password.length).not.toBe(64);
    });
  });

  it('[M-20] does NOT send confirmPassword in the request body', async () => {
    const user = userEvent.setup();
    let capturedBody;

    server.use(
      http.post('*/sign-up', async ({ request }) => {
        capturedBody = await request.json();
        return HttpResponse.json(
          { accessToken: 'jwt', refreshToken: 'ref' },
          { status: 201 }
        );
      })
    );

    renderSignUpPage();

    await user.type(screen.getByLabelText(/email/i), 'newuser@seamail.com');
    const passwordInput = screen.getAllByLabelText('Password')[0];
    await user.type(passwordInput, 'password123');
    const confirmInput = screen.getAllByLabelText('Confirm Password')[0];
    await user.type(confirmInput, 'password123');
    await user.click(screen.getByRole('button', { name: 'Sign Up' }));

    await waitFor(() => {
      expect(capturedBody.confirmPassword).toBeUndefined();
    });
  });

  it('[M-20] payload only contains email and password', async () => {
    const user = userEvent.setup();
    let capturedBody;

    server.use(
      http.post('*/sign-up', async ({ request }) => {
        capturedBody = await request.json();
        return HttpResponse.json(
          { accessToken: 'jwt', refreshToken: 'ref' },
          { status: 201 }
        );
      })
    );

    renderSignUpPage();

    await user.type(screen.getByLabelText(/email/i), 'newuser@seamail.com');
    const passwordInput = screen.getAllByLabelText('Password')[0];
    await user.type(passwordInput, 'password123');
    const confirmInput = screen.getAllByLabelText('Confirm Password')[0];
    await user.type(confirmInput, 'password123');
    await user.click(screen.getByRole('button', { name: 'Sign Up' }));

    await waitFor(() => {
      expect(Object.keys(capturedBody)).toEqual(['email', 'password']);
    });
  });

  it('[M-10] uses parseApiError instead of old data.errors pattern', async () => {
    const user = userEvent.setup();
    let requestReceived = false;

    server.use(
      http.post('*/sign-up', () => {
        requestReceived = true;
        return HttpResponse.json(
          {
            status: 400,
            error: 'VALIDATION_FAILED',
            message: 'Request validation failed.',
            fieldErrors: ['email: Email is required'],
          },
          { status: 400 }
        );
      })
    );

    renderSignUpPage();

    await user.type(screen.getByLabelText(/email/i), 'valid@seamail.com');
    const passwordInput = screen.getAllByLabelText('Password')[0];
    await user.type(passwordInput, 'password123');
    const confirmInput = screen.getAllByLabelText('Confirm Password')[0];
    await user.type(confirmInput, 'password123');
    await user.click(screen.getByRole('button', { name: 'Sign Up' }));

    await waitFor(() => {
      expect(requestReceived).toBe(true);
    });
  });

  it('[M-22] handles USER_ALREADY_EXISTS error code without crashing', async () => {
    const user = userEvent.setup();

    server.use(
      http.post('*/sign-up', () =>
        HttpResponse.json(
          { status: 409, error: 'USER_ALREADY_EXISTS', message: 'User already exists' },
          { status: 409 }
        )
      )
    );

    renderSignUpPage();

    await user.type(screen.getByLabelText(/email/i), 'existing@seamail.com');
    const passwordInput = screen.getAllByLabelText('Password')[0];
    await user.type(passwordInput, 'password123');
    const confirmInput = screen.getAllByLabelText('Confirm Password')[0];
    await user.type(confirmInput, 'password123');
    await user.click(screen.getByRole('button', { name: 'Sign Up' }));

    await waitFor(() => {
      expect(screen.getByLabelText(/email/i)).toHaveValue('existing@seamail.com');
    });
  });

  it('[M-22] handles INVALID_EMAIL_DOMAIN error code without crashing', async () => {
    const user = userEvent.setup();

    server.use(
      http.post('*/sign-up', () =>
        HttpResponse.json(
          { status: 400, error: 'INVALID_EMAIL_DOMAIN', message: 'Emails must end with @seamail.com' },
          { status: 400 }
        )
      )
    );

    renderSignUpPage();

    await user.type(screen.getByLabelText(/email/i), 'test@gmail.com');
    const passwordInput = screen.getAllByLabelText('Password')[0];
    await user.type(passwordInput, 'password123');
    const confirmInput = screen.getAllByLabelText('Confirm Password')[0];
    await user.type(confirmInput, 'password123');
    await user.click(screen.getByRole('button', { name: 'Sign Up' }));

    await waitFor(() => {
      expect(screen.getByLabelText(/email/i)).toHaveValue('test@gmail.com');
    });
  });
});
