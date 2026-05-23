import { describe, it, expect } from 'vitest';
import { parseApiError } from '../utils/parseApiError';

describe('parseApiError', () => {
  it('[G-04] returns NETWORK_ERROR for errors without a response', () => {
    const error = new Error('Network failure');

    const result = parseApiError(error);

    expect(result).toEqual({
      status: 0,
      errorCode: 'NETWORK_ERROR',
      message: 'Network error. Please check your connection.',
      fieldErrors: [],
    });
  });

  it('[G-04] parses Spring Security 401 shape with message only', () => {
    const error = {
      response: {
        status: 401,
        data: { message: 'Unauthorized' },
      },
    };

    const result = parseApiError(error);

    expect(result).toEqual({
      status: 401,
      errorCode: 'UNAUTHORIZED',
      message: 'Unauthorized',
      fieldErrors: [],
    });
  });

  it('[G-04] parses Spring Security 401 when data.message is missing', () => {
    const error = {
      response: {
        status: 401,
        data: {},
      },
    };

    const result = parseApiError(error);

    expect(result).toEqual({
      status: 401,
      errorCode: 'UNAUTHORIZED',
      message: 'Session expired. Please sign in again.',
      fieldErrors: [],
    });
  });

  it('[G-04] parses ValidationErrorResponse with fieldErrors', () => {
    const error = {
      response: {
        status: 400,
        data: {
          status: 400,
          error: 'VALIDATION_FAILED',
          message: 'Request validation failed.',
          path: '/api/v1/sign-up',
          timestamp: '2026-05-23T14:30:00',
          fieldErrors: [
            'email: Email is required',
            'password: Password is required',
          ],
        },
      },
    };

    const result = parseApiError(error);

    expect(result).toEqual({
      status: 400,
      errorCode: 'VALIDATION_FAILED',
      message: 'Request validation failed.',
      fieldErrors: [
        'email: Email is required',
        'password: Password is required',
      ],
    });
  });

  it('[G-04] parses Standard ErrorResponse with error code and message', () => {
    const error = {
      response: {
        status: 404,
        data: {
          status: 404,
          error: 'USER_NOT_FOUND',
          message: 'User not found',
          path: '/api/v1/sign-in',
          timestamp: '2026-05-23T14:30:00',
        },
      },
    };

    const result = parseApiError(error);

    expect(result).toEqual({
      status: 404,
      errorCode: 'USER_NOT_FOUND',
      message: 'User not found',
      fieldErrors: [],
    });
  });

  it('[G-04] parses ErrorResponse with REQUEST_ERROR code', () => {
    const error = {
      response: {
        status: 401,
        data: {
          status: 401,
          error: 'REQUEST_ERROR',
          message: 'Invalid or expired refresh token',
          path: '/api/v1/auth/refresh',
          timestamp: '2026-05-23T14:30:00',
        },
      },
    };

    const result = parseApiError(error);

    expect(result).toEqual({
      status: 401,
      errorCode: 'REQUEST_ERROR',
      message: 'Invalid or expired refresh token',
      fieldErrors: [],
    });
  });

  it('[G-04] parses generic ErrorResponse with UNKNOWN_ERROR fallback', () => {
    const error = {
      response: {
        status: 500,
        data: {
          status: 500,
          error: 'INTERNAL_ERROR',
          message: 'An unexpected error occurred.',
        },
      },
    };

    const result = parseApiError(error);

    expect(result).toEqual({
      status: 500,
      errorCode: 'INTERNAL_ERROR',
      message: 'An unexpected error occurred.',
      fieldErrors: [],
    });
  });

  it('[G-04] handles completely unknown response with fallback values', () => {
    const error = {
      response: {
        status: 418,
        data: {},
      },
    };

    const result = parseApiError(error);

    expect(result).toEqual({
      status: 418,
      errorCode: 'UNKNOWN_ERROR',
      message: 'An unexpected error occurred.',
      fieldErrors: [],
    });
  });

  it('[M-10] does NOT read from field "errors" (which the backend never sends)', () => {
    const error = {
      response: {
        status: 400,
        data: {
          errors: ['email is invalid', 'password is required'],
        },
      },
    };

    const result = parseApiError(error);

    expect(result.fieldErrors).toEqual([]);
    expect(result.errorCode).not.toBe('VALIDATION_FAILED');
  });
});
