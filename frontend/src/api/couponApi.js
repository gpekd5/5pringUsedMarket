import apiClient from './apiClient.js';

function unwrapApiResponse(response) {
  return response.data?.data ?? response.data;
}

function cleanParams(params) {
  return Object.fromEntries(
    Object.entries(params).filter(([, value]) => value !== undefined && value !== null && value !== ''),
  );
}

export function normalizeCouponPage(pageData = {}) {
  const content = Array.isArray(pageData.content) ? pageData.content : [];

  return {
    content,
    page: Number(pageData.number ?? pageData.page ?? 0),
    size: Number(pageData.size ?? pageData.pageable?.pageSize ?? content.length),
    totalElements: Number(pageData.totalElements ?? content.length),
    totalPages: Number(pageData.totalPages ?? 0),
  };
}

export function getCouponApiErrorMessage(error, fallbackMessage = '쿠폰 정보를 불러오지 못했습니다.') {
  return error?.response?.data?.message || error?.message || fallbackMessage;
}

export async function getCoupons({ page = 0, size = 20 } = {}) {
  const response = await apiClient.get('/api/coupons', {
    params: cleanParams({ page, size }),
  });

  return normalizeCouponPage(unwrapApiResponse(response));
}

export async function issueCoupon(couponId) {
  const response = await apiClient.post(`/api/coupons/${couponId}/issue`);

  return unwrapApiResponse(response);
}

export async function getMyCoupons({ used, page = 0, size = 100 } = {}) {
  const response = await apiClient.get('/api/members/me/coupons', {
    params: cleanParams({ used, page, size }),
  });

  return normalizeCouponPage(unwrapApiResponse(response));
}

export async function useCoupon(userCouponId) {
  const response = await apiClient.patch(`/api/user-coupons/${userCouponId}/use`);

  return unwrapApiResponse(response);
}
