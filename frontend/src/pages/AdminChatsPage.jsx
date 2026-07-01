import { Headphones, ShieldCheck } from 'lucide-react';
import PageHeader from '../components/PageHeader.jsx';

const csRooms = [
  { id: 501, title: '상품 거래 관련 문의', customer: '봄이웃', status: 'WAITING', message: '상품 거래 관련 문의드립니다.' },
  { id: 502, title: '쿠폰 발급 문의', customer: 'springfan', status: 'IN_PROGRESS', message: '쿠폰이 보이지 않아요.' },
  { id: 503, title: '채팅 신고 확인', customer: 'orangeuser', status: 'COMPLETED', message: '처리 완료되었습니다.' },
];

const statusStyle = {
  WAITING: 'bg-[var(--color-primary-soft)] text-[var(--color-primary)]',
  IN_PROGRESS: 'bg-[var(--color-status-reserved-bg)] text-[var(--color-status-reserved-text)]',
  COMPLETED: 'bg-[var(--color-status-sold-bg)] text-[var(--color-status-sold-text)]',
};

export default function AdminChatsPage() {
  return (
    <div>
      <PageHeader
        eyebrow="Admin"
        title="관리자 CS 채팅"
        description="문의 상태를 확인하고 대기 중인 상담을 빠르게 처리할 수 있습니다."
        action={
          <span className="theme-primary-button inline-flex items-center gap-2 rounded-2xl px-4 py-3 text-sm font-black">
            <ShieldCheck size={18} />
            Admin Mode
          </span>
        }
      />

      <section className="theme-card overflow-hidden rounded-[32px]">
        {csRooms.map((room, index) => (
          <article
            key={room.id}
            className={`flex flex-col gap-3 p-5 sm:flex-row sm:items-center ${index !== 0 ? 'border-t border-[var(--color-border)]' : ''}`}
          >
            <div className="theme-soft-surface flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl">
              <Headphones size={23} />
            </div>
            <div className="min-w-0 flex-1">
              <div className="flex flex-wrap items-center gap-2">
                <h2 className="font-black">{room.title}</h2>
                <span className={`rounded-full px-3 py-1 text-xs font-black ${statusStyle[room.status]}`}>
                  {room.status}
                </span>
              </div>
              <p className="mt-1 text-sm text-[var(--color-text-sub)]">
                {room.customer} · {room.message}
              </p>
            </div>
            <button className="theme-secondary-button rounded-2xl px-4 py-2 text-sm font-black transition">
              입장
            </button>
          </article>
        ))}
      </section>
    </div>
  );
}
