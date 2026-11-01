package com.vehiclerental.storage;

import com.vehiclerental.common.CustomExceptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class LocalStorageServiceImpl implements FileStorageService {

    private final Path rootLocation;

    public LocalStorageServiceImpl(@Value("${app.storage.local-path:./uploads}") String rootDir) {
        this.rootLocation = Paths.get(rootDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage directory", e);
        }
    }

    @Override
    public String storeFile(MultipartFile file, String categoryDirectory) {
        if (file == null || file.isEmpty()) {
            throw new CustomExceptions.BadRequestException("Cannot upload empty file");
        }

        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "file");
        if (originalFileName.contains("..")) {
            throw new CustomExceptions.BadRequestException("Filename contains invalid path sequence " + originalFileName);
        }

        String extension = "";
        int extIndex = originalFileName.lastIndexOf(".");
        if (extIndex > 0) {
            extension = originalFileName.substring(extIndex);
        }

        String cleanCategory = categoryDirectory.replaceAll("[^a-zA-Z0-9_-]", "");
        String storedFileName = UUID.randomUUID() + extension;
        String relativeKey = cleanCategory + "/" + storedFileName;

        try {
            Path targetDir = this.rootLocation.resolve(cleanCategory).normalize();
            Files.createDirectories(targetDir);

            Path targetPath = targetDir.resolve(storedFileName).normalize();
            if (!targetPath.startsWith(this.rootLocation)) {
                throw new CustomExceptions.BadRequestException("Cannot store file outside upload directory");
            }

            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            return relativeKey;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file " + originalFileName, e);
        }
    }

    @Override
    public Resource loadFileAsResource(String storageKey) {
        try {
            Path filePath = this.rootLocation.resolve(storageKey).normalize();
            if (!filePath.startsWith(this.rootLocation)) {
                throw new CustomExceptions.ForbiddenException("Access to path outside upload directory is forbidden");
            }
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new CustomExceptions.ResourceNotFoundException("File not found: " + storageKey);
            }
        } catch (MalformedURLException e) {
            throw new CustomExceptions.ResourceNotFoundException("File not found: " + storageKey);
        }
    }

    @Override
    public void deleteFile(String storageKey) {
        try {
            Path filePath = this.rootLocation.resolve(storageKey).normalize();
            if (filePath.startsWith(this.rootLocation)) {
                Files.deleteIfExists(filePath);
            }
        } catch (IOException ignored) {
        }
    }
}
