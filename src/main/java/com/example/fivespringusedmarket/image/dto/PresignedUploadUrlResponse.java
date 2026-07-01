package com.example.fivespringusedmarket.image.dto;

public record PresignedUploadUrlResponse(
        String imageKey,
        String uploadUrl
) {
}
