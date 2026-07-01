import apiClient from './apiClient.js';

function unwrapApiResponse(response) {
  return response.data?.data ?? response.data;
}

function cleanParams(params) {
  return Object.fromEntries(
    Object.entries(params).filter(([, value]) => value !== undefined && value !== null && value !== ''),
  );
}

export function normalizeChatRoomPage(pageData = {}) {
  const content = Array.isArray(pageData.content) ? pageData.content : [];

  return {
    content,
    page: Number(pageData.number ?? pageData.page ?? 0),
    size: Number(pageData.size ?? pageData.pageable?.pageSize ?? content.length),
    totalElements: Number(pageData.totalElements ?? content.length),
    totalPages: Number(pageData.totalPages ?? 0),
  };
}

export function getChatApiErrorMessage(error, fallbackMessage = '채팅 정보를 불러오지 못했습니다.') {
  return error?.response?.data?.message || error?.message || fallbackMessage;
}

export async function createTradeChatRoom(productId) {
  const response = await apiClient.post('/api/chat/rooms/trade', {
    productId: Number(productId),
  });

  return unwrapApiResponse(response);
}

export async function createCsChatRoom(title) {
  const response = await apiClient.post('/api/chat/rooms/cs', {
    title,
  });

  return unwrapApiResponse(response);
}

export async function getChatRooms({ page = 0, size = 20 } = {}) {
  const response = await apiClient.get('/api/chat/rooms', {
    params: cleanParams({ page, size }),
  });

  return normalizeChatRoomPage(unwrapApiResponse(response));
}

export async function getChatRoom(roomId) {
  const response = await apiClient.get(`/api/chat/rooms/${roomId}`);

  return unwrapApiResponse(response);
}

export async function getChatMessages(roomId, { lastMessageId, size = 30 } = {}) {
  const response = await apiClient.get(`/api/chat/rooms/${roomId}/messages`, {
    params: cleanParams({ lastMessageId, size }),
  });

  const messageList = unwrapApiResponse(response);

  return {
    messages: Array.isArray(messageList.messages) ? messageList.messages : [],
    hasNext: Boolean(messageList.hasNext),
    nextCursorId: messageList.nextCursorId ?? null,
  };
}

export async function markChatRoomAsRead(roomId) {
  const response = await apiClient.patch(`/api/chat/rooms/${roomId}/read`);

  return unwrapApiResponse(response);
}
