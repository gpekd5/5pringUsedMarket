package com.example.fivespringusedmarket.image.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

class S3PresignedUrlServiceTest {

    @Test
    void createPresignedUrlReturnsTenMinutePrivateObjectUrl() throws Exception {
        // given
        S3Presigner s3Presigner = Mockito.mock(S3Presigner.class);
        S3PresignedUrlService service = new S3PresignedUrlService(s3Presigner, "test-bucket");
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
        // given
        S3Presigner s3Presigner = Mockito.mock(S3Presigner.class);
        S3PresignedUrlService service = new S3PresignedUrlService(s3Presigner, "test-bucket");

        // when
        String presignedUrl = service.createPresignedUrl(" ");

        // then
        assertThat(presignedUrl).isNull();
    }
}
