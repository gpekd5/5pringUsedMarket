export default function PageHeader({ eyebrow, title, description, action, className = '' }) {
  return (
    <div className={`theme-card mb-6 flex flex-col gap-4 rounded-[28px] p-5 sm:flex-row sm:items-end sm:justify-between ${className}`}>
      <div>
        {eyebrow && (
          <p className="mb-2 text-sm font-extrabold uppercase tracking-wide text-[var(--color-primary)]">
            {eyebrow}
          </p>
        )}
        <h1 className="text-2xl font-black tracking-tight text-[var(--color-text-main)] sm:text-3xl">{title}</h1>
        {description && <p className="mt-2 max-w-2xl text-sm leading-6 text-[var(--color-text-sub)]">{description}</p>}
      </div>
      {action}
    </div>
  );
}
