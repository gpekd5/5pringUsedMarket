import { AlertCircle, Loader2, Lock, Mail, ShoppingBag } from 'lucide-react';
import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { fetchMe, getApiErrorMessage, login } from '../api/authApi.js';
import routePaths from '../routes/routePaths.js';

export default function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const [form, setForm] = useState({ email: '', password: '' });
  const [errorMessage, setErrorMessage] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const successMessage = location.state?.message;

  const handleChange = (event) => {
    const { name, value } = event.target;
    setForm((prevForm) => ({ ...prevForm, [name]: value }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setErrorMessage('');

    if (!form.email.trim() || !form.password) {
      setErrorMessage('이메일과 비밀번호를 입력해주세요.');
      return;
    }

    setIsLoading(true);

    try {
      await login(form);

      try {
        await fetchMe();
      } catch {
        // 토큰 저장이 성공했다면 내 정보 조회 실패만으로 로그인 흐름을 막지 않는다.
      }

      navigate(routePaths.home);
    } catch (error) {
      setErrorMessage(getApiErrorMessage(error, '로그인에 실패했습니다.'));
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="mx-auto grid max-w-5xl gap-6 lg:grid-cols-[0.95fr_1.05fr] lg:items-center">
      <section className="hidden rounded-[32px] bg-white p-8 shadow-[0_12px_32px_rgba(15,23,42,0.06)] ring-1 ring-[var(--color-border)] lg:block">
        <span className="flex h-14 w-14 items-center justify-center rounded-2xl bg-[var(--color-primary)] text-white">
          <ShoppingBag size={28} />
        </span>
        <p className="mt-8 text-sm font-extrabold uppercase tracking-wide text-[var(--color-primary)]">5pring Market</p>
        <h1 className="mt-3 text-4xl font-black leading-tight tracking-tight">
          오늘의 동네 거래를
          <br />
          바로 확인하세요
        </h1>
        <p className="mt-4 max-w-md text-sm leading-6 text-[var(--color-text-sub)]">
          관심상품, 채팅, 쿠폰까지 로그인 후 더 편하게 이용할 수 있습니다.
        </p>
      </section>

      <div className="theme-card rounded-[32px] p-6 sm:p-8">
        <p className="text-sm font-extrabold uppercase tracking-wide text-[var(--color-primary)]">Login</p>
        <h1 className="mt-2 text-3xl font-black tracking-tight">다시 만나서 반가워요</h1>
        <p className="mt-2 text-sm text-[var(--color-text-sub)]">내 동네의 새로운 거래 소식을 확인해 보세요.</p>

        {successMessage && (
          <div className="mt-5 rounded-2xl border border-emerald-100 bg-[var(--color-primary-soft)] px-4 py-3 text-sm font-bold text-[var(--color-primary-dark)]">
            {successMessage}
          </div>
        )}

        {errorMessage && (
          <div className="mt-5 flex items-center gap-2 rounded-2xl border border-red-100 bg-red-50 px-4 py-3 text-sm font-bold text-red-600">
            <AlertCircle size={17} />
            {errorMessage}
          </div>
        )}

        <form className="mt-6 space-y-4" onSubmit={handleSubmit}>
          <label className="block">
            <span className="mb-1 block text-sm font-bold text-[var(--color-text-main)]">이메일</span>
            <span className="theme-input flex items-center gap-2 rounded-2xl px-4 py-3">
              <Mail size={18} className="text-[var(--color-primary)]" />
              <input
                className="w-full bg-transparent outline-none"
                name="email"
                value={form.email}
                onChange={handleChange}
                placeholder="buyer@example.com"
                type="email"
                autoComplete="email"
              />
            </span>
          </label>
          <label className="block">
            <span className="mb-1 block text-sm font-bold text-[var(--color-text-main)]">비밀번호</span>
            <span className="theme-input flex items-center gap-2 rounded-2xl px-4 py-3">
              <Lock size={18} className="text-[var(--color-primary)]" />
              <input
                className="w-full bg-transparent outline-none"
                name="password"
                value={form.password}
                onChange={handleChange}
                placeholder="비밀번호 입력"
                type="password"
                autoComplete="current-password"
              />
            </span>
          </label>
          <button
            type="submit"
            disabled={isLoading}
            className="theme-primary-button flex w-full items-center justify-center gap-2 rounded-2xl px-4 py-3 font-black transition disabled:cursor-not-allowed disabled:opacity-70"
          >
            {isLoading && <Loader2 size={18} className="animate-spin" />}
            {isLoading ? '로그인 중' : '로그인'}
          </button>
        </form>

        <p className="mt-5 text-center text-sm text-[var(--color-text-sub)]">
          계정이 없나요?{' '}
          <Link to={routePaths.signup} className="font-bold text-[var(--color-primary)]">
            회원가입
          </Link>
        </p>
      </div>
    </div>
  );
}
