const routePaths = {
  home: '/',
  login: '/login',
  signup: '/signup',
  productNew: '/products/new',
  productDetail: (productId = ':productId') => `/products/${productId}`,
  productEdit: (productId = ':productId') => `/products/${productId}/edit`,
  wishes: '/wishes',
  myPage: '/mypage',
  chats: '/chats',
  chatRoom: (chatRoomId = ':chatRoomId') => `/chats/${chatRoomId}`,
  coupons: '/coupons',
  adminChats: '/admin/chats',
};

export default routePaths;
