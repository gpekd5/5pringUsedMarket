import apiClient from './apiClient.js';
import { getStoredAuthUser, setStoredAuthUser } from './authStorage.js';

function unwrapApiResponse(response) {
  return response.data?.data ?? response.data;
}

export function getMyPageApiErrorMessage(error, fallbackMessage = '마이페이지 정보를 불러오지 못했습니다.') {
  return error?.response?.data?.message || error?.message || fallbackMessage;
}

export async function getMyPage() {
  const response = await apiClient.get('/api/mypage');

  return unwrapApiResponse(response);
}

export async function updateMyInfo({ nickname }) {
  const response = await apiClient.patch('/api/members/me', {
    nickname: nickname.trim(),
  });
  const user = unwrapApiResponse(response);

  setStoredAuthUser({ ...getStoredAuthUser(), ...user });
  return user;
}
