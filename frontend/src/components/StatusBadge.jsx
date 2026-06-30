const statusMap = {
  ON_SALE: {
    label: '판매중',
    className: 'bg-[var(--color-primary-soft)] text-[var(--color-primary)] ring-[color-mix(in_srgb,var(--color-primary)_24%,transparent)]',
  },
  RESERVED: {
    label: '예약중',
    className: 'bg-[var(--color-status-reserved-bg)] text-[var(--color-status-reserved-text)] ring-[var(--color-status-reserved-ring)]',
  },
  SOLD: {
    label: '판매완료',
    className: 'bg-[var(--color-status-sold-bg)] text-[var(--color-status-sold-text)] ring-[var(--color-status-sold-ring)]',
  },
};

export default function StatusBadge({ status = 'ON_SALE' }) {
  const meta = statusMap[status] ?? statusMap.ON_SALE;

  return (
    <span className={`inline-flex items-center rounded-full px-2.5 py-1 text-xs font-black ring-1 ${meta.className}`}>
      {meta.label}
    </span>
  );
}
