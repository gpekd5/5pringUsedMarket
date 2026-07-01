import axios from 'axios';
import apiClient from './apiClient.js';

const ALLOWED_IMAGE_CONTENT_TYPES = new Set(['image/jpeg', 'image/png']);
const ALLOWED_IMAGE_EXTENSIONS = new Set(['jpg', 'jpeg', 'png']);

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

export async function uploadProductImage(file) {
  validateProductImageFile(file);

  const presignedResponse = await apiClient.post('/api/images/presigned-url', {
    fileName: file.name,
    contentType: file.type,
    fileSize: file.size,
  });
  const { imageKey, uploadUrl } = unwrapApiResponse(presignedResponse);

  await axios.put(uploadUrl, file, {
    headers: {
      'Content-Type': file.type,
    },
  });

  return { imageKey };
}

function validateProductImageFile(file) {
  if (!file) {
    throw new Error('이미지 파일을 선택해주세요.');
  }

  const contentType = file.type?.toLowerCase();
  const extension = file.name?.split('.').pop()?.toLowerCase();

  if (!ALLOWED_IMAGE_CONTENT_TYPES.has(contentType) || !ALLOWED_IMAGE_EXTENSIONS.has(extension)) {
    throw new Error('상품 이미지는 jpg, jpeg, png 파일만 업로드할 수 있습니다.');
  }
}

export async function createProduct({ title, price, description, category, imageKeys = [] }) {
  const response = await apiClient.post('/api/products', {
    title: title.trim(),
    price: Number(price),
    description: description.trim(),
    category,
    imageKeys,
  });

  return unwrapApiResponse(response);
}

export async function updateProduct(productId, { title, price, description, category, imageKeys }) {
  const payload = {};

  if (title !== undefined) {
    payload.title = title.trim();
  }

  if (price !== undefined && price !== null && price !== '') {
    payload.price = Number(price);
  }

  if (description !== undefined) {
    payload.description = description.trim();
  }

  if (category) {
    payload.category = category;
  }

  if (imageKeys !== undefined) {
    payload.imageKeys = imageKeys;
  }

  const response = await apiClient.patch(`/api/products/${productId}`, payload);

  return unwrapApiResponse(response);
}

export async function deleteProduct(productId) {
  const response = await apiClient.delete(`/api/products/${productId}`);

  return unwrapApiResponse(response);
}

export async function updateProductStatus(productId, status) {
  const response = await apiClient.patch(`/api/products/${productId}/status`, {
    status,
  });

  return unwrapApiResponse(response);
}

export async function cancelProductReservation(productId) {
  const response = await apiClient.patch(`/api/products/${productId}/status/cancel-reservation`);

  return unwrapApiResponse(response);
}

export async function getMyProducts({ status, page = 0, size = 20 } = {}) {
  const response = await apiClient.get('/api/products/me', {
    params: cleanParams({ status, page, size }),
  });

  return normalizeProductPage(unwrapApiResponse(response));
}

export async function getMemberProfile(memberId) {
  const response = await apiClient.get(`/api/members/${memberId}/profile`);

  return unwrapApiResponse(response);
}

export async function getPopularSearches() {
  const response = await apiClient.get('/api/search/popular');
  const popularSearches = unwrapApiResponse(response);

  return Array.isArray(popularSearches) ? popularSearches : [];
}

export async function getRecentSearches() {
  const response = await apiClient.get('/api/search/recent');
  const recentSearches = unwrapApiResponse(response);

  return Array.isArray(recentSearches) ? recentSearches : [];
}

export async function deleteRecentSearch(searchLogId) {
  const response = await apiClient.delete(`/api/search/recent/${searchLogId}`);

  return unwrapApiResponse(response);
}
