import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import React from 'react';
import { server } from './server';
import { http, HttpResponse } from 'msw';

const originalLocation = window.location;

describe('SigninWithDiscord', () => {
  beforeEach(() => {
    Object.defineProperty(window, 'location', {
      configurable: true,
      writable: true,
      value: new URL('http://localhost:8080/sign-in'),
    });
    server.use(
      http.get('*/auth/discord/state', () =>
        HttpResponse.json({ state: 'test-csrf-state' })
      )
    );
  });

  afterEach(() => {
    Object.defineProperty(window, 'location', {
      configurable: true,
      writable: true,
      value: new URL('http://localhost:8080/sign-in'),
    });
  });

  it('[M-06] uses VITE_DISCORD_REDIRECT_URI from env (not hardcoded /DiscordSignin)', async () => {
    const { default: SignInWithDiscord } = await import('../components/SigninWithDiscord');
    const user = userEvent.setup();
    const redirectUri = import.meta.env.VITE_DISCORD_REDIRECT_URI;

    render(<SignInWithDiscord />);

    await user.click(screen.getByRole('button', { name: /sign in with discord/i }));

    await waitFor(() => {
      const url = new URL(window.location.href);
      const redirectParam = url.searchParams.get('redirect_uri');
      expect(redirectParam).toBe(redirectUri);
    });
  });

  it('[M-06] constructs Discord OAuth URL with client_id, redirect_uri, scope, and state', async () => {
    const { default: SignInWithDiscord } = await import('../components/SigninWithDiscord');
    const user = userEvent.setup();
    const clientId = import.meta.env.VITE_CLIENT_ID;
    const discordPattern = /^https:\/\/discord\.com\/oauth2\/authorize\?/;

    render(<SignInWithDiscord />);

    await user.click(screen.getByRole('button', { name: /sign in with discord/i }));

    await waitFor(() => {
      expect(window.location.href).toMatch(discordPattern);
      expect(window.location.href).toContain(`client_id=${clientId}`);
      expect(window.location.href).toContain('response_type=code');
      expect(window.location.href).toContain('scope=identify%20email');
      expect(window.location.href).toContain('state=');
    });
  });

  it('[M-06] does NOT hardcode /DiscordSignin in the redirect URI', () => {
    const uri = import.meta.env.VITE_DISCORD_REDIRECT_URI;
    expect(uri).not.toMatch(/\/DiscordSignin$/);
    expect(uri).toMatch(/\/api\/v1\/auth\/discord$/);
  });

  it('[M-06] generates a unique state parameter for CSRF protection', async () => {
    let stateCounter = 0;
    server.use(
      http.get('*/auth/discord/state', () => {
        stateCounter++;
        return HttpResponse.json({ state: `state-${stateCounter}` });
      })
    );

    const { default: SignInWithDiscord } = await import('../components/SigninWithDiscord');
    const user = userEvent.setup();

    render(<SignInWithDiscord />);
    await user.click(screen.getByRole('button', { name: /sign in with discord/i }));
    await waitFor(() => expect(window.location.href).toContain('state=state-1'));
    const firstState = new URL(window.location.href).searchParams.get('state');

    Object.defineProperty(window, 'location', {
      configurable: true,
      writable: true,
      value: new URL('http://localhost:8080/sign-in'),
    });
    const { default: SignInWithDiscord2 } = await import('../components/SigninWithDiscord');
    render(<SignInWithDiscord2 />);
    await user.click(screen.getAllByRole('button', { name: /sign in with discord/i })[0]);
    await waitFor(() => expect(window.location.href).toContain('state=state-2'));
    const secondState = new URL(window.location.href).searchParams.get('state');

    expect(firstState).not.toBe(secondState);
  });
});
