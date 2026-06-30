import { AlertCircle, ChevronRight, Clock, Headphones, Loader2, ShieldCheck } from 'lucide-react';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  enterAdminCsRoom,
  getAdminChatApiErrorMessage,
  getAdminCsRooms,
  updateAdminCsRoomStatus,
} from '../api/adminChatApi.js';
import { getAccessToken } from '../api/authStorage.js';
import EmptyState from '../components/EmptyState.jsx';
import PageHeader from '../components/PageHeader.jsx';
import routePaths from '../routes/routePaths.js';

const PAGE_SIZE = 10;

const statusFilters = [
  { label: '대기', value: 'WAITING' },
  { label: '진행 중', value: 'IN_PROGRESS' },
  { label: '완료', value: 'COMPLETED' },
  { label: '전체', value: 'ALL' },
];

const statusMeta = {
  WAITING: {
    label: '대기',
    className: 'bg-amber-50 text-amber-700 ring-amber-100',
  },
  IN_PROGRESS: {
    label: '처리 중',
    className: 'bg-blue-50 text-blue-700 ring-blue-100',
  },
  COMPLETED: {
    label: '완료',
    className: 'bg-slate-100 text-slate-600 ring-slate-200',
  },
};

function formatChatTime(value) {
  if (!value) {
    return '메시지 없음';
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return '메시지 없음';
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
    month: '2-digit',
    day: '2-digit',
  });
}

function StatusBadge({ status }) {
  const meta = statusMeta[status] || {
    label: status || '상태 없음',
    className: 'bg-slate-100 text-slate-600 ring-slate-200',
  };

  return (
    <span className={`inline-flex rounded-full px-3 py-1 text-xs font-black ring-1 ${meta.className}`}>
      {meta.label}
    </span>
  );
}

