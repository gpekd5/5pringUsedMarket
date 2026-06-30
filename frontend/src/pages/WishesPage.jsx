import { Footprints, Headphones, Smartphone } from 'lucide-react';
import EmptyState from '../components/EmptyState.jsx';
import PageHeader from '../components/PageHeader.jsx';
import ProductCard from '../components/ProductCard.jsx';

const wishedProducts = [
  { id: 1, title: '아이폰 15 판매합니다', price: 800000, location: '서초동', timeAgo: '방금 전', icon: Smartphone, wished: true, status: 'ON_SALE' },
  { id: 4, title: '에어팟 프로 2세대', price: 180000, location: '잠실동', timeAgo: '1시간 전', icon: Headphones, wished: true, status: 'SOLD' },
  { id: 8, title: '나이키 운동화 270', price: 60000, location: '망원동', timeAgo: '2시간 전', icon: Footprints, wished: true, status: 'ON_SALE' },
];

export default function WishesPage() {
  return (
    <div>
      <PageHeader
        eyebrow="Wishes"
        title="내가 찜한 상품"
        description="관심 있는 상품을 모아보고 거래 상태를 빠르게 확인하세요."
      />
      {wishedProducts.length > 0 ? (
        <section className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {wishedProducts.map((product) => (
            <ProductCard key={product.id} product={product} />
          ))}
        </section>
      ) : (
        <EmptyState title="찜한 상품이 없습니다" description="마음에 드는 상품을 발견하면 찜 목록에 담아보세요." />
      )}
    </div>
  );
}
