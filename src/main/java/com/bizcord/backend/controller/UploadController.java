package com.bizcord.backend.controller;

import com.bizcord.backend.dto.FileUploadResponse;
import com.bizcord.backend.service.UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
public class UploadController {
    private final UploadService uploadService;

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileUploadResponse> uploadImage(@RequestParam("file") MultipartFile file) {
        return ResponseEntity
                .status(CREATED)
                .body(uploadService.uploadImage(file));
    }

    @PostMapping(value = "/message", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileUploadResponse> uploadMessageFile(@RequestParam("file") MultipartFile file) {
        return ResponseEntity
                .status(CREATED)
                .body(uploadService.uploadMessageFile(file));
    }

    @GetMapping("/images/{file_name:.+}")
    public ResponseEntity<org.springframework.core.io.Resource> getImage(
            @PathVariable("file_name") String fileName
    ) {
        UploadService.PublicFileResource image = uploadService.loadImage(fileName);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .body(image.resource());
    }
}

