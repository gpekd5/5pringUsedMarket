import { Mail, UserPlus, UserRound } from 'lucide-react';
import { Link } from 'react-router-dom';
import routePaths from '../routes/routePaths.js';

export default function SignupPage() {
  return (
    <div className="mx-auto max-w-md">
      <div className="theme-card rounded-[32px] p-6">
        <p className="text-sm font-extrabold uppercase tracking-wide text-[var(--color-primary)]">Signup</p>
        <h1 className="mt-2 text-3xl font-black tracking-tight">동네 거래 시작하기</h1>
        <p className="mt-2 text-sm text-[var(--color-text-sub)]">간단한 정보로 가까운 이웃과 거래를 시작해 보세요.</p>

        <form className="mt-6 space-y-4">
          <label className="block">
            <span className="mb-1 block text-sm font-bold text-[var(--color-text-main)]">닉네임</span>
            <span className="theme-input flex items-center gap-2 rounded-2xl px-4 py-3">
              <UserRound size={18} className="text-[var(--color-primary)]" />
              <input className="w-full bg-transparent outline-none" placeholder="오렌지마켓" />
            </span>
          </label>
          <label className="block">
            <span className="mb-1 block text-sm font-bold text-[var(--color-text-main)]">이메일</span>
            <span className="theme-input flex items-center gap-2 rounded-2xl px-4 py-3">
              <Mail size={18} className="text-[var(--color-primary)]" />
              <input className="w-full bg-transparent outline-none" placeholder="buyer@example.com" />
            </span>
          </label>
          <button className="theme-primary-button flex w-full items-center justify-center gap-2 rounded-2xl px-4 py-3 font-black transition">
            <UserPlus size={19} />
            회원가입
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
