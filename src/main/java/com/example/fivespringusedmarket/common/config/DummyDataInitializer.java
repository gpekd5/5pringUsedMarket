package com.example.fivespringusedmarket.common.config;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import net.datafaker.Faker;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 인덱스 성능 테스트용 대용량 더미 데이터를 적재한다.
 * products 5만 건 + product_images 5만 건(대표 이미지 1장)을 1,000건 단위 배치 INSERT로 삽입한다.
 * 이미 데이터가 존재하면 중복 적재를 건너뛴다.
 */
@Profile("local")
@Component
public class DummyDataInitializer implements ApplicationRunner {

    private static final int TARGET_COUNT = 50_000;
    private static final int BATCH_SIZE = 1_000;

    private static final String[] CATEGORIES = {
            "DIGITAL", "CLOTHING", "BOOK", "SPORTS", "FURNITURE",
            "BEAUTY", "KIDS", "PET", "FOOD", "ETC"
    };
    private static final String[] STATUSES = {"ON_SALE", "RESERVED", "SOLD"};

    private final JdbcTemplate jdbcTemplate;

    public DummyDataInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<Long> memberIds = jdbcTemplate.queryForList(
                "SELECT id FROM members WHERE role = 'MEMBER' LIMIT 1", Long.class);
        Long memberId = memberIds.isEmpty() ? null : memberIds.get(0);

        if (memberId == null) {
            System.out.println("[DummyData] MEMBER 계정 없음 — 적재 생략 (LocalTestAccountInitializer 먼저 실행 필요)");
            return;
        }

        insertProducts(memberId);
        insertChatRooms(memberId);
    }

    private void insertProducts(Long sellerId) {
        Integer existing = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM products", Integer.class);

        if (existing != null && existing >= TARGET_COUNT) {
            System.out.printf("[DummyData][products] 이미 %,d건 존재 — 적재 생략%n", existing);
            return;
        }

        System.out.printf("[DummyData][products] %,d건 적재 시작%n", TARGET_COUNT);
        long start = System.currentTimeMillis();

        Faker faker = new Faker(Locale.KOREAN);
        Random random = new Random();

        int inserted = 0;
        while (inserted < TARGET_COUNT) {
            int batchCount = Math.min(BATCH_SIZE, TARGET_COUNT - inserted);
            LocalDateTime now = LocalDateTime.now();

            List<Object[]> productRows = new ArrayList<>(batchCount);
            for (int i = 0; i < batchCount; i++) {
                String category = CATEGORIES[random.nextInt(CATEGORIES.length)];
                String status = STATUSES[random.nextInt(STATUSES.length)];
                int price = (random.nextInt(200) + 1) * 1_000;
                String title = faker.commerce().productName();
                String description = faker.lorem().sentence(10);
                LocalDateTime createdAt = now.minusDays(random.nextInt(365));

                productRows.add(new Object[]{
                        sellerId, title, description, price, category, status,
                        Timestamp.valueOf(createdAt), Timestamp.valueOf(createdAt)
                });
            }

            jdbcTemplate.batchUpdate(
                    """
                    INSERT INTO products (member_id, title, description, price, category, status, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    productRows
            );

            Long maxId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM products", Long.class);
            if (maxId == null) break;
            long minId = maxId - batchCount + 1;

            List<Object[]> imageRows = new ArrayList<>(batchCount);
            for (long productId = minId; productId <= maxId; productId++) {
                String imageUrl = "https://picsum.photos/seed/" + productId + "/400/400";
                imageRows.add(new Object[]{
                        productId, imageUrl, 0,
                        Timestamp.valueOf(now), Timestamp.valueOf(now)
                });
            }

            jdbcTemplate.batchUpdate(
                    """
                    INSERT INTO product_images (product_id, image_url, sort_order, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?)
                    """,
                    imageRows
            );

            inserted += batchCount;
            System.out.printf("[DummyData][products] %,d / %,d 건 완료%n", inserted, TARGET_COUNT);
        }

        long elapsed = System.currentTimeMillis() - start;
        System.out.printf("[DummyData][products] 적재 완료 — %,d건 / %,d ms%n", TARGET_COUNT, elapsed);
    }

    private void insertChatRooms(Long memberId) {
        Integer existing = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM chat_rooms", Integer.class);

        if (existing != null && existing >= TARGET_COUNT) {
            System.out.printf("[DummyData][chat_rooms] 이미 %,d건 존재 — 적재 생략%n", existing);
            return;
        }

        System.out.printf("[DummyData][chat_rooms] %,d건 적재 시작%n", TARGET_COUNT);
        long start = System.currentTimeMillis();

        Faker faker = new Faker(Locale.KOREAN);
        Random random = new Random();

        int inserted = 0;
        while (inserted < TARGET_COUNT) {
            int batchCount = Math.min(BATCH_SIZE, TARGET_COUNT - inserted);
            LocalDateTime now = LocalDateTime.now();

            // chat_rooms INSERT
            List<Object[]> roomRows = new ArrayList<>(batchCount);
            for (int i = 0; i < batchCount; i++) {
                LocalDateTime createdAt = now.minusDays(random.nextInt(365));
                // lastMessageAt: 30% 확률로 null (메시지 없는 방)
                LocalDateTime lastMessageAt = random.nextInt(10) < 3
                        ? null
                        : createdAt.plusHours(random.nextInt(720));
                String lastContent = lastMessageAt != null ? faker.lorem().sentence(5) : null;

                roomRows.add(new Object[]{
                        "TRADE",
                        faker.commerce().productName(),
                        lastContent,
                        lastMessageAt != null ? Timestamp.valueOf(lastMessageAt) : null,
                        Timestamp.valueOf(createdAt),
                        Timestamp.valueOf(createdAt)
                });
            }

            jdbcTemplate.batchUpdate(
                    """
                    INSERT INTO chat_rooms (type, title, last_message_content, last_message_at, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """,
                    roomRows
            );

            Long maxRoomId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM chat_rooms", Long.class);
            if (maxRoomId == null) break;
            long minRoomId = maxRoomId - batchCount + 1;

            // chat_members INSERT — 테스트 대상 memberId를 모든 채팅방에 BUYER로 참여
            List<Object[]> memberRows = new ArrayList<>(batchCount);
            LocalDateTime joinedAt = now;
            for (long roomId = minRoomId; roomId <= maxRoomId; roomId++) {
                memberRows.add(new Object[]{
                        roomId, memberId, "MEMBER", 0L, 0L,
                        Timestamp.valueOf(joinedAt)
                });
            }

            jdbcTemplate.batchUpdate(
                    """
                    INSERT INTO chat_members (chat_room_id, member_id, member_role, last_read_message_id, unread_count, joined_at)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """,
                    memberRows
            );

            inserted += batchCount;
            System.out.printf("[DummyData][chat_rooms] %,d / %,d 건 완료%n", inserted, TARGET_COUNT);
        }

        long elapsed = System.currentTimeMillis() - start;
        System.out.printf("[DummyData][chat_rooms] 적재 완료 — %,d건 / %,d ms%n", TARGET_COUNT, elapsed);
    }
}
