import { AlertCircle, Heart, ImageIcon, Loader2, MessageCircle, Pencil, ShieldCheck } from 'lucide-react';
import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { fetchMe } from '../api/authApi.js';
import { getAccessToken, getStoredAuthUser } from '../api/authStorage.js';
import { createTradeChatRoom, getChatApiErrorMessage } from '../api/chatApi.js';
import { getMemberProfile, getProduct, getProductApiErrorMessage } from '../api/productApi.js';
import { addWish, getMyWishes, getWishApiErrorMessage, removeWish } from '../api/wishApi.js';
import StatusBadge from '../components/StatusBadge.jsx';
import routePaths from '../routes/routePaths.js';

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

function formatDateTime(value) {
  if (!value) {
    return '';
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return '';
  }

  return date.toLocaleString('ko-KR', {
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export default function ProductDetailPage() {
  const { productId } = useParams();
  const navigate = useNavigate();
  const [product, setProduct] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isWishLoading, setIsWishLoading] = useState(false);
  const [isChatStarting, setIsChatStarting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [wishMessage, setWishMessage] = useState('');
  const [chatMessage, setChatMessage] = useState('');
  const [currentUser, setCurrentUser] = useState(getStoredAuthUser);
  const [sellerProfile, setSellerProfile] = useState(null);

  useEffect(() => {
    let isActive = true;

    setIsLoading(true);
    setErrorMessage('');

    async function loadProductDetail() {
      try {
        const productData = await getProduct(productId);
        let wished = productData.wished;
        let authUser = getStoredAuthUser();

        if (getAccessToken()) {
          if (!authUser) {
            try {
              authUser = await fetchMe();
            } catch {
              authUser = null;
            }
          }

          try {
            const wishes = await getMyWishes();
            wished = wishes.some((wish) => String(wish.productId) === String(productData.productId));
          } catch {
            wished = productData.wished;
          }
        }

        if (isActive) {
          setCurrentUser(authUser);
          setProduct({ ...productData, wished });
        }

        getMemberProfile(productData.sellerId)
          .then((profile) => {
            if (isActive) {
              setSellerProfile(profile);
            }
          })
          .catch(() => {
            if (isActive) {
              setSellerProfile(null);
            }
          });
      } catch (error) {
        if (isActive) {
          setErrorMessage(getProductApiErrorMessage(error, '상품 상세 정보를 불러오지 못했습니다.'));
        }
      } finally {
        if (isActive) {
          setIsLoading(false);
        }
      }
    }

    loadProductDetail();

    return () => {
      isActive = false;
    };
  }, [productId]);

  if (isLoading) {
    return (
      <div className="theme-card flex min-h-80 flex-col items-center justify-center gap-4 rounded-[32px] p-8 text-center">
        <Loader2 size={36} className="animate-spin text-[var(--color-primary)]" />
        <p className="text-sm font-bold text-[var(--color-text-sub)]">상품 상세 정보를 불러오는 중입니다</p>
      </div>
    );
  }

  if (errorMessage || !product) {
    return (
      <div className="theme-card flex min-h-80 flex-col items-center justify-center rounded-[32px] p-8 text-center">
        <div className="mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-red-50 text-red-500">
          <AlertCircle size={28} />
        </div>
        <h1 className="text-2xl font-black">상품을 찾을 수 없습니다</h1>
        <p className="mt-2 max-w-sm text-sm leading-6 text-[var(--color-text-sub)]">
          {errorMessage || '상품이 삭제되었거나 존재하지 않습니다.'}
        </p>
        <Link
          to={routePaths.home}
          className="theme-primary-button mt-5 inline-flex rounded-2xl px-5 py-3 text-sm font-black transition"
        >
          홈으로 가기
        </Link>
      </div>
    );
  }

  const imageUrl = product.imageUrls?.[0];
  const categoryLabel = categoryLabelMap[product.category] || product.category || '동네 거래';
  const isWished = Boolean(product.wished);
  const isOwner = currentUser && String(currentUser.memberId) === String(product.sellerId);

  const handleWishToggle = async () => {
    setIsWishLoading(true);
    setWishMessage('');

    try {
      const wishStatus = isWished ? await removeWish(product.productId) : await addWish(product.productId);
      setProduct((prevProduct) => ({
        ...prevProduct,
        wished: wishStatus.wished,
      }));
    } catch (error) {
      const errorCode = error?.response?.data?.code;

      if (errorCode === 'WISH_ALREADY_EXISTS') {
        setProduct((prevProduct) => ({ ...prevProduct, wished: true }));
        setWishMessage('이미 관심상품으로 등록된 상품입니다.');
      } else if (errorCode === 'WISH_NOT_FOUND') {
        setProduct((prevProduct) => ({ ...prevProduct, wished: false }));
        setWishMessage('이미 관심상품에서 해제된 상품입니다.');
      } else {
        setWishMessage(getWishApiErrorMessage(error));
      }
    } finally {
      setIsWishLoading(false);
    }
  };

  const handleStartChat = async () => {
    setChatMessage('');

    if (!getAccessToken()) {
      navigate(routePaths.login, {
        state: { message: '채팅은 로그인 후 이용할 수 있습니다.' },
      });
      return;
    }

    if (isOwner) {
      setChatMessage('내가 등록한 상품에는 채팅을 시작할 수 없습니다.');
      return;
    }

    setIsChatStarting(true);

    try {
      const chatRoom = await createTradeChatRoom(product.productId);
      navigate(routePaths.chatRoom(chatRoom.roomId));
    } catch (error) {
      setChatMessage(getChatApiErrorMessage(error, '채팅방을 열지 못했습니다.'));
    } finally {
      setIsChatStarting(false);
    }
  };

  return (
    <div className="grid gap-6 md:grid-cols-[0.9fr_1.1fr]">
      <section className="theme-card overflow-hidden rounded-[32px]">
        <div className="product-image-box aspect-square">
          {imageUrl ? (
            <img src={imageUrl} alt={product.title} className="h-full w-full object-cover" />
          ) : (
            <div className="flex h-full flex-col items-center justify-center gap-3 text-[var(--color-text-sub)]">
              <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-white text-[var(--color-primary)] shadow-sm ring-1 ring-[var(--color-border)]">
                <ImageIcon size={30} strokeWidth={1.8} />
              </div>
              <span className="text-sm font-bold">이미지 준비중</span>
            </div>
          )}
        </div>
      </section>

      <section className="theme-card rounded-[32px] p-6">
        <div className="flex items-center justify-between gap-3">
          <p className="text-sm font-extrabold uppercase tracking-wide text-[var(--color-primary)]">
            Product #{product.productId}
          </p>
          <StatusBadge status={product.status} />
        </div>
        <h1 className="mt-3 text-3xl font-black tracking-tight">{product.title}</h1>
        <p className="mt-2 text-sm font-semibold text-[var(--color-text-sub)]">
          {categoryLabel}
          {formatDateTime(product.createdAt) && ` · ${formatDateTime(product.createdAt)}`}
        </p>
        <p className="mt-5 text-3xl font-black">{formatPrice(product.price)}</p>
        <p className="mt-5 whitespace-pre-line leading-7 text-[var(--color-text-sub)]">
          {product.description || '상품 설명이 등록되지 않았습니다.'}
        </p>

        <div className="mt-6 rounded-2xl bg-slate-50 p-4">
          <div className="flex items-center gap-3">
            <div className="flex h-12 w-12 items-center justify-center rounded-full bg-[var(--color-primary)] font-black text-[var(--color-on-primary)]">
              {product.sellerNickname?.slice(0, 1) || '봄'}
            </div>
            <div>
              <p className="font-black">{product.sellerNickname || '판매자'}</p>
              <p className="text-sm text-[var(--color-text-sub)]">
                판매 상품 {(sellerProfile?.productCount ?? 0).toLocaleString()}개
              </p>
            </div>
            <ShieldCheck className="ml-auto text-[var(--color-primary)]" />
          </div>
        </div>

        {wishMessage && (
          <div className="mt-4 rounded-2xl border border-amber-100 bg-amber-50 px-4 py-3 text-sm font-bold text-amber-700">
            {wishMessage}
          </div>
        )}

        {chatMessage && (
          <div className="mt-4 rounded-2xl border border-amber-100 bg-amber-50 px-4 py-3 text-sm font-bold text-amber-700">
            {chatMessage}
          </div>
        )}

        <div className={['mt-6 grid gap-3', isOwner ? 'sm:grid-cols-3' : 'sm:grid-cols-2'].join(' ')}>
          {isOwner && (
            <Link
              to={routePaths.productEdit(product.productId)}
              className="theme-secondary-button flex items-center justify-center gap-2 rounded-2xl px-4 py-3 font-black transition"
            >
              <Pencil size={18} />
              수정하기
            </Link>
          )}
          <button
            type="button"
            onClick={handleWishToggle}
            disabled={isWishLoading}
            className={[
              'flex items-center justify-center gap-2 rounded-2xl px-4 py-3 font-black transition disabled:cursor-not-allowed disabled:opacity-70',
              isWished ? 'theme-primary-button' : 'theme-secondary-button',
            ].join(' ')}
          >
            <Heart size={19} className={isWished ? 'fill-current' : ''} />
            {isWishLoading ? '처리 중' : isWished ? '찜 해제' : '찜하기'}
          </button>
          <button
            type="button"
            onClick={handleStartChat}
            disabled={isChatStarting}
            className="theme-primary-button flex items-center justify-center gap-2 rounded-2xl px-4 py-3 font-black transition"
          >
            {isChatStarting ? <Loader2 size={19} className="animate-spin" /> : <MessageCircle size={19} />}
            {isChatStarting ? '연결 중' : '채팅하기'}
          </button>
        </div>
      </section>
    </div>
  );
}
