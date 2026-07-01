package com.example.fivespringusedmarket.image.service;

import com.example.fivespringusedmarket.common.config.S3Properties;
import com.example.fivespringusedmarket.common.exception.CustomException;
import com.example.fivespringusedmarket.common.exception.ErrorCode;
import com.example.fivespringusedmarket.image.dto.PresignedUploadUrlRequest;
import com.example.fivespringusedmarket.image.dto.PresignedUploadUrlResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
public class S3PresignedUrlService {

    private static final Duration IMAGE_URL_EXPIRATION = Duration.ofMinutes(10);
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png");
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png");

    private final S3Presigner s3Presigner;
    private final S3Properties s3Properties;
    private final String bucket;

    public S3PresignedUrlService(
            S3Presigner s3Presigner,
            S3Properties s3Properties,
            @Value("${spring.cloud.aws.s3.bucket:}") String bucket
    ) {
        this.s3Presigner = s3Presigner;
        this.s3Properties = s3Properties;
        this.bucket = bucket;
    }

    /**
     * 이미지 파일을 S3에 직접 업로드할 수 있는 Presigned PUT URL과 DB 저장용 imageKey를 발급한다.
     */
    public PresignedUploadUrlResponse createUploadPresignedUrl(PresignedUploadUrlRequest request) {
        validateUploadRequest(request);

        String extension = extractExtension(request.fileName());
        String imageKey = createObjectKey(extension);
        String contentType = request.contentType().toLowerCase(Locale.ROOT);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(imageKey)
                .contentType(contentType)
                .contentLength(request.fileSize())
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(IMAGE_URL_EXPIRATION)
                .putObjectRequest(putObjectRequest)
                .build();

        String uploadUrl = s3Presigner.presignPutObject(presignRequest)
                .url()
                .toString();

        return new PresignedUploadUrlResponse(imageKey, uploadUrl);
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

    private void validateUploadRequest(PresignedUploadUrlRequest request) {
        if (request == null
                || !StringUtils.hasText(request.fileName())
                || !StringUtils.hasText(request.contentType())) {
            throw new CustomException(ErrorCode.INVALID_IMAGE_FILE);
        }

        if (request.fileSize() == null || request.fileSize() <= 0) {
            throw new CustomException(ErrorCode.EMPTY_IMAGE_FILE);
        }

        if (request.fileSize() > s3Properties.getMaxFileSize()) {
            throw new CustomException(ErrorCode.IMAGE_FILE_SIZE_EXCEEDED);
        }

        String contentType = request.contentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new CustomException(ErrorCode.INVALID_IMAGE_FILE);
        }

        String extension = extractExtension(request.fileName());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new CustomException(ErrorCode.INVALID_IMAGE_FILE);
        }
    }

    private String extractExtension(String originalFilename) {
        String filename = StringUtils.cleanPath(originalFilename == null ? "" : originalFilename);
        int extensionIndex = filename.lastIndexOf('.');

        if (extensionIndex < 0 || extensionIndex == filename.length() - 1) {
            throw new CustomException(ErrorCode.INVALID_IMAGE_FILE);
        }

        return filename.substring(extensionIndex + 1).toLowerCase(Locale.ROOT);
    }

    private String createObjectKey(String extension) {
        String directory = normalizeDirectory(s3Properties.getDirectory());
        String filename = UUID.randomUUID() + "." + extension;

        if (directory.isBlank()) {
            return filename;
        }

        return directory + "/" + filename;
    }

    private String normalizeDirectory(String directory) {
        if (directory == null) {
            return "";
        }

        return directory.replaceAll("^/+", "").replaceAll("/+$", "");
    }
}
