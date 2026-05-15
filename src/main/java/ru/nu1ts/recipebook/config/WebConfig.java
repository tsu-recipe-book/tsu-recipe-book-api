package ru.nu1ts.recipebook.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final FileUploadProperties uploadProperties;

    public WebConfig(FileUploadProperties uploadProperties) {
        this.uploadProperties = uploadProperties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadPath = uploadProperties.getUploadPath().toAbsolutePath().toString().replace("\\", "/");

        if (!uploadPath.startsWith("/")) {
            uploadPath = "/" + uploadPath;
        }

        if (!uploadPath.endsWith("/")) {
            uploadPath += "/";
        }

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .exposedHeaders("Content-Disposition")
                .allowCredentials(true);
    }
}