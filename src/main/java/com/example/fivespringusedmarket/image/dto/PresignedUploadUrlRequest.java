package com.example.fivespringusedmarket.image.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PresignedUploadUrlRequest(
        @NotBlank
        String fileName,

        @NotBlank
        String contentType,

        @NotNull
        Long fileSize
) {
}
