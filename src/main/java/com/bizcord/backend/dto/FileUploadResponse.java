package com.bizcord.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FileUploadResponse {
    private String originalFilename;
    private String contentType;
    private long size;
    private String url;
}

