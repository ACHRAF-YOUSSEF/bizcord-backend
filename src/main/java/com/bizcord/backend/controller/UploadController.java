package com.bizcord.backend.controller;

import com.bizcord.backend.config.ratelimit.RateLimit;
import com.bizcord.backend.config.ratelimit.RateLimitKeyType;
import com.bizcord.backend.dto.FileUploadResponse;
import com.bizcord.backend.service.UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
public class UploadController {
    private final UploadService uploadService;

    @RateLimit(limit = 10, keyType = RateLimitKeyType.UID)
    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileUploadResponse> uploadImage(@RequestParam("file") MultipartFile file) {
        return ResponseEntity
                .status(CREATED)
                .body(sanitizeUploadResponse(uploadService.uploadImage(file)));
    }

    @RateLimit(limit = 15, keyType = RateLimitKeyType.UID)
    @PostMapping(value = "/message", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileUploadResponse> uploadMessageFile(@RequestParam("file") MultipartFile file) {
        return ResponseEntity
                .status(CREATED)
                .body(sanitizeUploadResponse(uploadService.uploadMessageFile(file)));
    }

    @RateLimit(limit = 120, keyType = RateLimitKeyType.IP)
    @GetMapping("/images/{file_name:[0-9a-f]{64}(?:\\.[A-Za-z0-9]{1,10})?}")
    public ResponseEntity<Resource> getImage(
            @PathVariable("file_name") String fileName
    ) {
        UploadService.PublicFileResource image = uploadService.loadImage(fileName);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .contentLength(image.contentLength())
                .body(image.resource());
    }

    @RateLimit(limit = 120, keyType = RateLimitKeyType.IP)
    @GetMapping("/messages/{file_name:[0-9a-f]{64}(?:\\.[A-Za-z0-9]{1,10})?}")
    public ResponseEntity<Resource> getMessageFile(
            @PathVariable("file_name") String fileName
    ) {
        UploadService.PublicFileResource file = uploadService.loadMessageFile(fileName);

        return ResponseEntity.ok()
                .header("X-Content-Type-Options", "nosniff")
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(file.fileName(), StandardCharsets.UTF_8)
                                .build()
                                .toString()
                )
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(file.contentLength())
                .body(file.resource());
    }

    private FileUploadResponse sanitizeUploadResponse(FileUploadResponse response) {
        return new FileUploadResponse(
                HtmlUtils.htmlEscape(response.originalFilename()),
                response.contentType(),
                response.size(),
                response.url()
        );
    }
}
