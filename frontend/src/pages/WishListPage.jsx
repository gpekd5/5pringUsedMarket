import { AlertCircle, Heart, Loader2 } from 'lucide-react';
import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getMyWishes, getWishApiErrorMessage, removeWish } from '../api/wishApi.js';
import EmptyState from '../components/EmptyState.jsx';
import PageHeader from '../components/PageHeader.jsx';
import ProductCard from '../components/ProductCard.jsx';
import routePaths from '../routes/routePaths.js';

function mapWishProduct(product) {
  return {
    ...product,
    wished: true,
    createdAt: product.wishedAt,
  };
}

export default function WishListPage() {
  const [wishedProducts, setWishedProducts] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [removingProductId, setRemovingProductId] = useState(null);
  const [errorMessage, setErrorMessage] = useState('');

  const loadWishes = async () => {
    setIsLoading(true);
    setErrorMessage('');

    try {
      const wishes = await getMyWishes();
      setWishedProducts(wishes.map(mapWishProduct));
    } catch (error) {
      setErrorMessage(getWishApiErrorMessage(error, '관심상품 목록을 불러오지 못했습니다.'));
      setWishedProducts([]);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadWishes();
  }, []);

  const handleRemoveWish = async (product) => {
    setRemovingProductId(product.productId);
    setErrorMessage('');

    try {
      await removeWish(product.productId);
      setWishedProducts((prevProducts) =>
        prevProducts.filter((prevProduct) => prevProduct.productId !== product.productId),
      );
    } catch (error) {
      const errorCode = error?.response?.data?.code;

      if (errorCode === 'WISH_NOT_FOUND') {
        setWishedProducts((prevProducts) =>
          prevProducts.filter((prevProduct) => prevProduct.productId !== product.productId),
        );
      } else {
        setErrorMessage(getWishApiErrorMessage(error));
      }
    } finally {
      setRemovingProductId(null);
    }
  };

  return (
    <div>
      <PageHeader
        eyebrow="Wishes"
        title="내가 찜한 상품"
        description={`관심 있는 상품 ${wishedProducts.length.toLocaleString()}개를 모아보고 거래 상태를 빠르게 확인하세요.`}
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
          <p className="text-sm font-bold text-[var(--color-text-sub)]">관심상품을 불러오는 중입니다</p>
        </div>
      ) : wishedProducts.length > 0 ? (
        <section className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          {wishedProducts.map((product) => (
            <ProductCard
              key={product.productId}
              product={product}
              onWishClick={handleRemoveWish}
              isWishLoading={removingProductId === product.productId}
            />
          ))}
        </section>
      ) : (
        <EmptyState
          icon={Heart}
          title="찜한 상품이 없습니다"
          description="마음에 드는 상품을 발견하면 찜 목록에 담아보세요."
          action={
            <Link
              to={routePaths.home}
              className="theme-primary-button inline-flex rounded-2xl px-5 py-3 text-sm font-black transition"
            >
              상품 보러가기
            </Link>
          }
        />
      )}
    </div>
  );
}
