-- =========================================================
-- demo-data.sql
-- 프론트 연결 / 시연용 기본 데이터
--
-- 현재 코드 기준:
-- members
-- products
-- product_images
-- wishes
-- chat_rooms
-- chat_members
-- chat_messages
-- coupons
-- user_coupons
--
-- 실행 전:
-- local 프로필에서 생성되는 기본 계정을 사용합니다.
-- 계정이 없다면 회원가입 API로 먼저 생성해주세요.
--
-- admin@test.com  / Password123! / 관리자
-- member@test.com / Password123! / 일반회원
-- =========================================================

START TRANSACTION;

-- =========================================================
-- 0. 회원 ID 조회 및 관리자 권한 부여
-- =========================================================

SET @seller_id = (
    SELECT id
    FROM members
    WHERE email = 'admin@test.com'
);

SET @buyer_id = (
    SELECT id
    FROM members
    WHERE email = 'member@test.com'
);

SET @admin_id = (
    SELECT id
    FROM members
    WHERE email = 'admin@test.com'
);

UPDATE members
SET role = 'ADMIN'
WHERE email = 'admin@test.com';


-- =========================================================
-- 1. 기존 DEMO 데이터 삭제
-- FK 관계 때문에 자식 테이블부터 삭제
-- =========================================================

DELETE FROM chat_messages
WHERE chat_room_id IN (
    SELECT cr.id
    FROM chat_rooms cr
             LEFT JOIN products p ON cr.product_id = p.id
    WHERE p.title LIKE '[DEMO]%'
       OR cr.title LIKE '[DEMO]%'
);

DELETE FROM chat_members
WHERE chat_room_id IN (
    SELECT cr.id
    FROM chat_rooms cr
             LEFT JOIN products p ON cr.product_id = p.id
    WHERE p.title LIKE '[DEMO]%'
       OR cr.title LIKE '[DEMO]%'
);

DELETE FROM chat_rooms
WHERE id IN (
    SELECT room_id
    FROM (
             SELECT cr.id AS room_id
             FROM chat_rooms cr
                      LEFT JOIN products p ON cr.product_id = p.id
             WHERE p.title LIKE '[DEMO]%'
                OR cr.title LIKE '[DEMO]%'
         ) t
);

DELETE FROM wishes
WHERE product_id IN (
    SELECT id
    FROM products
    WHERE title LIKE '[DEMO]%'
);

DELETE FROM product_images
WHERE product_id IN (
    SELECT id
    FROM products
    WHERE title LIKE '[DEMO]%'
);

DELETE FROM products
WHERE title LIKE '[DEMO]%';

DELETE FROM user_coupons
WHERE member_id IN (@seller_id, @buyer_id, @admin_id)
   OR coupon_id IN (
    SELECT id
    FROM coupons
    WHERE name LIKE '[DEMO]%'
);

DELETE FROM coupons
WHERE name LIKE '[DEMO]%';


-- =========================================================
-- 2. 시연용 상품 데이터 생성
--
-- ProductCategory:
-- DIGITAL, FURNITURE, CLOTHING, BOOK, SPORTS,
-- KIDS, BEAUTY, FOOD, PET, ETC
--
-- ProductStatus:
-- ON_SALE, RESERVED, SOLD, DELETED
-- =========================================================

