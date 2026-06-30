import apiClient from './apiClient.js';

function unwrapApiResponse(response) {
  return response.data?.data ?? response.data;
}

function cleanParams(params) {
  return Object.fromEntries(
    Object.entries(params).filter(([, value]) => value !== undefined && value !== null && value !== ''),
  );
}

export function normalizeProductPage(pageData = {}) {
  const content = Array.isArray(pageData.content) ? pageData.content : [];

  return {
    content,
    page: Number(pageData.page ?? pageData.number ?? 0),
    size: Number(pageData.size ?? pageData.pageable?.pageSize ?? content.length),
    totalElements: Number(pageData.totalElements ?? content.length),
    totalPages: Number(pageData.totalPages ?? 0),
  };
}

export function getProductApiErrorMessage(error, fallbackMessage = '상품 정보를 불러오지 못했습니다.') {
  return error?.response?.data?.message || error?.message || fallbackMessage;
}

export async function searchProducts({ keyword, category, status, sort = 'LATEST', page = 0, size = 12 } = {}) {
  const response = await apiClient.get('/api/v3/products/search', {
    params: cleanParams({
      keyword,
      category,
      status,
      sort,
      page,
      size,
    }),
  });

  return normalizeProductPage(unwrapApiResponse(response));
}

export async function getProduct(productId) {
  const response = await apiClient.get(`/api/products/${productId}`);

  return unwrapApiResponse(response);
}

export async function getPopularSearches() {
  const response = await apiClient.get('/api/search/popular');
  const popularSearches = unwrapApiResponse(response);

  return Array.isArray(popularSearches) ? popularSearches : [];
}
