package com.vehiclerental.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String storeFile(MultipartFile file, String categoryDirectory);
    Resource loadFileAsResource(String storageKey);
    void deleteFile(String storageKey);
}
