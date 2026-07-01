import { AlertCircle, ArrowLeft, Loader2, Send } from 'lucide-react';
import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { fetchMe } from '../api/authApi.js';
import { getAccessToken, getStoredAuthUser } from '../api/authStorage.js';
import {
  getChatApiErrorMessage,
  getChatMessages,
  getChatRoom,
  markChatRoomAsRead,
} from '../api/chatApi.js';
import {
  connectChatSocket,
  createChatSocketClient,
  disconnectChatSocket,
  sendMessage as sendSocketMessage,
  subscribeRoom,
} from '../api/chatSocketClient.js';
import StatusBadge from '../components/StatusBadge.jsx';
import routePaths from '../routes/routePaths.js';

const MESSAGE_PAGE_SIZE = 30;

function formatPrice(price) {
  if (typeof price === 'number') {
    return `${price.toLocaleString()}원`;
  }

  return price;
}

function formatMessageTime(value) {
  if (!value) {
    return '';
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return '';
  }

  return date.toLocaleTimeString('ko-KR', {
    hour: '2-digit',
    minute: '2-digit',
  });
}

function normalizeSocketMessage(message) {
  return {
    messageId: message.messageId,
    type: message.type,
    content: message.content,
    senderId: message.senderId,
    senderNickname: message.senderNickname,
    createdAt: message.createdAt,
  };
}

