import { AlertCircle, CalendarDays, CheckCircle2, Clock, Loader2, Ticket, WalletCards } from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getAccessToken } from '../api/authStorage.js';
import {
  getCouponApiErrorMessage,
  getCoupons,
  getMyCoupons,
  issueCoupon,
  useCoupon,
} from '../api/couponApi.js';
import EmptyState from '../components/EmptyState.jsx';
import PageHeader from '../components/PageHeader.jsx';
import routePaths from '../routes/routePaths.js';

const PAGE_SIZE = 20;
const ORANGE = '#FF6F0F';

function formatDateTime(value) {
  if (!value) {
    return '-';
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return '-';
  }

  return date.toLocaleString('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
}

function getCouponState(coupon) {
  const now = Date.now();
  const startTime = coupon.eventStartAt ? new Date(coupon.eventStartAt).getTime() : null;
  const endTime = coupon.eventEndAt ? new Date(coupon.eventEndAt).getTime() : null;

  if (coupon.remainQty <= 0) {
    return { label: '소진 완료', tone: 'soldout' };
  }

  if (startTime && now < startTime) {
    return { label: '오픈 예정', tone: 'pending' };
  }

  if (endTime && now > endTime) {
    return { label: '이벤트 종료', tone: 'ended' };
  }

  return { label: '발급 가능', tone: 'active' };
}

function getStateClassName(tone) {
  if (tone === 'active') {
    return 'bg-orange-50 text-orange-600 ring-orange-100';
  }

  if (tone === 'pending') {
    return 'bg-blue-50 text-blue-600 ring-blue-100';
  }

  return 'bg-slate-100 text-slate-500 ring-slate-200';
}

export default function CouponsPage() {
  const navigate = useNavigate();
  const [coupons, setCoupons] = useState([]);
  const [myCoupons, setMyCoupons] = useState([]);
  const [issuedCouponIds, setIssuedCouponIds] = useState(new Set());
  const [pageInfo, setPageInfo] = useState({
    page: 0,
    size: PAGE_SIZE,
    totalElements: 0,
    totalPages: 0,
  });
  const [isLoading, setIsLoading] = useState(true);
  const [isLoadingMore, setIsLoadingMore] = useState(false);
  const [issuingCouponId, setIssuingCouponId] = useState(null);
  const [usingCouponId, setUsingCouponId] = useState(null);
  const [message, setMessage] = useState(null);

  const hasMore = pageInfo.page + 1 < pageInfo.totalPages;

  const summary = useMemo(() => {
    const totalQty = coupons.reduce((sum, coupon) => sum + coupon.totalQty, 0);
    const remainQty = coupons.reduce((sum, coupon) => sum + coupon.remainQty, 0);

    return {
      issuedCount: issuedCouponIds.size,
      totalQty,
      remainQty,
    };
  }, [coupons, issuedCouponIds]);

  const loadIssuedCoupons = async () => {
    if (!getAccessToken()) {
      setIssuedCouponIds(new Set());
      setMyCoupons([]);
      return;
    }

    try {
      const myCouponPage = await getMyCoupons({ page: 0, size: 100 });
      setMyCoupons(myCouponPage.content);
      setIssuedCouponIds(new Set(myCouponPage.content.map((coupon) => String(coupon.couponId))));
    } catch {
      setMyCoupons([]);
      setIssuedCouponIds(new Set());
    }
  };

  const loadCoupons = async ({ page = 0, append = false } = {}) => {
    if (append) {
      setIsLoadingMore(true);
    } else {
      setIsLoading(true);
    }

    setMessage(null);

    try {
      const nextPage = await getCoupons({ page, size: PAGE_SIZE });
      setCoupons((prevCoupons) => (append ? [...prevCoupons, ...nextPage.content] : nextPage.content));
      setPageInfo(nextPage);
    } catch (error) {
      setMessage({
        type: 'error',
        text: getCouponApiErrorMessage(error, '쿠폰 목록을 불러오지 못했습니다.'),
      });

      if (!append) {
        setCoupons([]);
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
    loadCoupons();
    loadIssuedCoupons();
  }, []);

  const handleIssueCoupon = async (coupon) => {
    if (!getAccessToken()) {
      navigate(routePaths.login, {
        state: { message: '쿠폰 발급은 로그인 후 이용할 수 있습니다.' },
      });
      return;
    }

    setIssuingCouponId(coupon.couponId);
    setMessage(null);

    try {
      const issuedCoupon = await issueCoupon(coupon.couponId);
      setIssuedCouponIds((prevIds) => new Set(prevIds).add(String(coupon.couponId)));
      await loadIssuedCoupons();
      setCoupons((prevCoupons) =>
        prevCoupons.map((prevCoupon) =>
          prevCoupon.couponId === coupon.couponId
            ? { ...prevCoupon, remainQty: Math.max(0, prevCoupon.remainQty - 1) }
            : prevCoupon,
        ),
      );
      setMessage({
        type: 'success',
        text: `${issuedCoupon.couponName || coupon.name} 쿠폰이 발급되었습니다.`,
      });
    } catch (error) {
      const errorCode = error?.response?.data?.code;

      if (errorCode === 'COUPON_ALREADY_ISSUED') {
        setIssuedCouponIds((prevIds) => new Set(prevIds).add(String(coupon.couponId)));
      }

      setMessage({
        type: 'error',
        text: getCouponApiErrorMessage(error, '쿠폰 발급에 실패했습니다.'),
      });
    } finally {
      setIssuingCouponId(null);
    }
  };

  const handleUseCoupon = async (coupon) => {
    setUsingCouponId(coupon.userCouponId);
    setMessage(null);

    try {
      await useCoupon(coupon.userCouponId);
      const usedAt = new Date().toISOString();
      setMyCoupons((prevCoupons) =>
        prevCoupons.map((prevCoupon) =>
          prevCoupon.userCouponId === coupon.userCouponId ? { ...prevCoupon, usedAt } : prevCoupon,
        ),
      );
      setMessage({
        type: 'success',
        text: `${coupon.couponName} 쿠폰이 사용 처리되었습니다.`,
      });
    } catch (error) {
      setMessage({
        type: 'error',
        text: getCouponApiErrorMessage(error, '쿠폰 사용 처리에 실패했습니다.'),
      });
    } finally {
      setUsingCouponId(null);
    }
  };

  return (
    <div>
      <PageHeader
        eyebrow="Coupons"
        title="선착순 쿠폰"
        description="상품을 등록한 회원에게 제공되는 외부 제휴 쿠폰을 확인하고 빠르게 받아보세요."
        action={
          <div className="grid grid-cols-3 gap-2 text-center text-xs font-black sm:min-w-[320px]">
            <div className="rounded-2xl bg-slate-50 px-3 py-2">
              <p className="text-[var(--color-text-sub)]">보유</p>
              <p className="mt-1 text-lg text-[var(--color-text-main)]">{summary.issuedCount}</p>
            </div>
            <div className="rounded-2xl bg-slate-50 px-3 py-2">
              <p className="text-[var(--color-text-sub)]">총 수량</p>
              <p className="mt-1 text-lg text-[var(--color-text-main)]">{summary.totalQty.toLocaleString()}</p>
            </div>
            <div className="rounded-2xl bg-slate-50 px-3 py-2">
              <p className="text-[var(--color-text-sub)]">잔여</p>
              <p className="mt-1 text-lg text-[var(--color-text-main)]">{summary.remainQty.toLocaleString()}</p>
            </div>
          </div>
        }
      />

      <div className="mb-5 rounded-2xl border border-emerald-100 bg-emerald-50 px-4 py-3 text-sm font-bold text-emerald-700">
        상품을 1개 이상 등록한 회원만 쿠폰을 발급받을 수 있어요.
      </div>

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

      {isLoading ? (
        <div className="theme-card flex min-h-64 flex-col items-center justify-center gap-4 rounded-[28px] p-8 text-center">
          <Loader2 size={34} className="animate-spin text-[var(--color-primary)]" />
          <p className="text-sm font-bold text-[var(--color-text-sub)]">쿠폰을 불러오는 중입니다</p>
        </div>
      ) : coupons.length > 0 ? (
        <>
          <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            {coupons.map((coupon) => {
              const issuedQty = Math.max(0, coupon.totalQty - coupon.remainQty);
              const issuedPercent = coupon.totalQty > 0 ? Math.min(100, Math.round((issuedQty / coupon.totalQty) * 100)) : 0;
              const isIssued = issuedCouponIds.has(String(coupon.couponId));
              const state = getCouponState(coupon);
              const isDisabled = isIssued || state.tone !== 'active' || issuingCouponId === coupon.couponId;

              return (
                <article
                  key={coupon.couponId}
                  className="relative overflow-hidden rounded-[28px] border border-[var(--color-border)] bg-white p-5 shadow-[0_12px_32px_rgba(15,23,42,0.06)]"
                >
                  <span className="absolute -left-4 top-1/2 h-8 w-8 -translate-y-1/2 rounded-full bg-[var(--color-background)] ring-1 ring-[var(--color-border)]" />
                  <span className="absolute -right-4 top-1/2 h-8 w-8 -translate-y-1/2 rounded-full bg-[var(--color-background)] ring-1 ring-[var(--color-border)]" />

                  <div className="mb-5 flex items-start justify-between gap-4">
                    <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-orange-50 text-orange-600">
                      <Ticket size={27} />
                    </div>
                    <span className={`rounded-full px-3 py-1 text-xs font-black ring-1 ${getStateClassName(state.tone)}`}>
                      {isIssued ? '발급 완료' : state.label}
                    </span>
                  </div>

                  <h2 className="line-clamp-2 min-h-14 text-xl font-black leading-tight text-[var(--color-text-main)]">
                    {coupon.name}
                  </h2>

                  <div className="my-5 border-t border-dashed border-slate-200" />

                  <div className="space-y-3 text-sm">
                    <div className="flex items-center justify-between gap-3">
                      <span className="flex items-center gap-1.5 font-bold text-[var(--color-text-sub)]">
                        <WalletCards size={16} />
                        수량
                      </span>
                      <span className="font-black text-[var(--color-text-main)]">
                        {issuedQty.toLocaleString()} / {coupon.totalQty.toLocaleString()} 발급
                      </span>
                    </div>
                    <div className="h-2 overflow-hidden rounded-full bg-slate-100">
                      <div
                        className="h-full rounded-full bg-orange-500"
                        style={{ width: `${issuedPercent}%` }}
                      />
                    </div>
                    <div className="flex items-start justify-between gap-3">
                      <span className="flex shrink-0 items-center gap-1.5 font-bold text-[var(--color-text-sub)]">
                        <CalendarDays size={16} />
                        이벤트
                      </span>
                      <span className="text-right font-bold text-[var(--color-text-main)]">
                        {formatDateTime(coupon.eventStartAt)}
                        <br />
                        ~ {formatDateTime(coupon.eventEndAt)}
                      </span>
                    </div>
                    <div className="flex items-center justify-between gap-3">
                      <span className="flex items-center gap-1.5 font-bold text-[var(--color-text-sub)]">
                        <Clock size={16} />
                        만료일
                      </span>
                      <span className="font-bold text-[var(--color-text-main)]">{formatDateTime(coupon.expireAt)}</span>
                    </div>
                  </div>

                  <button
                    type="button"
                    onClick={() => handleIssueCoupon(coupon)}
                    disabled={isDisabled}
                    className="mt-6 flex w-full items-center justify-center gap-2 rounded-2xl px-4 py-3 font-black text-white shadow-[0_10px_22px_rgba(255,111,15,0.24)] transition disabled:cursor-not-allowed disabled:bg-slate-300 disabled:shadow-none"
                    style={{ background: isDisabled ? undefined : ORANGE }}
                  >
                    {issuingCouponId === coupon.couponId ? (
                      <Loader2 size={19} className="animate-spin" />
                    ) : isIssued ? (
                      <CheckCircle2 size={19} />
                    ) : (
                      <Ticket size={19} />
                    )}
                    {issuingCouponId === coupon.couponId ? '발급 중' : isIssued ? '이미 발급받음' : '쿠폰 받기'}
                  </button>
                </article>
              );
            })}
          </section>

          {hasMore && (
            <div className="mt-6 flex justify-center">
              <button
                type="button"
                onClick={() => loadCoupons({ page: pageInfo.page + 1, append: true })}
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
          icon={Ticket}
          title="진행 중인 쿠폰이 없습니다"
          description="새로운 쿠폰 이벤트가 등록되면 이곳에서 확인할 수 있습니다."
        />
      )}

      <section className="mt-8">
        <PageHeader
          eyebrow="My Coupons"
          title="내 쿠폰함"
          description="발급받은 쿠폰의 코드, 만료일, 사용 상태를 확인하세요."
          className="mb-4"
        />

        {!getAccessToken() ? (
          <div className="theme-card rounded-[28px] p-6 text-sm font-bold text-[var(--color-text-sub)]">
            로그인하면 발급받은 쿠폰을 확인할 수 있습니다.
          </div>
        ) : myCoupons.length > 0 ? (
          <div className="grid gap-3 md:grid-cols-2">
            {myCoupons.map((coupon) => {
              const isUsed = Boolean(coupon.usedAt);
              const isUsing = usingCouponId === coupon.userCouponId;

              return (
                <article
                  key={coupon.userCouponId}
                  className="rounded-[24px] border border-[var(--color-border)] bg-white p-4 shadow-[0_10px_28px_rgba(15,23,42,0.05)]"
                >
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <h3 className="font-black text-[var(--color-text-main)]">{coupon.couponName}</h3>
                      <p className="mt-1 font-mono text-sm font-black text-orange-600">{coupon.code}</p>
                    </div>
                    <span
                      className={[
                        'rounded-full px-3 py-1 text-xs font-black ring-1',
                        isUsed ? 'bg-slate-100 text-slate-500 ring-slate-200' : 'bg-orange-50 text-orange-600 ring-orange-100',
                      ].join(' ')}
                    >
                      {isUsed ? '사용 완료' : '사용 가능'}
                    </span>
                  </div>
                  <div className="mt-4 grid gap-2 text-xs font-bold text-[var(--color-text-sub)]">
                    <p>발급일 {formatDateTime(coupon.issuedAt)}</p>
                    <p>만료일 {formatDateTime(coupon.expireAt)}</p>
                    {isUsed && <p>사용일 {formatDateTime(coupon.usedAt)}</p>}
                  </div>
                  <button
                    type="button"
                    onClick={() => handleUseCoupon(coupon)}
                    disabled={isUsed || isUsing}
                    className="mt-4 inline-flex w-full items-center justify-center gap-2 rounded-2xl bg-slate-900 px-4 py-3 text-sm font-black text-white transition hover:bg-slate-700 disabled:cursor-not-allowed disabled:bg-slate-300"
                  >
                    {isUsing ? <Loader2 size={17} className="animate-spin" /> : <CheckCircle2 size={17} />}
                    {isUsed ? '이미 사용됨' : isUsing ? '처리 중' : '사용 처리'}
                  </button>
                </article>
              );
            })}
          </div>
        ) : (
          <EmptyState icon={Ticket} title="보유 쿠폰이 없습니다" description="위 쿠폰 목록에서 쿠폰을 발급받아보세요." />
        )}
      </section>
    </div>
  );
}
