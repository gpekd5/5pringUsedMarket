import { Heart, ImageIcon, MapPin } from 'lucide-react';
import { Link } from 'react-router-dom';
import routePaths from '../routes/routePaths.js';
import StatusBadge from './StatusBadge.jsx';

const categoryLabelMap = {
  DIGITAL: '디지털',
  FURNITURE: '가구',
  CLOTHING: '의류',
  BOOK: '도서',
  SPORTS: '스포츠',
  KIDS: '유아/아동',
  BEAUTY: '뷰티',
  FOOD: '식품',
  PET: '반려동물',
  ETC: '기타',
};

function formatPrice(price) {
  if (typeof price === 'number') {
    return `${price.toLocaleString()}원`;
  }

  return price;
}

function formatTimeAgo(createdAt) {
  if (!createdAt) {
    return '';
  }

  const createdDate = new Date(createdAt);
  if (Number.isNaN(createdDate.getTime())) {
    return '';
  }

  const diffSeconds = Math.max(0, Math.floor((Date.now() - createdDate.getTime()) / 1000));
  const diffMinutes = Math.floor(diffSeconds / 60);
  const diffHours = Math.floor(diffMinutes / 60);
  const diffDays = Math.floor(diffHours / 24);

  if (diffMinutes < 1) {
    return '방금 전';
  }

  if (diffMinutes < 60) {
    return `${diffMinutes}분 전`;
  }

  if (diffHours < 24) {
    return `${diffHours}시간 전`;
  }

  if (diffDays < 7) {
    return `${diffDays}일 전`;
  }

  return createdDate.toLocaleDateString('ko-KR', {
    month: 'numeric',
    day: 'numeric',
  });
}

export default function ProductCard({ product, onWishClick, isWishLoading = false }) {
  const Icon = product.icon || ImageIcon;
  const productId = product.id ?? product.productId;
  const imageUrl = product.imageUrl ?? product.thumbnailUrl;
  const categoryLabel = product.location || categoryLabelMap[product.category] || product.category || '동네 거래';
  const timeLabel = product.timeAgo || formatTimeAgo(product.createdAt);

  return (
    <Link
      to={routePaths.productDetail(productId)}
      className="product-card group overflow-hidden rounded-[22px] transition duration-200 hover:-translate-y-0.5"
    >
      <div className="product-image-box relative aspect-[5/4] overflow-hidden">
        {imageUrl ? (
          <img src={imageUrl} alt={product.title} className="h-full w-full object-cover" />
        ) : (
          <div className="flex h-full w-full flex-col items-center justify-center gap-2 px-4 text-center text-[var(--color-text-sub)]">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-white text-[var(--primary-dark)] shadow-sm ring-1 ring-[var(--color-border)]">
              <Icon size={21} strokeWidth={1.8} />
            </div>
            <span className="text-[11px] font-semibold">이미지 준비중</span>
          </div>
        )}

        <div className="absolute left-3 top-3">
          <StatusBadge status={product.status} />
        </div>
        <button
          type="button"
          className="absolute right-3 top-3 flex h-9 w-9 items-center justify-center rounded-full bg-white/95 text-[var(--color-text-sub)] shadow-sm ring-1 ring-[var(--color-border)] transition hover:text-[var(--color-primary)] disabled:cursor-not-allowed disabled:opacity-70"
          aria-label="관심상품"
          disabled={isWishLoading}
          onClick={(event) => {
            event.preventDefault();
            onWishClick?.(product);
          }}
        >
          <Heart
            size={19}
            className={product.wished ? 'fill-[var(--color-primary)] text-[var(--color-primary)]' : ''}
          />
        </button>
      </div>
      <div className="p-3.5">
        <div className="mb-1.5">
          <h2 className="line-clamp-2 min-h-9 text-[14px] font-extrabold leading-snug text-[var(--color-text-main)] group-hover:text-[var(--color-primary)]">
            {product.title}
          </h2>
        </div>
        <p className="text-[17px] font-black text-[var(--color-text-main)]">{formatPrice(product.price)}</p>
        <div className="mt-1.5 flex items-center justify-between gap-3 text-xs font-bold text-[var(--color-text-sub)]">
          <p className="flex min-w-0 items-center gap-1">
            <MapPin size={14} />
            <span className="truncate">{categoryLabel}</span>
          </p>
          {timeLabel && <span className="shrink-0">{timeLabel}</span>}
        </div>
      </div>
    </Link>
  );
}