export default function AdminCsChatPage() {
  const navigate = useNavigate();
  const [selectedStatus, setSelectedStatus] = useState('WAITING');
  const [rooms, setRooms] = useState([]);
  const [pageInfo, setPageInfo] = useState({
    page: 0,
    size: PAGE_SIZE,
    totalElements: 0,
    totalPages: 0,
  });
  const [isLoading, setIsLoading] = useState(true);
  const [isLoadingMore, setIsLoadingMore] = useState(false);
  const [enteringRoomId, setEnteringRoomId] = useState(null);
  const [updatingStatusRoomId, setUpdatingStatusRoomId] = useState(null);
  const [summary, setSummary] = useState({
    waiting: 0,
    inProgress: 0,
    completed: 0,
  });
  const [errorMessage, setErrorMessage] = useState('');

  const hasMore = pageInfo.page + 1 < pageInfo.totalPages;

  const loadSummaryCounts = async () => {
    try {
      const [waitingPage, inProgressPage, completedPage] = await Promise.all([
        getAdminCsRooms({ status: 'WAITING', page: 0, size: 1 }),
        getAdminCsRooms({ status: 'IN_PROGRESS', page: 0, size: 1 }),
        getAdminCsRooms({ status: 'COMPLETED', page: 0, size: 1 }),
      ]);

      setSummary({
        waiting: waitingPage.totalElements,
        inProgress: inProgressPage.totalElements,
        completed: completedPage.totalElements,
      });
    } catch {
      setSummary({
        waiting: 0,
        inProgress: 0,
        completed: 0,
      });
    }
  };

  const loadRooms = async ({ status = selectedStatus, page = 0, append = false } = {}) => {
    if (!getAccessToken()) {
      navigate(routePaths.login, {
        state: { message: '관리자 CS 채팅은 로그인 후 이용할 수 있습니다.' },
      });
      return;
    }

    if (append) {
      setIsLoadingMore(true);
    } else {
      setIsLoading(true);
    }

    setErrorMessage('');

    try {
      const nextPage = await getAdminCsRooms({ status, page, size: PAGE_SIZE });
      setRooms((prevRooms) => (append ? [...prevRooms, ...nextPage.content] : nextPage.content));
      setPageInfo(nextPage);
    } catch (error) {
      const statusCode = error?.response?.status;
      const message = statusCode === 403
        ? '관리자 권한이 필요합니다. 관리자 계정으로 로그인해주세요.'
        : getAdminChatApiErrorMessage(error, '관리자 CS 채팅 목록을 불러오지 못했습니다.');

      setErrorMessage(message);

      if (!append) {
        setRooms([]);
        setPageInfo({
          page: 0,
          size: PAGE_SIZE,
          totalElements: 0,
          totalPages: 0,
        });
      }
    } finally {
      setIsLoading(false);
      setIsLoadingMore(false);
    }
  };

  useEffect(() => {
    loadSummaryCounts();
    loadRooms({ status: selectedStatus, page: 0 });
  }, []);

  const handleStatusChange = (status) => {
    setSelectedStatus(status);
    loadSummaryCounts();
    loadRooms({ status, page: 0 });
  };

  const handleEnterRoom = async (room) => {
    setErrorMessage('');

    if (room.csStatus !== 'WAITING') {
      navigate(routePaths.chatRoom(room.roomId));
      return;
    }

    setEnteringRoomId(room.roomId);

    try {
      const enterResponse = await enterAdminCsRoom(room.roomId);
      navigate(routePaths.chatRoom(enterResponse.roomId));
    } catch (error) {
      const statusCode = error?.response?.status;
      const message = statusCode === 403
        ? '관리자 권한이 필요합니다. 관리자 계정으로 로그인해주세요.'
        : getAdminChatApiErrorMessage(error, 'CS 채팅방에 입장하지 못했습니다.');

      setErrorMessage(message);
    } finally {
      setEnteringRoomId(null);
    }
  };

  const handleCompleteRoom = async (room) => {
    setUpdatingStatusRoomId(room.roomId);
    setErrorMessage('');

    try {
      const response = await updateAdminCsRoomStatus(room.roomId, 'COMPLETED');
      setRooms((prevRooms) =>
        prevRooms.map((prevRoom) =>
          prevRoom.roomId === room.roomId ? { ...prevRoom, csStatus: response.csStatus } : prevRoom,
        ),
      );
      await loadSummaryCounts();
      await loadRooms({ status: selectedStatus, page: 0 });
    } catch (error) {
      const statusCode = error?.response?.status;
      const message = statusCode === 403
        ? '관리자 권한이 필요합니다. 관리자 계정으로 로그인해주세요.'
        : getAdminChatApiErrorMessage(error, 'CS 문의 상태를 변경하지 못했습니다.');

      setErrorMessage(message);
    } finally {
      setUpdatingStatusRoomId(null);
    }
  };

  return (
    <div>
      <PageHeader
        eyebrow="Admin"
        title="관리자 CS 채팅"
        description="대기 중인 문의를 확인하고 상담 상태를 빠르게 관리하세요."
        action={
          <span className="theme-primary-button inline-flex items-center gap-2 rounded-2xl px-4 py-3 text-sm font-black">
            <ShieldCheck size={18} />
            Admin Mode
          </span>
        }
      />

      <section className="mb-5 grid gap-3 md:grid-cols-3">
        <div className="theme-card rounded-[24px] p-4">
          <p className="text-sm font-bold text-[var(--color-text-sub)]">대기 문의</p>
          <p className="mt-2 text-3xl font-black text-amber-600">{summary.waiting}</p>
        </div>
        <div className="theme-card rounded-[24px] p-4">
          <p className="text-sm font-bold text-[var(--color-text-sub)]">처리 중</p>
          <p className="mt-2 text-3xl font-black text-blue-600">{summary.inProgress}</p>
        </div>
        <div className="theme-card rounded-[24px] p-4">
          <p className="text-sm font-bold text-[var(--color-text-sub)]">완료</p>
          <p className="mt-2 text-3xl font-black text-slate-600">{summary.completed}</p>
        </div>
      </section>

      <section className="mb-5 flex gap-2 overflow-x-auto pb-1">
        {statusFilters.map((filter) => (
          <button
            key={filter.value}
            type="button"
            onClick={() => handleStatusChange(filter.value)}
            className={[
              'category-chip theme-card-hover inline-flex shrink-0 items-center rounded-full px-4 py-2.5 text-sm font-black transition',
              selectedStatus === filter.value ? 'category-chip-active' : '',
            ].join(' ')}
          >
            {filter.label}
          </button>
        ))}
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
          <p className="text-sm font-bold text-[var(--color-text-sub)]">CS 채팅방을 불러오는 중입니다</p>
        </div>
      ) : rooms.length > 0 ? (
        <>
          <section className="theme-card overflow-hidden rounded-[32px]">
            {rooms.map((room, index) => (
              <article
                key={room.roomId}
                className={`flex flex-col gap-4 p-5 lg:flex-row lg:items-center ${index !== 0 ? 'border-t border-[var(--color-border)]' : ''}`}
              >
                <div className="theme-soft-surface flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl">
                  <Headphones size={23} />
                </div>
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <h2 className="truncate font-black">{room.title || 'CS 문의'}</h2>
                    <StatusBadge status={room.csStatus} />
                  </div>
                  <p className="mt-1 truncate text-sm font-semibold text-[var(--color-text-sub)]">
                    {room.customerNickname || '고객'} · {room.lastMessage || '아직 메시지가 없습니다.'}
                  </p>
                </div>
                <div className="flex items-center justify-between gap-3 lg:min-w-[230px]">
                  <span className="flex items-center gap-1.5 text-xs font-bold text-[var(--color-text-sub)]">
                    <Clock size={15} />
                    {formatChatTime(room.lastMessageAt)}
                  </span>
                  <button
                    type="button"
                    onClick={() => handleEnterRoom(room)}
                    disabled={enteringRoomId === room.roomId}
                    className="theme-secondary-button inline-flex items-center gap-1.5 rounded-2xl px-4 py-2 text-sm font-black transition disabled:cursor-not-allowed disabled:opacity-70"
                  >
                    {enteringRoomId === room.roomId ? <Loader2 size={16} className="animate-spin" /> : null}
                    {room.csStatus === 'WAITING' ? '입장' : '상세 보기'}
                    <ChevronRight size={16} />
                  </button>
                  {room.csStatus === 'IN_PROGRESS' && (
                    <button
                      type="button"
                      onClick={() => handleCompleteRoom(room)}
                      disabled={updatingStatusRoomId === room.roomId}
                      className="inline-flex items-center gap-1.5 rounded-2xl bg-slate-900 px-4 py-2 text-sm font-black text-white transition hover:bg-slate-700 disabled:cursor-not-allowed disabled:opacity-70"
                    >
                      {updatingStatusRoomId === room.roomId ? <Loader2 size={16} className="animate-spin" /> : null}
                      완료 처리
                    </button>
                  )}
                </div>
              </article>
            ))}
          </section>

          {hasMore && (
            <div className="mt-6 flex justify-center">
              <button
                type="button"
                onClick={() => loadRooms({ status: selectedStatus, page: pageInfo.page + 1, append: true })}
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
          icon={Headphones}
          title="표시할 CS 문의가 없습니다"
          description="상태 필터를 변경하거나 새 문의가 등록된 뒤 다시 확인해 주세요."
        />
      )}
    </div>
  );
}
