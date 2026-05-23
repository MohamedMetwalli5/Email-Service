import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import React from 'react';
import { BrowserRouter } from 'react-router-dom';
import { AppProvider } from '../AppContext';
import NewMessageComposer from '../components/NewMessageComposer';
import { server } from './server';
import { http, HttpResponse } from 'msw';

const renderComposer = (onClose = vi.fn()) =>
  render(
    <BrowserRouter>
      <AppProvider>
        <NewMessageComposer onClose={onClose} />
      </AppProvider>
    </BrowserRouter>
  );

describe('NewMessageComposer', () => {
  beforeEach(() => {
    localStorage.clear();
    localStorage.setItem('authToken', 'test-token');
    localStorage.setItem('sharedUserEmail', 'sender@seamail.com');
  });

  it('[M-10] uses parseApiError to display fieldErrors', async () => {
    const user = userEvent.setup();
    server.use(
      http.post('*/send-email', () =>
        HttpResponse.json(
          {
            status: 400,
            error: 'VALIDATION_FAILED',
            message: 'Request validation failed.',
            fieldErrors: ['receiver: Receiver email is required', 'subject: Subject is required'],
          },
          { status: 400 }
        )
      )
    );

    renderComposer();

    const inputs = screen.getAllByRole('textbox');
    await user.type(inputs[0], 'test@test.com');
    await user.type(inputs[1], 'Test');
    const textareas = screen.getAllByRole('textbox');
    await user.type(textareas[2], 'Hello');
    await user.click(screen.getByRole('button', { name: /send/i }));

    await waitFor(() => {
      expect(screen.getByText(/receiver email is required/i)).toBeInTheDocument();
    });
  });

  it('[M-10] handles RECEIVER_NOT_FOUND error code with specific message', async () => {
    const user = userEvent.setup();
    server.use(
      http.post('*/send-email', () =>
        HttpResponse.json(
          {
            status: 404,
            error: 'RECEIVER_NOT_FOUND',
            message: 'Receiver not found',
          },
          { status: 404 }
        )
      )
    );

    renderComposer();

    const inputs = screen.getAllByRole('textbox');
    await user.type(inputs[0], 'nonexistent@test.com');
    await user.type(inputs[1], 'Test');
    await user.type(inputs[2], 'Body');
    await user.click(screen.getByRole('button', { name: /send/i }));

    await waitFor(() => {
      expect(screen.getByText(/recipient was not found/i)).toBeInTheDocument();
    });
  });

  it('[M-10] displays generic error message when no fieldErrors and no specific code', async () => {
    const user = userEvent.setup();
    server.use(
      http.post('*/send-email', () =>
        HttpResponse.json(
          { status: 500, error: 'INTERNAL_ERROR', message: 'Server error' },
          { status: 500 }
        )
      )
    );

    renderComposer();

    const inputs = screen.getAllByRole('textbox');
    await user.type(inputs[0], 'test@test.com');
    await user.type(inputs[1], 'Test');
    await user.type(inputs[2], 'Body');
    await user.click(screen.getByRole('button', { name: /send/i }));

    await waitFor(() => {
      expect(screen.getByText(/Server error/i)).toBeInTheDocument();
    });
  });

  it('[M-10] does NOT read error.response?.data?.errors (old broken field)', async () => {
    const user = userEvent.setup();
    server.use(
      http.post('*/send-email', () =>
        HttpResponse.json(
          {
            errors: ['Field error 1', 'Field error 2'],
          },
          { status: 400 }
        )
      )
    );

    renderComposer();

    const inputs = screen.getAllByRole('textbox');
    await user.type(inputs[0], 'test@test.com');
    await user.type(inputs[1], 'Test');
    await user.type(inputs[2], 'Body');
    await user.click(screen.getByRole('button', { name: /send/i }));

    await waitFor(() => {
      expect(screen.queryByText(/Field error 1/)).not.toBeInTheDocument();
    });
  });

  it('sends correct payload (receiver, subject, body, priority)', async () => {
    const user = userEvent.setup();
    let capturedBody;

    server.use(
      http.post('*/send-email', async ({ request }) => {
        capturedBody = await request.json();
        return HttpResponse.json(null, { status: 201 });
      })
    );

    const onClose = vi.fn();
    renderComposer(onClose);

    const inputs = screen.getAllByRole('textbox');
    await user.type(inputs[0], 'receiver@seamail.com');
    await user.type(inputs[1], 'Hello');
    await user.type(inputs[2], 'Message body');
    await user.click(screen.getByRole('button', { name: /send/i }));

    await waitFor(() => {
      expect(capturedBody).toEqual({
        receiver: 'receiver@seamail.com',
        subject: 'Hello',
        body: 'Message body',
        priority: '1',
      });
    });
  });
});
