import {
  AlertCircle,
  CheckCircle2,
  Loader2,
  Package,
  Pencil,
  RotateCcw,
  Save,
  ShoppingBag,
  Ticket,
  Trash2,
} from 'lucide-react';
import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { getAccessToken } from '../api/authStorage.js';
import { getMyPage, getMyPageApiErrorMessage, updateMyInfo } from '../api/myPageApi.js';
import {
  cancelProductReservation,
  deleteProduct,
  getMyProducts,
  getProductApiErrorMessage,
  updateProductStatus,
} from '../api/productApi.js';
import EmptyState from '../components/EmptyState.jsx';
import PageHeader from '../components/PageHeader.jsx';
import StatusBadge from '../components/StatusBadge.jsx';
import routePaths from '../routes/routePaths.js';

const PAGE_SIZE = 10;

const productStatusFilters = [
  { label: '전체', value: '' },
  { label: '판매중', value: 'ON_SALE' },
  { label: '예약중', value: 'RESERVED' },
  { label: '판매완료', value: 'SOLD' },
];

function formatPrice(price) {
  if (typeof price === 'number') {
    return `${price.toLocaleString()}원`;
  }

  return price;
}

function formatDate(value) {
  if (!value) {
    return '';
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return '';
  }

  return date.toLocaleDateString('ko-KR', {
    month: '2-digit',
    day: '2-digit',
  });
}

