export default function Loading({ label = '불러오는 중입니다' }) {
  return (
    <div className="theme-card flex min-h-48 flex-col items-center justify-center gap-4 rounded-[28px] p-8 text-center">
      <span className="h-10 w-10 animate-spin rounded-full border-4 border-[var(--color-primary-soft)] border-t-[var(--color-primary)]" />
      <p className="text-sm font-bold text-[var(--color-text-sub)]">{label}</p>
    </div>
  );
}
