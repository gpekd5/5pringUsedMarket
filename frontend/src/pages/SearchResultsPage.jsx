import { AlertCircle, ChevronRight, Loader2, RotateCcw, Search } from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { getAccessToken } from '../api/authStorage.js';
import { getProductApiErrorMessage, searchProducts } from '../api/productApi.js';
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

const sortOptions = [
  { label: '최신순', value: 'LATEST' },
  { label: '낮은 가격순', value: 'PRICE_ASC' },
  { label: '높은 가격순', value: 'PRICE_DESC' },
];

function getCategoryLabel(categoryValue) {
  return categoryOptions.find((category) => category.value === categoryValue)?.label || '전체';
}

function buildSearchParams({ keyword, category, sort }) {
  const params = new URLSearchParams();

  if (keyword?.trim()) {
    params.set('keyword', keyword.trim());
  }

  if (category) {
    params.set('category', category);
  }

  if (sort && sort !== 'LATEST') {
    params.set('sort', sort);
  }

  return params;
}

export default function SearchResultsPage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const keyword = searchParams.get('keyword') || '';
  const selectedCategory = searchParams.get('category') || '';
  const selectedSort = searchParams.get('sort') || 'LATEST';

  const [searchInput, setSearchInput] = useState(keyword);
  const [products, setProducts] = useState([]);
  const [pageInfo, setPageInfo] = useState({
    page: 0,
    size: PAGE_SIZE,
    totalElements: 0,
    totalPages: 0,
  });
  const [isLoading, setIsLoading] = useState(true);
  const [isLoadingMore, setIsLoadingMore] = useState(false);
  const [wishUpdatingProductId, setWishUpdatingProductId] = useState(null);
  const [errorMessage, setErrorMessage] = useState('');

  const hasMore = pageInfo.page + 1 < pageInfo.totalPages;
  const hasFilter = Boolean(keyword || selectedCategory || selectedSort !== 'LATEST');

  const pageTitle = keyword ? `"${keyword}" 검색 결과` : '상품 검색';
  const pageDescription = useMemo(
    () =>
      `${getCategoryLabel(selectedCategory)} 카테고리에서 ${pageInfo.totalElements.toLocaleString()}개 상품을 찾았어요.`,
    [pageInfo.totalElements, selectedCategory],
  );

  const updateSearchParams = (nextValues) => {
    const params = buildSearchParams({
      keyword,
      category: selectedCategory,
      sort: selectedSort,
      ...nextValues,
    });

    setSearchParams(params);
  };

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

  const loadProducts = async ({ page = 0, append = false } = {}) => {
    if (append) {
      setIsLoadingMore(true);
    } else {
      setIsLoading(true);
    }

    setErrorMessage('');

    try {
      const nextPage = await searchProducts({
        keyword,
        category: selectedCategory,
        sort: selectedSort,
        page,
        size: PAGE_SIZE,
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

  useEffect(() => {
    setSearchInput(keyword);
    loadProducts({ page: 0 });
  }, [keyword, selectedCategory, selectedSort]);

  const handleSearchSubmit = (event) => {
    event.preventDefault();
    updateSearchParams({ keyword: searchInput, category: selectedCategory, sort: selectedSort });
  };

  const handleCategoryChange = (category) => {
    updateSearchParams({ category });
  };

  const handleSortChange = (event) => {
    updateSearchParams({ sort: event.target.value });
  };

  const handleReset = () => {
    setSearchInput('');
    setSearchParams(new URLSearchParams());
  };

  const handleLoadMore = () => {
    loadProducts({ page: pageInfo.page + 1, append: true });
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

  return (
    <div>
      <PageHeader
        eyebrow="Search"
        title={pageTitle}
        description={pageDescription}
        action={
          hasFilter && (
            <button
              type="button"
              onClick={handleReset}
              className="theme-secondary-button inline-flex items-center gap-1 rounded-full px-4 py-2.5 text-sm font-black transition"
            >
              초기화
              <RotateCcw size={16} />
            </button>
          )
        }
      />

      <section className="theme-card mb-5 rounded-[28px] p-4">
        <form className="flex flex-col gap-3 md:flex-row md:items-center" onSubmit={handleSearchSubmit}>
          <div className="flex min-w-0 flex-1 items-center gap-3 rounded-2xl border border-[var(--color-border)] bg-white px-4 py-3 text-sm">
            <Search size={20} className="shrink-0 text-[var(--color-primary)]" />
            <input
              className="min-w-0 flex-1 bg-transparent font-bold text-[var(--color-text-main)] outline-none placeholder:text-[var(--color-text-sub)]"
              value={searchInput}
              onChange={(event) => setSearchInput(event.target.value)}
              placeholder="검색어를 입력하세요"
            />
          </div>
          <select
            value={selectedSort}
            onChange={handleSortChange}
            className="theme-input rounded-2xl px-4 py-3 text-sm font-black outline-none"
          >
            {sortOptions.map((sort) => (
              <option key={sort.value} value={sort.value}>
                {sort.label}
              </option>
            ))}
          </select>
          <button
            type="submit"
            disabled={isLoading}
            className="theme-primary-button rounded-2xl px-5 py-3 text-sm font-black transition disabled:cursor-not-allowed disabled:opacity-70"
          >
            검색
          </button>
        </form>
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

      {errorMessage && (
        <div className="mb-5 flex items-center gap-2 rounded-2xl border border-red-100 bg-red-50 px-4 py-3 text-sm font-bold text-red-600">
          <AlertCircle size={17} />
          {errorMessage}
        </div>
      )}

      {isLoading ? (
        <div className="theme-card flex min-h-64 flex-col items-center justify-center gap-4 rounded-[28px] p-8 text-center">
          <Loader2 size={34} className="animate-spin text-[var(--color-primary)]" />
          <p className="text-sm font-bold text-[var(--color-text-sub)]">검색 결과를 불러오는 중입니다</p>
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
                <ChevronRight size={17} />
              </button>
            </div>
          )}
        </>
      ) : (
        <EmptyState
          icon={Search}
          title="검색 결과가 없습니다"
          description="검색어를 바꾸거나 카테고리 필터를 초기화해보세요."
        />
      )}
    </div>
  );
}