export default function MyPage() {
  const navigate = useNavigate();
  const [summary, setSummary] = useState(null);
  const [nicknameInput, setNicknameInput] = useState('');
  const [products, setProducts] = useState([]);
  const [pageInfo, setPageInfo] = useState({
    page: 0,
    size: PAGE_SIZE,
    totalElements: 0,
    totalPages: 0,
  });
  const [selectedStatus, setSelectedStatus] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [isLoadingMore, setIsLoadingMore] = useState(false);
  const [isSavingNickname, setIsSavingNickname] = useState(false);
  const [updatingProductId, setUpdatingProductId] = useState(null);
  const [message, setMessage] = useState(null);

  const hasMore = pageInfo.page + 1 < pageInfo.totalPages;

  const loadSummary = async () => {
    const myPage = await getMyPage();
    setSummary(myPage);
    setNicknameInput(myPage.nickname || '');
  };

  const loadProducts = async ({ status = selectedStatus, page = 0, append = false } = {}) => {
    if (append) {
      setIsLoadingMore(true);
    }

    const nextPage = await getMyProducts({ status, page, size: PAGE_SIZE });
    setProducts((prevProducts) => (append ? [...prevProducts, ...nextPage.content] : nextPage.content));
    setPageInfo(nextPage);
    setIsLoadingMore(false);
  };

  const loadPage = async ({ status = selectedStatus } = {}) => {
    if (!getAccessToken()) {
      navigate(routePaths.login, {
        state: { message: '마이페이지는 로그인 후 이용할 수 있습니다.' },
      });
      return;
    }

    setIsLoading(true);
    setMessage(null);

    try {
      await Promise.all([loadSummary(), loadProducts({ status, page: 0 })]);
    } catch (error) {
      setMessage({
        type: 'error',
        text: getMyPageApiErrorMessage(error, '마이페이지 정보를 불러오지 못했습니다.'),
      });
      setProducts([]);
    } finally {
      setIsLoading(false);
      setIsLoadingMore(false);
    }
  };

  useEffect(() => {
    loadPage({ status: '' });
  }, []);

  const handleStatusFilterChange = async (status) => {
    setSelectedStatus(status);
    setMessage(null);

    try {
      await loadProducts({ status, page: 0 });
    } catch (error) {
      setMessage({
        type: 'error',
        text: getProductApiErrorMessage(error, '내 상품 목록을 불러오지 못했습니다.'),
      });
      setProducts([]);
    }
  };

  const handleNicknameSubmit = async (event) => {
    event.preventDefault();

    const nickname = nicknameInput.trim();
    if (!nickname || isSavingNickname) {
      return;
    }

    setIsSavingNickname(true);
    setMessage(null);

    try {
      const updatedUser = await updateMyInfo({ nickname });
      setSummary((prevSummary) => ({ ...prevSummary, nickname: updatedUser.nickname }));
      setMessage({ type: 'success', text: '닉네임이 수정되었습니다.' });
    } catch (error) {
      setMessage({
        type: 'error',
        text: getMyPageApiErrorMessage(error, '닉네임 수정에 실패했습니다.'),
      });
    } finally {
      setIsSavingNickname(false);
    }
  };

  const handleProductStatusChange = async (product, nextStatus) => {
    if (updatingProductId || product.status === nextStatus) {
      return;
    }

    setUpdatingProductId(product.productId);
    setMessage(null);

    try {
      if (product.status === 'RESERVED' && nextStatus === 'ON_SALE') {
        await cancelProductReservation(product.productId);
      } else {
        await updateProductStatus(product.productId, nextStatus);
      }

      setProducts((prevProducts) =>
        prevProducts.map((prevProduct) =>
          prevProduct.productId === product.productId ? { ...prevProduct, status: nextStatus } : prevProduct,
        ),
      );
      await loadSummary();
      setMessage({ type: 'success', text: '상품 상태가 변경되었습니다.' });
    } catch (error) {
      setMessage({
        type: 'error',
        text: getProductApiErrorMessage(error, '상품 상태 변경에 실패했습니다.'),
      });
    } finally {
      setUpdatingProductId(null);
    }
  };

  const handleDeleteProduct = async (product) => {
    if (updatingProductId) {
      return;
    }

    const ok = window.confirm('상품을 삭제할까요? 삭제 후에는 목록에서 보이지 않습니다.');
    if (!ok) {
      return;
    }

    setUpdatingProductId(product.productId);
    setMessage(null);

    try {
      await deleteProduct(product.productId);
      setProducts((prevProducts) => prevProducts.filter((prevProduct) => prevProduct.productId !== product.productId));
      await loadSummary();
      setMessage({ type: 'success', text: '상품이 삭제되었습니다.' });
    } catch (error) {
      setMessage({
        type: 'error',
        text: getProductApiErrorMessage(error, '상품 삭제에 실패했습니다.'),
      });
    } finally {
      setUpdatingProductId(null);
    }
  };

  if (isLoading) {
    return (
      <div className="theme-card flex min-h-80 flex-col items-center justify-center gap-4 rounded-[32px] p-8 text-center">
        <Loader2 size={36} className="animate-spin text-[var(--color-primary)]" />
        <p className="text-sm font-bold text-[var(--color-text-sub)]">마이페이지를 불러오는 중입니다</p>
      </div>
    );
  }

  return (
    <div>
      <PageHeader
        eyebrow="My Page"
        title={`${summary?.nickname || '회원'}님의 마이페이지`}
        description="내 정보와 판매 상품 상태를 한곳에서 관리하세요."
        action={
          <form className="flex w-full flex-col gap-2 sm:w-auto sm:flex-row" onSubmit={handleNicknameSubmit}>
            <input
              className="theme-input rounded-2xl px-4 py-3 text-sm font-semibold outline-none sm:w-48"
              value={nicknameInput}
              onChange={(event) => setNicknameInput(event.target.value)}
              maxLength={50}
              placeholder="닉네임"
            />
            <button
              type="submit"
              disabled={isSavingNickname || !nicknameInput.trim()}
              className="theme-primary-button inline-flex items-center justify-center gap-2 rounded-2xl px-4 py-3 text-sm font-black transition disabled:cursor-not-allowed disabled:opacity-70"
            >
              {isSavingNickname ? <Loader2 size={17} className="animate-spin" /> : <Save size={17} />}
              저장
            </button>
          </form>
        }
      />

      <section className="mb-5 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <div className="theme-card rounded-[24px] p-4">
          <p className="text-sm font-bold text-[var(--color-text-sub)]">판매 상품</p>
          <p className="mt-2 text-3xl font-black text-[var(--color-text-main)]">
            {(summary?.sellingProductCount ?? 0).toLocaleString()}
          </p>
        </div>
        <div className="theme-card rounded-[24px] p-4">
          <p className="text-sm font-bold text-[var(--color-text-sub)]">관심상품</p>
          <p className="mt-2 text-3xl font-black text-[var(--color-primary)]">
            {(summary?.wishedProductCount ?? 0).toLocaleString()}
          </p>
        </div>
        <div className="theme-card rounded-[24px] p-4">
          <p className="text-sm font-bold text-[var(--color-text-sub)]">채팅방</p>
          <p className="mt-2 text-3xl font-black text-blue-600">
            {(summary?.chatRoomCount ?? 0).toLocaleString()}
          </p>
        </div>
        <div className="theme-card rounded-[24px] p-4">
          <p className="text-sm font-bold text-[var(--color-text-sub)]">보유 쿠폰</p>
          <p className="mt-2 text-3xl font-black text-orange-500">
            {(summary?.couponCount ?? 0).toLocaleString()}
          </p>
        </div>
      </section>

      {message && (
        <div
          className={[
            'mb-5 flex items-center gap-2 rounded-2xl border px-4 py-3 text-sm font-bold',
            message.type === 'success'
              ? 'border-emerald-100 bg-emerald-50 text-emerald-700'
              : 'border-red-100 bg-red-50 text-red-600',
          ].join(' ')}
        >
          {message.type === 'success' ? <CheckCircle2 size={17} /> : <AlertCircle size={17} />}
          {message.text}
        </div>
      )}

      <section className="mb-5 flex gap-2 overflow-x-auto pb-1">
        {productStatusFilters.map((filter) => (
          <button
            key={filter.value}
            type="button"
            onClick={() => handleStatusFilterChange(filter.value)}
            className={[
              'category-chip theme-card-hover inline-flex shrink-0 items-center rounded-full px-4 py-2.5 text-sm font-black transition',
              selectedStatus === filter.value ? 'category-chip-active' : '',
            ].join(' ')}
          >
            {filter.label}
          </button>
        ))}
      </section>

      {products.length > 0 ? (
        <>
          <section className="theme-card overflow-hidden rounded-[32px]">
            {products.map((product, index) => {
              const isUpdating = updatingProductId === product.productId;

              return (
                <article
                  key={product.productId}
                  className={`flex flex-col gap-4 p-5 lg:flex-row lg:items-center ${index !== 0 ? 'border-t border-[var(--color-border)]' : ''}`}
                >
                  <Link
                    to={routePaths.productDetail(product.productId)}
                    className="flex min-w-0 flex-1 items-center gap-4"
                  >
                    <div className="flex h-20 w-20 shrink-0 items-center justify-center overflow-hidden rounded-2xl bg-slate-100 text-[var(--color-text-sub)]">
                      {product.thumbnailUrl ? (
                        <img src={product.thumbnailUrl} alt={product.title} className="h-full w-full object-cover" />
                      ) : (
                        <Package size={26} />
                      )}
                    </div>
                    <div className="min-w-0">
                      <div className="flex flex-wrap items-center gap-2">
                        <h2 className="truncate font-black">{product.title}</h2>
                        <StatusBadge status={product.status} />
                      </div>
                      <p className="mt-1 text-sm font-black text-[var(--color-text-main)]">
                        {formatPrice(product.price)}
                      </p>
                      <p className="mt-1 text-xs font-bold text-[var(--color-text-sub)]">
                        {product.category} · {formatDate(product.createdAt)}
                      </p>
                    </div>
                  </Link>

                  <div className="flex flex-wrap items-center gap-2 lg:justify-end">
                    {product.status === 'ON_SALE' && (
                      <>
                        <button
                          type="button"
                          onClick={() => handleProductStatusChange(product, 'RESERVED')}
                          disabled={isUpdating}
                          className="theme-secondary-button rounded-2xl px-3 py-2 text-xs font-black disabled:cursor-not-allowed disabled:opacity-60"
                        >
                          예약중
                        </button>
                        <button
                          type="button"
                          onClick={() => handleProductStatusChange(product, 'SOLD')}
                          disabled={isUpdating}
                          className="theme-secondary-button rounded-2xl px-3 py-2 text-xs font-black disabled:cursor-not-allowed disabled:opacity-60"
                        >
                          판매완료
                        </button>
                      </>
                    )}
                    {product.status === 'RESERVED' && (
                      <>
                        <button
                          type="button"
                          onClick={() => handleProductStatusChange(product, 'ON_SALE')}
                          disabled={isUpdating}
                          className="theme-secondary-button inline-flex items-center gap-1 rounded-2xl px-3 py-2 text-xs font-black disabled:cursor-not-allowed disabled:opacity-60"
                        >
                          <RotateCcw size={14} />
                          예약취소
                        </button>
                        <button
                          type="button"
                          onClick={() => handleProductStatusChange(product, 'SOLD')}
                          disabled={isUpdating}
                          className="theme-secondary-button rounded-2xl px-3 py-2 text-xs font-black disabled:cursor-not-allowed disabled:opacity-60"
                        >
                          판매완료
                        </button>
                      </>
                    )}
                    <Link
                      to={routePaths.productEdit(product.productId)}
                      className="theme-secondary-button inline-flex items-center gap-1 rounded-2xl px-3 py-2 text-xs font-black"
                    >
                      <Pencil size={14} />
                      수정
                    </Link>
                    <button
                      type="button"
                      onClick={() => handleDeleteProduct(product)}
                      disabled={isUpdating}
                      className="inline-flex items-center gap-1 rounded-2xl border border-red-100 bg-red-50 px-3 py-2 text-xs font-black text-red-600 transition hover:bg-red-100 disabled:cursor-not-allowed disabled:opacity-60"
                    >
                      {isUpdating ? <Loader2 size={14} className="animate-spin" /> : <Trash2 size={14} />}
                      삭제
                    </button>
                  </div>
                </article>
              );
            })}
          </section>

          {hasMore && (
            <div className="mt-6 flex justify-center">
              <button
                type="button"
                onClick={() => loadProducts({ status: selectedStatus, page: pageInfo.page + 1, append: true })}
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
          icon={ShoppingBag}
          title="내 판매 상품이 없습니다"
          description="상품을 등록하면 이곳에서 수정, 상태 변경, 삭제를 관리할 수 있습니다."
        />
      )}

      <div className="mt-6 flex flex-wrap gap-2">
        <Link
          to={routePaths.productNew}
          className="theme-primary-button inline-flex items-center gap-2 rounded-2xl px-4 py-3 text-sm font-black"
        >
          <ShoppingBag size={17} />
          상품 등록
        </Link>
        <Link
          to={routePaths.coupons}
          className="theme-secondary-button inline-flex items-center gap-2 rounded-2xl px-4 py-3 text-sm font-black"
        >
          <Ticket size={17} />
          쿠폰함 보기
        </Link>
      </div>
    </div>
  );
}
