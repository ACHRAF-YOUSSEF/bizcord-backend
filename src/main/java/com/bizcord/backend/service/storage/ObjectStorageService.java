package com.bizcord.backend.service.storage;

import org.springframework.core.io.Resource;

import java.io.InputStream;

public interface ObjectStorageService {

    record StoredObjectResource(Resource resource, String contentType, long contentLength) {
    }

    boolean objectExists(String objectKey);

    void putObject(String objectKey, String contentType, long size, InputStream inputStream);

    StoredObjectResource getObject(String objectKey);
}
