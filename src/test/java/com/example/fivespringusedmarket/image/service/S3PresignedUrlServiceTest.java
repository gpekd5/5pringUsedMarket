package com.example.fivespringusedmarket.image.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.fivespringusedmarket.common.config.S3Properties;
import com.example.fivespringusedmarket.common.exception.CustomException;
import com.example.fivespringusedmarket.common.exception.ErrorCode;
import com.example.fivespringusedmarket.image.dto.PresignedUploadUrlRequest;
import com.example.fivespringusedmarket.image.dto.PresignedUploadUrlResponse;
import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

class S3PresignedUrlServiceTest {

    private S3Presigner s3Presigner;
    private S3Client s3Client;
    private S3PresignedUrlService service;

    @BeforeEach
    void setUp() {
        s3Presigner = Mockito.mock(S3Presigner.class);
        s3Client = Mockito.mock(S3Client.class);
        S3Properties s3Properties = new S3Properties();
        s3Properties.setMaxFileSize(1024L);
        service = new S3PresignedUrlService(s3Presigner, s3Client, s3Properties, "test-bucket");
    }

    @Test
    void createUploadPresignedUrlReturnsTenMinutePutObjectUrl() throws Exception {
        // given
        PresignedPutObjectRequest presignedRequest = Mockito.mock(PresignedPutObjectRequest.class);
        PresignedUploadUrlRequest request = new PresignedUploadUrlRequest("sample.png", "image/png", 512L);

        when(s3Presigner.presignPutObject(Mockito.any(PutObjectPresignRequest.class)))
                .thenReturn(presignedRequest);
        when(presignedRequest.url())
                .thenReturn(URI.create("https://test-bucket.s3.ap-northeast-2.amazonaws.com/products/sample.png?X-Amz-Signature=put-test").toURL());

        // when
        PresignedUploadUrlResponse response = service.createUploadPresignedUrl(request);

        // then
        ArgumentCaptor<PutObjectPresignRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectPresignRequest.class);
        verify(s3Presigner).presignPutObject(requestCaptor.capture());

