import { Home } from 'lucide-react';
import { Link } from 'react-router-dom';
import EmptyState from '../components/EmptyState.jsx';
import routePaths from '../routes/routePaths.js';

export default function NotFoundPage() {
  return (
    <div className="flex min-h-[60vh] items-center justify-center px-4">
      <EmptyState
        icon={Home}
        title="페이지를 찾을 수 없어요"
        description="주소를 다시 확인하거나 홈으로 이동해 주세요."
        action={
          <Link
            to={routePaths.home}
            className="theme-primary-button inline-flex rounded-2xl px-5 py-3 font-black transition"
          >
            홈으로 이동
          </Link>
        }
      />
    </div>
  );
}
