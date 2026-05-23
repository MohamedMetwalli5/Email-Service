/**
 * Parses any error shape the renovated backend can return:
 *   1. ErrorResponse      — { status, error, message, path, timestamp }
 *   2. ValidationErrorResponse — { ..., error: "VALIDATION_FAILED", fieldErrors: string[] }
 *   3. Spring Security 401 — { message: "Unauthorized" }
 *   4. Network / unknown  — no response object
 *
 * Returns: { status, errorCode, message, fieldErrors }
 *
 * Unified error parser for multiple backend error shapes
 */
export function parseApiError(error) {
  if (!error.response) {
    return {
      status: 0,
      errorCode: 'NETWORK_ERROR',
      message: 'Network error. Please check your connection.',
      fieldErrors: [],
    };
  }

  const data = error.response.data || {};
  const status = error.response.status;

  // Spring Security 401 shape: { message: "Unauthorized" }
  if (status === 401 && !data.error) {
    return {
      status: 401,
      errorCode: 'UNAUTHORIZED',
      message: data.message || 'Session expired. Please sign in again.',
      fieldErrors: [],
    };
  }

  // ValidationErrorResponse: fieldErrors is a string[] of "field: message"
  // Backend sends fieldErrors, not errors
  if (data.error === 'VALIDATION_FAILED' && Array.isArray(data.fieldErrors)) {
    return {
      status: data.status || status,
      errorCode: 'VALIDATION_FAILED',
      message: data.message || 'Validation failed.',
      fieldErrors: data.fieldErrors,
    };
  }

  // Standard ErrorResponse
  return {
    status: data.status || status,
    errorCode: data.error || 'UNKNOWN_ERROR',
    message: data.message || 'An unexpected error occurred.',
    fieldErrors: [],
  };
}
