package com.example.fivespringusedmarket.image.controller;

import com.example.fivespringusedmarket.common.response.ApiResponse;
import com.example.fivespringusedmarket.image.dto.ImageUploadResponse;
import com.example.fivespringusedmarket.image.service.S3Uploader;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/images")
public class ImageUploadController {

    private final S3Uploader s3Uploader;

    /**
     * 상품 이미지를 S3에 먼저 업로드하고, 상품 등록 API에서 사용할 imageKey를 반환한다.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ImageUploadResponse>> uploadImage(
            @RequestPart("file") MultipartFile file
    ) {
        String imageKey = s3Uploader.uploadImage(file);
        // 공통 응답 형식은 유지하되, data에는 Public URL이 아닌 imageKey만 담는다.
        return ResponseEntity.ok(ApiResponse.success("이미지 업로드에 성공했습니다.", new ImageUploadResponse(imageKey)));
    }
}
