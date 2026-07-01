package com.example.fivespringusedmarket.image.service;

import java.util.UUID;
import java.util.regex.Pattern;

public final class ImageKeyPolicy {

    public static final String PRODUCT_IMAGE_PREFIX = "products";

    private static final Pattern PRODUCT_IMAGE_KEY_PATTERN =
            Pattern.compile("^products/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\.(jpg|jpeg|png)$");

    private ImageKeyPolicy() {
    }

    public static String createProductImageKey(String extension) {
        return PRODUCT_IMAGE_PREFIX + "/" + UUID.randomUUID() + "." + extension;
    }

    public static boolean isValidProductImageKey(String imageKey) {
        return imageKey != null && PRODUCT_IMAGE_KEY_PATTERN.matcher(imageKey).matches();
    }
}
