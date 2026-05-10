package ru.nu1ts.recipebook.exception;

public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String resourceType, String id) {
        super(ErrorCode.NOT_FOUND,
                String.format("%s with id '%s' not found", resourceType, id));
    }

    public ResourceNotFoundException(String message) {
        super(ErrorCode.NOT_FOUND, message);
    }
}