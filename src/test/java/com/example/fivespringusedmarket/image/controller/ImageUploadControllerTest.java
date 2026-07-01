package com.example.fivespringusedmarket.image.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.example.fivespringusedmarket.common.response.ApiResponse;
import com.example.fivespringusedmarket.image.dto.PresignedUploadUrlRequest;
import com.example.fivespringusedmarket.image.dto.PresignedUploadUrlResponse;
import com.example.fivespringusedmarket.image.service.S3PresignedUrlService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

class ImageUploadControllerTest {

    @Test
    void createUploadPresignedUrlReturnsImageKeyAndUploadUrl() {
        // given
        S3PresignedUrlService s3PresignedUrlService = Mockito.mock(S3PresignedUrlService.class);
        ImageUploadController controller = new ImageUploadController(s3PresignedUrlService);
        PresignedUploadUrlRequest request = new PresignedUploadUrlRequest("sample.png", "image/png", 1234L);
        PresignedUploadUrlResponse serviceResponse = new PresignedUploadUrlResponse(
                "products/11111111-1111-1111-1111-111111111111.png",
                "https://test-bucket.s3.ap-northeast-2.amazonaws.com/products/11111111-1111-1111-1111-111111111111.png?X-Amz-Signature=test"
        );

        when(s3PresignedUrlService.createUploadPresignedUrl(request)).thenReturn(serviceResponse);

        // when
        ResponseEntity<ApiResponse<PresignedUploadUrlResponse>> response = controller.createUploadPresignedUrl(request);

        // then
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().data().imageKey())
                .isEqualTo("products/11111111-1111-1111-1111-111111111111.png");
        assertThat(response.getBody().data().uploadUrl()).contains("X-Amz-Signature=test");
    }
}