INSERT INTO products (
    member_id,
    title,
    description,
    price,
    category,
    status,
    created_at,
    updated_at
) VALUES
      (
          @seller_id,
          '[DEMO] 아이폰 15 판매합니다',
          '프론트 연결 확인용 상품입니다. 상태 좋은 아이폰 15입니다.',
          800000,
          'DIGITAL',
          'ON_SALE',
          NOW(),
          NOW()
      ),
      (
          @seller_id,
          '[DEMO] 맥북 에어 M2 판매합니다',
          '프론트 연결 확인용 상품입니다. 개발용으로 사용한 맥북입니다.',
          1200000,
          'DIGITAL',
          'ON_SALE',
          NOW(),
          NOW()
      ),
      (
          @seller_id,
          '[DEMO] 갤럭시 S24 판매합니다',
          '프론트 연결 확인용 상품입니다. 실사용 기간 짧고 상태 좋습니다.',
          700000,
          'DIGITAL',
          'RESERVED',
          NOW(),
          NOW()
      ),
      (
          @seller_id,
          '[DEMO] 에어팟 프로 2세대',
          '프론트 연결 확인용 상품입니다. 케이스 포함 정상 작동합니다.',
          180000,
          'DIGITAL',
          'ON_SALE',
          NOW(),
          NOW()
      ),
      (
          @seller_id,
          '[DEMO] 기계식 키보드 판매',
          '프론트 연결 확인용 상품입니다. 갈축 기계식 키보드입니다.',
          50000,
          'DIGITAL',
          'ON_SALE',
          NOW(),
          NOW()
      ),
      (
          @seller_id,
          '[DEMO] 게이밍 마우스',
          '프론트 연결 확인용 상품입니다. 무선 게이밍 마우스입니다.',
          40000,
          'DIGITAL',
          'SOLD',
          NOW(),
          NOW()
      ),
      (
          @seller_id,
          '[DEMO] 27인치 모니터',
          '프론트 연결 확인용 상품입니다. 사무용 27인치 모니터입니다.',
          130000,
          'DIGITAL',
          'ON_SALE',
          NOW(),
          NOW()
      ),
      (
          @seller_id,
          '[DEMO] 나이키 운동화',
          '프론트 연결 확인용 상품입니다. 사이즈 270, 상태 좋습니다.',
          60000,
          'CLOTHING',
          'ON_SALE',
          NOW(),
          NOW()
      ),
      (
          @seller_id,
          '[DEMO] 겨울 패딩',
          '프론트 연결 확인용 상품입니다. 따뜻한 겨울 패딩입니다.',
          90000,
          'CLOTHING',
          'RESERVED',
          NOW(),
          NOW()
      ),
      (
          @seller_id,
          '[DEMO] 백팩 가방',
          '프론트 연결 확인용 상품입니다. 노트북 수납 가능한 백팩입니다.',
          35000,
          'CLOTHING',
          'ON_SALE',
          NOW(),
          NOW()
      ),
      (
          @seller_id,
          '[DEMO] 원목 책상',
          '프론트 연결 확인용 상품입니다. 깔끔한 원목 책상입니다.',
          70000,
          'FURNITURE',
          'ON_SALE',
          NOW(),
          NOW()
      ),
      (
          @seller_id,
          '[DEMO] 사무용 의자',
          '프론트 연결 확인용 상품입니다. 편한 사무용 의자입니다.',
          45000,
          'FURNITURE',
          'ON_SALE',
          NOW(),
          NOW()
      );


-- =========================================================
-- 3. 상품 대표 이미지 데이터 생성
--
-- ProductImage Entity 기준:
-- product_id
-- image_key
-- sort_order
-- created_at
-- updated_at
-- =========================================================

INSERT INTO product_images (
    product_id,
    image_key,
    sort_order,
    created_at,
    updated_at
)
SELECT
    id,
    CASE
        WHEN title LIKE '%아이폰%' THEN 'products/demo/iphone15.png'
        WHEN title LIKE '%맥북%' THEN 'products/demo/macbook-air-m2.png'
        WHEN title LIKE '%갤럭시%' THEN 'products/demo/galaxy-s24.png'
        WHEN title LIKE '%에어팟%' THEN 'products/demo/airpods-pro-2.png'
        WHEN title LIKE '%키보드%' THEN 'products/demo/keyboard.png'
        WHEN title LIKE '%마우스%' THEN 'products/demo/mouse.png'
        WHEN title LIKE '%모니터%' THEN 'products/demo/monitor.png'
        WHEN title LIKE '%나이키%' THEN 'products/demo/nike-shoes.png'
        WHEN title LIKE '%패딩%' THEN 'products/demo/padding.png'
        WHEN title LIKE '%백팩%' THEN 'products/demo/backpack.png'
        WHEN title LIKE '%책상%' THEN 'products/demo/desk.png'
        WHEN title LIKE '%의자%' THEN 'products/demo/chair.png'
        ELSE CONCAT('products/demo/', id, '.png')
        END,
    0,
    NOW(),
    NOW()
FROM products
WHERE member_id = @seller_id
  AND title LIKE '[DEMO]%';


-- =========================================================
-- 4. 관심상품 데이터 생성
--
-- 현재 코드 기준 관심상품 테이블명: wishes
-- 구매자가 일부 상품을 찜한 상태
-- =========================================================

INSERT INTO wishes (
    member_id,
    product_id,
    created_at,
    updated_at
)
SELECT
    @buyer_id,
    id,
    NOW(),
    NOW()
FROM products
WHERE title IN (
                '[DEMO] 아이폰 15 판매합니다',
                '[DEMO] 맥북 에어 M2 판매합니다',
                '[DEMO] 에어팟 프로 2세대',
                '[DEMO] 나이키 운동화'
    );


-- =========================================================
-- 5. 거래 채팅방 생성
--
-- ChatRoom Entity 기준:
-- type
-- title
-- product_id
-- cs_status
-- last_message_at
-- last_message_content
-- created_at
-- updated_at
--
-- 거래 채팅방은 type = TRADE
-- cs_status는 null
-- =========================================================

