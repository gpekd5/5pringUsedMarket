import { AlertCircle, ChevronRight, Headphones, Loader2, MessageCircle, Plus } from 'lucide-react';
import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { createCsChatRoom, getChatApiErrorMessage, getChatRooms } from '../api/chatApi.js';
import { getAccessToken } from '../api/authStorage.js';
import EmptyState from '../components/EmptyState.jsx';
import PageHeader from '../components/PageHeader.jsx';
import StatusBadge from '../components/StatusBadge.jsx';
import routePaths from '../routes/routePaths.js';

const PAGE_SIZE = 20;

function formatChatTime(value) {
  if (!value) {
    return '';
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return '';
  }

  const now = new Date();
  const isToday = date.toDateString() === now.toDateString();

  if (isToday) {
    return date.toLocaleTimeString('ko-KR', {
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  return date.toLocaleDateString('ko-KR', {
    month: 'numeric',
    day: 'numeric',
  });
}

export default function ChatRoomListPage() {
  const navigate = useNavigate();
  const [rooms, setRooms] = useState([]);
  const [pageInfo, setPageInfo] = useState({
    page: 0,
    size: PAGE_SIZE,
    totalElements: 0,
    totalPages: 0,
  });
  const [isLoading, setIsLoading] = useState(true);
  const [isLoadingMore, setIsLoadingMore] = useState(false);
  const [isCreatingCsRoom, setIsCreatingCsRoom] = useState(false);
  const [csTitle, setCsTitle] = useState('');
  const [errorMessage, setErrorMessage] = useState('');

  const hasMore = pageInfo.page + 1 < pageInfo.totalPages;

  const loadRooms = async ({ page = 0, append = false } = {}) => {
    if (append) {
      setIsLoadingMore(true);
    } else {
      setIsLoading(true);
    }

    setErrorMessage('');

    try {
      const nextPage = await getChatRooms({ page, size: PAGE_SIZE });
      setRooms((prevRooms) => (append ? [...prevRooms, ...nextPage.content] : nextPage.content));
      setPageInfo(nextPage);
    } catch (error) {
      setErrorMessage(getChatApiErrorMessage(error, '채팅방 목록을 불러오지 못했습니다.'));
      if (!append) {
        setRooms([]);
      }
    } finally {
      setIsLoading(false);
      setIsLoadingMore(false);
    }
  };

  useEffect(() => {
    if (!getAccessToken()) {
      navigate(routePaths.login, {
        state: { message: '채팅은 로그인 후 이용할 수 있습니다.' },
      });
      return;
    }

    loadRooms();
  }, [navigate]);

  const handleCreateCsRoom = async (event) => {
    event.preventDefault();

    const title = csTitle.trim();
    if (!title || isCreatingCsRoom) {
      return;
    }

    if (!getAccessToken()) {
      navigate(routePaths.login, {
        state: { message: 'CS 문의는 로그인 후 이용할 수 있습니다.' },
      });
      return;
    }

    setIsCreatingCsRoom(true);
    setErrorMessage('');

    try {
      const createdRoom = await createCsChatRoom(title);
      setCsTitle('');
      navigate(routePaths.chatRoom(createdRoom.roomId));
    } catch (error) {
      setErrorMessage(getChatApiErrorMessage(error, 'CS 문의 채팅방을 만들지 못했습니다.'));
    } finally {
      setIsCreatingCsRoom(false);
    }
  };

  return (
    <div>
      <PageHeader
        eyebrow="Chats"
        title="채팅"
        description="거래 채팅과 고객센터 문의를 한곳에서 확인하세요."
      />

      <section className="theme-card mb-5 rounded-[28px] p-5">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          <div className="flex items-start gap-3">
            <div className="theme-soft-surface flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl">
              <Headphones size={23} />
            </div>
            <div>
              <h2 className="font-black text-[var(--color-text-main)]">고객센터 문의하기</h2>
              <p className="mt-1 text-sm leading-6 text-[var(--color-text-sub)]">
                문의 제목을 입력하면 관리자에게 전달되는 CS 채팅방이 생성됩니다.
              </p>
            </div>
          </div>

          <form className="flex w-full flex-col gap-2 sm:flex-row lg:max-w-xl" onSubmit={handleCreateCsRoom}>
            <input
              className="theme-input flex-1 rounded-2xl px-4 py-3 text-sm font-semibold outline-none"
              value={csTitle}
              onChange={(event) => setCsTitle(event.target.value)}
              maxLength={100}
              placeholder="예: 상품 거래 관련 문의"
            />
            <button
              type="submit"
              disabled={isCreatingCsRoom || !csTitle.trim()}
              className="theme-primary-button inline-flex items-center justify-center gap-2 rounded-2xl px-4 py-3 text-sm font-black transition disabled:cursor-not-allowed disabled:opacity-70"
            >
              {isCreatingCsRoom ? <Loader2 size={17} className="animate-spin" /> : <Plus size={17} />}
              문의하기
            </button>
          </form>
        </div>
      </section>

      {errorMessage && (
        <div className="mb-5 flex items-center gap-2 rounded-2xl border border-red-100 bg-red-50 px-4 py-3 text-sm font-bold text-red-600">
          <AlertCircle size={17} />
          {errorMessage}
        </div>
      )}

      {isLoading ? (
        <div className="theme-card flex min-h-64 flex-col items-center justify-center gap-4 rounded-[28px] p-8 text-center">
          <Loader2 size={34} className="animate-spin text-[var(--color-primary)]" />
          <p className="text-sm font-bold text-[var(--color-text-sub)]">채팅방을 불러오는 중입니다</p>
        </div>
      ) : rooms.length > 0 ? (
        <>
          <section className="space-y-3">
            {rooms.map((room) => (
              <Link
                key={room.roomId}
                to={routePaths.chatRoom(room.roomId)}
                className="theme-card theme-card-hover flex items-center gap-4 rounded-[26px] p-4 transition hover:-translate-y-0.5"
              >
                <div className="theme-soft-surface flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl">
                  <MessageCircle size={25} />
                </div>
                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-2">
                    <h2 className="truncate font-black">{room.title || room.product?.title || '채팅방'}</h2>
                    {room.product?.status && <StatusBadge status={room.product.status} />}
                    <span className="ml-auto shrink-0 text-xs font-semibold text-[var(--color-text-sub)] opacity-75">
                      {formatChatTime(room.lastMessageAt)}
                    </span>
                  </div>
                  <p className="mt-1 truncate text-sm font-semibold text-[var(--color-text-sub)]">
                    {room.lastMessage || '아직 주고받은 메시지가 없습니다.'}
                  </p>
                  <p className="mt-1 text-xs font-bold text-[var(--color-text-sub)]">
                    {room.counterpart?.nickname ? `${room.counterpart.nickname}님과 대화 중` : room.type}
                  </p>
                </div>
                {room.unreadCount > 0 && (
                  <span className="flex h-6 min-w-6 items-center justify-center rounded-full bg-[var(--color-primary)] px-2 text-xs font-black text-[var(--color-on-primary)]">
                    {room.unreadCount > 99 ? '99+' : room.unreadCount}
                  </span>
                )}
                <ChevronRight className="hidden text-[var(--color-text-sub)] opacity-45 sm:block" size={20} />
              </Link>
            ))}
          </section>

          {hasMore && (
            <div className="mt-6 flex justify-center">
              <button
                type="button"
                onClick={() => loadRooms({ page: pageInfo.page + 1, append: true })}
                disabled={isLoadingMore}
                className="theme-secondary-button inline-flex items-center gap-2 rounded-full px-5 py-3 text-sm font-black transition disabled:cursor-not-allowed disabled:opacity-70"
              >
                {isLoadingMore && <Loader2 size={17} className="animate-spin" />}
                더보기
              </button>
            </div>
          )}
        </>
      ) : (
        <EmptyState
          icon={MessageCircle}
          title="진행 중인 채팅이 없습니다"
          description="상품 상세에서 거래 채팅을 시작하거나, 위에서 고객센터 문의를 만들어보세요."
        />
      )}
    </div>
  );
}
