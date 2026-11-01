package com.vehiclerental.modules.operations.controller;

import com.vehiclerental.common.CustomExceptions;
import com.vehiclerental.common.response.ApiResponse;
import com.vehiclerental.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private static final Set<String> ALLOWED_CATEGORIES = Set.of(
            "vehicle_images",
            "driving_licences",
            "partner_documents",
            "contributor_documents",
            "receipts",
            "inspection_media",
            "damage_evidence",
            "general"
    );

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".webp", ".pdf"
    );

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

    private final FileStorageService fileStorageService;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", defaultValue = "general") String category
    ) {
        if (file == null || file.isEmpty()) {
            throw new CustomExceptions.BadRequestException("Cannot upload empty file");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new CustomExceptions.BadRequestException("File size exceeds 10MB limit");
        }

        if (!ALLOWED_CATEGORIES.contains(category)) {
            throw new CustomExceptions.BadRequestException("Invalid category: " + category + ". Allowed: " + ALLOWED_CATEGORIES);
        }

        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        int extIdx = originalName.lastIndexOf(".");
        if (extIdx < 0 || !ALLOWED_EXTENSIONS.contains(originalName.substring(extIdx))) {
            throw new CustomExceptions.BadRequestException("Unsupported file format. Allowed: " + ALLOWED_EXTENSIONS);
        }

        String key = fileStorageService.storeFile(file, category);
        return ResponseEntity.ok(ApiResponse.success(Map.of("storageObjectKey", key), "File uploaded successfully"));
    }

    @GetMapping({"/download/{category}/{filename}", "/download/{filename}"})
    public ResponseEntity<Resource> downloadFile(
            @PathVariable(required = false) String category,
            @PathVariable String filename
    ) {
        String objectKey = (category != null && !category.isBlank()) ? category + "/" + filename : filename;

        if (category != null && !category.equals("vehicle_images")) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
                throw new CustomExceptions.ForbiddenException("Authentication required to download private document");
            }
        }

        Resource resource = fileStorageService.loadFileAsResource(objectKey);
        String contentType = "application/octet-stream";
        if (objectKey.endsWith(".jpg") || objectKey.endsWith(".jpeg")) contentType = "image/jpeg";
        else if (objectKey.endsWith(".png")) contentType = "image/png";
        else if (objectKey.endsWith(".webp")) contentType = "image/webp";
        else if (objectKey.endsWith(".pdf")) contentType = "application/pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
