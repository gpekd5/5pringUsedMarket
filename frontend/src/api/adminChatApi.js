import apiClient from './apiClient.js';

function unwrapApiResponse(response) {
  return response.data?.data ?? response.data;
}

function cleanParams(params) {
  return Object.fromEntries(
    Object.entries(params).filter(([, value]) => value !== undefined && value !== null && value !== ''),
  );
}

export function normalizeAdminCsRoomPage(pageData = {}) {
  const content = Array.isArray(pageData.content) ? pageData.content : [];

  return {
    content,
    page: Number(pageData.number ?? pageData.page ?? 0),
    size: Number(pageData.size ?? pageData.pageable?.pageSize ?? content.length),
    totalElements: Number(pageData.totalElements ?? content.length),
    totalPages: Number(pageData.totalPages ?? 0),
  };
}

export function getAdminChatApiErrorMessage(error, fallbackMessage = '관리자 CS 채팅 정보를 불러오지 못했습니다.') {
  return error?.response?.data?.message || error?.message || fallbackMessage;
}

export async function getAdminCsRooms({ status = 'WAITING', page = 0, size = 10 } = {}) {
  const response = await apiClient.get('/api/admin/chat/rooms/cs', {
    params: cleanParams({ status, page, size }),
  });

  return normalizeAdminCsRoomPage(unwrapApiResponse(response));
}

export async function enterAdminCsRoom(roomId) {
  const response = await apiClient.post(`/api/admin/chat/rooms/cs/${roomId}/enter`);

  return unwrapApiResponse(response);
}

export async function updateAdminCsRoomStatus(roomId, status) {
  const response = await apiClient.patch(`/api/admin/chat/rooms/cs/${roomId}/status`, {
    status,
  });

  return unwrapApiResponse(response);
}
