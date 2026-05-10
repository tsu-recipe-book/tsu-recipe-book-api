package ru.nu1ts.recipebook.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    VALIDATION_ERROR("VALIDATION_ERROR"),
    NOT_FOUND("NOT_FOUND"),
    BJU_SUM_EXCEEDED("BJU_SUM_EXCEEDED"),
    PRODUCT_IN_USE("PRODUCT_IN_USE"),
    FLAG_NOT_AVAILABLE("FLAG_NOT_AVAILABLE"),
    INTERNAL_ERROR("INTERNAL_ERROR");

    private final String code;
}