INSERT INTO chat_rooms (
    type,
    title,
    product_id,
    cs_status,
    last_message_at,
    last_message_content,
    created_at,
    updated_at
)
SELECT
    'TRADE',
    p.title,
    p.id,
    NULL,
    NOW(),
    '네, 아직 거래 가능합니다.',
    NOW(),
    NOW()
FROM products p
WHERE p.title IN (
                  '[DEMO] 아이폰 15 판매합니다',
                  '[DEMO] 맥북 에어 M2 판매합니다',
                  '[DEMO] 에어팟 프로 2세대'
    );


-- =========================================================
-- 6. 거래 채팅방 참여자 생성
--
-- 현재 구조는 chat_rooms에 seller_id/buyer_id가 없음.
-- 참여자는 chat_members로 관리함.
--
-- 거래 채팅방:
-- 구매자 MEMBER
-- 판매자 MEMBER
-- =========================================================

INSERT INTO chat_members (
    chat_room_id,
    member_id,
    member_role,
    last_read_message_id,
    unread_count,
    joined_at
)
SELECT
    cr.id,
    @buyer_id,
    'MEMBER',
    0,
    1,
    NOW()
FROM chat_rooms cr
         JOIN products p ON cr.product_id = p.id
WHERE p.title IN (
                  '[DEMO] 아이폰 15 판매합니다',
                  '[DEMO] 맥북 에어 M2 판매합니다',
                  '[DEMO] 에어팟 프로 2세대'
    );

INSERT INTO chat_members (
    chat_room_id,
    member_id,
    member_role,
    last_read_message_id,
    unread_count,
    joined_at
)
SELECT
    cr.id,
    @seller_id,
    'MEMBER',
    0,
    0,
    NOW()
FROM chat_rooms cr
         JOIN products p ON cr.product_id = p.id
WHERE p.title IN (
                  '[DEMO] 아이폰 15 판매합니다',
                  '[DEMO] 맥북 에어 M2 판매합니다',
                  '[DEMO] 에어팟 프로 2세대'
    );


-- =========================================================
-- 7. 거래 채팅 메시지 생성
--
-- ChatMessage Entity 기준:
-- chat_room_id
-- sender_id
-- type
-- content
-- created_at
-- =========================================================

INSERT INTO chat_messages (
    chat_room_id,
    sender_id,
    type,
    content,
    created_at
)
SELECT
    cr.id,
    @buyer_id,
    'TALK',
    '안녕하세요. 아직 구매 가능할까요?',
    DATE_SUB(NOW(), INTERVAL 5 MINUTE)
FROM chat_rooms cr
         JOIN products p ON cr.product_id = p.id
WHERE p.title IN (
                  '[DEMO] 아이폰 15 판매합니다',
                  '[DEMO] 맥북 에어 M2 판매합니다',
                  '[DEMO] 에어팟 프로 2세대'
    );

INSERT INTO chat_messages (
    chat_room_id,
    sender_id,
    type,
    content,
    created_at
)
SELECT
    cr.id,
    @seller_id,
    'TALK',
    '네, 아직 거래 가능합니다.',
    DATE_SUB(NOW(), INTERVAL 3 MINUTE)
FROM chat_rooms cr
         JOIN products p ON cr.product_id = p.id
WHERE p.title IN (
                  '[DEMO] 아이폰 15 판매합니다',
                  '[DEMO] 맥북 에어 M2 판매합니다',
                  '[DEMO] 에어팟 프로 2세대'
    );


-- =========================================================
-- 8. CS 채팅방 생성
--
-- 관리자 CS 목록 확인용
-- CS 채팅방은 product_id가 null
-- type = CS
-- cs_status = WAITING
-- =========================================================

INSERT INTO chat_rooms (
    type,
    title,
    product_id,
    cs_status,
    last_message_at,
    last_message_content,
    created_at,
    updated_at
) VALUES
    (
        'CS',
        '[DEMO] 상품 거래 관련 문의',
        NULL,
        'WAITING',
        NOW(),
        '상품 거래 관련 문의드립니다.',
        NOW(),
        NOW()
    );


SET @cs_room_id = LAST_INSERT_ID();


-- CS 채팅방 고객 참여자
INSERT INTO chat_members (
    chat_room_id,
    member_id,
    member_role,
    last_read_message_id,
    unread_count,
    joined_at
) VALUES (
             @cs_room_id,
             @buyer_id,
             'MEMBER',
             0,
             0,
             NOW()
         );


-- CS 문의 메시지
INSERT INTO chat_messages (
    chat_room_id,
    sender_id,
    type,
    content,
    created_at
) VALUES (
             @cs_room_id,
             @buyer_id,
             'TALK',
             '상품 거래 관련 문의드립니다.',
             NOW()
         );


-- =========================================================
-- 9. 쿠폰 데이터 생성
--
-- Coupon Entity 기준:
-- name
-- total_qty
-- issued_qty
-- event_start_at
-- event_end_at
-- expire_at
-- created_at
-- updated_at 없음
-- =========================================================

