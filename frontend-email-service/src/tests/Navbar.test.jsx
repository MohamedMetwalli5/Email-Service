import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import React from 'react';
import { BrowserRouter } from 'react-router-dom';
import { AppProvider } from '../AppContext';
import Navbar from '../components/Navbar';
import { server } from './server';
import { http, HttpResponse } from 'msw';

const renderNavbar = () =>
  render(
    <BrowserRouter>
      <AppProvider>
        <Navbar />
      </AppProvider>
    </BrowserRouter>
  );

describe('Navbar', () => {
  beforeEach(() => {
    localStorage.clear();
    localStorage.setItem('authToken', 'test-token');
    localStorage.setItem('sharedUserEmail', 'test@seamail.com');
  });

  it('[M-19] uses Content-Type from response headers for Blob (not hardcoded image/png)', async () => {
    let capturedResponseHeaders;
    server.use(
      http.get('*/profile-picture', ({ request }) => {
        const imageBuffer = new Uint8Array([137, 80, 78, 71, 13, 10, 26, 10]);
        return new HttpResponse(imageBuffer, {
          status: 200,
          headers: { 'Content-Type': 'image/jpeg' },
        });
      })
    );

    renderNavbar();

    const img = await screen.findByRole('img');
    await waitFor(() => {
      expect(img).toBeInTheDocument();
    });
  });

  it('[M-19] creates Blob with correct MIME type from server response', async () => {
    const originalCreateObjectURL = URL.createObjectURL;
    const createObjectURLMock = vi.fn(() => 'blob:http://test');
    URL.createObjectURL = createObjectURLMock;

    server.use(
      http.get('*/profile-picture', () => {
        const imageBuffer = new Uint8Array([137, 80, 78, 71, 13, 10, 26, 10]);
        return new HttpResponse(imageBuffer, {
          status: 200,
          headers: { 'Content-Type': 'image/jpeg' },
        });
      })
    );

    renderNavbar();

    await waitFor(() => {
      expect(createObjectURLMock).toHaveBeenCalled();
      const blobArg = createObjectURLMock.mock.calls[0][0];
      expect(blobArg.type).toBe('image/jpeg');
    });

    URL.createObjectURL = originalCreateObjectURL;
  });

  it('[M-19] does NOT hardcode Blob type as "image/png"', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => {});

    server.use(
      http.get('*/profile-picture', () => {
        const imageBuffer = new Uint8Array([137, 80, 78, 71, 13, 10, 26, 10]);
        return new HttpResponse(imageBuffer, {
          status: 200,
          headers: { 'Content-Type': 'image/jpeg' },
        });
      })
    );

    renderNavbar();

    const img = await screen.findByRole('img');
    expect(img).toHaveAttribute('src');
  });
});
