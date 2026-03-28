package com.bizcord.backend.service;

import com.bizcord.backend.config.UploadProperties;
import com.bizcord.backend.dto.FileUploadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UploadService {
    private static final long MAX_IMAGE_SIZE_BYTES = 4L * 1024L * 1024L;
    private static final Set<String> MESSAGE_ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/zip",
            "application/x-zip",
            "application/x-zip-compressed",
            "multipart/x-zip"
    );

    private final UploadProperties uploadProperties;

    public record PublicFileResource(Resource resource, String contentType) {
    }

    public FileUploadResponse uploadImage(MultipartFile file) {
        validateFileProvided(file);

        String contentType = normalizeContentType(file);
        if (!isImage(contentType)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Only image files are allowed");
        }

        if (file.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(413), "Image size must be 4MB or less");
        }

        return storeHashedFile(file, contentType, "images");
    }

    public FileUploadResponse uploadMessageFile(MultipartFile file) {
        validateFileProvided(file);

        String contentType = normalizeContentType(file);
        if (!isImage(contentType) && !isVideo(contentType) && !isText(contentType)
                && !MESSAGE_ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new ResponseStatusException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Only images, videos, PDF, ZIP, and text files are allowed"
            );
        }

        return storeHashedFile(file, contentType, "messages");
    }

    public PublicFileResource loadImage(String fileName) {
        String sanitizedFilename = sanitizeRequestedFilename(fileName);

        Path uploadRoot = Path.of(uploadProperties.getBaseDirectory()).toAbsolutePath().normalize();
        Path imageDirectory = uploadRoot.resolve("images").normalize();
        Path imagePath = imageDirectory.resolve(sanitizedFilename).normalize();

        if (!imagePath.startsWith(imageDirectory)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file name");
        }

        if (!Files.exists(imagePath) || !Files.isRegularFile(imagePath)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found");
        }

        String contentType = detectContentType(imagePath);
        if (!isImage(contentType.toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Requested file is not an image");
        }

        try {
            Resource resource = new UrlResource(imagePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found");
            }

            return new PublicFileResource(resource, contentType);
        } catch (IOException _) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to load image");
        }
    }

    private FileUploadResponse storeHashedFile(MultipartFile file, String contentType, String subdirectory) {
        String originalFilename = sanitizeOriginalFilename(file.getOriginalFilename());
        String storedFilename = computeSha256Hex(file) + extensionForContentType(contentType, originalFilename);

        Path uploadRoot = Path.of(uploadProperties.getBaseDirectory()).toAbsolutePath().normalize();
        Path uploadDirectory = uploadRoot.resolve(subdirectory).normalize();
        Path destination = uploadDirectory.resolve(storedFilename).normalize();

        if (!destination.startsWith(uploadDirectory)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file name");
        }

        try {
            Files.createDirectories(uploadDirectory);
            if (!Files.exists(destination)) {
                try (InputStream inputStream = file.getInputStream()) {
                    Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (IOException _) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to store uploaded file");
        }

        String basePath = normalizePublicBasePath(uploadProperties.getPublicBasePath());

        return FileUploadResponse.builder()
                .originalFilename(originalFilename)
                .contentType(contentType)
                .size(file.getSize())
                .url(basePath + "/" + subdirectory + "/" + storedFilename)
                .build();
    }

    private void validateFileProvided(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
        }
    }

    private String normalizeContentType(MultipartFile file) {
        return file.getContentType() == null ? "" : file.getContentType().toLowerCase();
    }

    private String computeSha256Hex(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream inputStream = file.getInputStream()) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }

            return toHex(digest.digest());
        } catch (NoSuchAlgorithmException | IOException _) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to process uploaded file");
        }
    }

    private String extensionForContentType(String contentType, String originalFilename) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            case "image/bmp" -> ".bmp";
            case "image/svg+xml" -> ".svg";
            case "image/avif" -> ".avif";
            case "image/tiff" -> ".tiff";
            case "application/pdf" -> ".pdf";
            case "application/zip", "application/x-zip", "application/x-zip-compressed", "multipart/x-zip" -> ".zip";
            case "video/mp4" -> ".mp4";
            case "video/webm" -> ".webm";
            case "video/quicktime" -> ".mov";
            case "video/x-matroska" -> ".mkv";
            case "video/x-msvideo" -> ".avi";
            case "video/mpeg" -> ".mpeg";
            default -> extractExtension(originalFilename);
        };
    }

    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }

        return builder.toString();
    }

    private boolean isImage(String contentType) {
        return contentType.startsWith("image/");
    }

    private boolean isText(String contentType) {
        return contentType.startsWith("text/");
    }

    private boolean isVideo(String contentType) {
        return contentType.startsWith("video/");
    }

    private String sanitizeOriginalFilename(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "file";
        }

        String cleaned = StringUtils.cleanPath(filename);
        return StringUtils.hasText(cleaned) ? cleaned : "file";
    }

    private String sanitizeRequestedFilename(String filename) {
        if (!StringUtils.hasText(filename)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File name is required");
        }

        String cleaned = StringUtils.cleanPath(filename);
        if (!StringUtils.hasText(cleaned)
                || cleaned.contains("..")
                || cleaned.contains("/")
                || cleaned.contains("\\")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file name");
        }

        return cleaned;
    }

    private String detectContentType(Path filePath) {
        try {
            String contentType = Files.probeContentType(filePath);
            return contentType == null ? "application/octet-stream" : contentType;
        } catch (IOException _) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to determine file type");
        }
    }

    private String extractExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == filename.length() - 1) {
            return "";
        }

        return filename.substring(dotIndex);
    }

    private String normalizePublicBasePath(String value) {
        if (!StringUtils.hasText(value)) {
            return "/uploads";
        }

        String normalized = value.startsWith("/") ? value : "/" + value;
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }
}
