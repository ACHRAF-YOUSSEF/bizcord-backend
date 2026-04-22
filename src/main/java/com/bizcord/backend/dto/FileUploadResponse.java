package com.bizcord.backend.dto;

public record FileUploadResponse(String originalFilename, String contentType, long size, String url) {
}
