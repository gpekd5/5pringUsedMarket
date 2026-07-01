import { AlertCircle, Loader2, Lock, Mail, ShieldCheck, UserPlus, UserRound } from 'lucide-react';
import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { getApiErrorMessage, signup } from '../api/authApi.js';
import routePaths from '../routes/routePaths.js';

export default function SignupPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState({ nickname: '', email: '', password: '' });
  const [errorMessage, setErrorMessage] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const handleChange = (event) => {
    const { name, value } = event.target;
    setForm((prevForm) => ({ ...prevForm, [name]: value }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setErrorMessage('');

    if (!form.nickname.trim() || !form.email.trim() || !form.password) {
      setErrorMessage('닉네임, 이메일, 비밀번호를 모두 입력해주세요.');
      return;
    }

    setIsLoading(true);

    try {
      await signup(form);
      navigate(routePaths.login, {
        state: { message: '회원가입이 완료되었습니다. 로그인해주세요.' },
      });
    } catch (error) {
      setErrorMessage(getApiErrorMessage(error, '회원가입에 실패했습니다.'));
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="mx-auto grid max-w-5xl gap-6 lg:grid-cols-[0.95fr_1.05fr] lg:items-center">
      <section className="hidden rounded-[32px] bg-white p-8 shadow-[0_12px_32px_rgba(15,23,42,0.06)] ring-1 ring-[var(--color-border)] lg:block">
        <span className="flex h-14 w-14 items-center justify-center rounded-2xl bg-[var(--color-primary-soft)] text-[var(--color-primary)]">
          <ShieldCheck size={28} />
        </span>
        <p className="mt-8 text-sm font-extrabold uppercase tracking-wide text-[var(--color-primary)]">Join 5pring</p>
        <h1 className="mt-3 text-4xl font-black leading-tight tracking-tight">
          가까운 이웃과
          <br />
          안전하게 거래해요
        </h1>
        <p className="mt-4 max-w-md text-sm leading-6 text-[var(--color-text-sub)]">
          이메일, 비밀번호, 닉네임만 입력하면 5pring Market을 바로 시작할 수 있습니다.
        </p>
      </section>

      <div className="theme-card rounded-[32px] p-6 sm:p-8">
        <p className="text-sm font-extrabold uppercase tracking-wide text-[var(--color-primary)]">Signup</p>
        <h1 className="mt-2 text-3xl font-black tracking-tight">동네 거래 시작하기</h1>
        <p className="mt-2 text-sm text-[var(--color-text-sub)]">간단한 정보로 가까운 이웃과 거래를 시작해 보세요.</p>

        {errorMessage && (
          <div className="mt-5 flex items-center gap-2 rounded-2xl border border-red-100 bg-red-50 px-4 py-3 text-sm font-bold text-red-600">
            <AlertCircle size={17} />
            {errorMessage}
          </div>
        )}

        <form className="mt-6 space-y-4" onSubmit={handleSubmit}>
          <label className="block">
            <span className="mb-1 block text-sm font-bold text-[var(--color-text-main)]">닉네임</span>
            <span className="theme-input flex items-center gap-2 rounded-2xl px-4 py-3">
              <UserRound size={18} className="text-[var(--color-primary)]" />
              <input
                className="w-full bg-transparent outline-none"
                name="nickname"
                value={form.nickname}
                onChange={handleChange}
                placeholder="봄마켓"
                autoComplete="nickname"
              />
            </span>
          </label>
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
                autoComplete="new-password"
              />
            </span>
          </label>
          <button
            type="submit"
            disabled={isLoading}
            className="theme-primary-button flex w-full items-center justify-center gap-2 rounded-2xl px-4 py-3 font-black transition disabled:cursor-not-allowed disabled:opacity-70"
          >
            {isLoading ? <Loader2 size={19} className="animate-spin" /> : <UserPlus size={19} />}
            {isLoading ? '가입 중' : '회원가입'}
          </button>
        </form>

        <p className="mt-5 text-center text-sm text-[var(--color-text-sub)]">
          이미 계정이 있나요?{' '}
          <Link to={routePaths.login} className="font-bold text-[var(--color-primary)]">
            로그인
          </Link>
        </p>
      </div>
    </div>
  );
}
