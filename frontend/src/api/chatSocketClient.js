import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { apiBaseUrl } from './apiClient.js';
import { getAccessToken } from './authStorage.js';

export const CHAT_SOCKET_ENDPOINT = `${apiBaseUrl}/ws-chat`;
export const CHAT_SUBSCRIBE_DESTINATION_PREFIX = '/sub/chat/rooms';
export const CHAT_PUBLISH_DESTINATION_PREFIX = '/pub/chat/rooms';

export function createChatSocketClient({ onConnect, onDisconnect, onError } = {}) {
  const accessToken = getAccessToken();

  return new Client({
    webSocketFactory: () => new SockJS(CHAT_SOCKET_ENDPOINT),
    connectHeaders: accessToken
      ? {
          Authorization: `Bearer ${accessToken}`,
        }
      : {},
    reconnectDelay: 0,
    debug: () => {},
    onConnect,
    onDisconnect,
    onStompError: (frame) => {
      onError?.(new Error(frame.headers?.message || frame.body || 'STOMP 연결 오류가 발생했습니다.'));
    },
    onWebSocketError: () => {
      onError?.(new Error('채팅 서버와 연결하지 못했습니다.'));
    },
  });
}

export function connectChatSocket(client) {
  if (!client.active) {
    client.activate();
  }
}

export function disconnectChatSocket(client) {
  if (client?.active) {
    client.deactivate();
  }
}

export function subscribeRoom(client, roomId, onMessage) {
  return client.subscribe(`${CHAT_SUBSCRIBE_DESTINATION_PREFIX}/${roomId}`, (message) => {
    if (!message.body) {
      return;
    }

    onMessage(JSON.parse(message.body));
  });
}

export function sendMessage(client, roomId, content) {
  if (!client?.connected) {
    throw new Error('채팅 서버에 연결되지 않았습니다.');
  }

  client.publish({
    destination: `${CHAT_PUBLISH_DESTINATION_PREFIX}/${roomId}/messages`,
    body: JSON.stringify({
      type: 'TALK',
      content: content.trim(),
    }),
  });
}
