package com.example.fivespringusedmarket.image.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.example.fivespringusedmarket.common.response.ApiResponse;
import com.example.fivespringusedmarket.image.dto.ImageUploadResponse;
import com.example.fivespringusedmarket.image.service.S3Uploader;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

class ImageUploadControllerTest {

    @Test
    void uploadImageReturnsImageKey() {
        // given
        S3Uploader s3Uploader = Mockito.mock(S3Uploader.class);
        ImageUploadController controller = new ImageUploadController(s3Uploader);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.png",
                "image/png",
                "image".getBytes()
        );

        when(s3Uploader.uploadImage(file)).thenReturn("products/sample.png");

        // when
        ResponseEntity<ApiResponse<ImageUploadResponse>> response = controller.uploadImage(file);

        // then
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().data().imageKey()).isEqualTo("products/sample.png");
    }
}
