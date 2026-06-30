import {
  AlertCircle,
  ChevronRight,
  Loader2,
  MessageCircle,
  Plus,
  RotateCcw,
  Search,
  ShieldCheck,
  Ticket,
  X,
} from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getAccessToken } from '../api/authStorage.js';
import {
  deleteRecentSearch,
  getPopularSearches,
  getProductApiErrorMessage,
  getRecentSearches,
  searchProducts,
} from '../api/productApi.js';
import { addWish, getMyWishes, getWishApiErrorMessage, removeWish } from '../api/wishApi.js';
import EmptyState from '../components/EmptyState.jsx';
import PageHeader from '../components/PageHeader.jsx';
import ProductCard from '../components/ProductCard.jsx';
import routePaths from '../routes/routePaths.js';

const PAGE_SIZE = 12;

const categoryOptions = [
  { label: '전체', value: '' },
  { label: '디지털', value: 'DIGITAL' },
  { label: '가구', value: 'FURNITURE' },
  { label: '의류', value: 'CLOTHING' },
  { label: '도서', value: 'BOOK' },
  { label: '스포츠', value: 'SPORTS' },
  { label: '생활용품', value: 'ETC' },
];

const baseStats = [
  { label: '오늘 등록 상품', icon: Plus },
  { label: '진행 중 채팅', value: '24', icon: MessageCircle },
  { label: '발급 가능 쿠폰', value: '2', icon: Ticket },
  { label: '고객 문의', value: '5', icon: ShieldCheck },
];

function getCategoryLabel(categoryValue) {
  return categoryOptions.find((category) => category.value === categoryValue)?.label || '전체';
}