INSERT INTO coupons (
    name,
    total_qty,
    issued_qty,
    event_start_at,
    event_end_at,
    expire_at,
    created_at
) VALUES
      (
          '[DEMO] 메가커피 아메리카노 쿠폰',
          100,
          0,
          DATE_SUB(NOW(), INTERVAL 1 DAY),
          DATE_ADD(NOW(), INTERVAL 7 DAY),
          DATE_ADD(NOW(), INTERVAL 30 DAY),
          NOW()
      ),
      (
          '[DEMO] 배달의민족 5천원 쿠폰',
          100,
          1,
          DATE_SUB(NOW(), INTERVAL 1 DAY),
          DATE_ADD(NOW(), INTERVAL 7 DAY),
          DATE_ADD(NOW(), INTERVAL 30 DAY),
          NOW()
      ),
      (
          '[DEMO] GS25 편의점 3천원 쿠폰',
          100,
          1,
          DATE_SUB(NOW(), INTERVAL 1 DAY),
          DATE_ADD(NOW(), INTERVAL 7 DAY),
          DATE_ADD(NOW(), INTERVAL 30 DAY),
          NOW()
      );

-- =========================================================
-- 10. 사용자 보유 쿠폰 데이터 생성
--
-- CouponService 정책 기준:
-- coupons.issued_qty는 user_coupons 발급 건수와 맞춰 둔다.
-- used_at이 null이면 사용 가능, 값이 있으면 사용 완료 상태다.
-- =========================================================

INSERT INTO user_coupons (
    member_id,
    coupon_id,
    code,
    issued_at,
    expire_at,
    used_at
)
SELECT
    @buyer_id,
    c.id,
    'DEMO-BAEMIN-0001',
    DATE_SUB(NOW(), INTERVAL 2 HOUR),
    c.expire_at,
    NULL
FROM coupons c
WHERE c.name = '[DEMO] 배달의민족 5천원 쿠폰';

INSERT INTO user_coupons (
    member_id,
    coupon_id,
    code,
    issued_at,
    expire_at,
    used_at
)
SELECT
    @buyer_id,
    c.id,
    'DEMO-GS25-0001',
    DATE_SUB(NOW(), INTERVAL 1 DAY),
    c.expire_at,
    DATE_SUB(NOW(), INTERVAL 3 HOUR)
FROM coupons c
WHERE c.name = '[DEMO] GS25 편의점 3천원 쿠폰';


COMMIT;


-- =========================================================
-- 10. 확인용 조회
-- =========================================================

SELECT id, email, nickname, role
FROM members
WHERE email IN ('admin@test.com', 'member@test.com');

SELECT id, title, price, category, status, member_id
FROM products
WHERE title LIKE '[DEMO]%';

SELECT pi.id, pi.product_id, pi.image_key, pi.sort_order
FROM product_images pi
         JOIN products p ON pi.product_id = p.id
WHERE p.title LIKE '[DEMO]%';

SELECT w.id, w.member_id, w.product_id
FROM wishes w
         JOIN products p ON w.product_id = p.id
WHERE p.title LIKE '[DEMO]%';

SELECT cr.id, cr.type, cr.title, cr.product_id, cr.cs_status, cr.last_message_content
FROM chat_rooms cr
         LEFT JOIN products p ON cr.product_id = p.id
WHERE p.title LIKE '[DEMO]%'
   OR cr.title LIKE '[DEMO]%';

SELECT cm.id, cm.chat_room_id, cm.member_id, cm.member_role, cm.unread_count
FROM chat_members cm
         JOIN chat_rooms cr ON cm.chat_room_id = cr.id
         LEFT JOIN products p ON cr.product_id = p.id
WHERE p.title LIKE '[DEMO]%'
   OR cr.title LIKE '[DEMO]%';

SELECT msg.id, msg.chat_room_id, msg.sender_id, msg.type, msg.content, msg.created_at
FROM chat_messages msg
         JOIN chat_rooms cr ON msg.chat_room_id = cr.id
         LEFT JOIN products p ON cr.product_id = p.id
WHERE p.title LIKE '[DEMO]%'
   OR cr.title LIKE '[DEMO]%'
ORDER BY msg.chat_room_id, msg.id;

SELECT id, name, total_qty, issued_qty, event_start_at, event_end_at, expire_at
FROM coupons
WHERE name LIKE '[DEMO]%';

SELECT uc.id, uc.member_id, uc.coupon_id, c.name, uc.code, uc.issued_at, uc.expire_at, uc.used_at
FROM user_coupons uc
         JOIN coupons c ON uc.coupon_id = c.id
WHERE c.name LIKE '[DEMO]%'
ORDER BY uc.id;
