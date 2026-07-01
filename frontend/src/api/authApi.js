import apiClient from './apiClient.js';
import {
  clearAuthStorage,
  getAccessToken,
  setAuthTokens,
  setStoredAuthUser,
} from './authStorage.js';

function unwrapApiResponse(response) {
  return response.data?.data ?? response.data;
}

export function getApiErrorMessage(error, fallbackMessage = '요청 처리 중 문제가 발생했습니다.') {
  return error?.response?.data?.message || error?.message || fallbackMessage;
}

export async function login({ email, password }) {
  const response = await apiClient.post('/api/auth/login', {
    email: email.trim(),
    password,
  });
  const tokenResponse = unwrapApiResponse(response);

  setAuthTokens(tokenResponse);
  return tokenResponse;
}

export async function signup({ email, password, nickname }) {
  const response = await apiClient.post('/api/auth/signup', {
    email: email.trim(),
    password,
    nickname: nickname.trim(),
  });

  return unwrapApiResponse(response);
}

export async function fetchMe() {
  const response = await apiClient.get('/api/members/me');
  const user = unwrapApiResponse(response);

  setStoredAuthUser(user);
  return user;
}

export async function logout() {
  try {
    if (getAccessToken()) {
      await apiClient.post('/api/auth/logout');
    }
  } finally {
    clearAuthStorage();
  }
}
