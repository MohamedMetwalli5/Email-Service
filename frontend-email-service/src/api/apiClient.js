// Centralized axios instance with automatic Bearer header injection
// Token refresh interceptor: catches 401, calls POST /api/v1/auth/refresh, retries original request
// Unified 401 recovery with refresh token rotation
// Single axios instance replaces per-component axios calls on all protected endpoints
import axios from 'axios';
import { parseApiError } from '../utils/parseApiError';

const BASE_URL = import.meta.env.VITE_BACKEND_API_URL;

const apiClient = axios.create({
  baseURL: BASE_URL,
  headers: { 'Content-Type': 'application/json' },
});

// ── Request interceptor: attach access token ──────────────────────────────
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('authToken');
  if (token) {
    config.headers['Authorization'] = `Bearer ${token}`;
  }
  return config;
});

// ── Response interceptor: handle 401 with refresh ────────────────────────
let isRefreshing = false;
let failedQueue = [];

const processQueue = (error, token = null) => {
  failedQueue.forEach((prom) => {
    if (error) prom.reject(error);
    else prom.resolve(token);
  });
  failedQueue = [];
};

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (error.response?.status === 401 && !originalRequest._retry) {
      const refreshToken = localStorage.getItem('refreshToken');

      if (!refreshToken) {
        redirectToSignIn();
        return Promise.reject(error);
      }

      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        }).then((token) => {
          originalRequest.headers['Authorization'] = `Bearer ${token}`;
          return apiClient(originalRequest);
        }).catch((err) => Promise.reject(err));
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        // Public endpoint — use plain axios to avoid circular interceptor
        const { data } = await axios.post(`${BASE_URL}/auth/refresh`, {
          refreshToken,
        });

        const newAccessToken = data.accessToken;
        const newRefreshToken = data.refreshToken;

        localStorage.setItem('authToken', newAccessToken);
        localStorage.setItem('refreshToken', newRefreshToken);
        apiClient.defaults.headers['Authorization'] = `Bearer ${newAccessToken}`;

        processQueue(null, newAccessToken);

        originalRequest.headers['Authorization'] = `Bearer ${newAccessToken}`;
        return apiClient(originalRequest);

      } catch (refreshError) {
        processQueue(refreshError, null);
        redirectToSignIn();
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);

function redirectToSignIn() {
  localStorage.removeItem('authToken');
  localStorage.removeItem('refreshToken');
  localStorage.removeItem('sharedUserEmail');
  window.location.href = '/sign-in';
}

export default apiClient;
