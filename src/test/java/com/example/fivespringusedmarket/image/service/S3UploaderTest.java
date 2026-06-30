package com.example.fivespringusedmarket.image.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.fivespringusedmarket.common.config.S3Properties;
import com.example.fivespringusedmarket.common.exception.CustomException;
import com.example.fivespringusedmarket.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

class S3UploaderTest {

    private S3Client s3Client;
    private S3Uploader s3Uploader;

    @BeforeEach
    void setUp() {
        s3Client = Mockito.mock(S3Client.class);
        S3Properties s3Properties = new S3Properties();
        s3Properties.setDirectory("products");
        s3Properties.setMaxFileSize(10);
        s3Uploader = new S3Uploader(s3Client, s3Properties, "test-bucket");

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
    }

    @Test
    void uploadImageReturnsImageKeyAfterUploadingToS3() {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.png",
                "image/png",
                "image".getBytes()
        );

        // when
        String imageKey = s3Uploader.uploadImage(file);

        // then
        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));

        PutObjectRequest request = requestCaptor.getValue();
        assertThat(request.bucket()).isEqualTo("test-bucket");
        assertThat(request.key()).startsWith("products/");
        assertThat(request.key()).endsWith(".png");
        assertThat(request.contentType()).isEqualTo("image/png");
        assertThat(request.contentLength()).isEqualTo(file.getSize());
        assertThat(imageKey).isEqualTo(request.key());
    }

    @Test
    void uploadImageRejectsEmptyFile() {
        // given
        MockMultipartFile file = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        // when & then
        assertThatThrownBy(() -> s3Uploader.uploadImage(file))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.EMPTY_IMAGE_FILE);
    }

    @Test
    void uploadImageRejectsNonImageMimeType() {
        // given
        MockMultipartFile file = new MockMultipartFile("file", "sample.png", "text/plain", "image".getBytes());

        // when & then
        assertThatThrownBy(() -> s3Uploader.uploadImage(file))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_IMAGE_FILE);
    }

    @Test
    void uploadImageRejectsUnsupportedExtension() {
        // given
        MockMultipartFile file = new MockMultipartFile("file", "sample.gif", "image/png", "image".getBytes());

        // when & then
        assertThatThrownBy(() -> s3Uploader.uploadImage(file))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_IMAGE_FILE);
    }

    @Test
    void uploadImageRejectsExceededFileSize() {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.webp",
                "image/webp",
                "too-large-file".getBytes()
        );

        // when & then
        assertThatThrownBy(() -> s3Uploader.uploadImage(file))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.IMAGE_FILE_SIZE_EXCEEDED);
    }
}
