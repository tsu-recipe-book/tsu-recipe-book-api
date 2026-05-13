package ru.nu1ts.recipebook.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

@Data
@Component
@Validated
@ConfigurationProperties(prefix = "app.upload")
public class FileUploadProperties {

    @NotBlank
    private String uploadDir = "uploads";

    @Positive
    private long maxFileSize = 10 * 1024 * 1024L;

    @Positive
    @Max(10)
    private int maxFilesPerItem = 5;

    private List<String> allowedContentTypes = List.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private Set<String> allowedExtensions = Set.of(
            "jpg", "jpeg", "png", "webp"
    );

    public Path getUploadPath() {
        return Path.of(uploadDir);
    }
}