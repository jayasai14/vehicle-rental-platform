package com.vehiclerental.modules.operations.controller;

import com.vehiclerental.common.response.ApiResponse;
import com.vehiclerental.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", defaultValue = "general") String category
    ) {
        String key = fileStorageService.storeFile(file, category);
        return ResponseEntity.ok(ApiResponse.success(Map.of("storageObjectKey", key), "File uploaded successfully"));
    }

    @GetMapping("/download/{objectKey}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String objectKey) {
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
