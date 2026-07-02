import axios from 'axios';
import {
  clearAuthStorage,
  getAccessToken,
  getRefreshToken,
  setAuthTokens,
} from './authStorage.js';

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
let tokenRefreshPromise = null;

const apiClient = axios.create({
  baseURL: apiBaseUrl,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

apiClient.interceptors.request.use((config) => {
  const accessToken = getAccessToken();

  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }

  return config;
});

async function reissueToken() {
  const refreshToken = getRefreshToken();

  if (!refreshToken) {
    throw new Error('Refresh token is missing.');
  }

  const response = await axios.post(
    `${apiBaseUrl}/api/auth/reissue`,
    { refreshToken },
    {
      headers: {
        'Content-Type': 'application/json',
      },
    },
  );
  const tokenResponse = response.data?.data ?? response.data;

  setAuthTokens(tokenResponse);
  return tokenResponse;
}

function shouldTryReissue(error) {
  const originalRequest = error.config;
  const errorCode = error.response?.data?.code;

  return (
    error.response?.status === 401 &&
    errorCode === 'EXPIRED_TOKEN' &&
    !originalRequest?._retry &&
    !originalRequest?.url?.includes('/api/auth/reissue') &&
    Boolean(getRefreshToken())
  );
}

function isPublicReadRequest(config = {}) {
  const method = (config.method || 'get').toLowerCase();
  const url = config.url || '';

  return (
    method === 'get' &&
    (url === '/api/products' ||
      url.startsWith('/api/v3/products/search') ||
      url.startsWith('/api/search/popular') ||
      /^\/api\/products\/\d+$/.test(url))
  );
}

function removeAuthorizationHeader(config) {
  if (config?.headers) {
    delete config.headers.Authorization;
    delete config.headers.authorization;
  }
}

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (!shouldTryReissue(error)) {
      if (error.response?.status === 401 && !getRefreshToken()) {
        clearAuthStorage();

        if (isPublicReadRequest(originalRequest) && !originalRequest._publicRetry) {
          originalRequest._publicRetry = true;
          removeAuthorizationHeader(originalRequest);
          return apiClient(originalRequest);
        }
      }

      return Promise.reject(error);
    }

    originalRequest._retry = true;

    try {
      tokenRefreshPromise ??= reissueToken();
      const tokenResponse = await tokenRefreshPromise;

      originalRequest.headers.Authorization = `${tokenResponse.tokenType || 'Bearer'} ${tokenResponse.accessToken}`;
      return apiClient(originalRequest);
    } catch (refreshError) {
      clearAuthStorage();

      if (isPublicReadRequest(originalRequest) && !originalRequest._publicRetry) {
        originalRequest._publicRetry = true;
        removeAuthorizationHeader(originalRequest);
        return apiClient(originalRequest);
      }

      return Promise.reject(refreshError);
    } finally {
      tokenRefreshPromise = null;
    }
  },
);

export default apiClient;
export { apiBaseUrl };
