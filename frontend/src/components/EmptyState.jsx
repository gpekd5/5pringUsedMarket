import { PackageOpen } from 'lucide-react';

export default function EmptyState({
  title = '표시할 내용이 없습니다',
  description = '조건을 바꾸거나 잠시 후 다시 확인해 주세요.',
  action,
  icon: Icon = PackageOpen,
}) {
  return (
    <div className="theme-card flex min-h-64 flex-col items-center justify-center rounded-[28px] p-8 text-center">
      <div className="theme-soft-surface mb-4 flex h-14 w-14 items-center justify-center rounded-3xl">
        <Icon size={28} />
      </div>
      <h2 className="text-xl font-black text-[var(--color-text-main)]">{title}</h2>
      <p className="mt-2 max-w-sm text-sm leading-6 text-[var(--color-text-sub)]">{description}</p>
      {action && <div className="mt-5">{action}</div>}
    </div>
  );
}
