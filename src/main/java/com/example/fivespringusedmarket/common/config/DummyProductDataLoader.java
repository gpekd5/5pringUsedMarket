package com.example.fivespringusedmarket.common.config;

import com.example.fivespringusedmarket.product.entity.ProductCategory;
import com.example.fivespringusedmarket.product.entity.ProductStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Random;

/**
 * 로컬 환경에서 검색 성능 테스트용 상품 더미 데이터를 적재하는 클래스입니다.
 *
 * <p>대량 데이터 적재 시 JPA Repository의 save()를 반복 호출하면
 * 영속성 컨텍스트에 엔티티가 계속 쌓여 메모리 사용량이 증가할 수 있습니다.
 * 이를 피하기 위해 JdbcTemplate의 batchUpdate()를 사용하여
 * 일정 개수 단위로 상품 데이터와 대표 이미지 데이터를 직접 INSERT합니다.</p>
 *
 * <p>Datafaker는 더미 상품 설명을 랜덤하게 생성하기 위해 사용합니다.
 * 다만 검색 테스트에 필요한 키워드가 유지되어야 하므로,
 * 상품명은 직접 정의한 키워드 배열과 문구 배열을 조합해 생성합니다.</p>
 *
 * <p>이 클래스는 local 프로필에서만 동작하며,
 * 이미 더미 데이터가 존재하는 경우 중복 적재를 방지합니다.</p>
 */
@Slf4j
@Profile("local")
@Component
@RequiredArgsConstructor
public class DummyProductDataLoader implements ApplicationRunner {

    private static final int TOTAL_COUNT = 50_000;
    private static final int BATCH_SIZE = 1_000;

    private final JdbcTemplate jdbcTemplate;

    private final Random random = new Random();

    // Datafaker 랜덤 데이터 생성객체
    private final Faker faker = new Faker(Locale.KOREAN);

    // 검색 테스트용 상품 키워드
    private final String[] keywords = {
            "아이폰", "맥북", "갤럭시", "에어팟", "키보드",
            "마우스", "의자", "책상", "나이키", "가방",
            "모니터", "패딩", "자전거", "캠핑의자", "책"
    };

    // 상품명 뒤에 붙일 랜덤 문구
    private final String[] suffixes = {
            "중고 판매합니다",
            "급처합니다",
            "거의 새상품입니다",
            "상태 좋습니다",
            "저렴하게 판매합니다",
            "직거래 원합니다"
    };

    @Override
    public void run(ApplicationArguments args) {
        // 이미 더미 데이터가 있으면 중복 적재 방지
        if (alreadyInserted()) {
            log.info("더미 상품 데이터가 이미 존재하여 적재를 건너뜁니다.");
            return;
        }

        // 로컬 테스트 회원을 판매자로 사용
        Long sellerId = findLocalMemberId();

        log.info("더미 상품 데이터 적재시작.totalCount={}, batchSize={}", TOTAL_COUNT, BATCH_SIZE);

        long startTime = System.currentTimeMillis();

        // 전체 데이터를 batchSize 단위로 나누어 적재
        for (int offset = 0; offset < TOTAL_COUNT; offset += BATCH_SIZE) {
            int currentBatchSize = Math.min(BATCH_SIZE, TOTAL_COUNT - offset);

            insertProducts(sellerId, offset, currentBatchSize);
            insertProductImages(offset, currentBatchSize);

            log.info("더미 상품 적재 진행: {} / {}", offset + currentBatchSize, TOTAL_COUNT);
        }

        long elapsedTime = System.currentTimeMillis() - startTime;
        log.info("더미 상품 데이터 적재 완료. elapsed={}ms", elapsedTime);
    }

    // 더미 상품 데이터가 이미 적재되어 있는지 확인
    private boolean alreadyInserted() {
        Integer productCount  = jdbcTemplate.queryForObject(
                "select count(*) from products where title like '더미상품-%'",
                Integer.class
        );

        Integer imageCount = jdbcTemplate.queryForObject(
                """
                select count(*)
                from product_images pi
                join products p on pi.product_id = p.id
                where p.title like '더미상품-%'
                """,
                Integer.class
        );

        return productCount != null
                && imageCount != null
                && productCount >= TOTAL_COUNT
                && imageCount >= TOTAL_COUNT;
    }

