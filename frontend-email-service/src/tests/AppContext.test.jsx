import { describe, it, expect, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import React, { useContext } from 'react';
import { AppProvider, AppContext } from '../AppContext';

const TestConsumer = () => {
  const ctx = useContext(AppContext);
  return (
    <div>
      <div data-testid="authToken">{String(ctx.authToken)}</div>
      <div data-testid="refreshToken">{String(ctx.refreshToken)}</div>
      <div data-testid="sharedUserEmail">{String(ctx.sharedUserEmail)}</div>
      <div data-testid="sharedMailBoxOption">{ctx.sharedMailBoxOption}</div>
      <div data-testid="sharedEmailToFullyView">{JSON.stringify(ctx.sharedEmailToFullyView)}</div>
      <div data-testid="sharedUserLanguage">{ctx.sharedUserLanguage}</div>
      <button data-testid="setAuthToken" onClick={() => ctx.setAuthToken('test-token')}>Set Auth</button>
      <button data-testid="setRefreshToken" onClick={() => ctx.setRefreshToken('test-refresh')}>Set Refresh</button>
      <button data-testid="clearSession" onClick={() => ctx.clearSession()}>Clear</button>
      <button data-testid="setSharedEmailToFullyView" onClick={() => ctx.setSharedEmailToFullyView({ emailID: 42, subject: 'Test' })}>Set Email</button>
    </div>
  );
};

const renderWithProvider = () => render(<AppProvider><TestConsumer /></AppProvider>);

describe('AppContext', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('[G-03] initializes authToken from localStorage', () => {
    localStorage.setItem('authToken', 'stored-token');
    const { getByTestId } = renderWithProvider();
    expect(getByTestId('authToken').textContent).toBe('stored-token');
  });

  it('[G-03] initializes refreshToken from localStorage', () => {
    localStorage.setItem('refreshToken', 'stored-refresh');
    const { getByTestId } = renderWithProvider();
    expect(getByTestId('refreshToken').textContent).toBe('stored-refresh');
  });

  it('[G-03] setAuthToken persists to localStorage and updates context', async () => {
    const user = userEvent.setup();
    const { getByTestId } = renderWithProvider();
    await user.click(getByTestId('setAuthToken'));
    expect(localStorage.getItem('authToken')).toBe('test-token');
    expect(getByTestId('authToken').textContent).toBe('test-token');
  });

  it('[G-03] setRefreshToken persists to localStorage and updates context', async () => {
    const user = userEvent.setup();
    const { getByTestId } = renderWithProvider();
    await user.click(getByTestId('setRefreshToken'));
    expect(localStorage.getItem('refreshToken')).toBe('test-refresh');
    expect(getByTestId('refreshToken').textContent).toBe('test-refresh');
  });

  it('[M-12] stores refreshToken alongside authToken', async () => {
    const user = userEvent.setup();
    const { getByTestId } = renderWithProvider();
    await user.click(getByTestId('setAuthToken'));
    await user.click(getByTestId('setRefreshToken'));
    expect(localStorage.getItem('authToken')).toBe('test-token');
    expect(localStorage.getItem('refreshToken')).toBe('test-refresh');
  });

  it('[M-16] setSharedEmailToFullyView serializes object with JSON.stringify', async () => {
    const user = userEvent.setup();
    const { getByTestId } = renderWithProvider();
    await user.click(getByTestId('setSharedEmailToFullyView'));
    const stored = localStorage.getItem('sharedEmailToFullyView');
    expect(() => JSON.parse(stored)).not.toThrow();
    expect(JSON.parse(stored)).toEqual({ emailID: 42, subject: 'Test' });
  });

  it('[M-16] sharedEmailToFullyView reads back as a proper object, not "[object Object]"', async () => {
    const user = userEvent.setup();
    const { getByTestId } = renderWithProvider();
    await user.click(getByTestId('setSharedEmailToFullyView'));
    const displayed = getByTestId('sharedEmailToFullyView').textContent;
    expect(displayed).not.toBe('"[object Object]"');
    expect(JSON.parse(displayed)).toEqual({ emailID: 42, subject: 'Test' });
  });

  it('[M-16] gracefully handles corrupted sharedEmailToFullyView in localStorage', () => {
    localStorage.setItem('sharedEmailToFullyView', '[object Object]');
    const { getByTestId } = renderWithProvider();
    expect(getByTestId('sharedEmailToFullyView').textContent).toBe('null');
  });

  it('[G-03] clearSession removes all tokens from localStorage and context', async () => {
    localStorage.setItem('authToken', 'tok');
    localStorage.setItem('refreshToken', 'ref');
    localStorage.setItem('sharedUserEmail', 'test@test.com');
    const user = userEvent.setup();
    const { getByTestId } = renderWithProvider();
    await user.click(getByTestId('clearSession'));
    expect(localStorage.getItem('authToken')).toBeNull();
    expect(localStorage.getItem('refreshToken')).toBeNull();
    expect(localStorage.getItem('sharedUserEmail')).toBeNull();
    expect(getByTestId('authToken').textContent).toBe('null');
    expect(getByTestId('refreshToken').textContent).toBe('null');
  });
});
