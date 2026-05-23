import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import MockAdapter from 'axios-mock-adapter';
import axios from 'axios';
import apiClient from '../api/apiClient';

describe('apiClient', () => {
  let mockAdapter;

  beforeEach(() => {
    localStorage.clear();
    localStorage.setItem('authToken', 'test-access-token');
    mockAdapter = new MockAdapter(apiClient);
  });

  afterEach(() => {
    mockAdapter.restore();
  });

  it('[G-01] attaches Authorization Bearer header from localStorage to every request', async () => {
    let capturedHeaders;
    mockAdapter.onGet('/emails').reply((config) => {
      capturedHeaders = config.headers;
      return [200, []];
    });

    await apiClient.get('/emails');

    expect(capturedHeaders['Authorization']).toBe('Bearer test-access-token');
  });

  it('[G-01] does not attach Bearer header when no token in localStorage', async () => {
    localStorage.removeItem('authToken');
    let capturedHeaders;
    mockAdapter.onGet('/inbox').reply((config) => {
      capturedHeaders = config.headers;
      return [200, []];
    });

    await apiClient.get('/inbox');

    expect(capturedHeaders['Authorization']).toBeUndefined();
  });

  it('[G-01] has Content-Type application/json by default', async () => {
    let capturedHeaders;
    mockAdapter.onGet('/inbox').reply((config) => {
      capturedHeaders = config.headers;
      return [200, []];
    });

    await apiClient.get('/inbox');

    expect(capturedHeaders['Content-Type']).toBe('application/json');
  });

  it('[M-13] intercepts 401 and calls POST /api/v1/auth/refresh when refreshToken exists', async () => {
    localStorage.setItem('refreshToken', 'test-refresh-token');

    const refreshAdapter = new MockAdapter(axios);
    refreshAdapter.onPost('/api/v1/auth/refresh').reply(200, {
      accessToken: 'new-access-token',
      refreshToken: 'new-refresh-token',
    });

    let firstCall = true;
    mockAdapter.onGet('/inbox').reply(() => {
      if (firstCall) {
        firstCall = false;
        return [401, { message: 'Unauthorized' }];
      }
      return [200, [{ emailID: 1, subject: 'Retried' }]];
    });

    const result = await apiClient.get('/inbox');

    expect(result.status).toBe(200);
    expect(result.data).toEqual([{ emailID: 1, subject: 'Retried' }]);
    expect(localStorage.getItem('authToken')).toBe('new-access-token');
    expect(localStorage.getItem('refreshToken')).toBe('new-refresh-token');

    refreshAdapter.restore();
  });

  it('[M-13] redirects to sign-in on 401 when no refreshToken exists', async () => {
    localStorage.removeItem('refreshToken');

    const originalLocation = window.location;
    delete window.location;
    window.location = { href: '' };

    mockAdapter.onGet('/inbox').reply(401, { message: 'Unauthorized' });

    await expect(apiClient.get('/inbox')).rejects.toThrow();

    expect(localStorage.getItem('authToken')).toBeNull();
    expect(window.location.href).toBe('/sign-in');

    window.location = originalLocation;
  });

  it('[G-02] queues concurrent requests during token refresh and retries them all', async () => {
    localStorage.setItem('refreshToken', 'test-refresh-token');

    const refreshAdapter = new MockAdapter(axios);
    refreshAdapter.onPost('/api/v1/auth/refresh').reply(200, {
      accessToken: 'new-access-token',
      refreshToken: 'new-refresh-token',
    });

    let callCount = 0;
    mockAdapter.onGet('/inbox').reply(() => {
      callCount++;
      if (callCount <= 1) {
        return [401, { message: 'Unauthorized' }];
      }
      return [200, [{ emailID: 1 }]];
    });

    mockAdapter.onGet('/outbox').reply(() => {
      callCount++;
      if (callCount <= 2) {
        return [401, { message: 'Unauthorized' }];
      }
      return [200, [{ emailID: 2 }]];
    });

    const [inboxResult, outboxResult] = await Promise.all([
      apiClient.get('/inbox'),
      apiClient.get('/outbox'),
    ]);

    expect(inboxResult.status).toBe(200);
    expect(outboxResult.status).toBe(200);

    refreshAdapter.restore();
  });

  it('[G-01] uses VITE_BACKEND_API_URL as baseURL', () => {
    expect(apiClient.defaults.baseURL).toBe(import.meta.env.VITE_BACKEND_API_URL);
  });

  it('[M-11] uses a single axios instance (not per-component axios imports)', () => {
    expect(typeof apiClient.get).toBe('function');
    expect(typeof apiClient.post).toBe('function');
    expect(typeof apiClient.put).toBe('function');
    expect(typeof apiClient.delete).toBe('function');
    expect(apiClient.interceptors.request.handlers.length).toBeGreaterThan(0);
    expect(apiClient.interceptors.response.handlers.length).toBeGreaterThan(0);
  });
});
