import { createBrowserRouter, RouterProvider } from 'react-router-dom';
import App from './App.jsx';
import AdminCsChatPage from './pages/AdminCsChatPage.jsx';
import ChatRoomDetailPage from './pages/ChatRoomDetailPage.jsx';
import ChatRoomListPage from './pages/ChatRoomListPage.jsx';
import CouponsPage from './pages/CouponsPage.jsx';
import HomePage from './pages/HomePage.jsx';
import LoginPage from './pages/LoginPage.jsx';
import MyPage from './pages/MyPage.jsx';
import NotFoundPage from './pages/NotFoundPage.jsx';
import ProductCreatePage from './pages/ProductCreatePage.jsx';
import ProductDetailPage from './pages/ProductDetailPage.jsx';
import ProductEditPage from './pages/ProductEditPage.jsx';
import SearchResultsPage from './pages/SearchResultsPage.jsx';
import SignupPage from './pages/SignupPage.jsx';
import WishListPage from './pages/WishListPage.jsx';

const router = createBrowserRouter([
  {
    path: '/',
    element: <App />,
    errorElement: <NotFoundPage />,
    children: [
      { index: true, element: <HomePage /> },
      { path: 'search', element: <SearchResultsPage /> },
      { path: 'login', element: <LoginPage /> },
      { path: 'signup', element: <SignupPage /> },
      { path: 'mypage', element: <MyPage /> },
      { path: 'products/new', element: <ProductCreatePage /> },
      { path: 'products/:productId/edit', element: <ProductEditPage /> },
      { path: 'products/:productId', element: <ProductDetailPage /> },
      { path: 'wishes', element: <WishListPage /> },
      { path: 'chats', element: <ChatRoomListPage /> },
      { path: 'chats/:chatRoomId', element: <ChatRoomDetailPage /> },
      { path: 'coupons', element: <CouponsPage /> },
      { path: 'admin/chats', element: <AdminCsChatPage /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
]);

export default function AppRouter() {
  return <RouterProvider router={router} />;
}