export default function HomePage() {
  const navigate = useNavigate();
  const [products, setProducts] = useState([]);
  const [pageInfo, setPageInfo] = useState({
    page: 0,
    size: PAGE_SIZE,
    totalElements: 0,
    totalPages: 0,
  });
  const [searchInput, setSearchInput] = useState('');
  const [submittedKeyword, setSubmittedKeyword] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('');
  const [popularSearches, setPopularSearches] = useState([]);
  const [recentSearches, setRecentSearches] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isLoadingMore, setIsLoadingMore] = useState(false);
  const [wishUpdatingProductId, setWishUpdatingProductId] = useState(null);
  const [deletingRecentSearchId, setDeletingRecentSearchId] = useState(null);
  const [errorMessage, setErrorMessage] = useState('');

  const hasFilter = Boolean(submittedKeyword || selectedCategory);
  const hasMore = pageInfo.page + 1 < pageInfo.totalPages;

  const stats = useMemo(
    () =>
      baseStats.map((stat, index) => ({
        ...stat,
        value: index === 0 ? pageInfo.totalElements.toLocaleString() : stat.value,
      })),
    [pageInfo.totalElements],
  );

  const mergeWishState = async (nextProducts) => {
    if (!getAccessToken() || nextProducts.length === 0) {
      return nextProducts;
    }

    try {
      const wishes = await getMyWishes();
      const wishedProductIds = new Set(wishes.map((wish) => String(wish.productId)));

      return nextProducts.map((product) => ({
        ...product,
        wished: wishedProductIds.has(String(product.productId)),
      }));
    } catch {
      return nextProducts;
    }
  };

  const loadProducts = async ({
    page = 0,
    append = false,
    keyword = submittedKeyword,
    category = selectedCategory,
  } = {}) => {
    const normalizedKeyword = keyword.trim();

    if (append) {
      setIsLoadingMore(true);
    } else {
      setIsLoading(true);
    }

    setErrorMessage('');

    try {
      const nextPage = await searchProducts({
        keyword: normalizedKeyword,
        category,
        page,
        size: PAGE_SIZE,
        sort: 'LATEST',
      });

      const nextProducts = await mergeWishState(nextPage.content);

      setProducts((prevProducts) => (append ? [...prevProducts, ...nextProducts] : nextProducts));
      setPageInfo(nextPage);
    } catch (error) {
      setErrorMessage(getProductApiErrorMessage(error));
      if (!append) {
        setProducts([]);
        setPageInfo({
          page: 0,
          size: PAGE_SIZE,
          totalElements: 0,
          totalPages: 0,
        });
      }
    } finally {
      setIsLoading(false);
      setIsLoadingMore(false);
    }
  };

  const loadPopularSearches = async () => {
    try {
      const popularKeywords = await getPopularSearches();
      setPopularSearches(popularKeywords);
    } catch {
      setPopularSearches([]);
    }
  };

  const loadRecentSearches = async () => {
    if (!getAccessToken()) {
      setRecentSearches([]);
      return;
    }

    try {
      const recentKeywords = await getRecentSearches();
      setRecentSearches(recentKeywords);
    } catch {
      setRecentSearches([]);
    }
  };

  useEffect(() => {
    loadProducts({ keyword: '', category: '' });
    loadRecentSearches();
    loadPopularSearches();
  }, []);

  const handleSearchSubmit = async (event) => {
    event.preventDefault();

    const nextKeyword = searchInput.trim();
    setSubmittedKeyword(nextKeyword);
    await loadProducts({ page: 0, keyword: nextKeyword, category: selectedCategory });
    await loadRecentSearches();
    await loadPopularSearches();
  };

  const handleCategoryChange = (categoryValue) => {
    setSelectedCategory(categoryValue);
    loadProducts({ page: 0, keyword: submittedKeyword, category: categoryValue });
  };

  const handlePopularKeywordClick = async (keyword) => {
    setSearchInput(keyword);
    setSubmittedKeyword(keyword);
    await loadProducts({ page: 0, keyword, category: selectedCategory });
    await loadRecentSearches();
    await loadPopularSearches();
  };

  const handleRecentKeywordClick = async (keyword) => {
    setSearchInput(keyword);
    setSubmittedKeyword(keyword);
    await loadProducts({ page: 0, keyword, category: selectedCategory });
    await loadRecentSearches();
    await loadPopularSearches();
  };

  const handleDeleteRecentSearch = async (searchLogId) => {
    setDeletingRecentSearchId(searchLogId);
    setErrorMessage('');

    try {
      await deleteRecentSearch(searchLogId);
      setRecentSearches((prevSearches) =>
        prevSearches.filter((recent) => recent.searchLogId !== searchLogId),
      );
    } catch (error) {
      setErrorMessage(getProductApiErrorMessage(error, '최근 검색어를 삭제하지 못했습니다.'));
    } finally {
      setDeletingRecentSearchId(null);
    }
  };

  const handleReset = () => {
    setSearchInput('');
    setSubmittedKeyword('');
    setSelectedCategory('');
    loadProducts({ page: 0, keyword: '', category: '' });
  };

  const handleLoadMore = () => {
    loadProducts({
      page: pageInfo.page + 1,
      append: true,
      keyword: submittedKeyword,
      category: selectedCategory,
    });
  };

  const handleRefresh = () => {
    loadProducts({ page: 0, keyword: submittedKeyword, category: selectedCategory });
    loadRecentSearches();
    loadPopularSearches();
  };

  const handleWishToggle = async (product) => {
    if (!getAccessToken()) {
      navigate(routePaths.login, {
        state: { message: '관심상품은 로그인 후 이용할 수 있습니다.' },
      });
      return;
    }

    setWishUpdatingProductId(product.productId);
    setErrorMessage('');

    try {
      const wishStatus = product.wished
        ? await removeWish(product.productId)
        : await addWish(product.productId);

      setProducts((prevProducts) =>
        prevProducts.map((prevProduct) =>
          prevProduct.productId === product.productId
            ? { ...prevProduct, wished: wishStatus.wished }
            : prevProduct,
        ),
      );
    } catch (error) {
      const errorCode = error?.response?.data?.code;

      if (errorCode === 'WISH_ALREADY_EXISTS') {
        setProducts((prevProducts) =>
          prevProducts.map((prevProduct) =>
            prevProduct.productId === product.productId ? { ...prevProduct, wished: true } : prevProduct,
          ),
        );
      } else if (errorCode === 'WISH_NOT_FOUND') {
        setProducts((prevProducts) =>
          prevProducts.map((prevProduct) =>
            prevProduct.productId === product.productId ? { ...prevProduct, wished: false } : prevProduct,
          ),
        );
      } else {
        setErrorMessage(getWishApiErrorMessage(error));
      }
    } finally {
      setWishUpdatingProductId(null);
    }
  };

  const pageTitle = submittedKeyword ? `"${submittedKeyword}" 검색 결과` : '오늘 올라온 추천 상품';
  const pageDescription = `${getCategoryLabel(selectedCategory)} 카테고리에서 ${pageInfo.totalElements.toLocaleString()}개 상품을 확인할 수 있어요.`;

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

            <form
              className="mt-6 flex max-w-[620px] items-center gap-2 rounded-full bg-white px-4 py-2.5 text-[var(--color-text-sub)] shadow-sm ring-1 ring-[var(--color-border)]"
              onSubmit={handleSearchSubmit}
            >
              <Search size={20} className="shrink-0 text-[var(--color-primary)]" />
              <input
                className="min-w-0 flex-1 bg-transparent text-sm font-bold text-[var(--color-text-main)] outline-none placeholder:text-[var(--color-text-sub)]"
                value={searchInput}
                onChange={(event) => setSearchInput(event.target.value)}
                placeholder="상품명, 지역, 카테고리 검색"
              />
              <button
                type="submit"
                disabled={isLoading}
                className="theme-primary-button shrink-0 rounded-full px-4 py-2 text-sm font-black transition disabled:cursor-not-allowed disabled:opacity-70"
              >
                검색
              </button>
            </form>
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

      {getAccessToken() && recentSearches.length > 0 && (
        <section className="mb-3 rounded-[28px] border border-[var(--color-border)] bg-white p-4 shadow-[0_10px_28px_rgba(15,23,42,0.04)]">
          <div className="flex flex-col gap-3 md:flex-row md:items-center">
            <div className="shrink-0 text-sm font-black text-[var(--color-text-main)]">내 검색어</div>
            <div className="flex min-w-0 flex-1 gap-2 overflow-x-auto pb-1 md:pb-0">
              {recentSearches.map((recent) => (
                <span
                  key={recent.searchLogId}
                  className="inline-flex shrink-0 items-center overflow-hidden rounded-full border border-[var(--color-border)] bg-white text-sm font-black text-[var(--color-text-main)] transition hover:border-[var(--color-primary)] hover:bg-[var(--color-primary-soft)]"
                >
                  <button
                    type="button"
                    onClick={() => handleRecentKeywordClick(recent.keyword)}
                    className="px-3 py-2 transition hover:text-[var(--color-primary-dark)]"
                  >
                    {recent.keyword}
                  </button>
                  <button
                    type="button"
                    aria-label={`${recent.keyword} 검색어 삭제`}
                    onClick={() => handleDeleteRecentSearch(recent.searchLogId)}
                    disabled={deletingRecentSearchId === recent.searchLogId}
                    className="flex h-full items-center border-l border-[var(--color-border)] px-2.5 py-2 text-[var(--color-text-sub)] transition hover:bg-red-50 hover:text-red-500 disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    <X size={14} />
                  </button>
                </span>
              ))}
            </div>
          </div>
        </section>
      )}

      <section className="mb-5 rounded-[28px] border border-[var(--color-border)] bg-white p-4 shadow-[0_10px_28px_rgba(15,23,42,0.04)]">
        <div className="flex flex-col gap-3 md:flex-row md:items-center">
          <div className="shrink-0 text-sm font-black text-[var(--color-text-main)]">인기 검색어 TOP10</div>
          <div className="flex min-w-0 flex-1 gap-2 overflow-x-auto pb-1 md:pb-0">
            {popularSearches.length > 0 ? (
              popularSearches.map((popular) => (
                <button
                  key={popular.keyword}
                  type="button"
                  onClick={() => handlePopularKeywordClick(popular.keyword)}
                  className="inline-flex shrink-0 items-center gap-1.5 rounded-full border border-[var(--color-border)] bg-white px-3 py-2 text-sm font-black text-[var(--color-text-main)] transition hover:border-[var(--color-primary)] hover:bg-[var(--color-primary-soft)] hover:text-[var(--color-primary-dark)]"
                >
                  {popular.keyword}
                  <span className="text-xs font-bold text-[var(--color-text-sub)]">{popular.searchCount}</span>
                </button>
              ))
            ) : (
              <span className="rounded-full bg-slate-50 px-3 py-2 text-sm font-bold text-[var(--color-text-sub)]">
                아직 집계된 인기 검색어가 없습니다
              </span>
            )}
          </div>
        </div>
      </section>

      <section className="mb-6 flex gap-2 overflow-x-auto pb-1">
        {categoryOptions.map((category) => (
          <button
            key={category.label}
            type="button"
            onClick={() => handleCategoryChange(category.value)}
            className={[
              'category-chip theme-card-hover inline-flex shrink-0 items-center rounded-full px-4 py-2.5 text-sm font-black text-[var(--color-text-main)] transition',
              selectedCategory === category.value ? 'category-chip-active' : '',
            ].join(' ')}
          >
            {category.label}
          </button>
        ))}
      </section>

      <PageHeader
        eyebrow="Products"
        title={pageTitle}
        description={pageDescription}
        action={
          hasFilter ? (
            <button
              type="button"
              onClick={handleReset}
              className="theme-primary-button inline-flex items-center gap-1 rounded-full px-4 py-2.5 text-sm font-black transition"
            >
              전체 보기
              <RotateCcw size={16} />
            </button>
          ) : (
            <button
              type="button"
              onClick={handleRefresh}
              className="theme-secondary-button inline-flex items-center gap-1 rounded-full px-4 py-2.5 text-sm font-black transition"
            >
              새로고침
              <ChevronRight size={17} />
            </button>
          )
        }
      />

      {errorMessage && (
        <div className="mb-5 flex items-center gap-2 rounded-2xl border border-red-100 bg-red-50 px-4 py-3 text-sm font-bold text-red-600">
          <AlertCircle size={17} />
          {errorMessage}
        </div>
      )}

      {isLoading ? (
        <div className="theme-card flex min-h-64 flex-col items-center justify-center gap-4 rounded-[28px] p-8 text-center">
          <Loader2 size={34} className="animate-spin text-[var(--color-primary)]" />
          <p className="text-sm font-bold text-[var(--color-text-sub)]">상품을 불러오는 중입니다</p>
        </div>
      ) : products.length > 0 ? (
        <>
          <section className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            {products.map((product) => (
              <ProductCard
                key={product.productId}
                product={product}
                onWishClick={handleWishToggle}
                isWishLoading={wishUpdatingProductId === product.productId}
              />
            ))}
          </section>

          {hasMore && (
            <div className="mt-6 flex justify-center">
              <button
                type="button"
                onClick={handleLoadMore}
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
        <EmptyState title="상품을 찾을 수 없습니다" description="검색어를 바꾸거나 다른 카테고리를 선택해 보세요." />
      )}
    </div>
  );
}
