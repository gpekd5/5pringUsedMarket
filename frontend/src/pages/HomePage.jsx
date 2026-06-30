import {
  ChevronRight,
  MessageCircle,
  Plus,
  Search,
  ShieldCheck,
  Ticket,
} from 'lucide-react';
import EmptyState from '../components/EmptyState.jsx';
import PageHeader from '../components/PageHeader.jsx';
import ProductCard from '../components/ProductCard.jsx';

const products = [
  {
    id: 1,
    title: '아이폰 15 판매합니다',
    price: 800000,
    location: '서초동',
    timeAgo: '방금 전',
    wished: true,
    status: 'ON_SALE',
  },
  {
    id: 2,
    title: '맥북 에어 M2 실사용 적음',
    price: 1200000,
    location: '역삼동',
    timeAgo: '8분 전',
    wished: false,
    status: 'ON_SALE',
  },
  {
    id: 3,
    title: '원목 의자 깔끔하게 써요',
    price: 70000,
    location: '성수동',
    timeAgo: '23분 전',
    wished: false,
    status: 'RESERVED',
  },
  {
    id: 4,
    title: '에어팟 프로 2세대',
    price: 180000,
    location: '잠실동',
    timeAgo: '1시간 전',
    wished: true,
    status: 'SOLD',
  },
  {
    id: 5,
    title: '나이키 운동화 270',
    price: 60000,
    location: '망원동',
    timeAgo: '2시간 전',
    wished: false,
    status: 'ON_SALE',
  },
  {
    id: 6,
    title: '27인치 사무용 모니터',
    price: 130000,
    location: '판교동',
    timeAgo: '3시간 전',
    wished: false,
    status: 'ON_SALE',
  },
];

const stats = [
  { label: '오늘 등록 상품', value: '128', icon: Plus },
  { label: '진행 중 채팅', value: '24', icon: MessageCircle },
  { label: '발급 가능 쿠폰', value: '2', icon: Ticket },
  { label: '고객 문의', value: '5', icon: ShieldCheck },
];

const categories = ['전체', '디지털', '가구', '의류', '도서', '스포츠', '생활용품'];

export default function HomePage() {
  return (
    <div>
      <section className="hero-section mb-6 min-h-[340px] overflow-hidden rounded-[32px] px-6 py-8 sm:px-8 md:min-h-[380px] md:px-10">
        <div className="grid gap-7 md:grid-cols-[1.08fr_0.92fr] md:items-center">
          <div>
            <p className="theme-hero-pill mb-4 inline-flex rounded-full px-4 py-2 text-sm font-black backdrop-blur">
              우리 동네 인기 거래
            </p>
            <h1 className="max-w-2xl text-[34px] font-black leading-[1.22] tracking-tight text-[var(--text-main)] sm:text-[43px]">
              가까운 이웃과 쉽고 따뜻하게 거래해요
            </h1>
            <p className="theme-hero-muted mt-4 max-w-xl text-base font-medium leading-7">
              필요한 물건은 더 합리적으로, 쓰지 않는 물건은 더 가치 있게. 5pring Market에서 오늘의 좋은 거래를 만나보세요.
            </p>
            <div className="mt-6 flex max-w-[520px] items-center gap-3 rounded-full bg-white px-5 py-3.5 text-[var(--color-text-sub)] shadow-sm ring-1 ring-[var(--color-border)]">
              <Search size={20} className="text-[var(--color-primary)]" />
              <span className="text-sm font-bold">아이폰, 맥북, 의자 검색</span>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            {stats.map(({ label, value, icon: Icon }) => (
              <div key={label} className="theme-hero-panel min-h-[104px] rounded-[22px] p-4 backdrop-blur">
                <span className="mb-2 flex h-8 w-8 items-center justify-center rounded-full bg-[var(--color-primary-soft)] text-[var(--hero-icon)]">
                  <Icon size={19} />
                </span>
                <p className="text-[25px] font-black leading-tight text-[var(--hero-icon)]">{value}</p>
                <p className="text-sm font-semibold text-[var(--text-sub)]">{label}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="mb-6 flex gap-2 overflow-x-auto pb-1">
        {categories.map((category, index) => (
          <button
            key={category}
            className={[
              'category-chip theme-card-hover inline-flex shrink-0 items-center rounded-full px-4 py-2.5 text-sm font-black text-[var(--color-text-main)] transition',
              index === 0 ? 'category-chip-active' : '',
            ].join(' ')}
          >
            {category}
          </button>
        ))}
      </section>

      <PageHeader
        eyebrow="Products"
        title="오늘 올라온 추천 상품"
        description="상태와 가격, 위치를 한눈에 확인하고 마음에 드는 상품을 빠르게 살펴보세요."
        action={
          <button className="theme-primary-button inline-flex items-center gap-1 rounded-full px-4 py-2.5 text-sm font-black transition">
            전체 보기
            <ChevronRight size={17} />
          </button>
        }
      />

      {products.length > 0 ? (
        <section className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          {products.map((product) => (
            <ProductCard key={product.id} product={product} />
          ))}
        </section>
      ) : (
        <EmptyState title="등록된 상품이 없습니다" description="새로운 상품이 올라오면 이곳에서 바로 확인할 수 있어요." />
      )}
    </div>
  );
}