        PutObjectPresignRequest presignRequest = requestCaptor.getValue();
        assertThat(presignRequest.signatureDuration()).isEqualTo(Duration.ofMinutes(10));
        assertThat(presignRequest.putObjectRequest().bucket()).isEqualTo("test-bucket");
        assertThat(presignRequest.putObjectRequest().key()).startsWith("products/");
        assertThat(presignRequest.putObjectRequest().key()).endsWith(".png");
        assertThat(presignRequest.putObjectRequest().key())
                .matches("^products/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\.png$");
        assertThat(presignRequest.putObjectRequest().contentType()).isEqualTo("image/png");
        assertThat(presignRequest.putObjectRequest().contentLength()).isEqualTo(512L);
        assertThat(response.imageKey()).isEqualTo(presignRequest.putObjectRequest().key());
        assertThat(response.uploadUrl()).contains("X-Amz-Signature=put-test");
    }

    @Test
    void createUploadPresignedUrlCreatesImageKeyAcceptedByProductImageKeyPolicy() throws Exception {
        // given
        PresignedPutObjectRequest presignedRequest = Mockito.mock(PresignedPutObjectRequest.class);
        PresignedUploadUrlRequest request = new PresignedUploadUrlRequest("sample.png", "image/png", 512L);

        when(s3Presigner.presignPutObject(Mockito.any(PutObjectPresignRequest.class)))
                .thenReturn(presignedRequest);
        when(presignedRequest.url())
                .thenReturn(URI.create("https://test-bucket.s3.ap-northeast-2.amazonaws.com/products/sample.png?X-Amz-Signature=put-test").toURL());

        // when
        PresignedUploadUrlResponse response = service.createUploadPresignedUrl(request);

        // then
        assertThat(ImageKeyPolicy.isValidProductImageKey(response.imageKey())).isTrue();
    }

    @Test
    void createUploadPresignedUrlRejectsInvalidContentType() {
        // given
        PresignedUploadUrlRequest request = new PresignedUploadUrlRequest("sample.png", "text/plain", 512L);

        // when & then
        assertThatThrownBy(() -> service.createUploadPresignedUrl(request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_IMAGE_FILE);
    }

    @Test
    void createUploadPresignedUrlRejectsInvalidExtension() {
        // given
        PresignedUploadUrlRequest request = new PresignedUploadUrlRequest("sample.gif", "image/png", 512L);

        // when & then
        assertThatThrownBy(() -> service.createUploadPresignedUrl(request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_IMAGE_FILE);
    }

    @Test
    void createUploadPresignedUrlRejectsWebp() {
        // given
        PresignedUploadUrlRequest request = new PresignedUploadUrlRequest("sample.webp", "image/webp", 512L);

        // when & then
        assertThatThrownBy(() -> service.createUploadPresignedUrl(request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_IMAGE_FILE);
    }

    @Test
    void createUploadPresignedUrlRejectsExceededFileSize() {
        // given
        PresignedUploadUrlRequest request = new PresignedUploadUrlRequest("sample.jpg", "image/jpeg", 2048L);

        // when & then
        assertThatThrownBy(() -> service.createUploadPresignedUrl(request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.IMAGE_FILE_SIZE_EXCEEDED);
    }

    @Test
    void createPresignedUrlReturnsTenMinutePrivateObjectUrl() throws Exception {
        // given
        PresignedGetObjectRequest presignedRequest = Mockito.mock(PresignedGetObjectRequest.class);

        when(s3Presigner.presignGetObject(Mockito.any(GetObjectPresignRequest.class)))
                .thenReturn(presignedRequest);
        when(presignedRequest.url())
                .thenReturn(URI.create("https://test-bucket.s3.ap-northeast-2.amazonaws.com/products/sample.png?X-Amz-Signature=test").toURL());

        // when
        String presignedUrl = service.createPresignedUrl("products/sample.png");

        // then
        ArgumentCaptor<GetObjectPresignRequest> requestCaptor = ArgumentCaptor.forClass(GetObjectPresignRequest.class);
        verify(s3Presigner).presignGetObject(requestCaptor.capture());

        GetObjectPresignRequest request = requestCaptor.getValue();
        assertThat(request.signatureDuration()).isEqualTo(Duration.ofMinutes(10));
        assertThat(request.getObjectRequest().bucket()).isEqualTo("test-bucket");
        assertThat(request.getObjectRequest().key()).isEqualTo("products/sample.png");
        assertThat(presignedUrl).contains("X-Amz-Signature=test");
    }

    @Test
    void createPresignedUrlReturnsNullWhenImageKeyIsBlank() {
        // when
        String presignedUrl = service.createPresignedUrl(" ");

        // then
        assertThat(presignedUrl).isNull();
    }

    @Test
    void validateUploadedImageExistsChecksS3HeadObject() {
        // given
        String imageKey = "products/11111111-1111-1111-1111-111111111111.png";
        when(s3Client.headObject(Mockito.any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder().build());

        // when
        service.validateUploadedImageExists(imageKey);

        // then
        ArgumentCaptor<HeadObjectRequest> requestCaptor = ArgumentCaptor.forClass(HeadObjectRequest.class);
        verify(s3Client).headObject(requestCaptor.capture());
        assertThat(requestCaptor.getValue().bucket()).isEqualTo("test-bucket");
        assertThat(requestCaptor.getValue().key()).isEqualTo(imageKey);
    }

    @Test
    void validateUploadedImageExistsRejectsMissingS3Object() {
        // given
        String imageKey = "products/11111111-1111-1111-1111-111111111111.png";
        when(s3Client.headObject(Mockito.any(HeadObjectRequest.class)))
                .thenThrow(S3Exception.builder().statusCode(404).build());

        // when & then
        assertThatThrownBy(() -> service.validateUploadedImageExists(imageKey))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_IMAGE_KEY);
    }
}
