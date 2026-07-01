package com.example.fivespringusedmarket.image.controller;

import com.example.fivespringusedmarket.common.response.ApiResponse;
import com.example.fivespringusedmarket.image.dto.PresignedUploadUrlRequest;
import com.example.fivespringusedmarket.image.dto.PresignedUploadUrlResponse;
import com.example.fivespringusedmarket.image.service.S3PresignedUrlService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/images")
public class ImageUploadController {

    private final S3PresignedUrlService s3PresignedUrlService;

    /**
     * 클라이언트가 Private S3 Bucket에 직접 업로드할 수 있는 Presigned PUT URL을 발급한다.
     */
    @PostMapping("/presigned-url")
    public ResponseEntity<ApiResponse<PresignedUploadUrlResponse>> createUploadPresignedUrl(
            @Valid @RequestBody PresignedUploadUrlRequest request
    ) {
        PresignedUploadUrlResponse response = s3PresignedUrlService.createUploadPresignedUrl(request);
        return ResponseEntity.ok(ApiResponse.success("이미지 업로드 URL이 발급되었습니다.", response));
    }
}
