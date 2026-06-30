const routePaths = {
  home: '/',
  login: '/login',
  signup: '/signup',
  productDetail: (productId = ':productId') => `/products/${productId}`,
  wishes: '/wishes',
  chats: '/chats',
  chatRoom: (chatRoomId = ':chatRoomId') => `/chats/${chatRoomId}`,
  coupons: '/coupons',
  adminChats: '/admin/chats',
};

export default routePaths;
