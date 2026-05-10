package ru.nu1ts.recipebook.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.nu1ts.recipebook.config.FileUploadProperties;
import ru.nu1ts.recipebook.dto.UploadedFile;
import ru.nu1ts.recipebook.exception.BusinessException;
import ru.nu1ts.recipebook.exception.ErrorCode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final FileUploadProperties props;

    @PostConstruct
    void init() {
        try {
            Files.createDirectories(props.getUploadPath());
            log.info("Upload directory initialized: {}", props.getUploadPath().toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Cannot create upload directory: " + props.getUploadPath(), e);
        }
    }

    public List<UploadedFile> saveFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }

        if (files.size() > props.getMaxFilesPerItem()) {
            throw new BusinessException(ErrorCode.TOO_MANY_FILES,
                    "Too many files. Maximum allowed: " + props.getMaxFilesPerItem());
        }

        return files.stream()
                .map(this::saveFile)
                .collect(Collectors.toList());
    }

    public UploadedFile saveFile(MultipartFile file) {
        validateFile(file);

        String originalName = file.getOriginalFilename();
        String extension = getExtension(originalName);
        String storedName = UUID.randomUUID() + "." + extension;
        Path targetPath = props.getUploadPath().resolve(storedName);

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_STORAGE_ERROR,
                    "Failed to store file: " + originalName, e);
        }

        String url = "/uploads/" + storedName;

        log.info("File saved: {} -> {} ({} bytes)", originalName, url, file.getSize());

        return UploadedFile.builder()
                .url(url)
                .originalName(originalName)
                .size(file.getSize())
                .build();
    }

    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }

        String fileName = extractFileName(fileUrl);
        if (fileName == null) {
            return;
        }

        Path filePath = props.getUploadPath().resolve(fileName);
        try {
            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                log.info("File deleted: {}", fileUrl);
            } else {
                log.warn("File not found for deletion: {}", fileUrl);
            }
        } catch (IOException e) {
            log.error("Failed to delete file: {}", fileUrl, e);
        }
    }

    public void deleteFiles(List<String> fileUrls) {
        if (fileUrls == null) {
            return;
        }
        fileUrls.forEach(this::deleteFile);
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Uploaded file is empty");
        }

        if (file.getSize() > props.getMaxFileSize()) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE,
                    "File is too large. Maximum allowed: " + props.getMaxFileSize() / (1024 * 1024) + "MB");
        }

        String contentType = file.getContentType();
        if (contentType != null && !props.getAllowedContentTypes().contains(contentType)) {
            throw new BusinessException(ErrorCode.INVALID_FILE_TYPE,
                    "File type '" + contentType + "' is not allowed");
        }

        String extension = getExtension(file.getOriginalFilename());
        if (extension == null || !props.getAllowedExtensions().contains(extension.toLowerCase())) {
            throw new BusinessException(ErrorCode.INVALID_FILE_TYPE,
                    "File extension is not allowed. Allowed: " + String.join(", ", props.getAllowedExtensions()));
        }
    }

    private String getExtension(String filename) {
        if (filename == null || filename.isBlank() || !filename.contains(".")) {
            return null;
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    private String extractFileName(String fileUrl) {
        if (fileUrl == null || !fileUrl.startsWith("/uploads/")) {
            return null;
        }
        return fileUrl.substring("/uploads/".length());
    }
}