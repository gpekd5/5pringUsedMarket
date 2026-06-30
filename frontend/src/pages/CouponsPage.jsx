import { Gift, Ticket } from 'lucide-react';
import PageHeader from '../components/PageHeader.jsx';

const coupons = [
  { id: 1, name: '스타벅스 아메리카노 기프티콘', period: '7일 남음', stock: 100 },
  { id: 2, name: '파리바게트 5천원 기프티콘', period: '7일 남음', stock: 100 },
];

export default function CouponsPage() {
  return (
    <div>
      <PageHeader
        eyebrow="Coupons"
        title="선착순 쿠폰"
        description="놓치기 아쉬운 혜택을 확인하고 원하는 쿠폰을 받아보세요."
      />

      <section className="grid gap-4 md:grid-cols-2">
        {coupons.map((coupon) => (
          <article key={coupon.id} className="theme-card rounded-[32px] p-5">
            <div className="mb-5 flex items-center justify-between">
              <span className="theme-soft-surface flex h-14 w-14 items-center justify-center rounded-2xl">
                <Gift size={26} />
              </span>
              <span className="theme-soft-surface rounded-full px-3 py-1 text-xs font-black">
                {coupon.period}
              </span>
            </div>
            <h2 className="text-xl font-black">{coupon.name}</h2>
            <p className="mt-2 text-sm text-[var(--color-text-sub)]">남은 수량 {coupon.stock.toLocaleString()}개</p>
            <button className="theme-primary-button mt-5 flex w-full items-center justify-center gap-2 rounded-2xl px-4 py-3 font-black transition">
              <Ticket size={19} />
              쿠폰 받기
            </button>
          </article>
        ))}
      </section>
    </div>
  );
}
