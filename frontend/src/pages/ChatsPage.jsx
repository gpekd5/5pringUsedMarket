import { ChevronRight, MessageCircle } from 'lucide-react';
import { Link } from 'react-router-dom';
import PageHeader from '../components/PageHeader.jsx';
import routePaths from '../routes/routePaths.js';

const chatRooms = [
  { id: 101, title: '아이폰 15 판매합니다', message: '네, 아직 거래 가능합니다.', time: '오후 2:31', unread: 1 },
  { id: 102, title: '맥북 에어 M2 판매합니다', message: '오늘 저녁에 확인 가능해요.', time: '오후 1:12', unread: 0 },
  { id: 103, title: '에어팟 프로 2세대', message: '직거래 장소 어디가 편하세요?', time: '어제', unread: 2 },
];

export default function ChatsPage() {
  return (
    <div>
      <PageHeader
        eyebrow="Chats"
        title="거래 채팅"
        description="상품별 대화와 읽지 않은 메시지를 한눈에 확인하세요."
      />
      <section className="space-y-3">
        {chatRooms.map((room) => (
          <Link
            key={room.id}
            to={routePaths.chatRoom(room.id)}
            className="theme-card theme-card-hover flex items-center gap-4 rounded-[28px] p-4 transition hover:-translate-y-0.5"
          >
            <div className="theme-soft-surface flex h-14 w-14 items-center justify-center rounded-2xl">
              <MessageCircle size={25} />
            </div>
            <div className="min-w-0 flex-1">
              <div className="flex items-center gap-2">
                <h2 className="truncate font-black">{room.title}</h2>
                <span className="ml-auto shrink-0 text-xs font-semibold text-[var(--color-text-sub)] opacity-70">{room.time}</span>
              </div>
              <p className="mt-1 truncate text-sm text-[var(--color-text-sub)]">{room.message}</p>
            </div>
            {room.unread > 0 && (
              <span className="flex h-6 min-w-6 items-center justify-center rounded-full bg-[var(--color-primary)] px-2 text-xs font-black text-[var(--color-on-primary)]">
                {room.unread}
              </span>
            )}
            <ChevronRight className="text-[var(--color-text-sub)] opacity-45" size={20} />
          </Link>
        ))}
      </section>
    </div>
  );
}
