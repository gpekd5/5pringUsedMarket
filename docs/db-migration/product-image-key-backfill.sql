-- product_images.image_url -> product_images.image_key 전환용 참고 SQL
-- 이 저장소는 현재 Flyway/Liquibase를 사용하지 않으므로 운영 배포 절차에서 명시적으로 실행한다.
-- 실행 전 DB 백업과 샘플 데이터 검증이 필요하다.

ALTER TABLE product_images
    ADD COLUMN image_key VARCHAR(500) NULL;

UPDATE product_images
SET image_key = SUBSTRING_INDEX(
        TRIM(LEADING '/' FROM SUBSTRING_INDEX(image_url, '.amazonaws.com/', -1)),
        '?',
        1
    )
WHERE image_key IS NULL
  AND image_url IS NOT NULL
  AND image_url LIKE 'http%://%.amazonaws.com/%';

-- 백필 결과 확인 후 애플리케이션 배포와 함께 NOT NULL 전환 여부를 결정한다.
-- SELECT id, image_url, image_key FROM product_images WHERE image_key IS NULL;