export default function ChatRoomDetailPage() {
  const { chatRoomId } = useParams();
  const navigate = useNavigate();
  const messageEndRef = useRef(null);
  const socketClientRef = useRef(null);
  const roomSubscriptionRef = useRef(null);
  const [currentUser, setCurrentUser] = useState(getStoredAuthUser);
  const [room, setRoom] = useState(null);
  const [messages, setMessages] = useState([]);
  const [nextCursorId, setNextCursorId] = useState(null);
  const [hasNext, setHasNext] = useState(false);
  const [messageInput, setMessageInput] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [isLoadingMore, setIsLoadingMore] = useState(false);
  const [isSending, setIsSending] = useState(false);
  const [socketStatus, setSocketStatus] = useState('idle');
  const [errorMessage, setErrorMessage] = useState('');

  useEffect(() => {
    let isActive = true;

    if (!getAccessToken()) {
      navigate(routePaths.login, {
        state: { message: '채팅은 로그인 후 이용할 수 있습니다.' },
      });
      return () => {
        isActive = false;
      };
    }

    async function loadChatRoom() {
      setIsLoading(true);
      setErrorMessage('');

      try {
        const authUser = getStoredAuthUser() || await fetchMe();
        const [roomData, messageData] = await Promise.all([
          getChatRoom(chatRoomId),
          getChatMessages(chatRoomId, { size: MESSAGE_PAGE_SIZE }),
        ]);

        if (isActive) {
          setCurrentUser(authUser);
          setRoom(roomData);
          setMessages(messageData.messages);
          setHasNext(messageData.hasNext);
          setNextCursorId(messageData.nextCursorId);
        }

        markChatRoomAsRead(chatRoomId).catch(() => {});

        const socketClient = createChatSocketClient({
          onConnect: () => {
            if (!isActive) {
              return;
            }

            setSocketStatus('connected');
            roomSubscriptionRef.current = subscribeRoom(socketClient, chatRoomId, (incomingMessage) => {
              const normalizedMessage = normalizeSocketMessage(incomingMessage);

              setMessages((prevMessages) => {
                const alreadyExists = prevMessages.some(
                  (message) => String(message.messageId) === String(normalizedMessage.messageId),
                );

                return alreadyExists ? prevMessages : [...prevMessages, normalizedMessage];
              });
              markChatRoomAsRead(chatRoomId).catch(() => {});
            });
          },
          onDisconnect: () => {
            if (isActive) {
              setSocketStatus('idle');
            }
          },
          onError: (error) => {
            if (isActive) {
              setSocketStatus('error');
              setErrorMessage(error.message || '채팅 서버 연결에 실패했습니다.');
            }
          },
        });

        socketClientRef.current = socketClient;
        setSocketStatus('connecting');
        connectChatSocket(socketClient);
      } catch (error) {
        if (isActive) {
          setErrorMessage(getChatApiErrorMessage(error, '채팅방을 불러오지 못했습니다.'));
        }
      } finally {
        if (isActive) {
          setIsLoading(false);
        }
      }
    }

    loadChatRoom();

    return () => {
      isActive = false;
      roomSubscriptionRef.current?.unsubscribe();
      disconnectChatSocket(socketClientRef.current);
    };
  }, [chatRoomId, navigate]);

  useEffect(() => {
    if (!isLoading) {
      messageEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }
  }, [isLoading, messages.length]);

  const handleLoadMore = async () => {
    if (!nextCursorId) {
      return;
    }

    setIsLoadingMore(true);
    setErrorMessage('');

    try {
      const olderMessages = await getChatMessages(chatRoomId, {
        lastMessageId: nextCursorId,
        size: MESSAGE_PAGE_SIZE,
      });

      setMessages((prevMessages) => [...olderMessages.messages, ...prevMessages]);
      setHasNext(olderMessages.hasNext);
      setNextCursorId(olderMessages.nextCursorId);
    } catch (error) {
      setErrorMessage(getChatApiErrorMessage(error, '이전 메시지를 불러오지 못했습니다.'));
    } finally {
      setIsLoadingMore(false);
    }
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    const content = messageInput.trim();
    if (!content || isSending) {
      return;
    }

    setIsSending(true);
    setErrorMessage('');

    try {
      sendSocketMessage(socketClientRef.current, chatRoomId, content);
      setMessageInput('');
    } catch (error) {
      setErrorMessage(error.message || '메시지 전송에 실패했습니다.');
    } finally {
      setIsSending(false);
    }
  };

  if (isLoading) {
    return (
      <div className="theme-card flex min-h-80 flex-col items-center justify-center gap-4 rounded-[32px] p-8 text-center">
        <Loader2 size={36} className="animate-spin text-[var(--color-primary)]" />
        <p className="text-sm font-bold text-[var(--color-text-sub)]">채팅방을 불러오는 중입니다</p>
      </div>
    );
  }

  if (errorMessage && !room) {
    return (
      <div className="theme-card flex min-h-80 flex-col items-center justify-center rounded-[32px] p-8 text-center">
        <div className="mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-red-50 text-red-500">
          <AlertCircle size={28} />
        </div>
        <h1 className="text-2xl font-black">채팅방을 찾을 수 없습니다</h1>
        <p className="mt-2 max-w-sm text-sm leading-6 text-[var(--color-text-sub)]">{errorMessage}</p>
        <Link
          to={routePaths.chats}
          className="theme-primary-button mt-5 inline-flex rounded-2xl px-5 py-3 text-sm font-black transition"
        >
          채팅 목록으로
        </Link>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-4xl">
      <section className="theme-card overflow-hidden rounded-[32px]">
        <div className="flex items-center gap-3 border-b border-[var(--color-border)] bg-white p-4">
          <Link
            to={routePaths.chats}
            className="rounded-xl p-2 text-[var(--color-text-sub)] hover:bg-[var(--color-primary-soft)] hover:text-[var(--color-primary)]"
            aria-label="채팅 목록으로"
          >
            <ArrowLeft size={20} />
          </Link>
          <div className="min-w-0">
            <p className="text-xs font-bold uppercase tracking-wide text-[var(--color-primary)]">
              {room?.counterpart?.nickname ? `${room.counterpart.nickname}님과 대화` : `Chat #${chatRoomId}`}
            </p>
            <h1 className="truncate font-black">{room?.product?.title || room?.title || '채팅방'}</h1>
            {room?.product && (
              <p className="mt-0.5 text-xs font-bold text-[var(--color-text-sub)]">
                {formatPrice(room.product.price)}
              </p>
            )}
          </div>
          <div className="ml-auto shrink-0">
            {room?.product?.status ? <StatusBadge status={room.product.status} /> : <span className="text-xs font-bold text-[var(--color-text-sub)]">{room?.type}</span>}
          </div>
        </div>

        {errorMessage && (
          <div className="m-4 flex items-center gap-2 rounded-2xl border border-red-100 bg-red-50 px-4 py-3 text-sm font-bold text-red-600">
            <AlertCircle size={17} />
            {errorMessage}
          </div>
        )}

        {socketStatus === 'connecting' && (
          <div className="mx-4 mb-4 flex items-center gap-2 rounded-2xl border border-emerald-100 bg-[var(--color-primary-soft)] px-4 py-3 text-sm font-bold text-[var(--color-primary-dark)]">
            <Loader2 size={17} className="animate-spin" />
            채팅 서버에 연결 중입니다
          </div>
        )}

        <div className="max-h-[62vh] min-h-[520px] overflow-y-auto bg-[#f8fafc] p-4">
          {hasNext && (
            <div className="mb-4 flex justify-center">
              <button
                type="button"
                onClick={handleLoadMore}
                disabled={isLoadingMore}
                className="theme-secondary-button inline-flex items-center gap-2 rounded-full px-4 py-2 text-xs font-black transition disabled:cursor-not-allowed disabled:opacity-70"
              >
                {isLoadingMore && <Loader2 size={15} className="animate-spin" />}
                이전 메시지 보기
              </button>
            </div>
          )}

          {messages.length > 0 ? (
            <div className="space-y-4">
              {messages.map((message) => {
                const isSystem = message.type === 'SYSTEM';
                const isMine = currentUser && String(message.senderId) === String(currentUser.memberId);

                if (isSystem) {
                  return (
                    <div key={message.messageId} className="flex justify-center">
                      <span className="rounded-full bg-white px-3 py-1 text-xs font-bold text-[var(--color-text-sub)] ring-1 ring-[var(--color-border)]">
                        {message.content}
                      </span>
                    </div>
                  );
                }

                return (
                  <div key={message.messageId} className={`flex ${isMine ? 'justify-end' : 'justify-start'}`}>
                    <div className={['max-w-[78%]', isMine ? 'items-end' : 'items-start'].join(' ')}>
                      {!isMine && (
                        <p className="mb-1 text-xs font-bold text-[var(--color-text-sub)]">
                          {message.senderNickname || room?.counterpart?.nickname || '상대방'}
                        </p>
                      )}
                      <div
                        className={[
                          'rounded-3xl px-4 py-3 text-sm leading-6 shadow-sm',
                          isMine
                            ? 'rounded-br-lg bg-[#ff7a1a] text-white'
                            : 'rounded-bl-lg bg-white text-[var(--color-text-main)] ring-1 ring-[var(--color-border)]',
                        ].join(' ')}
                      >
                        <p className="whitespace-pre-line">{message.content}</p>
                        <p className={`mt-1 text-[11px] ${isMine ? 'text-white/75' : 'text-[var(--color-text-sub)] opacity-70'}`}>
                          {formatMessageTime(message.createdAt)}
                        </p>
                      </div>
                    </div>
                  </div>
                );
              })}
              <div ref={messageEndRef} />
            </div>
          ) : (
            <div className="flex min-h-[420px] flex-col items-center justify-center text-center">
              <p className="text-lg font-black text-[var(--color-text-main)]">아직 메시지가 없습니다</p>
              <p className="mt-2 text-sm text-[var(--color-text-sub)]">첫 메시지를 보내 거래 대화를 시작해보세요.</p>
            </div>
          )}
        </div>

        <form className="sticky bottom-0 flex gap-2 border-t border-[var(--color-border)] bg-white p-4" onSubmit={handleSubmit}>
          <input
            className="theme-input flex-1 rounded-2xl px-4 py-3 outline-none"
            value={messageInput}
            onChange={(event) => setMessageInput(event.target.value)}
            placeholder="메시지를 입력하세요"
          />
          <button
            type="submit"
            disabled={isSending || socketStatus !== 'connected' || !messageInput.trim()}
            className="theme-primary-button flex h-12 w-12 items-center justify-center rounded-2xl transition disabled:cursor-not-allowed disabled:opacity-60"
            aria-label="메시지 전송"
          >
            {isSending ? <Loader2 size={20} className="animate-spin" /> : <Send size={20} />}
          </button>
        </form>
      </section>
    </div>
  );
}
