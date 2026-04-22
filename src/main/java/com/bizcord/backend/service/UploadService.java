package com.bizcord.backend.service;

import com.bizcord.backend.config.StorageProperties;
import com.bizcord.backend.dto.FileUploadResponse;
import com.bizcord.backend.service.storage.ObjectStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UploadService {
    private static final long MAX_IMAGE_SIZE_BYTES = 4L * 1024L * 1024L;
    private static final String IMAGE_CONTENT_TYPE_PREFIX = "image/";
    private static final Set<String> MESSAGE_ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/vnd.oasis.opendocument.text",
            "application/vnd.oasis.opendocument.spreadsheet",
            "application/vnd.oasis.opendocument.presentation",
            "application/rtf",
            "application/zip",
            "application/x-zip",
            "application/x-zip-compressed",
            "multipart/x-zip",
            "application/x-rar-compressed",
            "application/vnd.rar",
            "application/x-7z-compressed",
            "application/gzip",
            "application/x-tar",
            "audio/mpeg",
            "audio/ogg",
            "audio/wav",
            "audio/webm",
            "audio/flac",
            "audio/aac",
            "application/json",
            "application/xml",
            "text/csv",
            "text/xml"
    );

    private final StorageProperties storageProperties;
    private final ObjectStorageService objectStorageService;

    public record PublicFileResource(Resource resource, String contentType, long contentLength) {
    }

    public FileUploadResponse uploadImage(MultipartFile file) {
        validateFileProvided(file);
        String contentType = normalizeContentType(file);
        if (!contentType.startsWith(IMAGE_CONTENT_TYPE_PREFIX)) {
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
        if (!contentType.startsWith(IMAGE_CONTENT_TYPE_PREFIX) && !contentType.startsWith("video/")
                && !contentType.startsWith("audio/") && !contentType.startsWith("text/")
                && !MESSAGE_ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new ResponseStatusException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Only images, videos, PDF, ZIP, and text files are allowed"
            );
        }
        return storeHashedFile(file, contentType, "messages");
    }

    public PublicFileResource loadImage(String fileName) {
        return loadPublicFile(fileName, "images", true);
    }

    public PublicFileResource loadMessageFile(String fileName) {
        return loadPublicFile(fileName, "messages", false);
    }

    private PublicFileResource loadPublicFile(String fileName, String subdirectory, boolean imageOnly) {
        String sanitizedFilename = sanitizeRequestedFilename(fileName);
        ObjectStorageService.StoredObjectResource storedObject = objectStorageService.getObject(subdirectory + "/" + sanitizedFilename);
        String contentType = storedObject.contentType();
        if (imageOnly && !contentType.toLowerCase().startsWith(IMAGE_CONTENT_TYPE_PREFIX)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Requested file is not an image");
        }
        return new PublicFileResource(storedObject.resource(), contentType, storedObject.contentLength());
    }

    private FileUploadResponse storeHashedFile(MultipartFile file, String contentType, String subdirectory) {
        String originalFilename = sanitizeOriginalFilename(file.getOriginalFilename());
        String storedFilename = computeSha256Hex(file) + extensionForContentType(contentType);

        if (!storedFilename.matches("[0-9a-f]{64}(\\.[a-zA-Z0-9]{1,10})?")) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to store uploaded file");
        }

        String objectKey = subdirectory + "/" + storedFilename;
        if (!objectStorageService.objectExists(objectKey)) {
            try (InputStream inputStream = file.getInputStream()) {
                objectStorageService.putObject(objectKey, contentType, file.getSize(), inputStream);
            } catch (IOException _) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to store uploaded file");
            }
        }

        String basePath = normalizePublicBasePath(storageProperties.getPublicBasePath());
        return new FileUploadResponse(
                originalFilename,
                contentType,
                file.getSize(),
                basePath + "/" + subdirectory + "/" + storedFilename
        );
    }

    private void validateFileProvided(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
        }
    }

    private String normalizeContentType(MultipartFile file) {
        String ct = file.getContentType();
        if (!StringUtils.hasText(ct)) {
            return "";
        }

        try {
            MediaType parsed = MediaType.parseMediaType(ct);
            return (parsed.getType() + "/" + parsed.getSubtype()).toLowerCase(Locale.ROOT);
        } catch (InvalidMediaTypeException _) {
            return "";
        }
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

    private static final Map<String, String> CONTENT_TYPE_TO_EXTENSION = Map.ofEntries(
            Map.entry("image/jpeg", ".jpg"),
            Map.entry("image/png", ".png"),
            Map.entry("image/gif", ".gif"),
            Map.entry("image/webp", ".webp"),
            Map.entry("image/bmp", ".bmp"),
            Map.entry("image/svg+xml", ".svg"),
            Map.entry("image/avif", ".avif"),
            Map.entry("image/tiff", ".tiff"),
            Map.entry("application/pdf", ".pdf"),
            Map.entry("application/msword", ".doc"),
            Map.entry("application/vnd.openxmlformats-officedocument.wordprocessingml.document", ".docx"),
            Map.entry("application/vnd.ms-excel", ".xls"),
            Map.entry("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ".xlsx"),
            Map.entry("application/vnd.ms-powerpoint", ".ppt"),
            Map.entry("application/vnd.openxmlformats-officedocument.presentationml.presentation", ".pptx"),
            Map.entry("application/vnd.oasis.opendocument.text", ".odt"),
            Map.entry("application/vnd.oasis.opendocument.spreadsheet", ".ods"),
            Map.entry("application/vnd.oasis.opendocument.presentation", ".odp"),
            Map.entry("application/rtf", ".rtf"),
            Map.entry("application/zip", ".zip"),
            Map.entry("application/x-zip", ".zip"),
            Map.entry("application/x-zip-compressed", ".zip"),
            Map.entry("multipart/x-zip", ".zip"),
            Map.entry("application/x-rar-compressed", ".rar"),
            Map.entry("application/vnd.rar", ".rar"),
            Map.entry("application/x-7z-compressed", ".7z"),
            Map.entry("application/gzip", ".gz"),
            Map.entry("application/x-tar", ".tar"),
            Map.entry("video/mp4", ".mp4"),
            Map.entry("video/webm", ".webm"),
            Map.entry("video/quicktime", ".mov"),
            Map.entry("video/x-matroska", ".mkv"),
            Map.entry("video/x-msvideo", ".avi"),
            Map.entry("video/mpeg", ".mpeg"),
            Map.entry("audio/mpeg", ".mp3"),
            Map.entry("audio/ogg", ".ogg"),
            Map.entry("audio/wav", ".wav"),
            Map.entry("audio/webm", ".weba"),
            Map.entry("audio/flac", ".flac"),
            Map.entry("audio/aac", ".aac"),
            Map.entry("application/json", ".json"),
            Map.entry("application/xml", ".xml"),
            Map.entry("text/xml", ".xml"),
            Map.entry("text/csv", ".csv"),
            Map.entry("text/plain", ".txt"),
            Map.entry("text/html", ".html"),
            Map.entry("text/css", ".css"),
            Map.entry("text/javascript", ".js"),
            Map.entry("application/javascript", ".js")
    );

    private String extensionForContentType(String contentType) {
        return CONTENT_TYPE_TO_EXTENSION.getOrDefault(contentType, "");
    }

    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }

    private String sanitizeOriginalFilename(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "file";
        }

        String basename = StringUtils.cleanPath(filename);
        int slash = Math.max(basename.lastIndexOf('/'), basename.lastIndexOf('\\'));
        if (slash >= 0 && slash + 1 < basename.length()) {
            basename = basename.substring(slash + 1);
        }

        String safe = basename
                .replaceAll("[\\r\\n\\t\\x00-\\x1F\\x7F]", "")
                .replaceAll("[^A-Za-z0-9._ -]", "_")
                .trim();

        if (!StringUtils.hasText(safe)) {
            return "file";
        }
        if (safe.length() > 120) {
            return safe.substring(0, 120);
        }
        return safe;
    }

    private String sanitizeRequestedFilename(String filename) {
        if (!StringUtils.hasText(filename)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File name is required");
        }
        String cleaned = StringUtils.cleanPath(filename);
        if (!StringUtils.hasText(cleaned) || cleaned.contains("..") || cleaned.contains("/") || cleaned.contains("\\")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file name");
        }

        return cleaned;
    }

    private String normalizePublicBasePath(String value) {
        if (!StringUtils.hasText(value)) {
            return "/api/uploads";
        }

        String normalized = value.startsWith("/") ? value : "/" + value;
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }
}
