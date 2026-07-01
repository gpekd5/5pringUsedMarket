import { Outlet } from 'react-router-dom';
import Header from './Header.jsx';

export default function Layout() {
  return (
    <div className="min-h-screen bg-[var(--color-background)] text-[var(--color-text-main)]">
      <Header />
      <main className="mx-auto w-full max-w-[1320px] px-4 py-6 pb-24 md:py-8 lg:pb-10">
        <Outlet />
      </main>
    </div>
  );
}
