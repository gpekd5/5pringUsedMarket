import { Lock, Mail } from 'lucide-react';
import { Link } from 'react-router-dom';
import routePaths from '../routes/routePaths.js';

export default function LoginPage() {
  return (
    <div className="mx-auto max-w-md">
      <div className="theme-card rounded-[32px] p-6">
        <p className="text-sm font-extrabold uppercase tracking-wide text-[var(--color-primary)]">Login</p>
        <h1 className="mt-2 text-3xl font-black tracking-tight">다시 만나서 반가워요</h1>
        <p className="mt-2 text-sm text-[var(--color-text-sub)]">내 동네의 새로운 거래 소식을 확인해 보세요.</p>

        <form className="mt-6 space-y-4">
          <label className="block">
            <span className="mb-1 block text-sm font-bold text-[var(--color-text-main)]">이메일</span>
            <span className="theme-input flex items-center gap-2 rounded-2xl px-4 py-3">
              <Mail size={18} className="text-[var(--color-primary)]" />
              <input className="w-full bg-transparent outline-none" placeholder="buyer@example.com" />
            </span>
          </label>
          <label className="block">
            <span className="mb-1 block text-sm font-bold text-[var(--color-text-main)]">비밀번호</span>
            <span className="theme-input flex items-center gap-2 rounded-2xl px-4 py-3">
              <Lock size={18} className="text-[var(--color-primary)]" />
              <input className="w-full bg-transparent outline-none" placeholder="비밀번호 입력" type="password" />
            </span>
          </label>
          <button className="theme-primary-button w-full rounded-2xl px-4 py-3 font-black transition">
            로그인
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
