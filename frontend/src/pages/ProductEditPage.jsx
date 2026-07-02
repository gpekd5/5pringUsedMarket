import { AlertCircle, ImagePlus, Loader2, PackageCheck, X } from 'lucide-react';
import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { fetchMe } from '../api/authApi.js';
import { getAccessToken, getStoredAuthUser } from '../api/authStorage.js';
import { getProduct, getProductApiErrorMessage, updateProduct, uploadProductImage } from '../api/productApi.js';
import PageHeader from '../components/PageHeader.jsx';
import routePaths from '../routes/routePaths.js';

const categoryOptions = [
  { label: '디지털', value: 'DIGITAL' },
  { label: '가구', value: 'FURNITURE' },
  { label: '의류', value: 'CLOTHING' },
  { label: '도서', value: 'BOOK' },
  { label: '스포츠', value: 'SPORTS' },
  { label: '유아/아동', value: 'KIDS' },
  { label: '뷰티', value: 'BEAUTY' },
  { label: '식품', value: 'FOOD' },
  { label: '반려동물', value: 'PET' },
  { label: '기타', value: 'ETC' },
];

const initialForm = {
  title: '',
  price: '',
  category: 'DIGITAL',
  description: '',
};

export default function ProductEditPage() {
  const { productId } = useParams();
  const navigate = useNavigate();
  const [product, setProduct] = useState(null);
  const [form, setForm] = useState(initialForm);
  const [imageFiles, setImageFiles] = useState([]);
  const [imagePreviews, setImagePreviews] = useState([]);
  const [errorMessage, setErrorMessage] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    const nextPreviews = imageFiles.map((file) => ({
      file,
      url: URL.createObjectURL(file),
    }));

    setImagePreviews(nextPreviews);

    return () => {
      nextPreviews.forEach((preview) => URL.revokeObjectURL(preview.url));
    };
  }, [imageFiles]);

  useEffect(() => {
    let isActive = true;

    if (!getAccessToken()) {
      navigate(routePaths.login, {
        state: { message: '상품 수정은 로그인 후 이용할 수 있습니다.' },
      });
      return () => {
        isActive = false;
      };
    }

    async function loadEditableProduct() {
      setIsLoading(true);
      setErrorMessage('');

      try {
        const [productData, currentUser] = await Promise.all([
          getProduct(productId),
          getStoredAuthUser() ? Promise.resolve(getStoredAuthUser()) : fetchMe(),
        ]);

        if (!currentUser || String(currentUser.memberId) !== String(productData.sellerId)) {
          throw new Error('내가 등록한 상품만 수정할 수 있습니다.');
        }

        if (isActive) {
          setProduct(productData);
          setForm({
            title: productData.title || '',
            price: productData.price ?? '',
            category: productData.category || 'DIGITAL',
            description: productData.description || '',
          });
        }
      } catch (error) {
        if (isActive) {
          setErrorMessage(getProductApiErrorMessage(error, '상품 정보를 불러오지 못했습니다.'));
        }
      } finally {
        if (isActive) {
          setIsLoading(false);
        }
      }
    }

    loadEditableProduct();

    return () => {
      isActive = false;
    };
  }, [navigate, productId]);

  const handleChange = (event) => {
    const { name, value } = event.target;
    setForm((prevForm) => ({ ...prevForm, [name]: value }));
  };

  const handleImageChange = (event) => {
    const selectedFiles = Array.from(event.target.files || []);
    setImageFiles((prevFiles) => [...prevFiles, ...selectedFiles].slice(0, 5));
    event.target.value = '';
  };

  const handleRemoveImage = (targetFile) => {
    setImageFiles((prevFiles) => prevFiles.filter((file) => file !== targetFile));
  };

  const validateForm = () => {
    if (!form.title.trim()) {
      return '상품명을 입력해주세요.';
    }

    if (form.price === '' || Number(form.price) < 0) {
      return '가격은 0원 이상으로 입력해주세요.';
    }

    if (!form.category) {
      return '카테고리를 선택해주세요.';
    }

    return '';
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setErrorMessage('');

    const validationMessage = validateForm();
    if (validationMessage) {
      setErrorMessage(validationMessage);
      return;
    }

    setIsSubmitting(true);

    try {
      const uploadedImages = imageFiles.length > 0
        ? await Promise.all(imageFiles.map((file) => uploadProductImage(file)))
        : [];
      const imageKeys = uploadedImages.map((image) => image.imageKey).filter(Boolean);
      const updatedProduct = await updateProduct(productId, {
        ...form,
        ...(imageFiles.length > 0 ? { imageKeys } : {}),
      });

      navigate(routePaths.productDetail(updatedProduct.productId));
    } catch (error) {
      setErrorMessage(getProductApiErrorMessage(error, '상품 수정에 실패했습니다.'));
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isLoading) {
    return (
      <div className="theme-card flex min-h-80 flex-col items-center justify-center gap-4 rounded-[32px] p-8 text-center">
        <Loader2 size={36} className="animate-spin text-[var(--color-primary)]" />
        <p className="text-sm font-bold text-[var(--color-text-sub)]">상품 정보를 불러오는 중입니다</p>
      </div>
    );
  }

  if (errorMessage && !product) {
    return (
      <div className="theme-card flex min-h-80 flex-col items-center justify-center rounded-[32px] p-8 text-center">
        <div className="mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-red-50 text-red-500">
          <AlertCircle size={28} />
        </div>
        <h1 className="text-2xl font-black">상품을 수정할 수 없습니다</h1>
        <p className="mt-2 max-w-sm text-sm leading-6 text-[var(--color-text-sub)]">{errorMessage}</p>
        <Link
          to={routePaths.home}
          className="theme-primary-button mt-5 inline-flex rounded-2xl px-5 py-3 text-sm font-black transition"
        >
          홈으로 가기
        </Link>
      </div>
    );
  }

  return (
    <div>
      <PageHeader
        eyebrow="Edit"
        title="상품 수정"
        description="내가 등록한 상품의 사진과 정보를 최신 상태로 정리해보세요."
      />

      <form className="grid gap-6 lg:grid-cols-[0.9fr_1.1fr]" onSubmit={handleSubmit}>
        <section className="theme-card rounded-[32px] p-5">
          <div className="mb-4 flex items-center justify-between gap-3">
            <div>
              <h2 className="text-lg font-black">상품 이미지</h2>
              <p className="mt-1 text-sm text-[var(--color-text-sub)]">
                새 이미지를 선택하면 기존 이미지가 새 이미지로 교체됩니다.
              </p>
            </div>
            <span className="text-sm font-bold text-[var(--color-text-sub)]">{imageFiles.length}/5</span>
          </div>

          <label className="flex min-h-48 cursor-pointer flex-col items-center justify-center rounded-[24px] border border-dashed border-[var(--color-border-strong)] bg-slate-50 px-4 py-8 text-center transition hover:border-[var(--color-primary)] hover:bg-[var(--color-primary-soft)]">
            <ImagePlus size={32} className="text-[var(--color-primary)]" />
            <span className="mt-3 text-sm font-black text-[var(--color-text-main)]">새 이미지 선택</span>
            <span className="mt-1 text-xs font-bold text-[var(--color-text-sub)]">jpg, jpeg, png</span>
            <input
              className="hidden"
              type="file"
              accept="image/jpeg,image/png"
              multiple
              onChange={handleImageChange}
            />
          </label>

          {imagePreviews.length > 0 ? (
            <div className="mt-4">
              <p className="mb-2 text-xs font-black uppercase tracking-wide text-[var(--color-primary)]">새 이미지</p>
              <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
                {imagePreviews.map((preview) => (
                  <div key={`${preview.file.name}-${preview.file.lastModified}`} className="relative overflow-hidden rounded-2xl border border-[var(--color-border)] bg-white">
                    <img src={preview.url} alt={preview.file.name} className="aspect-square w-full object-cover" />
                    <button
                      type="button"
                      onClick={() => handleRemoveImage(preview.file)}
                      className="absolute right-2 top-2 flex h-8 w-8 items-center justify-center rounded-full bg-white/95 text-[var(--color-text-main)] shadow-sm"
                      aria-label="이미지 제거"
                    >
                      <X size={16} />
                    </button>
                  </div>
                ))}
              </div>
            </div>
          ) : product?.imageUrls?.length > 0 ? (
            <div className="mt-4">
              <p className="mb-2 text-xs font-black uppercase tracking-wide text-[var(--color-text-sub)]">현재 이미지</p>
              <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
                {product.imageUrls.map((imageUrl) => (
                  <div key={imageUrl} className="overflow-hidden rounded-2xl border border-[var(--color-border)] bg-white">
                    <img src={imageUrl} alt={product.title} className="aspect-square w-full object-cover" />
                  </div>
                ))}
              </div>
            </div>
          ) : (
            <p className="mt-4 rounded-2xl bg-slate-50 px-4 py-3 text-sm font-bold text-[var(--color-text-sub)]">
              현재 등록된 이미지가 없습니다.
            </p>
          )}
        </section>

        <section className="theme-card rounded-[32px] p-5">
          <h2 className="text-lg font-black">상품 정보</h2>
          <div className="mt-5 space-y-4">
            <label className="block">
              <span className="mb-1 block text-sm font-bold text-[var(--color-text-main)]">상품명</span>
              <input
                name="title"
                value={form.title}
                onChange={handleChange}
                maxLength={100}
                className="theme-input w-full rounded-2xl px-4 py-3 outline-none"
                placeholder="예: 아이폰 15 판매합니다"
              />
            </label>

            <div className="grid gap-4 sm:grid-cols-2">
              <label className="block">
                <span className="mb-1 block text-sm font-bold text-[var(--color-text-main)]">가격</span>
                <input
                  name="price"
                  value={form.price}
                  onChange={handleChange}
                  min="0"
                  type="number"
                  className="theme-input w-full rounded-2xl px-4 py-3 outline-none"
                  placeholder="0"
                />
              </label>

              <label className="block">
                <span className="mb-1 block text-sm font-bold text-[var(--color-text-main)]">카테고리</span>
                <select
                  name="category"
                  value={form.category}
                  onChange={handleChange}
                  className="theme-input w-full rounded-2xl px-4 py-3 outline-none"
                >
                  {categoryOptions.map((category) => (
                    <option key={category.value} value={category.value}>
                      {category.label}
                    </option>
                  ))}
                </select>
              </label>
            </div>

            <label className="block">
              <span className="mb-1 block text-sm font-bold text-[var(--color-text-main)]">설명</span>
              <textarea
                name="description"
                value={form.description}
                onChange={handleChange}
                className="theme-input min-h-40 w-full resize-y rounded-2xl px-4 py-3 leading-6 outline-none"
                placeholder="상태, 구매 시기, 거래 장소 등을 적어주세요."
              />
            </label>
          </div>

          {errorMessage && (
            <div className="mt-5 flex items-center gap-2 rounded-2xl border border-red-100 bg-red-50 px-4 py-3 text-sm font-bold text-red-600">
              <AlertCircle size={17} />
              {errorMessage}
            </div>
          )}

          <div className="mt-6 grid gap-3 sm:grid-cols-[1fr_1.5fr]">
            <Link
              to={routePaths.productDetail(productId)}
              className="theme-secondary-button flex items-center justify-center rounded-2xl px-4 py-3 font-black transition"
            >
              취소
            </Link>
            <button
              type="submit"
              disabled={isSubmitting}
              className="theme-primary-button flex items-center justify-center gap-2 rounded-2xl px-4 py-3 font-black transition disabled:cursor-not-allowed disabled:opacity-70"
            >
              {isSubmitting ? <Loader2 size={19} className="animate-spin" /> : <PackageCheck size={19} />}
              {isSubmitting ? '수정 중' : '수정 완료'}
            </button>
          </div>
        </section>
      </form>
    </div>
  );
}
