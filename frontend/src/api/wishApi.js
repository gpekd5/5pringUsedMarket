import apiClient from './apiClient.js';

function unwrapApiResponse(response) {
  return response.data?.data ?? response.data;
}

export function getWishApiErrorMessage(error, fallbackMessage = '관심상품 요청을 처리하지 못했습니다.') {
  return error?.response?.data?.message || error?.message || fallbackMessage;
}

export async function addWish(productId) {
  const response = await apiClient.post(`/api/products/${productId}/wishes`);

  return unwrapApiResponse(response);
}

export async function removeWish(productId) {
  const response = await apiClient.delete(`/api/products/${productId}/wishes`);

  return unwrapApiResponse(response);
}

export async function getMyWishes() {
  const response = await apiClient.get('/api/members/me/wishes');
  const wishes = unwrapApiResponse(response);

  return Array.isArray(wishes) ? wishes : [];
}
