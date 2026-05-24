import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import React from 'react';
import { BrowserRouter } from 'react-router-dom';
import { AppProvider, AppContext } from '../AppContext';
import SettingsMainContent from '../components/SettingsMainContent';
import { server } from './server';
import { http, HttpResponse } from 'msw';

const renderSettings = () =>
  render(
    <BrowserRouter>
      <AppProvider>
        <SettingsMainContent />
      </AppProvider>
    </BrowserRouter>
  );

describe('SettingsMainContent', () => {
  beforeEach(() => {
    localStorage.clear();
    localStorage.setItem('authToken', 'test-token');
    localStorage.setItem('sharedUserEmail', 'test@seamail.com');
  });

  it('[M-03] sends plain text newPassword (not SHA-256 hex) on password change', async () => {
    const user = userEvent.setup();
    let capturedBody;

    server.use(
      http.put('*/change-password', async ({ request }) => {
        capturedBody = await request.json();
        return HttpResponse.json(null, { status: 204 });
      })
    );

    renderSettings();

    const passwordInputs = screen.getAllByPlaceholderText(/new password/i);
    await user.type(passwordInputs[0], 'myNewPlainPwd');
    const confirmInputs = screen.getAllByPlaceholderText(/confirm new password/i);
    await user.type(confirmInputs[0], 'myNewPlainPwd');

    await user.click(screen.getByRole('button', { name: /change password/i }));

    await waitFor(() => {
      expect(capturedBody.newPassword).toBe('myNewPlainPwd');
    });
  });

  it('[M-03] does NOT send a 64-character hex string (SHA-256) as newPassword', async () => {
    const user = userEvent.setup();
    let capturedBody;

    server.use(
      http.put('*/change-password', async ({ request }) => {
        capturedBody = await request.json();
        return HttpResponse.json(null, { status: 204 });
      })
    );

    renderSettings();

    const passwordInputs = screen.getAllByPlaceholderText(/new password/i);
    await user.type(passwordInputs[0], 'plainPwd123');
    const confirmInputs = screen.getAllByPlaceholderText(/confirm new password/i);
    await user.type(confirmInputs[0], 'plainPwd123');

    await user.click(screen.getByRole('button', { name: /change password/i }));

    await waitFor(() => {
      expect(capturedBody.newPassword.length).not.toBe(64);
    });
  });

  it('[M-10] uses parseApiError for error display on password change failure', async () => {
    const user = userEvent.setup();
    server.use(
      http.put('*/change-password', () =>
        HttpResponse.json(
          {
            status: 400,
            error: 'VALIDATION_FAILED',
            message: 'Request validation failed.',
            fieldErrors: ['newPassword: Password must be at least 8 characters'],
          },
          { status: 400 }
        )
      )
    );

    renderSettings();

    const passwordInputs = screen.getAllByPlaceholderText(/new password/i);
    await user.type(passwordInputs[0], 'short');
    const confirmInputs = screen.getAllByPlaceholderText(/confirm new password/i);
    await user.type(confirmInputs[0], 'short');

    await user.click(screen.getByRole('button', { name: /change password/i }));

    await waitFor(() => {
      expect(screen.getByText(/Password must be at least 8 characters/i)).toBeInTheDocument();
    });
  });

  it('[M-10] uses parseApiError for error display on account deletion failure', async () => {
    const user = userEvent.setup();
    server.use(
      http.delete('*/delete-account', () =>
        HttpResponse.json(
          {
            status: 401,
            error: 'REQUEST_ERROR',
            message: 'Unauthorized',
          },
          { status: 401 }
        )
      )
    );

    renderSettings();

    await user.click(screen.getByRole('button', { name: /delete account/i }));

    await waitFor(() => {
      expect(screen.getByText(/Failed to delete account/i)).toBeInTheDocument();
    });
  });

  it('[M-10] uses parseApiError for error display on language update failure', async () => {
    const user = userEvent.setup();
    server.use(
      http.put('*/update-language', () =>
        HttpResponse.json(
          {
            status: 400,
            error: 'VALIDATION_FAILED',
            message: 'Request validation failed.',
            fieldErrors: ['language: Language is required'],
          },
          { status: 400 }
        )
      )
    );

    renderSettings();

    const languageSelect = screen.getByRole('combobox');
    await user.selectOptions(languageSelect, 'French');

    await waitFor(() => {
      expect(screen.getByText(/Language is required/i)).toBeInTheDocument();
    });
  });

  it('[M-18] file input accept attribute includes image/jpeg', () => {
    const { container } = renderSettings();

    const fileInput = container.querySelector('input[type="file"]');
    expect(fileInput).not.toBeNull();
    const acceptAttr = fileInput.getAttribute('accept');
    expect(acceptAttr).toContain('image/png');
    expect(acceptAttr).toContain('image/jpeg');
  });

  it('[M-18] file input does NOT accept only image/png', () => {
    const { container } = renderSettings();

    const fileInput = container.querySelector('input[type="file"]');
    expect(fileInput).not.toBeNull();
    const acceptAttr = fileInput.getAttribute('accept');
    expect(acceptAttr).not.toBe('image/png');
  });

  it('[M-22] handles INVALID_FILE_FORMAT error code with specific message', async () => {
    const user = userEvent.setup();

    const file = new File(['fake-image'], 'test.png', { type: 'image/png' });
    global.FileReader = class {
      onloadend = null;
      readAsArrayBuffer() {
        this.result = new ArrayBuffer(8);
        this.onloadend && this.onloadend();
      }
    };

    server.use(
      http.post('*/profile-picture', () =>
        HttpResponse.json(
          {
            status: 400,
            error: 'INVALID_FILE_FORMAT',
            message: 'Invalid file format',
          },
          { status: 400 }
        )
      )
    );

    const { container } = renderSettings();

    const fileInput = container.querySelector('input[type="file"]');
    expect(fileInput).not.toBeNull();
    await user.upload(fileInput, file);

    const uploadButton = screen.getByRole('button', { name: /upload profile picture/i });
    await user.click(uploadButton);

    await waitFor(() => {
      expect(screen.getByText(/Only PNG and JPEG images under 5 MB/i)).toBeInTheDocument();
    });
  });

  it('[M-22] handles USER_NOT_FOUND error code on password change', async () => {
    const user = userEvent.setup();
    server.use(
      http.put('*/change-password', () =>
        HttpResponse.json(
          {
            status: 404,
            error: 'USER_NOT_FOUND',
            message: 'User not found',
          },
          { status: 404 }
        )
      )
    );

    renderSettings();

    const passwordInputs = screen.getAllByPlaceholderText(/new password/i);
    await user.type(passwordInputs[0], 'password123');
    const confirmInputs = screen.getAllByPlaceholderText(/confirm new password/i);
    await user.type(confirmInputs[0], 'password123');

    await user.click(screen.getByRole('button', { name: /change password/i }));

    await waitFor(() => {
      expect(screen.getByText(/User not found/i)).toBeInTheDocument();
    });
  });
});
