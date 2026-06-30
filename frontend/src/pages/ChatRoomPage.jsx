import { ArrowLeft, Send } from 'lucide-react';
import { Link, useParams } from 'react-router-dom';
import StatusBadge from '../components/StatusBadge.jsx';
import routePaths from '../routes/routePaths.js';

const messages = [
  { id: 1, sender: 'buyer', content: '안녕하세요. 아직 구매 가능할까요?', time: '14:26' },
  { id: 2, sender: 'seller', content: '네, 아직 거래 가능합니다.', time: '14:28' },
  { id: 3, sender: 'buyer', content: '오늘 저녁에 직거래 가능하실까요?', time: '14:30' },
];

export default function ChatRoomPage() {
  const { chatRoomId } = useParams();

  return (
    <div className="mx-auto max-w-3xl">
      <section className="theme-card overflow-hidden rounded-[32px]">
        <div className="flex items-center gap-3 border-b border-[var(--color-border)] p-4">
          <Link to={routePaths.chats} className="rounded-xl p-2 text-[var(--color-text-sub)] hover:bg-[var(--color-primary-soft)] hover:text-[var(--color-primary)]">
            <ArrowLeft size={20} />
          </Link>
          <div>
            <p className="text-xs font-bold uppercase tracking-wide text-[var(--color-primary)]">Chat #{chatRoomId}</p>
            <h1 className="font-black">아이폰 15 판매합니다</h1>
          </div>
          <div className="ml-auto">
            <StatusBadge status="ON_SALE" />
          </div>
        </div>

        <div className="min-h-[460px] space-y-4 bg-[var(--color-background)] p-4">
          {messages.map((message) => {
            const isMine = message.sender === 'buyer';
            return (
              <div key={message.id} className={`flex ${isMine ? 'justify-end' : 'justify-start'}`}>
                <div
                  className={[
                    'max-w-[78%] rounded-3xl px-4 py-3 text-sm shadow-sm',
                    isMine ? 'bg-[var(--color-primary)] text-[var(--color-on-primary)]' : 'bg-white text-[var(--color-text-main)] ring-1 ring-[var(--color-border)]',
                  ].join(' ')}
                >
                  <p>{message.content}</p>
                  <p className={`mt-1 text-xs ${isMine ? 'text-white/75' : 'text-[var(--color-text-sub)] opacity-70'}`}>{message.time}</p>
                </div>
              </div>
            );
          })}
        </div>

        <div className="flex gap-2 border-t border-[var(--color-border)] p-4">
          <input
            className="theme-input flex-1 rounded-2xl px-4 py-3 outline-none"
            placeholder="메시지를 입력하세요"
          />
          <button className="theme-primary-button flex h-12 w-12 items-center justify-center rounded-2xl transition">
            <Send size={20} />
          </button>
        </div>
      </section>
    </div>
  );
}
