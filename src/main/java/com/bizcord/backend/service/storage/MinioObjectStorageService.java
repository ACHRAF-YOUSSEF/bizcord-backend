package com.bizcord.backend.service.storage;

import com.bizcord.backend.config.StorageProperties;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class MinioObjectStorageService implements ObjectStorageService {
    private final MinioClient minioClient;
    private final StorageProperties storageProperties;

    @Override
    public boolean objectExists(String objectKey) {
        ensureBucketExists();
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucket())
                    .object(objectKey)
                    .build());
            return true;
        } catch (ErrorResponseException e) {
            String code = e.errorResponse().code();
            if ("NoSuchKey".equalsIgnoreCase(code) || "NoSuchObject".equalsIgnoreCase(code)) {
                return false;
            }
            throw storageException("Unable to check stored object", e);
        } catch (Exception e) {
            throw storageException("Unable to check stored object", e);
        }
    }

    @Override
    public void putObject(String objectKey, String contentType, long size, InputStream inputStream) {
        ensureBucketExists();
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket())
                    .object(objectKey)
                    .contentType(contentType)
                    .stream(inputStream, size, -1L)
                    .build());
        } catch (Exception e) {
            throw storageException("Unable to store uploaded file", e);
        }
    }

    @Override
    public StoredObjectResource getObject(String objectKey) {
        ensureBucketExists();
        try {
            var stat = minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucket())
                    .object(objectKey)
                    .build());
            GetObjectResponse response = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket())
                    .object(objectKey)
                    .build());
            String contentType = stat.contentType();
            if (contentType == null || contentType.isBlank()) {
                contentType = "application/octet-stream";
            }
            return new StoredObjectResource(
                    new InputStreamResource(response),
                    contentType,
                    stat.size()
            );
        } catch (ErrorResponseException e) {
            String code = e.errorResponse().code();
            if ("NoSuchKey".equalsIgnoreCase(code) || "NoSuchObject".equalsIgnoreCase(code)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
            }
            throw storageException("Unable to load file", e);
        } catch (Exception e) {
            throw storageException("Unable to load file", e);
        }
    }

    private synchronized void ensureBucketExists() {
        try {
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket()).build())) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket()).build());
            }
        } catch (Exception e) {
            throw storageException("Unable to initialize object storage bucket", e);
        }
    }

    private String bucket() {
        return storageProperties.getMinio().getBucket();
    }

    private ResponseStatusException storageException(String message, Exception cause) {
        return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message, cause);
    }
}
