package com.example.fivespringusedmarket.image.service;

import com.example.fivespringusedmarket.common.config.S3Properties;
import com.example.fivespringusedmarket.common.exception.CustomException;
import com.example.fivespringusedmarket.common.exception.ErrorCode;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
public class S3Uploader {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    private final S3Client s3Client;
    private final S3Properties s3Properties;
    private final String bucket;

    public S3Uploader(
            S3Client s3Client,
            S3Properties s3Properties,
            @Value("${spring.cloud.aws.s3.bucket:}") String bucket
    ) {
        this.s3Client = s3Client;
        this.s3Properties = s3Properties;
        this.bucket = bucket;
    }

    /**
     * 이미지 파일을 검증한 뒤 Private S3 Bucket에 업로드하고 S3 Object Key를 반환한다.
     *
     * <p>Private Bucket 정책을 유지해야 하므로 업로드 결과로 Public S3 URL을 만들지 않는다.
     * 반환된 key는 상품 등록/수정 API의 {@code imageKeys} 값으로 전달되어 DB에 저장된다.</p>
     *
     * @param file 업로드할 이미지 파일
     * @return S3에 저장된 Object Key
     */
    public String uploadImage(MultipartFile file) {
        validate(file);

        String extension = extractExtension(file.getOriginalFilename());
        String key = createObjectKey(extension);

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .build();

        try (InputStream inputStream = file.getInputStream()) {
            s3Client.putObject(request, RequestBody.fromInputStream(inputStream, file.getSize()));
            // 조회용 URL은 별도 Presigned URL 서비스에서 생성하므로 여기서는 key만 반환한다.
            return key;
        } catch (IOException | S3Exception | SdkClientException e) {
            throw new CustomException(ErrorCode.IMAGE_UPLOAD_FAILED);
        }
    }

    /**
     * 업로드 가능한 이미지인지 파일 존재 여부, 크기, MIME 타입, 확장자를 검증한다.
     */
    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(ErrorCode.EMPTY_IMAGE_FILE);
        }

        if (file.getSize() > s3Properties.getMaxFileSize()) {
            throw new CustomException(ErrorCode.IMAGE_FILE_SIZE_EXCEEDED);
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new CustomException(ErrorCode.INVALID_IMAGE_FILE);
        }

        String extension = extractExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new CustomException(ErrorCode.INVALID_IMAGE_FILE);
        }
    }

    /**
     * 원본 파일명에서 확장자를 추출한다.
     */
    private String extractExtension(String originalFilename) {
        String filename = StringUtils.cleanPath(originalFilename == null ? "" : originalFilename);
        int extensionIndex = filename.lastIndexOf('.');

        if (extensionIndex < 0 || extensionIndex == filename.length() - 1) {
            throw new CustomException(ErrorCode.INVALID_IMAGE_FILE);
        }

        return filename.substring(extensionIndex + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * 업로드 디렉터리와 UUID 파일명을 조합해 S3 Object Key를 생성한다.
     */
    private String createObjectKey(String extension) {
        String directory = normalizeDirectory(s3Properties.getDirectory());
        String filename = UUID.randomUUID() + "." + extension;

        if (directory.isBlank()) {
            return filename;
        }

        return directory + "/" + filename;
    }

    /**
     * 설정된 디렉터리 앞뒤의 슬래시를 제거해 S3 key가 일관된 형태가 되도록 정리한다.
     */
    private String normalizeDirectory(String directory) {
        if (directory == null) {
            return "";
        }

        return directory.replaceAll("^/+", "").replaceAll("/+$", "");
    }

}
