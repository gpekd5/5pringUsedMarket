import { Heart, Home, LogOut, MessageCircle, Search, ShieldCheck, ShoppingBag, Ticket, UserRound } from 'lucide-react';
import { useEffect, useState } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { fetchMe, logout } from '../api/authApi.js';
import { AUTH_CHANGE_EVENT, clearAuthStorage, getAccessToken, getStoredAuthUser } from '../api/authStorage.js';
import routePaths from '../routes/routePaths.js';

const navItems = [
  { to: routePaths.home, label: '홈', icon: Home },
  { to: routePaths.wishes, label: '관심상품', icon: Heart },
  { to: routePaths.chats, label: '채팅', icon: MessageCircle },
  { to: routePaths.coupons, label: '쿠폰', icon: Ticket },
  { to: routePaths.adminChats, label: '관리자', icon: ShieldCheck },
];

function navClassName(isActive) {
  return [
    'flex items-center gap-1.5 rounded-full px-3 py-2 text-sm font-bold transition',
    isActive
      ? 'bg-[var(--color-primary-soft)] text-[var(--color-primary)]'
      : 'text-[var(--color-text-sub)] hover:bg-[var(--color-primary-soft)] hover:text-[var(--color-primary)]',
  ].join(' ');
}

function getAuthSnapshot() {
  const accessToken = getAccessToken();

  return {
    isAuthenticated: Boolean(accessToken),
    user: accessToken ? getStoredAuthUser() : null,
  };
}

export default function Header() {
  const navigate = useNavigate();
  const [auth, setAuth] = useState(getAuthSnapshot);
  const [isLoggingOut, setIsLoggingOut] = useState(false);

  useEffect(() => {
    const syncAuth = () => {
      setAuth(getAuthSnapshot());
    };

    window.addEventListener(AUTH_CHANGE_EVENT, syncAuth);
    window.addEventListener('storage', syncAuth);

    return () => {
      window.removeEventListener(AUTH_CHANGE_EVENT, syncAuth);
      window.removeEventListener('storage', syncAuth);
    };
  }, []);

  useEffect(() => {
    let isActive = true;

    if (!auth.isAuthenticated || auth.user) {
      return () => {
        isActive = false;
      };
    }

    fetchMe()
      .then((user) => {
        if (isActive) {
          setAuth({ isAuthenticated: true, user });
        }
      })
      .catch((error) => {
        if ([401, 403].includes(error?.response?.status)) {
          clearAuthStorage();
        }
      });

    return () => {
      isActive = false;
    };
  }, [auth.isAuthenticated, auth.user]);

  const handleLogout = async () => {
    setIsLoggingOut(true);

    try {
      await logout();
      setAuth(getAuthSnapshot());
      navigate(routePaths.home);
    } finally {
      setIsLoggingOut(false);
    }
  };

  return (
    <>
      <header className="sticky top-0 z-30 border-b border-[var(--color-border)] bg-white/95 backdrop-blur">
        <div className="mx-auto flex max-w-[1320px] items-center gap-5 px-4 py-4">
          <NavLink to={routePaths.home} className="flex min-w-fit items-center gap-2">
            <span className="flex h-11 w-11 items-center justify-center rounded-[18px] bg-[var(--color-primary)] text-[var(--color-on-primary)] shadow-sm">
              <ShoppingBag size={23} />
            </span>
            <div className="leading-tight">
              <p className="text-xl font-black tracking-tight text-[var(--color-text-main)]">5pring Market</p>
              <p className="text-xs font-bold text-[var(--color-primary)]">우리 동네 중고거래</p>
            </div>
          </NavLink>

          <div className="hidden max-w-[380px] flex-1 items-center gap-3 rounded-[22px] bg-white px-4 py-3 text-sm text-[var(--color-text-sub)] shadow-sm ring-1 ring-[var(--color-border)] md:flex">
            <Search size={20} className="text-[var(--color-primary)]" />
            <span className="font-semibold">상품명, 지역, 카테고리 검색</span>
          </div>

          <nav className="ml-auto hidden items-center gap-1 lg:flex">
            {navItems.map(({ to, label, icon: Icon }) => (
              <NavLink key={to} to={to} className={({ isActive }) => navClassName(isActive)}>
                <Icon size={17} />
                {label}
              </NavLink>
            ))}
          </nav>

          {auth.isAuthenticated ? (
            <div className="hidden items-center gap-2 sm:flex">
              <span className="max-w-[132px] truncate rounded-full border border-[var(--color-border)] bg-white px-3 py-2 text-sm font-black text-[var(--color-text-main)]">
                {auth.user?.nickname || '회원'}님
              </span>
              <button
                type="button"
                onClick={handleLogout}
                disabled={isLoggingOut}
                className="flex items-center gap-2 rounded-[22px] bg-[var(--color-text-main)] px-4 py-3 text-sm font-black text-white shadow-sm transition hover:bg-slate-700 disabled:cursor-not-allowed disabled:opacity-60"
              >
                <LogOut size={17} />
                {isLoggingOut ? '처리 중' : '로그아웃'}
              </button>
            </div>
          ) : (
            <NavLink
              to={routePaths.login}
              className="hidden items-center gap-2 rounded-[22px] bg-[var(--color-primary)] px-5 py-3 text-sm font-black text-[var(--color-on-primary)] shadow-sm transition hover:bg-[var(--color-primary-dark)] sm:flex"
            >
              <UserRound size={17} />
              로그인
            </NavLink>
          )}
        </div>
      </header>

      <nav className="fixed inset-x-0 bottom-0 z-30 border-t border-[var(--color-border)] bg-white/95 px-3 py-2 shadow-[0_-8px_24px_rgba(15,23,42,0.06)] backdrop-blur lg:hidden">
        <div className="mx-auto grid max-w-md grid-cols-5 gap-1">
          {navItems.map(({ to, label, icon: Icon }) => (
            <NavLink
              key={to}
              to={to}
              className={({ isActive }) =>
                [
                  'flex flex-col items-center gap-1 rounded-2xl px-2 py-1.5 text-xs font-bold transition',
                  isActive ? 'bg-[var(--color-primary-soft)] text-[var(--color-primary)]' : 'text-[var(--color-text-sub)]',
                ].join(' ')
              }
            >
              <Icon size={19} />
              <span className="truncate">{label}</span>
            </NavLink>
          ))}
        </div>
      </nav>
    </>
  );
}
