package com.example.fivespringusedmarket.image.service;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@Service
public class S3PresignedUrlService {

    private static final Duration IMAGE_URL_EXPIRATION = Duration.ofMinutes(10);

    private final S3Presigner s3Presigner;
    private final String bucket;

    public S3PresignedUrlService(
            S3Presigner s3Presigner,
            @Value("${spring.cloud.aws.s3.bucket:}") String bucket
    ) {
        this.s3Presigner = s3Presigner;
        this.bucket = bucket;
    }

    /**
     * Private S3 Object Key를 클라이언트가 잠시 조회할 수 있는 Presigned URL로 변환한다.
     *
     * <p>DB에는 imageKey만 저장하고, API 응답 직전에 이 메서드로 10분짜리 임시 URL을 만든다.
     * imageKey가 비어 있으면 대표 이미지가 없는 상품으로 보고 {@code null}을 반환한다.</p>
     *
     * @param imageKey S3 Object Key
     * @return 10분 동안 유효한 S3 GetObject Presigned URL
     */
    public String createPresignedUrl(String imageKey) {
        if (!StringUtils.hasText(imageKey)) {
            return null;
        }

        // 실제 조회 대상 Private S3 Object를 지정한다.
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(imageKey)
                .build();

        // URL 만료 시간을 10분으로 제한해 장기 노출을 막는다.
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(IMAGE_URL_EXPIRATION)
                .getObjectRequest(getObjectRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest)
                .url()
                .toString();
    }
}
