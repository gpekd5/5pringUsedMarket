import { Heart, MessageCircle, ShieldCheck, Smartphone } from 'lucide-react';
import { useParams } from 'react-router-dom';
import StatusBadge from '../components/StatusBadge.jsx';

export default function ProductDetailPage() {
  const { productId } = useParams();

  return (
    <div className="grid gap-6 md:grid-cols-[0.9fr_1.1fr]">
      <section className="theme-card theme-placeholder product-detail-image aspect-square rounded-[32px] p-8">
        <div className="flex h-full items-center justify-center rounded-[24px] bg-white/48 text-[var(--primary-dark)] shadow-inner backdrop-blur-[1px]">
          <Smartphone size={120} strokeWidth={1.5} />
        </div>
      </section>

      <section className="theme-card rounded-[32px] p-6">
        <div className="flex items-center justify-between gap-3">
          <p className="text-sm font-extrabold uppercase tracking-wide text-[var(--color-primary)]">Product #{productId}</p>
          <StatusBadge status="ON_SALE" />
        </div>
        <h1 className="mt-3 text-3xl font-black tracking-tight">아이폰 15 판매합니다</h1>
        <p className="mt-2 text-sm font-semibold text-[var(--color-text-sub)]">서초동 · 방금 전</p>
        <p className="mt-5 text-3xl font-black">800,000원</p>
        <p className="mt-5 leading-7 text-[var(--color-text-sub)]">
          상태 좋은 아이폰 15입니다. 생활 기스 거의 없고 구성품도 함께 드립니다. 서초역 근처에서 직거래 가능합니다.
        </p>

        <div className="mt-6 rounded-2xl bg-[var(--color-primary-soft)] p-4">
          <div className="flex items-center gap-3">
            <div className="flex h-12 w-12 items-center justify-center rounded-full bg-[var(--color-primary)] font-black text-[var(--color-on-primary)]">
              봄
            </div>
            <div>
              <p className="font-black">봄마켓</p>
              <p className="text-sm text-[var(--color-text-sub)]">응답이 빠른 판매자</p>
            </div>
            <ShieldCheck className="ml-auto text-[var(--color-primary)]" />
          </div>
        </div>

        <div className="mt-6 grid gap-3 sm:grid-cols-2">
          <button className="theme-secondary-button flex items-center justify-center gap-2 rounded-2xl px-4 py-3 font-black transition">
            <Heart size={19} />
            찜하기
          </button>
          <button className="theme-primary-button flex items-center justify-center gap-2 rounded-2xl px-4 py-3 font-black transition">
            <MessageCircle size={19} />
            채팅하기
          </button>
        </div>
      </section>
    </div>
  );
}
