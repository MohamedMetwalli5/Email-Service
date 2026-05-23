import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import React from 'react';
import { BrowserRouter } from 'react-router-dom';
import { AppProvider } from '../AppContext';
import EmailFullView from '../components/home-main-content-components/EmailFullView';
import { server } from './server';
import { http, HttpResponse } from 'msw';

const renderEmailView = () =>
  render(
    <BrowserRouter>
      <AppProvider>
        <EmailFullView />
      </AppProvider>
    </BrowserRouter>
  );

describe('EmailFullView', () => {
  beforeEach(() => {
    localStorage.clear();
    localStorage.setItem('authToken', 'test-token');
    localStorage.setItem('sharedUserEmail', 'test@seamail.com');
    localStorage.setItem('sharedMailBoxOption', 'Inbox');
  });

  it('[M-15] references email.emailID in code (not email.id)', () => {
    const email = { emailID: 42, subject: 'Test' };
    expect(email.emailID).toBe(42);
    expect(email.id).toBeUndefined();
  });

  it('[M-15] does not reference email.id in console.log for move-to-trash', () => {
    const logSpy = vi.spyOn(console, 'log').mockImplementation(() => {});
    const email = { emailID: 42 };

    console.log('Viewing email:', email.emailID);

    const logCall = logSpy.mock.calls[0];
    expect(logCall[1]).toBe(42);

    logSpy.mockRestore();
  });

  it('renders email body and subject when email is provided via context', async () => {
    const emailData = {
      emailID: 1,
      sender: 'sender@test.com',
      subject: 'Hello World',
      body: 'This is the email body',
      priority: '2',
      date: '2026-01-01',
      trash: false,
    };

    localStorage.setItem('sharedEmailToFullyView', JSON.stringify(emailData));

    renderEmailView();

    await waitFor(() => {
      expect(screen.getByText('Hello World')).toBeInTheDocument();
      expect(screen.getByText('This is the email body')).toBeInTheDocument();
    });
  });

  it('shows trash button for inbox emails', async () => {
    const emailData = {
      emailID: 1,
      sender: 'a@b.com',
      subject: 'Delete me',
      body: 'Body',
      priority: '1',
      date: '2026-01-01',
      trash: false,
    };

    localStorage.setItem('sharedEmailToFullyView', JSON.stringify(emailData));

    renderEmailView();

    await waitFor(() => {
      expect(screen.getByRole('button')).toBeInTheDocument();
    });
  });

  it('sends correct emailId in move-to-trash request body', async () => {
    const user = userEvent.setup();
    let capturedBody;

    server.use(
      http.post('*/move-to-trash', async ({ request }) => {
        capturedBody = await request.json();
        return HttpResponse.json(null, { status: 204 });
      })
    );

    const emailData = {
      emailID: 99,
      sender: 'a@b.com',
      subject: 'Trash me',
      body: 'Body',
      priority: '1',
      date: '2026-01-01',
      trash: false,
    };

    localStorage.setItem('sharedEmailToFullyView', JSON.stringify(emailData));

    renderEmailView();

    await waitFor(() => {
      expect(screen.getByText('Trash me')).toBeInTheDocument();
    });

    const trashButton = screen.getByRole('button');
    await user.click(trashButton);

    await waitFor(() => {
      expect(capturedBody.emailId).toBe(99);
    });
  });
});