    // 더미 상품의 판매자로 사용할 로컬 테스트 회원 id 조회
    private Long findLocalMemberId() {
        return jdbcTemplate.queryForObject(
                "select id from members where email = ?",
                Long.class,
                "member@test.com"
        );
    }

    // 상품 데이터를 JDBC BATCH Insert 방식으로 적재
    private void insertProducts(Long sellerId, int offset, int batchSize) {
        String sql = """
                insert into products
                    (member_id, title, description, price, category, status, created_at, updated_at)
                values
                    (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        // JDBC Batch Insert로 상품 데이터 적재
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {

            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                int index = offset + i + 1;

                // 검색 테스트용 키워드 기반 상품명 생성
                String keyword = keywords[random.nextInt(keywords.length)];
                String suffix = suffixes[random.nextInt(suffixes.length)];
                String title = "더미상품-" + index + " " + keyword + " " + suffix;

                // Datafaker를 사용한 랜덤 설명 생성
                String description = keyword + " 상품입니다. "
                        + faker.lorem().sentence();

                // 상품 속성 랜덤 생성
                int price = randomPrice();
                ProductCategory category = randomCategory();
                ProductStatus status = randomStatus();

                LocalDateTime now = LocalDateTime.now().minusMinutes(random.nextInt(100_000));

                // SQL 파라미터 바인딩
                ps.setLong(1, sellerId);
                ps.setString(2, title);
                ps.setString(3, description);
                ps.setInt(4, price);
                ps.setString(5, category.name());
                ps.setString(6, status.name());
                ps.setObject(7, now);
                ps.setObject(8, now);
            }

            @Override
            public int getBatchSize() {
                return batchSize;
            }
        });
    }

    // 상품 대표 이미지 데이터를 JDBC Batch Insert 방식으로 적재합니다.
    // 상품 대표 이미지 데이터를 JDBC Batch Insert 방식으로 적재합니다.
    private void insertProductImages(int offset, int batchSize) {
        String sql = """
            insert into product_images
                (product_id, image_url, image_key, sort_order, created_at, updated_at)
            values
                (?, ?, ?, ?, ?, ?)
            """;

        // 현재 batch의 첫 번째 상품 id 조회
        Long firstProductId = jdbcTemplate.queryForObject(
                """
                select id
                from products
                where title like ?
                order by id asc
                limit 1
                """,
                Long.class,
                "더미상품-" + (offset + 1) + " %"
        );

        // JDBC Batch Insert로 대표 이미지 데이터 적재
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {

            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Long productId = firstProductId + i;
                LocalDateTime now = LocalDateTime.now();
                String dummyImageUrl = "https://dummyimage.com/300x300/cccccc/000000&text=dummy";

                ps.setLong(1, productId);
                ps.setString(2, dummyImageUrl); // image_url 임시값
                ps.setString(3, dummyImageUrl); // image_key
                ps.setInt(4, 0);
                ps.setObject(5, now);
                ps.setObject(6, now);
            }

            @Override
            public int getBatchSize() {
                return batchSize;
            }
        });
    }

    // 더미 상품 가격을 랜덤으로 생성
    private int randomPrice() {
        // 1,000원 ~ 500,000원 범위 랜덤 가격
        return (random.nextInt(500) + 1) * 1_000;
    }

    // 상품 카테고리를 랜덤으로 선택
    private ProductCategory randomCategory() {
        // ProductCategory 중 랜덤 선택
        ProductCategory[] categories = ProductCategory.values();
        return categories[random.nextInt(categories.length)];
    }

    // 상품 상태를 랜덤으로 선택
    private ProductStatus randomStatus() {
        // 검색 대상 상태만 랜덤 선택
        ProductStatus[] statuses = {
                ProductStatus.ON_SALE,
                ProductStatus.RESERVED,
                ProductStatus.SOLD
        };

        return statuses[random.nextInt(statuses.length)];
    }


}
